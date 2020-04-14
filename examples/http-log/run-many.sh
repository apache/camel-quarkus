#!/bin/sh

# script to quickly run 100 instances of the native compiled example
# using http port numbers from 8000 - 8100
# you can then try each instance by its number such as:
#   http://localhost:8012/camel/hello
#
# to kill all instances at once, you can use pkill command:
#   pkill camel-quarkus
#
for i in $(seq 8000 8100); do
  QUARKUS_HTTP_PORT=$i ./target/camel-quarkus-examples-http-log-*-runner > http-log-$i.log &
done