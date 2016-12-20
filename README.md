[![Build Status](https://travis-ci.org/Eneco/kafka-connect-nationalgrid.svg?branch=master)](https://travis-ci.org/Eneco/kafka-connect-nationalgrid)

# kafka-connect-nationalgrid
Kafka connect for UK National Grid via SOAP

WIP

Use scalaxrb to generate case class from wsdl, converts these to Structs to write

Run to produce generated sources
```
mvn generate-sources
```

To package
```
mvn package
```

To build the docker

```
mvn docker:build 
```