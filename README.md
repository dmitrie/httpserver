HTTP Server
====
This Http Server is a learning project, it is not maintained, so use it on your own risk.

Basic functionality:

 - Server parses and validates incoming message according to RFC2616 and allows to send back a response.
 - Server is multi-threaded.
 - Server understands a different charset of incoming message body and by default uses ISO-8859-1 for all other operations, meaning that with default settings all files must be stored in ISO-8859-1 to use the standard file system handler. However, this behavior is easily changed by modifying bodyCharset field of response in a custom handler or modifying FileSystemHandler class.  
 - Server correctly parses parameters of a GET request and body of a POST request sent together with application/x-www-form-urlencoded Content-Type.
 - Server allows to generate response using multiple handlers that are executed in the order they are registered at and only if specified for each handler regular expression matches the request URI.
 - Server is coming with a simple file system handler that allows to get text/html files from some path and navigate through this path using directory listings.
 - Currently only GET, HEAD and POST methods are marked as implemented in default configuration, however handling other request types can be implemented by adding custom handlers.
 
Project is done in Java 8.

Run core.Server.main() and navigate to http://localhost:8080/ to see a demo.

P.S. Pay attention to core.Response.setBody(). It automatically sets the Content-Length header. In addition, it should be used in case of HEAD requests in the absolutely same manner as for GET requests. It automatically cuts body and leaves all the headers untouched. It is done to make sure that headers of responses sent to GET and HEAD requests are identical as specified in RFC2616.  