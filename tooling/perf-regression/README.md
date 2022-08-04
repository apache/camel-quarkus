# Camel Quarkus Performance regression detection tool

This Quarkus based command line tool takes a list of Camel Quarkus versions as argument.

For each Camel Quarkus versions, it:
 + Assembles a sample base Camel Quarkus project against the specified Camel Quarkus version
 + Setup a performance test in the maven integration-test phase
 + Runs the performance test with the help of the [hyperfoil-maven-plugin](https://hyperfoil.io/)
 + Collects the mean throughput of the Camel Quarkus route

At the end of the day, a report is presented to the console, including a status about possible regressions.

Please find more details about the process in below picture:
![Performance regression detection tool process](processes-schema-app.diagrams.net.drawio.png)
