[![Build Status](https://travis-ci.org/Eneco/kafka-connect-nationalgrid.svg?branch=master)](https://travis-ci.org/Eneco/kafka-connect-nationalgrid)

# kafka-connect-nationalgrid
Kafka connect for UK National Grid.

The National Grid connector calls the UK National Grid API to retrieve Instantaneous Flow Reports and MIPI.

## Configurations

| Name            | Optional | Default | Description | Example |
|-----------------|----------|---------|-------------|---------|
| connect.nationalgrid.mipi.requests | No | - | The MIPI requests, separated by a pipe. A full list is [here](http://www2.nationalgrid.com/WorkArea/DownloadAsset.aspx?id=8589935796). The request is in the form [DateItem];[PublicationHour]:[PublicationMinute];[Frequency of publiication in minutes]   | Nominations, Prevailing Nomination, Aldbrough, Storage Entry;06:00;1440 |
| connect.nationalgrid.mipi.topic | No | - | The topic to write MIPI requests to. | sys_national_grid_mip_raw |
  connect.nationalgrid.irf.topic | No | - | The topics to write the IFR requests to. | sys_national_grid_ifr_raw |

## Adding the Connector to the Classpath

The use the Connector it needs to be on the classpath of your Kafka Connect Cluster. The easiest way is to
explicitly add it like this and then start Kafka Connect.

```bash
export CLASSPATH=target/kafka-connect-nationalgrid-1.0-jar-with-dependencies.jar
```


### Example

Pull the following MIPI every day close as possible to 06:00, Frequency is 1440 minutes or 24 hours.

```bash
connector.class = com.eneco.trading.kafka.connect.nationalgrid.source.NationalGridSourceConnector
tasks.max = 1
name = ng
connect.nationalgrid.mipi.requests=Nominations, Prevailing Nomination, Aldbrough, Storage Entry;06:00;1440|Nominations, Prevailing Nomination, Aldbrough, Storage Exit;06:00;1440
connect.nationalgrid.mipi.topic=mipi
connect.nationalgrid.irf.topic=ifr
```

Post in the config to Connect with DataMountaineers [CLI](https://github.com/datamountaineer/kafka-connect-tools).

```bash
cli.sh create ng < src/main/resources/connector.properties
```


Use scalaxrb to generate case class from wsdl, converts these to Structs to write

Run to produce generated sources
```
mvn generate-sources
```

To package
```
mvn package
```