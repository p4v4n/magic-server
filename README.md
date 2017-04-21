# magicserver-clojure

A basic HTTP Server in CLojure.

Can handle GET and POST requests.

## Installation

Download the project and place your main app file in /src/magicserver_clojure.

## Usage

To start the server at port 8888

    $ lein run 8888
## How to use:

Static files have to be enclosed in 'views' directory under root.

```
/
views/
    js/
    img/
    css/
```

To map the dynamic pages, use the function `server.add-route()` which takes 3 parameters

1. HTTP Method.
2. Requested path.
3. Function that would return the dynamic content.

Eg:

```
(defn home [request response]
  (server.send-html-handler request response content))
  
(server.add-route 'get' '/' home)
```

To send html or json data response, use the following functions `server.send-html-handler`  and `server.send-json-handler` which take 3 arguments

1. request
2. response
3. requested HTML/JSON content

### Bugs

Raise a issue or send a pull request if you find any bugs.

## License

Copyright © 2017 p4v4n

Distributed under the Eclipse Public License (version 1.0)
