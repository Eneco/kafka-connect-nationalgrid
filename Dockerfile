FROM confluentinc/cp-kafka-connect:3.0.1
RUN cd /etc/${COMPONENT}/jars && wget https://github.com/datamountaineer/kafka-connect-tools/releases/download/v0.8/kafka-connect-cli-0.8-all.jar
COPY entrypoint.sh /etc/confluent/entrypoint.sh
COPY templates/connector.properties.template /etc/confluent/docker/connector.properties.template
COPY target/libs/kafka-connect-nationalgrid-1.0-jar-with-dependencies /etc/kafka-connect/jars
RUN apt-get update && apt-get install unzip
RUN chmod +x /etc/confluent/entrypoint.sh
RUN wget -O /usr/local/bin/dumb-init https://github.com/Yelp/dumb-init/releases/download/v1.2.0/dumb-init_1.2.0_amd64
RUN chmod +x /usr/local/bin/dumb-init
CMD ["dumb-init", "/etc/confluent/entrypoint.sh"]