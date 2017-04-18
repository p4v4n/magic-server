(ns magicserver-clojure.core
  (:gen-class))

;;(require '[clojure.tools.cli :refer [cli]])

(use 'magicserver-clojure.server)

(defn -main
    []
  (serve-persistent 8888 #(worker %))
)
