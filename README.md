# Deft Server
The Deft web server is an open source projected (licensed under [Apache version 2]) 
started by Roger Schildmeijer ([@rschildmeijer]) and Jim Petersson ([jimpetersson]).

Deft is a single threaded, asynchronous, event driven high performance web server. 

Source and issue tracker: http://github.com/rschildmeijer/deft
 
## Features
 
 * Specialized and optimized for thousands of simultaneous connections. ([C10k]) ([C500k])
 * Using pure Java NIO ([java.nio] & [java.nio.channels])
 * Asynchronous (*nonblocking I/O*)

## Requirements
* Java >= 1.6 

## Getting started
### Synchronous

    class SynchronousRequestHandler extends RequestHandler {

        @Override
        public void get(HttpRequest request, HttpResponse response) {
            response.write("hello world!");
        }

    }

    public static void main(String[] args) {
        Map<String, RequestHandler> handlers = new HashMap<String, RequestHandler>();
        handlers.put("/", new SynchronousRequestHandler());
        HttpServer server = new HttpServer(new Application(handlers));
        server.listen(8080).getIOLoop().start();
    }
    

### Asynchronous

    class AsynchronousRequestHandler extends RequestHandler {

        @Override
        @Asynchronous
        public void get(HttpRequest request, final HttpResponse response) {
            response.write("hello ");
            db.asyncIdentityGet("world", new AsyncCallback<String>() {
                public void onSuccess(String result) { response.write(result).finish(); }
            });
        }

    }

    public static void main(String[] args) {
        Map<String, RequestHandler> handlers = new HashMap<String, RequestHandler>();
        handlers.put("/", new AsynchronousRequestHandler());
        HttpServer server = new HttpServer(new Application(handlers));
        server.listen(8080).getIOLoop().start();
    }
By annotating the get method with the *org.deft.web.Asynchronous* annotation you tell Deft that the request is
not finished when the get method returns. When the asynchronous database client eventually calls the callback (i.e. onSuccess(String result)), 
the request is still open, and the response is finally flushed to the client with the call to response.finish(). 

[@rschildmeijer]: http://twitter.com/rschildmeijer
[jimpetersson]: http://github.com/jimpetersson
[C10k]: http://en.wikipedia.org/wiki/C10k_problem
[C500k]: http://blog.urbanairship.com/blog/2010/08/24/c500k-in-action-at-urban-airship/
[java.nio]: http://download.oracle.com/javase/6/docs/api/java/nio/package-summary.html
[java.nio.channels]: http://download.oracle.com/javase/6/docs/api/java/nio/channels/package-summary.html
[Apache version 2]: http://www.apache.org/licenses/LICENSE-2.0.html