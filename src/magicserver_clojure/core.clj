(ns magicserver-clojure.core
  (:gen-class))

;;(require '[clojure.tools.cli :refer [cli]])

(use 'magicserver-clojure.sessions)
(use 'magicserver-clojure.server)
(use 'magicserver-clojure.main)


(defn -main 
    [port]
  (serve-persistent (Integer/parseInt port) #(worker %)))
