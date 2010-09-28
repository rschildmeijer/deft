# Deft Server
The Deft web server is an open source projected started by Roger Schildmeijer ([@rschildmeijer]) and
Jim Petersson ([jimpetersson]).

Deft is a single threaded, asynchronous, event driven high performance web server. 

Source and issue tracker: http://github.com/rschildmeijer/deft
 
## Features
 
 * Specialized and optimized for thousands of simultaneous connections. ([C10k]) ([C500k])
 * Using pure Java NIO ([java.nio] & [java.nio.channels])
 * Asynchronous (*nonblocking I/O*)

## Requirements
* Java >= 1.6 

## Getting started
    private static class ExampleRequestHandler extends RequestHandler {

        @Override
        public void get(HttpRequest request, HttpResponse response) {
            response.write("hello world!");
        }

    }

    public static void main(String[] args) {
        Map<String, RequestHandler> handlers = new HashMap<String, RequestHandler>();
        reqHandlers.put("/", new ExampleRequestHandler());
        Application application = new Application(handlers);
        HttpServer server = new HttpServer(application);
        server.listen(8080).getIOLoop().start();
    }
    
[@rschildmeijer]: http://twitter.com/rschildmeijer
[jimpetersson]: http://github.com/jimpetersson
[C10k]: http://en.wikipedia.org/wiki/C10k_problem
[C500k]: http://blog.urbanairship.com/blog/2010/08/24/c500k-in-action-at-urban-airship/
[java.nio]: http://download.oracle.com/javase/6/docs/api/java/nio/package-summary.html
[java.nio.channels]: http://download.oracle.com/javase/6/docs/api/java/nio/channels/package-summary.html