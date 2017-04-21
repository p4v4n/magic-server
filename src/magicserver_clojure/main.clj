(ns magicserver-clojure.main)
(use 'magicserver-clojure.sessions)
(use 'magicserver-clojure.server)


(defn add-route [method path func]
    (swap! ROUTES assoc method (assoc (@ROUTES method) path func)))


(defn home [request response]
    (let [k (get-session request)
    	userid (if k (k "userid"))]
    (if userid
    	(send-html-handler request response (format (slurp "./views/dashboard.html") userid))
        (send-html-handler request response (slurp "./views/index.html")))))


(defn login [request response]
    (let [username ((request "content") "username")
          password ((request "content") "password")]
         (add-session request {"userid" username})
        (send-html-handler request response (format (slurp "./views/dashboard.html") username))))

(defn logout [request response]
	(del-session request)
    (send-html-handler request response (slurp "./views/index.html")))

(add-route "get" "/" home)
(add-route "post" "/login" login)
(add-route "post" "/logout" logout)
