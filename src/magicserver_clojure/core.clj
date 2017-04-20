(ns magicserver-clojure.core
  (:gen-class))

;;(require '[clojure.tools.cli :refer [cli]])

(use 'magicserver-clojure.server)


(defn -main 
    [port]
  (serve-persistent (Integer/parseInt port) #(worker %)))
