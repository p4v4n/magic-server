;; Magic Server in Clojure


(ns magicserver-clojure.server)
(use '[clojure.string])
(use '[clojure.data.json :as json])
(require '[clojure.java.io :as io])
(import '[java.net ServerSocket])


(def CONTENT-TYPE {"html" "text/html", "css" "text/css", "js" "application/javascript",
                   "jpeg" "image/jpeg", "jpg" "image/jpg", "png" "image/png", "gif" "image/gif",
                   "ico" "image/x-icon", "text" "text/plain", "json" "application/json"})


(def SESSIONS {})


(def test-get-str "GET / HTTP/1.1\r\nHost: localhost:8888\r\nConnection: keep-alive\r\nCache-Control: max-age=0\r\nUpgrade-Insecure-Requests: 1\r\nUser-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/56.0.2924.76 Chrome/56.0.2924.76 Safari/537.36\r\nAccept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\nDNT: 1\r\nAccept-Encoding: gzip, deflate, sdch, br\r\nAccept-Language: en-GB,en-US;q=0.8,en;q=0.6\r\n\r\n")
(def test-post-str "POST /submit HTTP/1.1\r\nHost: localhost:8888\r\nConnection: keep-alive\r\nContent-Length: 31\r\nCache-Control: max-age=0\r\nOrigin: http://localhost:8888\r\nUpgrade-Insecure-Requests: 1\r\nUser-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/56.0.2924.76 Chrome/56.0.2924.76 Safari/537.36\r\nContent-Type: application/x-www-form-urlencoded\r\nAccept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\nDNT: 1\r\nReferer: http://localhost:8888/\r\nAccept-Encoding: gzip, deflate, br\r\nAccept-Language: en-GB,en-US;q=0.8,en;q=0.6\r\n\r\n")
(def test-post-str2 "POST /submit HTTP/1.1\r\nHost: localhost:8888\r\nConnection: keep-alive\r\nContent-Length: 31\r\nCache-Control: max-age=0\r\nOrigin: http://localhost:8888\r\nUpgrade-Insecure-Requests: 1\r\nUser-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/57.0.2987.98 Chrome/57.0.2987.98 Safari/537.36\r\nContent-Type: application/x-www-form-urlencoded\r\nAccept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\nDNT: 1\r\nReferer: http://localhost:8888/\r\nAccept-Encoding: gzip, deflate, br\r\nAccept-Language: en-GB,en-US;q=0.8,en;q=0.6\r\n\r\nfirstname=Pavan&lastname=Mantha\r\n\r\n")


;;Parsers

(defn get-http-header [data]
    (if (re-find #"\r\n\r\n" data)
        (split data #"\r\n\r\n" 2)))


(defn process-request-first-line
    [input]
    (zipmap ["method" "path" "protocol"] (split input #"\s")))


(defn header-parser [input]
    (let [header-list (split input #"\r\n")]
        (loop 
            [header-map (process-request-first-line (first header-list))
            re (rest header-list)]
            (if (empty? re)
                header-map
                (recur
                    (assoc 
                        header-map
                        (first (split (first re) #":\s+"))
                        (second (split (first re) #":\s+")))
                    (rest re))))))


(def date (.toString (java.util.Date.)))


(defn response-stringify [response]
    (let [keys-needed (filter #(and (not= % "status") (not= % "Content")) (keys response))
          response-string (str (response "status") "\r\n")]
         (loop [re response-string
                h-keys keys-needed]
                (if (empty? h-keys)
                    (str re "\r\n" (response "Content") "\r\n\r\n")
                    (recur (str re (first h-keys) ": " (response (first h-keys)) "\r\n") (rest h-keys))))))

;;Handlers

(defn response-handler [request response]
    (response-stringify (assoc response "Date" date "Connection" "close" "Server" "magic-server-clojure")))


(defn ok-200-handler [request response]
    (if (and (response "Content") (response "Content-type"))
        (response-handler request (assoc response "status" "HTTP/1.1 200 OK" "Content-Length" (str (count (response "Content")))))
        (response-handler request (assoc response "status" "HTTP/1.1 200 OK"))))


(defn err-404-handler [request response]
    (response-handler request (assoc response "status" "HTTP/1.1 404 Not Found" "content" "Content Not Found" "Content-type" "text/html")))


(defn send-html-handler [request response content]
    (if (not (empty? content))
        (ok-200-handler request (assoc response "Content" content "Content-type" "text/html"))
        (err-404-handler request response)))


(defn send-json-handler [request response content]
    (if (not (empty? content))
        (ok-200-handler request (assoc response "content" (json/write-str content) "Content-type" "application/json"))
        (err-404-handler request response)))


(defn form-parser [request]
    (let [content-type (request "Content-Type")
          boundary (last (split content-type #"; "))
          boundary-value (str "--" (second (split boundary #"=")))
          content-list (split (request "body") (re-pattern boundary-value))]
        (loop [c-l content-list form {}]
            (if (empty? c-l)
                (assoc request "boundary" boundary-value "form" form)
                (let [form-data (split (first content-list) #"\r\n\r\n" 2)
                      form-header (split (first form-data) #"\r\n")
                      form-body (second form-data)
                      form-header-dict (into {} (map #(let [[k v] (clojure.string/split % #": " 2)] {k v}) form-header))
                      content-items (split (form-header-dict "Content-Disposition") #"; ")
                      data (into {} (map #(let [[k v] (clojure.string/split % #"=" 2)] {k v}) content-items))
                      new-data (assoc data "body" form-body)]
                    (recur (rest c-l) (assoc form (data "name") new-data)))))))


(defn parse-fields [body]
    (let [body-list (split body #"&")]
        (loop [body-dic {} b-list body-list]
            (if (empty? b-list)
                body-dic
                (let [[k v] (split (first b-list) #"=" 2)](recur (assoc body-dic k v) (rest b-list)))))))


(def ROUTES (atom {"get" {} "post" {} "put" {} "delete" {}}))


(defn get-handler [request response]
    (try
        (((@ROUTES "get") (request "path")) request response)
        (catch Exception e (err-404-handler request response))))


(defn post-handler [request response]
    (try
        (if (re-find #"multipart" (request "Content-Type"))
            (((@ROUTES "post") (request "path")) (assoc (form-parser request) "content" ((form-parser request) "form")) response)
            (((@ROUTES "post") (request "path")) (assoc request "content" (parse-fields (request "body"))) response))
        (catch Exception e (err-404-handler request response))))


(def METHOD {"GET" get-handler "POST" post-handler})


(defn method-handler [request response]
    ((METHOD (request "method")) request response))


(defn head-handler [request response]
    (response-handler request (assoc response "content" "")))


(defn request-handler [request]
    (method-handler request {}))


(defn worker [data]
    (let [[header-str body-str] (get-http-header data)]
        (request-handler (assoc (header-parser header-str) "body" (trim body-str)))))


(defn check-for-content-length [input]
    (let [header-dict (header-parser (trim input))
          content-length (header-dict "Content-Length")]
        (if content-length
           (Integer/parseInt content-length))))


(defn receive [socket]
    (let [x (io/reader socket)
          request-header (loop [s (.readLine x) 
                                f-s ""]
                                (if (empty? s)
                                    (str f-s "\r\n")
                                    (recur (.readLine x) (str f-s s "\r\n"))))
          content-length (check-for-content-length request-header)
          complete-request (if content-length 
                               (let [body (char-array content-length)]
                                   (.read x body 0 content-length)
                                   (str request-header (apply str body)))
                                request-header)]
    complete-request))


(defn send [socket msg]
    (let [writer (io/writer socket)]
        (.write writer msg)
        (.flush writer)))


(defn serve-persistent [port handler]
  (let [running (atom true)]
    (future
      (with-open [server-sock (ServerSocket. port)]
        (while @running
          (with-open [sock (.accept server-sock)]
             (let [msg-in (receive sock)
                  a (prn "Request: " msg-in)
                  msg-out (handler msg-in)
                  b (prn "Response: " msg-out)]
             (send sock msg-out))))))
    running))
