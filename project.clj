(defproject chat42 "0.1.0-SNAPSHOT"

  :description "Web chat using replikativ."

  :url "http://github.com/replikativ/chat42"

  :main chat42.core

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/clojurescript "1.9.229"]

                 [com.cognitect/transit-cljs "0.8.239" :scope "provided"]
                 [io.replikativ/replikativ "0.2.4"]
                 [sablono "0.8.0"]
                 [com.fzakaria/slf4j-timbre "0.3.5"]
                 [org.omcljs/om "1.0.0-alpha46" :exclusions [cljsjs/react
                                                             cljsjs/react-dom]]
                 [cljs-react-material-ui "0.2.44"]]

  :plugins [[lein-figwheel "0.5.8"]
            [lein-cljsbuild "1.1.4" :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src/cljs" "src/clj"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src/cljs"]

                ;; the presence of a :figwheel configuration here
                ;; will cause figwheel to inject the figwheel client
                ;; into your build
                :figwheel {:on-jsload "chat42.core/on-js-reload"
                           ;; :open-urls will pop open your application
                           ;; in the default browser once Figwheel has
                           ;; started and complied your application.
                           ;; Comment this out once it no longer serves you.
                           :open-urls ["http://localhost:13449/index.html"]}

                :compiler {:main chat42.core
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/chat42.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true
                           ;; To console.log CLJS data-structures make sure you enable devtools in Chrome
                           ;; https://github.com/binaryage/cljs-devtools
                           :preloads [devtools.preload]}}
               ;; This next build is an compressed minified build for
               ;; production. You can build this with:
               ;; lein cljsbuild once min
               {:id "min"
                :source-paths ["src/cljs"]
                :compiler {:output-to "resources/public/js/compiled/chat42.js"
                           :main chat42.core
                           :optimizations :advanced
                           :pretty-print false}}]}

  :figwheel {;; :http-server-root "public" ;; default and assumes "resources"
              :server-port 13449 ;; default
             ;; :server-ip "127.0.0.1"

             :css-dirs ["resources/public/css"] ;; watch and update CSS

             ;; Start an nREPL server into the running figwheel process
             ;; :nrepl-port 7888

             ;; Server Ring Handler (optional)
             ;; if you want to embed a ring handler into the figwheel http-kit
             ;; server, this is for simple ring servers, if this

             ;; doesn't work for you just run your own server :) (see lein-ring)

             ;; :ring-handler hello_world.server/handler

             ;; To be able to open files in your editor from the heads up display
             ;; you will need to put a script on your path.
             ;; that script will have to take a file path and a line number
             ;; ie. in  ~/bin/myfile-opener
             ;; #! /bin/sh
             ;; emacsclient -n +$2 $1
             ;;
             ;; :open-file-command "myfile-opener"

             ;; if you are using emacsclient you can just use
             ;; :open-file-command "emacsclient"

             ;; if you want to disable the REPL
             ;; :repl false

             ;; to configure a different figwheel logfile path
             ;; :server-logfile "tmp/logs/figwheel-logfile.log"
             }
  :profiles {:dev {:dependencies [[binaryage/devtools "0.8.2"]
                                  [figwheel-sidecar "0.5.8"]
                                  [com.cemerick/piggieback "0.2.1"]]
                   ;; need to add dev source path here to get user.clj loaded
                   :source-paths ["src" "dev"]
                   ;; for CIDER
                   ;; :plugins [[cider/cider-nrepl "0.12.0"]]
                   :repl-options {; for nREPL dev you really need to limit output
                                  :init (set! *print-length* 50)
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}
  )
