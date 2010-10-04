# Deft Server
The Deft web server is an open source projected (licensed under [Apache version 2]). Deft was intitially inspired by [facebook/tornado]

Deft is a single threaded, asynchronous, event driven high performance web server running on the JVM.

Source and issue tracker: [http://github.com/rschildmeijer/deft]

Documentation: [http://www.deftserver.org]
 
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

### Capturing groups with regular expressions

    class CapturingRequestHandler extends RequestHandler {

        @Override
        public void get(HttpRequest request, HttpResponse response) { 
            response.write(request.getRequestedPath());
        }

    }

    public static void main(String[] args) {
        Map<String, RequestHandler> handlers = new HashMap<String, RequestHandler>();
        handlers.put("/persons/([0-9]+)", new CapturingRequestHandler());
        HttpServer server = new HttpServer(new Application(handlers));
        server.listen(8080).getIOLoop().start();
    }

The code above creates a "dynamic mapping" to the group capturing request handler (CapturingRequestHandler). This type 
of mapping is convenient when creating e.g. RESTful web services where you usually address your resources with the path 
segment instead of using get parameters. The mapping above will "capture" all requests made against urls that start with 
"/persons/" and ends with a (positive) number, e.g. "/persons/1911" or "/persons/42". Capturing groups can only be used as the
last url path segment like the example above. It's (currently) not possible to have more than one capturing within one 
"dynamic mapping".

### Contact
irc [freenode] #deft

Roger Schildmeijer ([@rschildmeijer]) 

Jim Petersson ([jimpetersson]).

[@rschildmeijer]: http://twitter.com/rschildmeijer
[jimpetersson]: http://github.com/jimpetersson
[C10k]: http://en.wikipedia.org/wiki/C10k_problem
[C500k]: http://blog.urbanairship.com/blog/2010/08/24/c500k-in-action-at-urban-airship/
[java.nio]: http://download.oracle.com/javase/6/docs/api/java/nio/package-summary.html
[java.nio.channels]: http://download.oracle.com/javase/6/docs/api/java/nio/channels/package-summary.html
[Apache version 2]: http://www.apache.org/licenses/LICENSE-2.0.html
[freenode]: http://freenode.net/irc_servers.shtml
[facebook/tornado]: http://github.com/facebook/tornado
[http://github.com/rschildmeijer/deft]: http://github.com/rschildmeijer/deft
[http://www.deftserver.org]: http://www.deftserver.org