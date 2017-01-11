FROM eneco/connector-base:0.1.0

COPY target/kafka-connect-nationalgrid-1.0-jar-with-dependencies.jar /etc/kafka-connect/jars
