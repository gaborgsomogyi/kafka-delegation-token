### Introduction
Apache Kafka just released delegation token support in version 1.1.0: https://cwiki.apache.org/confluence/display/KAFKA/KIP-48+Delegation+token+support+for+Kafka.

Because there is no example application I've created one to validate it. It contains mainly 2 applications:
  * kafka-consumer
    * Connects to kafka broker using keytab file
    * Creates a delegation token
    * Saves the token into `/tmp/jaas_delegation_token.conf`
    * Starts a consumer
    * Renews delegation token time to time
  * kafka-producer
    * Connects to kafka broker using delegation token (reads `/tmp/jaas_delegation_token.conf`)
    * Starts a producer
    * Sends data with the following pattern: `streamtest-%i`

Please note: delegation token renewal can be done only until it's max date!
This is one week per default configured on the broker but it can be shortened on the client side with `CreateDelegationTokenOptions.maxlifeTimeMs`.

### Build the app
To build, you need Scala 2.11, git and maven on the box.
Do a git clone of this repo and then run:
```
cd kafka-delegation-token
mvn clean package
```

#### Creating kafka topic
The kafka topic has to be created where the result can be stored:
```
kafka-topics --create --zookeeper <zk-node>:2181 --topic test --partitions 4 --replication-factor 3
```

### Running the app
```
export KDC_HOST="__REPLACE_ME__"
export KERBEROS_REALM="__REPLACE_ME__"
export BOOTSTRAP_SERVERS="__REPLACE_ME__:9092"
export PROTOCOL="SASL_PLAINTEXT"
export KEYTAB="user.keytab"
export PRINCIPAL="user@MY.DOMAIN.COM"
export TOPIC="test"
java -Djava.security.krb5.kdc=${KDC_HOST}:88 -Djava.security.krb5.realm=${KERBEROS_REALM} -jar kafka-consumer/target/kafka-consumer-0.0.1-SNAPSHOT.jar ${BOOTSTRAP_SERVERS} ${PROTOCOL} ${KEYTAB} ${PRINCIPAL} ${TOPIC} 120000
java -Djava.security.auth.login.config=/tmp/jaas_delegation_token.conf -jar kafka-producer/target/kafka-producer-0.0.1-SNAPSHOT.jar ${BOOTSTRAP_SERVERS} ${PROTOCOL} ${TOPIC}
```

### What you should see

## kafka-consumer
```
>>> 18/09/07 13:15:45 INFO consumer.SecureKafkaConsumer$: JAAS config: com.sun.security.auth.module.Krb5LoginModule required useKeyTab=true storeKey=true serviceName="kafka" keyTab="user.keytab" principal="user@MY.DOMAIN.COM";
>>> 18/08/31 17:02:20 INFO consumer.SecureKafkaConsumer$: Creating AdminClient config properties...
>>> 18/08/31 17:02:20 INFO consumer.SecureKafkaConsumer$: OK
>>> 18/08/31 17:02:20 INFO consumer.SecureKafkaConsumer$: Creating AdminClient...
>>> 18/08/31 17:02:20 INFO admin.AdminClientConfig: AdminClientConfig values:
	bootstrap.servers = [host.MY.DOMAIN.COM:9092]
	client.id =
	connections.max.idle.ms = 300000
	metadata.max.age.ms = 300000
	metric.reporters = []
	metrics.num.samples = 2
	metrics.recording.level = INFO
	metrics.sample.window.ms = 30000
	receive.buffer.bytes = 65536
	reconnect.backoff.max.ms = 1000
	reconnect.backoff.ms = 50
	request.timeout.ms = 120000
	retries = 5
	retry.backoff.ms = 100
	sasl.client.callback.handler.class = null
	sasl.jaas.config = null
	sasl.kerberos.kinit.cmd = /usr/bin/kinit
	sasl.kerberos.min.time.before.relogin = 60000
	sasl.kerberos.service.name = null
	sasl.kerberos.ticket.renew.jitter = 0.05
	sasl.kerberos.ticket.renew.window.factor = 0.8
	sasl.login.callback.handler.class = null
	sasl.login.class = null
	sasl.login.refresh.buffer.seconds = 300
	sasl.login.refresh.min.period.seconds = 60
	sasl.login.refresh.window.factor = 0.8
	sasl.login.refresh.window.jitter = 0.05
	sasl.mechanism = GSSAPI
	security.protocol = SASL_PLAINTEXT
	send.buffer.bytes = 131072
	ssl.cipher.suites = null
	ssl.enabled.protocols = [TLSv1.2, TLSv1.1, TLSv1]
	ssl.endpoint.identification.algorithm = https
	ssl.key.password = null
	ssl.keymanager.algorithm = SunX509
	ssl.keystore.location = null
	ssl.keystore.password = null
	ssl.keystore.type = JKS
	ssl.protocol = TLS
	ssl.provider = null
	ssl.secure.random.implementation = null
	ssl.trustmanager.algorithm = PKIX
	ssl.truststore.location = null
	ssl.truststore.password = null
	ssl.truststore.type = JKS

>>> 18/08/31 17:02:21 INFO authenticator.AbstractLogin: Successfully logged in.
>>> 18/08/31 17:02:21 INFO kerberos.KerberosLogin: [Principal=user@MY.DOMAIN.COM]: TGT refresh thread started.
>>> 18/08/31 17:02:21 INFO kerberos.KerberosLogin: [Principal=user@MY.DOMAIN.COM]: TGT valid starting at: Fri Aug 31 17:02:20 CEST 2018
>>> 18/08/31 17:02:21 INFO kerberos.KerberosLogin: [Principal=user@MY.DOMAIN.COM]: TGT expires: Sun Sep 30 17:02:20 CEST 2018
>>> 18/08/31 17:02:21 INFO kerberos.KerberosLogin: [Principal=user@MY.DOMAIN.COM]: TGT refresh sleeping until: Tue Sep 25 19:01:27 CEST 2018
>>> 18/08/31 17:02:21 INFO utils.AppInfoParser: Kafka version : 2.0.0
>>> 18/08/31 17:02:21 INFO utils.AppInfoParser: Kafka commitId : 3402a8361b734732
>>> 18/08/31 17:02:21 INFO consumer.SecureKafkaConsumer$: OK
>>> 18/08/31 17:02:21 INFO consumer.SecureKafkaConsumer$: Creating token...
>>> 18/08/31 17:02:25 INFO consumer.SecureKafkaConsumer$: OK
>>> 18/08/31 17:02:25 INFO consumer.SecureKafkaConsumer$: TOKENID         HMAC                           OWNER           RENEWERS                  ISSUEDATE       EXPIRYDATE      MAXDATE
>>> 18/08/31 17:02:25 INFO consumer.SecureKafkaConsumer$: 1mRgHeVaTlqiFQ4pOjKNwg S4xBEvTcTlFwfX7U2iiAStaHsvOc7eBzGYxQC+vrP3ITBMXSWRYXu0H7hR6cL1LVcFyHADsIwy/gRjNPoaF9fQ== User:systest    []                        2018-08-31T17:02 2018-09-01T17:02 2018-09-07T17:02
>>> 18/08/31 17:02:25 INFO consumer.SecureKafkaConsumer$: Writing token to file /tmp/jaas_delegation_token.conf...
>>> 18/08/31 17:02:25 INFO consumer.SecureKafkaConsumer$: OK
>>> 18/08/31 17:02:25 INFO consumer.SecureKafkaConsumer$: Token renewal will happen every 120000 ms
>>> 18/08/31 17:02:25 INFO consumer.SecureKafkaConsumer$: Creating consumer config properties...
>>> 18/08/31 17:02:25 INFO consumer.SecureKafkaConsumer$: Renewing token...
>>> 18/08/31 17:02:25 INFO consumer.SecureKafkaConsumer$: OK
>>> 18/08/31 17:02:25 INFO consumer.SecureKafkaConsumer$: Creating kafka consumer...
>>> 18/08/31 17:02:25 INFO consumer.ConsumerConfig: ConsumerConfig values:
	auto.commit.interval.ms = 5000
	auto.offset.reset = latest
	bootstrap.servers = [host.MY.DOMAIN.COM:9092]
	check.crcs = true
	client.id =
	connections.max.idle.ms = 540000
	default.api.timeout.ms = 60000
	enable.auto.commit = true
	exclude.internal.topics = true
	fetch.max.bytes = 52428800
	fetch.max.wait.ms = 500
	fetch.min.bytes = 1
	group.id = 0e3c0d45-a367-4add-b20a-95ed9c40c341
	heartbeat.interval.ms = 3000
	interceptor.classes = []
	internal.leave.group.on.close = true
	isolation.level = read_uncommitted
	key.deserializer = class org.apache.kafka.common.serialization.StringDeserializer
	max.partition.fetch.bytes = 1048576
	max.poll.interval.ms = 300000
	max.poll.records = 500
	metadata.max.age.ms = 300000
	metric.reporters = []
	metrics.num.samples = 2
	metrics.recording.level = INFO
	metrics.sample.window.ms = 30000
	partition.assignment.strategy = [class org.apache.kafka.clients.consumer.RangeAssignor]
	receive.buffer.bytes = 65536
	reconnect.backoff.max.ms = 1000
	reconnect.backoff.ms = 50
	request.timeout.ms = 30000
	retry.backoff.ms = 100
	sasl.client.callback.handler.class = null
	sasl.jaas.config = null
	sasl.kerberos.kinit.cmd = /usr/bin/kinit
	sasl.kerberos.min.time.before.relogin = 60000
	sasl.kerberos.service.name = null
	sasl.kerberos.ticket.renew.jitter = 0.05
	sasl.kerberos.ticket.renew.window.factor = 0.8
	sasl.login.callback.handler.class = null
	sasl.login.class = null
	sasl.login.refresh.buffer.seconds = 300
	sasl.login.refresh.min.period.seconds = 60
	sasl.login.refresh.window.factor = 0.8
	sasl.login.refresh.window.jitter = 0.05
	sasl.mechanism = GSSAPI
	security.protocol = SASL_PLAINTEXT
	send.buffer.bytes = 131072
	session.timeout.ms = 10000
	ssl.cipher.suites = null
	ssl.enabled.protocols = [TLSv1.2, TLSv1.1, TLSv1]
	ssl.endpoint.identification.algorithm = https
	ssl.key.password = null
	ssl.keymanager.algorithm = SunX509
	ssl.keystore.location = null
	ssl.keystore.password = null
	ssl.keystore.type = JKS
	ssl.protocol = TLS
	ssl.provider = null
	ssl.secure.random.implementation = null
	ssl.trustmanager.algorithm = PKIX
	ssl.truststore.location = null
	ssl.truststore.password = null
	ssl.truststore.type = JKS
	value.deserializer = class org.apache.kafka.common.serialization.StringDeserializer

>>> 18/08/31 17:02:25 INFO utils.AppInfoParser: Kafka version : 2.0.0
>>> 18/08/31 17:02:25 INFO utils.AppInfoParser: Kafka commitId : 3402a8361b734732
>>> 18/08/31 17:02:25 INFO consumer.SecureKafkaConsumer$: OK
>>> 18/08/31 17:02:26 INFO consumer.SecureKafkaConsumer$: OK
>>> 18/08/31 17:02:26 INFO consumer.SecureKafkaConsumer$: New expiry date: 2018-08-31T17:05
>>> 18/08/31 17:02:28 INFO clients.Metadata: Cluster ID: OM6oMsuqT-u-3UiS-gbyvQ
>>> 18/08/31 17:02:28 INFO internals.AbstractCoordinator: [Consumer clientId=consumer-1, groupId=0e3c0d45-a367-4add-b20a-95ed9c40c341] Discovered group coordinator host.MY.DOMAIN.COM:9092 (id: 2147483646 rack: null)
>>> 18/08/31 17:02:28 INFO internals.ConsumerCoordinator: [Consumer clientId=consumer-1, groupId=0e3c0d45-a367-4add-b20a-95ed9c40c341] Revoking previously assigned partitions []
>>> 18/08/31 17:02:28 INFO internals.AbstractCoordinator: [Consumer clientId=consumer-1, groupId=0e3c0d45-a367-4add-b20a-95ed9c40c341] (Re-)joining group
>>> 18/08/31 17:02:32 INFO internals.AbstractCoordinator: [Consumer clientId=consumer-1, groupId=0e3c0d45-a367-4add-b20a-95ed9c40c341] Successfully joined group with generation 1
>>> 18/08/31 17:02:32 INFO internals.ConsumerCoordinator: [Consumer clientId=consumer-1, groupId=0e3c0d45-a367-4add-b20a-95ed9c40c341] Setting newly assigned partitions [test-0]
>>> 18/08/31 17:02:35 INFO internals.Fetcher: [Consumer clientId=consumer-1, groupId=0e3c0d45-a367-4add-b20a-95ed9c40c341] Resetting offset for partition test-0 to offset 17.
>>> 18/08/31 17:04:25 INFO consumer.SecureKafkaConsumer$: Renewing token...
>>> 18/08/31 17:04:26 INFO consumer.SecureKafkaConsumer$: OK
>>> 18/08/31 17:04:26 INFO consumer.SecureKafkaConsumer$: New expiry date: 2018-08-31T17:07
>>> 18/08/31 17:05:02 INFO consumer.SecureKafkaConsumer$: ConsumerRecord(topic = test, partition = 0, offset = 17, CreateTime = 1535727900702, serialized key size = -1, serialized value size = 12, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-0)
>>> 18/08/31 17:05:02 INFO consumer.SecureKafkaConsumer$: ConsumerRecord(topic = test, partition = 0, offset = 18, CreateTime = 1535727901715, serialized key size = -1, serialized value size = 12, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-1)
```

## kafka-producer
```
>>> 18/08/31 17:04:58 INFO producer.SecureKafkaProducer$: Creating producer config properties...
>>> 18/08/31 17:04:58 INFO producer.SecureKafkaProducer$: OK
>>> 18/08/31 17:04:58 INFO producer.SecureKafkaProducer$: Creating kafka producer...
>>> 18/08/31 17:04:58 INFO producer.ProducerConfig: ProducerConfig values:
	acks = 1
	batch.size = 16384
	bootstrap.servers = [host.MY.DOMAIN.COM:9092]
	buffer.memory = 33554432
	client.id =
	compression.type = none
	connections.max.idle.ms = 540000
	enable.idempotence = false
	interceptor.classes = []
	key.serializer = class org.apache.kafka.common.serialization.StringSerializer
	linger.ms = 0
	max.block.ms = 60000
	max.in.flight.requests.per.connection = 5
	max.request.size = 1048576
	metadata.max.age.ms = 300000
	metric.reporters = []
	metrics.num.samples = 2
	metrics.recording.level = INFO
	metrics.sample.window.ms = 30000
	partitioner.class = class org.apache.kafka.clients.producer.internals.DefaultPartitioner
	receive.buffer.bytes = 32768
	reconnect.backoff.max.ms = 1000
	reconnect.backoff.ms = 50
	request.timeout.ms = 30000
	retries = 0
	retry.backoff.ms = 100
	sasl.client.callback.handler.class = null
	sasl.jaas.config = null
	sasl.kerberos.kinit.cmd = /usr/bin/kinit
	sasl.kerberos.min.time.before.relogin = 60000
	sasl.kerberos.service.name = kafka
	sasl.kerberos.ticket.renew.jitter = 0.05
	sasl.kerberos.ticket.renew.window.factor = 0.8
	sasl.login.callback.handler.class = null
	sasl.login.class = null
	sasl.login.refresh.buffer.seconds = 300
	sasl.login.refresh.min.period.seconds = 60
	sasl.login.refresh.window.factor = 0.8
	sasl.login.refresh.window.jitter = 0.05
	sasl.mechanism = SCRAM-SHA-256
	security.protocol = SASL_PLAINTEXT
	send.buffer.bytes = 131072
	ssl.cipher.suites = null
	ssl.enabled.protocols = [TLSv1.2, TLSv1.1, TLSv1]
	ssl.endpoint.identification.algorithm = https
	ssl.key.password = null
	ssl.keymanager.algorithm = SunX509
	ssl.keystore.location = null
	ssl.keystore.password = null
	ssl.keystore.type = JKS
	ssl.protocol = TLS
	ssl.provider = null
	ssl.secure.random.implementation = null
	ssl.trustmanager.algorithm = PKIX
	ssl.truststore.location = null
	ssl.truststore.password = null
	ssl.truststore.type = JKS
	transaction.timeout.ms = 60000
	transactional.id = null
	value.serializer = class org.apache.kafka.common.serialization.StringSerializer

>>> 18/08/31 17:04:58 INFO authenticator.AbstractLogin: Successfully logged in.
>>> 18/08/31 17:04:58 INFO utils.AppInfoParser: Kafka version : 2.0.0
>>> 18/08/31 17:04:58 INFO utils.AppInfoParser: Kafka commitId : 3402a8361b734732
>>> 18/08/31 17:04:58 INFO producer.SecureKafkaProducer$: OK
>>> 18/08/31 17:04:58 INFO producer.SecureKafkaProducer$: Sending record: ProducerRecord(topic=test, partition=null, headers=RecordHeaders(headers = [], isReadOnly = false), key=null, value=streamtest-0, timestamp=null)
>>> 18/08/31 17:05:00 INFO clients.Metadata: Cluster ID: OM6oMsuqT-u-3UiS-gbyvQ
>>> 18/08/31 17:05:00 INFO producer.SecureKafkaProducer$: OK
>>> 18/08/31 17:05:01 INFO producer.SecureKafkaProducer$: Sending record: ProducerRecord(topic=test, partition=null, headers=RecordHeaders(headers = [], isReadOnly = false), key=null, value=streamtest-1, timestamp=null)
>>> 18/08/31 17:05:01 INFO producer.SecureKafkaProducer$: OK
```
