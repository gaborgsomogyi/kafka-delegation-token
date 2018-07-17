package com.kafka.delegationtoken.producer

import java.util._

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.config.{SaslConfigs, SslConfigs}
import org.apache.log4j.LogManager


object SecureKafkaProducer {

  @transient lazy val log = LogManager.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    if (args.length != 3) {
      log.error("Usage: SecureKafkaProducer [bootstrap] [protocol] [topic]")
      log.error("Example: SecureKafkaProducer localhost:9092 SASL_PLAINTEXT test")
      System.exit(1)
    }

    val bootstrapServer = args(0)
    val protocol = args(1)
    val topic = args(2)

    val isUsingSsl = protocol.endsWith("SSL")

    log.info("Creating producer config properties...")
    val producerProperties = new Properties
    producerProperties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer)
    producerProperties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, protocol)
    producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer")
    producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    producerProperties.put(SaslConfigs.SASL_KERBEROS_SERVICE_NAME, "kafka")
    producerProperties.put(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-256")
    if (isUsingSsl) {
      producerProperties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, "/etc/cdep-ssl-conf/CA_STANDARD/truststore.jks")
      producerProperties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "cloudera")
    }
    log.info("OK")

    log.info("Creating kafka producer...")
    val producer = new KafkaProducer[String, String](producerProperties)
    log.info("OK")
    var i = 0
    while (true) {
      val data = "streamtest-" + i
      val record = new ProducerRecord[String, String](topic, data)
      log.info("Sending record: " + record)
      producer.send(record)
      log.info("OK")
      Thread.sleep(1000)
      i += 1
    }

    log.info("Closing kafka producer...")
    producer.close()
    log.info("OK")
  }
}
