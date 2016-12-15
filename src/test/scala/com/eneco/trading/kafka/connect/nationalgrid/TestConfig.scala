package com.eneco.trading.kafka.connect.nationalgrid

import java.text.SimpleDateFormat
import java.util
import java.util.Date

import com.eneco.trading.kafka.connect.nationalgrid.config.{RequestType, SOAPSourceConfig}
import com.typesafe.scalalogging.StrictLogging
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.connect.data.{Schema, SchemaBuilder, Struct}
import org.apache.kafka.connect.sink.SinkRecord
import org.scalatest.mock.MockitoSugar

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * Created by andrew@datamountaineer.com on 14/04/16.
  * stream-reactor
  */
trait TestConfig extends StrictLogging with MockitoSugar {
  val TOPIC1 = "sink_test"
  val TOPIC2 = "sink_test2"
  val TABLE1 = TOPIC1
  val TABLE2 = "table2"
  val TABLE3 = TOPIC2

  val IFR_TOPIC="ifr"
  val MIPI_TOPIC="mipi"
  val IFR_REQUEST="A"
  val MIPI_REQUEST="B"

  protected val PARTITION: Int = 12
  protected val PARTITION2: Int = 13
  protected val TOPIC_PARTITION: TopicPartition = new TopicPartition(TOPIC1, PARTITION)
  protected val TOPIC_PARTITION2: TopicPartition = new TopicPartition(TOPIC2, PARTITION2)
  protected val ASSIGNMENT: util.Set[TopicPartition] =  new util.HashSet[TopicPartition]

  //Set topic assignments, used by the sinkContext mock
  ASSIGNMENT.add(TOPIC_PARTITION)
  ASSIGNMENT.add(TOPIC_PARTITION2)


  //get the assignment of topic partitions for the sinkTask
  def getAssignment: util.Set[TopicPartition] = {
    ASSIGNMENT
  }

  def getProps() = Map(SOAPSourceConfig.IFR_TOPIC->IFR_TOPIC,
    SOAPSourceConfig.MIPI_TOPIC->MIPI_TOPIC,
    SOAPSourceConfig.IFR_REQUESTS->IFR_REQUEST,
    SOAPSourceConfig.MIPI_REQUESTS->MIPI_REQUEST).asJava

  //build a test record schema
  def createSchema: Schema = {
    SchemaBuilder.struct.name("record")
      .version(1)
      .field("id", Schema.STRING_SCHEMA)
      .field("int_field", Schema.INT32_SCHEMA)
      .field("long_field", Schema.INT64_SCHEMA)
      .field("string_field", Schema.STRING_SCHEMA)
      .field("timeuuid_field", Schema.STRING_SCHEMA)
      .field("timestamp_field", Schema.STRING_SCHEMA)
      .build
  }

  //build a test record
  def createRecord(schema: Schema, id: String): Struct = {

    val dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ")
    new Struct(schema)
      .put("id", id)
      .put("int_field", 12)
      .put("long_field", 12L)
      .put("string_field", "foo")
      .put("timestamp_field", dateFormatter.format(new Date()))
  }

  //generate some test records
  def getTestRecords(table: String) : Set[SinkRecord]= {
    val schema = createSchema
    val assignment: mutable.Set[TopicPartition] = getAssignment.filter(tp=>tp.topic().equals(table))

    assignment.flatMap(a => {
      (1 to 7).map(i => {
        val record: Struct = createRecord(schema, a.topic() + "-" + a.partition() + "-" + i)
        new SinkRecord(a.topic(), a.partition(), Schema.STRING_SCHEMA, "key", schema, record, i)
      })
    }).toSet
  }


//  def getSourceTaskContext(lookupPartitionKey: String, offsetValue: String, offsetColumn : String, table : String) = {
//    /**
//      * offset holds a map of map[string, something],map[identifier, value]
//      *
//      * map(map(assign.import.table->table1) -> map("my_timeuuid"->"2013-01-01 00:05+0000")
//      */
//
//    //set up partition
//    val partition: util.Map[String, String] = Collections.singletonMap(lookupPartitionKey, table)
//    //as a list to search for
//    val partitionList: util.List[util.Map[String, String]] = List(partition)
//    //set up the offset
//    val offset: util.Map[String, Object] = Collections.singletonMap(offsetColumn, offsetValue)
//    //create offsets to initialize from
//    val offsets :util.Map[util.Map[String, String],util.Map[String, Object]] = Map(partition -> offset)
//
//    //mock out reader and task context
//    val taskContext = mock[SourceTaskContext]
//    val reader = mock[OffsetStorageReader]
//    when(reader.offsets(partitionList)).thenReturn(offsets)
//    when(taskContext.offsetStorageReader()).thenReturn(reader)
//
//    taskContext
//  }
//
//  def getSourceTaskContextDefault(url: String) = {
//    val lookupPartitionKey = ""
//    val offsetValue = "2013-01-01 00:05:00.0000000Z"
//    val offsetColumn = "timestampColName"
//    val table = url
//    getSourceTaskContext(lookupPartitionKey, offsetValue,offsetColumn, table)
//  }
}
