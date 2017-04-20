(ns magicserver-clojure.main)
(use 'magicserver-clojure.server)

(defn add-route [method path func]
    (swap! ROUTES assoc method (assoc (@ROUTES method) path func)))


(defn home [request response]
    (send-html-handler request response (slurp "./views/index.html")))


(defn login [request response]
    (let [username ((request "content") "username")
          password ((request "content") "password")]
        (send-html-handler request response (format (slurp "./views/dashboard.html") username))))

(add-route "get" "/" home)
(add-route "post" "/login" login)
