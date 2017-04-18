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


(defn add-route [method path func]
	(println "a"))


(def test-get-str "GET / HTTP/1.1\r\nHost: localhost:8888\r\nConnection: keep-alive\r\nCache-Control: max-age=0\r\nUpgrade-Insecure-Requests: 1\r\nUser-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/56.0.2924.76 Chrome/56.0.2924.76 Safari/537.36\r\nAccept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\nDNT: 1\r\nAccept-Encoding: gzip, deflate, sdch, br\r\nAccept-Language: en-GB,en-US;q=0.8,en;q=0.6\r\n\r\n")

(def test-post-str "POST /submit HTTP/1.1\r\nHost: localhost:8888\r\nConnection: keep-alive\r\nContent-Length: 31\r\nCache-Control: max-age=0\r\nOrigin: http://localhost:8888\r\nUpgrade-Insecure-Requests: 1\r\nUser-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/56.0.2924.76 Chrome/56.0.2924.76 Safari/537.36\r\nContent-Type: application/x-www-form-urlencoded\r\nAccept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\nDNT: 1\r\nReferer: http://localhost:8888/\r\nAccept-Encoding: gzip, deflate, br\r\nAccept-Language: en-GB,en-US;q=0.8,en;q=0.6\r\n\r\n")
(def  test-post-str2 "POST /submit HTTP/1.1\r\nHost: localhost:8888\r\nConnection: keep-alive\r\nContent-Length: 31\r\nCache-Control: max-age=0\r\nOrigin: http://localhost:8888\r\nUpgrade-Insecure-Requests: 1\r\nUser-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/57.0.2987.98 Chrome/57.0.2987.98 Safari/537.36\r\nContent-Type: application/x-www-form-urlencoded\r\nAccept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\nDNT: 1\r\nReferer: http://localhost:8888/\r\nAccept-Encoding: gzip, deflate, br\r\nAccept-Language: en-GB,en-US;q=0.8,en;q=0.6\r\n\r\nfirstname=Pavan&lastname=Mantha\r\n\r\n")


;;Parsers

(defn get-http-header [data]
	(if (re-find #"\r\n\r\n" data)
		(split data #"\r\n\r\n" 2)))


(defn process-request-first-line
	[input]
	(zipmap ["method" "path" "protocol"]
		(split input #"\s")))


(defn header-parser [input]
	(let [header_list (split input #"\r\n")]
		(loop 
			[result (process-request-first-line (first header_list))
			re (rest header_list)]
			(if (empty? re)
				result
				(recur
					(assoc 
						result
						(first (split (first re) #":\s+"))
						(second (split (first re) #":\s+")))
					(rest re))))))


(def date (.toString (java.util.Date.)))


(defn response-stringify [response]
	(let [keys-needed (filter #(and (not= % "status") (not= % "Content")) (keys response))
	      response-string (str (response "status") "\r\n")]
	      (loop [re response-string
	      	     k keys-needed]
	      	     (if (empty? k)
                     (str re "\r\n" (response "Content") "\r\n\r\n")
	      	         (recur (str re (first k) ": " (response (first k)) "\r\n") (rest k))))))

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


(defn home [request response]
	(send-html-handler request response (slurp "./views/index.html")))


(defn submit [request response]
	(let [first-name ((request "content") "firstname")
		  last-name ((request "content") "lastname")]
		  (send-html-handler request response (slurp "./views/submit.html"))

		  ))


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
		(loop [body-dic {} bl body-list]
			(if (empty? bl)
				body-dic
				(let [[k v] (split (first bl) #"=" 1)](recur (assoc body-dic k v) (rest bl)))))))


(def ROUTES {"get" {"/" home} "post" {"/submit" submit} "put" {} "delete" {}})


(defn get-handler [request response]
	(try
	(((ROUTES "get") (request "path")) request response)
	(catch Exception e (err-404-handler request response))))


(defn post-handler [request response]
	(try
		(if (re-find #"multipart" (request "Content-Type"))
			(((ROUTES "post") (request "path")) (assoc (form-parser request) "content" ((form-parser request) "form")) response)
			(((ROUTES "post") (request "path")) (assoc request "content" (parse-fields (request "body"))) response))
		(catch Exception e (err-404-handler request response))))


(defn delete-handler [request response]
	(try
		(((ROUTES "delete") (request "path")) request response)
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
	(let [x (header-parser (trim input))
		  y (x "Content-Length")]
	(if y
     (Integer/parseInt y))))



(defn receive [socket]
	(let [x (io/reader socket)
		  y (loop [s (.readLine x) f-s ""]
		  	(if (empty? s)
		  		(str f-s "\r\n")
		  		(recur (.readLine x) (str f-s s "\r\n"))))
		  t (check-for-content-length y)
		  k (prn t)
		  y-new (if t 
		  	        (loop [a y c 0]
		  	         (if (= t c)
		  	         	 (str a "\r\n")
		  	         	(let [next-line (.readLine x)] 
		  	         		 (recur (str a next-line "\r\n") (+ c (count next-line))))))
		  	        y)]
		 y-new	  	        
		 ))


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
