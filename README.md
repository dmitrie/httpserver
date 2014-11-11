TODO
====
- Make server multi-threaded, test speed using `ab -n 10000 -c 100 http://localhost:8080/test.html`
- Simplify ServerTest by reusing sendRequest()
- File handler: add support for creating/updating/deleting files
- Make use of charset passed inside http headers
- Make UI test for Content-Length being larger than body length, must return Time-Out error