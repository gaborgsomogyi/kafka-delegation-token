package com.kafka.delegationtoken.consumer

import java.io.{File, PrintWriter}
import java.text.SimpleDateFormat
import java.time.Duration
import java.util._

import scala.collection.JavaConversions._

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.{AdminClient, CreateDelegationTokenOptions, RenewDelegationTokenOptions}
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.common.config.{SaslConfigs, SslConfigs}
import org.apache.kafka.common.security.token.delegation.DelegationToken
import org.apache.log4j.LogManager


object SecureKafkaConsumer {

  @transient lazy val log = LogManager.getLogger(getClass)

  private def printToken(token: DelegationToken): Unit = {
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm")
    log.info("%-15s %-30s %-15s %-25s %-15s %-15s %-15s".format("TOKENID", "HMAC", "OWNER", "RENEWERS", "ISSUEDATE", "EXPIRYDATE", "MAXDATE"))
    val tokenInfo = token.tokenInfo
    log.info("%-15s %-30s %-15s %-25s %-15s %-15s %-15s".format(
      tokenInfo.tokenId,
      token.hmacAsBase64String,
      tokenInfo.owner,
      tokenInfo.renewersAsString,
      dateFormat.format(tokenInfo.issueTimestamp),
      dateFormat.format(tokenInfo.expiryTimestamp),
      dateFormat.format(tokenInfo.maxTimestamp)))
  }

  private def writeTokenToFile(token: DelegationToken, filePath: String): Unit = {
    log.info("Writing token to file " + filePath + "...")
    val pw = new PrintWriter(new File(filePath))
    pw.write("KafkaClient {\n")
    pw.write("  org.apache.kafka.common.security.scram.ScramLoginModule required\n")
    pw.write("  username=\"" + token.tokenInfo.tokenId + "\"\n")
    pw.write("  password=\"" + token.hmacAsBase64String + "\"\n")
    pw.write("  tokenauth=true;\n")
    pw.write("};\n")
    pw.close
    log.info("OK")
  }

  def main(args: Array[String]): Unit = {
    if (args.length != 6) {
      log.error("Usage: SecureKafkaConsumer [bootstrap] [protocol] [keytab] [principal] [topic] [tokenLifetimeMs]")
      log.error("Example: SecureKafkaConsumer localhost:9092 SASL_PLAINTEXT user.keytab user@MY.DOMAIN.COM test 12000")
      System.exit(1)
    }

    val bootstrapServer = args(0)
    val protocol = args(1)
    val keytab = args(2)
    val principal = args(3)
    val topic = args(4)
    val tokenLifetimeMs = args(5).toInt

    val isUsingSsl = protocol.endsWith("SSL")

    val jaasConfig =
      s"""
      |com.sun.security.auth.module.Krb5LoginModule required
      | debug=true
      | useKeyTab=true
      | storeKey=true
      | serviceName="kafka"
      | keyTab="$keytab"
      | principal="$principal";
      """.stripMargin.replace("\n", "")
    log.info(s"JAAS config: $jaasConfig")

    log.info("Creating AdminClient config properties...")
    val adminClientProperties = new Properties
    adminClientProperties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer)
    adminClientProperties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, protocol)
    adminClientProperties.put(SaslConfigs.SASL_JAAS_CONFIG, jaasConfig)
    if (isUsingSsl) {
      adminClientProperties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, "/etc/cdep-ssl-conf/CA_STANDARD/truststore.jks")
      adminClientProperties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "cloudera")
    }
    log.info("OK")

    log.info("Creating AdminClient...")
    val adminClient = AdminClient.create(adminClientProperties)
    log.info("OK")

    log.info("Creating token...")
    val createDelegationTokenOptions = new CreateDelegationTokenOptions()
    val createResult = adminClient.createDelegationToken(createDelegationTokenOptions)
    val token = createResult.delegationToken().get()
    log.info("OK")
    printToken(token)
    writeTokenToFile(token, "/tmp/jaas_delegation_token.conf")

    val renewPeriodMs = tokenLifetimeMs * 2 / 3
    log.info("Token renewal will happen every " + renewPeriodMs + " ms")
    val renewTask = new TimerTask() {
      @Override
      def run(){
        log.info("Renewing token...")
        val renewDelegationTokenOptions = new RenewDelegationTokenOptions().renewTimePeriodMs(tokenLifetimeMs)
        val renewResult = adminClient.renewDelegationToken(token.hmac, renewDelegationTokenOptions)
        val expiryTimeStamp = renewResult.expiryTimestamp().get()
        val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm")
        log.info("OK")
        log.info("New expiry date: %s".format(dateFormat.format(expiryTimeStamp)))
      }
    };
    new Timer().scheduleAtFixedRate(renewTask, 0, renewPeriodMs);

    log.info("Creating consumer config properties...")
    val consumerProperties = new Properties
    consumerProperties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer)
    consumerProperties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, protocol)
    consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString)
    consumerProperties.put(SaslConfigs.SASL_JAAS_CONFIG, jaasConfig)
    if (isUsingSsl) {
      consumerProperties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, "/etc/cdep-ssl-conf/CA_STANDARD/truststore.jks")
      consumerProperties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "cloudera")
    }
    log.info("OK")

    log.info("Creating kafka consumer...")
    val consumer = new KafkaConsumer[String, String](consumerProperties)
    consumer.subscribe(Arrays.asList(topic))
    log.info("OK")
    while (true) {
      val records = consumer.poll(Duration.ofMillis(1000))
      for (record <- records) {
        log.info(record)
      }
      Thread.sleep(1000)
    }

    log.info("Closing kafka consumer...")
    consumer.close()
    log.info("OK")
  }
}
