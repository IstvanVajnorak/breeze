# breeze

Breeze is a simple key value database supporting acid transactions.
It is built on Spring MVC's REST Capabilities to provide an interface to the outside world.

The API endpoints are documented in the __Breeze.postman_collection.json__ file.

The project uses maven as it's build system.
To run it easy, one can just import it to IntelliJ or to Eclipse, and run the __spring-boot:run__ command to get the server up and running listening on localhost:8080.

There are unit tests attached to scan the service itself, and the REST API as well.

To expose it properly, one can run the mvn package command and get the resulting war into a tomcat server or any other servlet container of choice.
