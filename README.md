```
java -jar app-0.0.1.jar
```

```
# Required connection configs for Kafka producer, consumer, and admin
bootstrap.servers=localhost:9092
client.dns.lookup=use_all_dns_ips

# Best practice for higher availability in Apache Kafka clients prior to 3.0
session.timeout.ms=45000

# Best practice for Kafka producer to prevent data loss
acks=all

max.poll.records=1000
application.id=srm-mig-offset
auto.offset.reset=earliest

srm.mig.source.topic.name=src_topic
srm.mig.target.topic.name=tgt_topic

srm.mig.target.connector.name=REPCONNECTOR
srm.mig.target.cluster.name=m16cdc
srm.mig.target.key.format=["$CONNECTORNAME",{"cluster":"$CLUSTERNAME","topic":"$TOPICNAME","partiton":$PARTITION}]
srm.mig.target.value.format={"offset":${OFFSET}}
#srm.mig.target.key.format=["${CONNECTORNAME}",{"topic":"${TOPICNAME}","partiton":${PARTITION}}]

```


https://drive.google.com/file/d/1feogygcAqAjL5MkPmLq3AL6XczSSNOAe/view?usp=sharing
