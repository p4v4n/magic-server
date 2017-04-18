(ns magicserver-clojure.main)
(use 'magicserver-clojure.server)

;;(def ROUTES {"get" {"/" home}})

(println (slurp "./views/index.html"))

(defn home [request response]
	(send-html-handler request response (slurp "./views/index.html"))
	)
