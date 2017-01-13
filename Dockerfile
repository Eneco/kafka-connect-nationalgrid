FROM eneco/connector-base:0.2.0

COPY target/kafka-connect-nationalgrid-1.0-jar-with-dependencies.jar /etc/kafka-connect/jars
