(ns magicserver-clojure.sessions)


(def SESSIONS (atom {}))


(defn uuid [] (str (java.util.UUID/randomUUID)))


(defn session-handler [request response]
	(let [browser-cookies (request "Cookie")]
		(if (and browser-cookies (browser-cookies "sid") (SESSIONS (browser-cookies "sid")))
			response
			(let [cookie (uuid)]
			    (swap! SESSIONS assoc cookie {})
				(assoc response "Set-Cookie" (str "sid=" cookie))))))


(defn add-session [request content]
	(let [browser-cookies (request "Cookie")]
		(if (browser-cookies "sid")
			(swap! SESSIONS assoc (browser-cookies "sid") content))))


(defn get-session [request]
	(let [browser-cookies (request "Cookie")]
		(if (and browser-cookies (browser-cookies "sid"))
			(SESSIONS (browser-cookies "sid")))))


(defn del-session [request]
	(let [browser-cookies (request "Cookie")]
		(if (browser-cookies "sid")
			(swap! SESSIONS dissoc (browser-cookies "sid")))))
