(ns main)
(use 'magicserver-clojure.sessions)
(use 'magicserver-clojure.server)


(defn add-route [method path func]
    (swap! ROUTES assoc method (assoc (@ROUTES method) path func)))


(defn home [request response]
    (let [session-cookie (get-session request)
    	userid (if session-cookie (session-cookie "userid"))]
    (if userid
    	(redirect-303-handler "/dashboard")
        (send-html-handler request response (slurp "./views/index.html")))))


(defn dashboard [request response]
    (let [session-cookie (get-session request)
        userid (if session-cookie (session-cookie "userid"))]
    (if userid
        (send-html-handler request response (format (slurp "./views/dashboard.html") userid))
        (redirect-303-handler "/"))))


(defn login [request response]
    (let [username ((request "Content") "username")
          password ((request "Content") "password")]
         (add-session request {"userid" username})
         (redirect-303-handler "/dashboard")))


(defn logout [request response]
	(del-session request)
    (redirect-303-handler "/"))

(add-route "get" "/" home)
(add-route "get" "/dashboard" dashboard)
(add-route "post" "/login" login)
(add-route "post" "/logout" logout)
