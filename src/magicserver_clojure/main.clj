(ns magicserver-clojure.main)
(use 'magicserver-clojure.server)

(defn add-route [method path func]
    (swap! ROUTES assoc method (assoc (@ROUTES method) path func)))


(defn home [request response]
    (send-html-handler request response (slurp "./views/index.html")))


(defn submit [request response]
    (let [first-name ((request "content") "firstname")
          last-name ((request "content") "lastname")]
        (send-html-handler request response (format (slurp "./views/submit.html") first-name last-name))))

(add-route "get" "/" home)
(add-route "post" "/submit" submit)
