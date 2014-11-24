HTTP Server
====
This Http Server is a learning project, it is not maintained, so use it on your own risk.

Basic functionality:

 - Server parses and validates incoming headers according to RFC2616 and allows to send back a response.
 - Server is multi-threaded.
 - Server understands a different charset of incoming message body and uses ISO-8859-1 for all other operations.
 - Server correctly parses parameters of a GET request and body of a POST request sent together with application/x-www-form-urlencoded Content-Type.
 - Server allows to generate response using multiple handlers that are executed in the order they are registered at and only if specified for each handler regular expression matches the request URI.
 - Server is coming with a simple file system handler that allows to get text/html files from some path and navigate through this path using directory listings.
 - Currently only GET, HEAD and POST methods are marked as implemented in default configuration, however handling other request types can be implemented by adding custom handlers.