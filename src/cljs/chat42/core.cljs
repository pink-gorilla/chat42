(ns chat42.core
  (:require [konserve.memory :refer [new-mem-store]]
            [replikativ.peer :refer [client-peer]]
            [replikativ.stage :refer [create-stage! connect!
                                      subscribe-crdts!]]

            [hasch.core :refer [uuid]]
            [replikativ.crdt.ormap.realize :refer [stream-into-identity!]]
            [replikativ.crdt.ormap.stage :as s]
            [cljs.core.async :refer [>! chan timeout]]
            [superv.async :refer [S] :as sasync]
            [cljsjs.material-ui] ;; TODO why?
            [om.next :as om :refer-macros [defui] :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs-react-material-ui.core :as ui]
            [cljs-react-material-ui.icons :as ic]
            [sablono.core :as html :refer-macros [html]]
            [cljs-react-material-ui.core :as ui]
            [cljs-react-material-ui.icons :as ic])
  (:require-macros [superv.async :refer [go-try <? go-loop-try]]
                   [cljs.core.async.macros :refer [go-loop]]))

;; 1. app constants
(def user "mail:alice@replikativ.io")
(def ormap-id #uuid "7d274663-9396-4247-910b-409ae35fe98d")
(def uri "ws://127.0.0.1:31744")


(enable-console-print!)

;; Have a look at the replikativ "Get started" tutorial to understand how the
;; replikativ parts work: http://replikativ.io/tut/get-started.html

(def stream-eval-fns
  {'assoc (fn [a new]
            (swap! a assoc (uuid new) new)
            a)
   'dissoc (fn [a new]
             (swap! a dissoc (uuid new))
             a)})


(defonce val-atom (atom {}))


(defn setup-replikativ []
  (go-try S
    (let [local-store (<? S (new-mem-store))
          local-peer (<? S (client-peer S local-store))
          stage (<? S (create-stage! user local-peer))
          stream (stream-into-identity! stage
                                        [user ormap-id]
                                        stream-eval-fns
                                        val-atom)]
      (<? S (s/create-ormap! stage
                             :description "messages"
                             :id ormap-id))
      (connect! stage uri)
      {:store local-store
       :stage stage
       :stream stream
       :peer local-peer})))

(declare client-state)
;; this is the only state changing function
(defn send-message! [app-state msg]
  (s/assoc! (:stage client-state)
            [user ormap-id]
            (uuid msg)
            [['assoc msg]]))




;; helper functions
(defn format-time [d]
  (let [secs (-> (.getTime (js/Date.))
                 (- d)
                 (/ 1000)
                 js/Math.floor)]
    (cond
      (>= secs 3600) (str (js/Math.floor (/ secs 3600)) " hours ago")
      (>= secs 60) (str (js/Math.floor (/ secs 60)) " minutes ago")
      (>= secs 0) (str  " seconds ago"))))


;; Material UI with Om
(defn create-msg [name text]
  {:text text
   :name name
   :date (.getTime (js/Date.))})


(defn target-val [e]
  (.. e -target -value))


(defn name-field [comp input-name]
  (dom/div #js {:className "center-xs"}
           (ui/text-field
            {:floating-label-text "Name"
             :class-name "w-80"
             :on-change #(om/update-state! comp assoc :input-name (target-val %))
             :value input-name})))


(defn message-field [comp input-text input-name ]
  (let [app-state (om/props comp)]
    (dom/div #js {:className "center-xs" :key "message"}
             (ui/text-field {:floating-label-text "Message"
                             :class-name "w-80"
                             :on-change
                             #(om/update-state!
                               comp assoc :input-text (target-val %))
                             :on-key-down
                             (fn [e]
                               (when
                                   (or (= (.-which e) 13)
                                       (= (.-keyCode e) 13))
                                 (send-message!
                                  app-state (create-msg input-name input-text))
                                 (om/update-state! comp assoc :input-text "")))
                             :value input-text}))))


(defn send-button [comp input-text input-name]
  (let [app-state (om/props comp)]
    (dom/div #js {:className "center-xs"}
             (ui/raised-button
              {:label "Send"
               :on-touch-tap
               #(do
                  (send-message! app-state (create-msg input-name input-text))
                  (om/update-state! comp assoc :input-text ""))}))))

(defn message-item [{:keys [text name date]}]
  (ui/list-item {:primary-text
                 (dom/div nil name
                          (dom/small nil (str " wrote " (format-time date))))
                 :secondary-text text
                 :secondary-text-lines 2
                 :key (uuid (str date))}))


;; React App
(defui App
  Object
  (componentWillMount [this]
    (om/set-state!
     this
     {:input-name ""
      :input-text ""
      :snackbar {:message "hello"
                 :open false}}))
  (render [this]
    (let [app-state (om/props this)
          {:keys [input-name input-text snackbar]} (om/get-state this)]
      (ui/mui-theme-provider
       {:mui-theme (ui/get-mui-theme)}
       (html
        [:div.col-xs-12.mar-top-10.row
         (ui/snackbar {:open (:open snackbar) :message (:message snackbar)})
         [:div.col-xs-3]
         [:div.col-xs-6
          (ui/paper {:className "mar-top-20"}
                    (ui/list
                     nil
                     (name-field this input-name)
                     (message-field this input-text input-name)
                     (send-button this input-text input-name)
                     (ui/subheader nil "Messages")
                     (mapv message-item (sort-by :date > (vals app-state)))
                     (ui/divider nil)))]])))))

(def reconciler
  (om/reconciler {:state val-atom}))

(defn main [& args]
  (go-try S
          (def client-state (<? S (setup-replikativ)))
          (.error js/console "INITED")))

;; for figwheel not in main
(om/add-root! reconciler App (.getElementById js/document "app"))


