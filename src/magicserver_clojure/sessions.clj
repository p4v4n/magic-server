(ns magicserver-clojure.sessions)


(def SESSIONS (atom {}))


(defn uuid [] (str (java.util.UUID/randomUUID)))


(defn session-handler [request response]
    (let [browser-cookies (request "Cookie")
          sid (if browser-cookies (browser-cookies "sid"))
          session-check (if sid (@SESSIONS sid))]
        (if (and browser-cookies sid session-check)
            response
            (let [cookie (uuid)]
                (do (swap! SESSIONS assoc cookie {})
                (assoc response "Set-Cookie" (str "sid=" cookie)))))))


(defn add-session [request content]
    (let [browser-cookies (request "Cookie")]
        (if (browser-cookies "sid")
            (swap! SESSIONS assoc (browser-cookies "sid") content))))


(defn get-session [request]
    (let [browser-cookies (request "Cookie")]
        (if (and browser-cookies (browser-cookies "sid"))
            (@SESSIONS (browser-cookies "sid")))))


(defn del-session [request]
    (let [browser-cookies (request "Cookie")]
        (if (browser-cookies "sid")
            (swap! SESSIONS dissoc (browser-cookies "sid")))))
