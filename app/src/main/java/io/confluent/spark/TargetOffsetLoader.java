package io.confluent.spark;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TargetOffsetLoader {
    Logger logger = LoggerFactory.getLogger(App.class);

    public TargetOffsetLoader(String topic, Properties props) throws Exception {
        this.topic = topic;
        this.connectorName = props.getProperty("srm.mig.target.connector.name");
        this.keyFormat = errorIfEmpty(props.getProperty("srm.mig.target.key.format"));        
        this.clusterName = props.getProperty("srm.mig.target.cluster.name");

        if( isNullOrEmpty(this.clusterName)) {
            if(this.keyFormat.indexOf("CLUSTERNAME") < 0 ) {
                this.clusterName = null;
            } else {
                throw new Exception("key format has '$CLUSTERNAME' buth 'srm.mig.target.cluster.name' in properties file is empty");
            }
        }

    }

    private String errorIfEmpty(String s) throws Exception {
        if ( isNullOrEmpty(s) ) {
            throw new Exception("'" + s +"' in properties file is empty");
        } 
        return s;
    }

    private boolean isNullOrEmpty(String s) {
        return s== null || s.trim().length() == 0;
    }

    
    private Map<TopicPartition, String> keyMap = new Hashtable<TopicPartition,String>();

    public Map<String, String> loadTargetKeyMap(Properties props, int partitionCount) {
        Map<String, String> map = new HashMap<String,String>();

        props.put(ConsumerConfig.GROUP_ID_CONFIG, "srm-mig-load-key");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        ArrayList<TopicPartition> partitions = new ArrayList<TopicPartition>();
        for( int i = 0 ; i < partitionCount; i++ ) {
            TopicPartition tp = new TopicPartition(topic, i);
            partitions.add(tp);
        }
        consumer.endOffsets(partitions);
        consumer.subscribe(Arrays.asList(topic));
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, String> record : records) {
                System.out.printf("key = %s, value = %s%n", record.key(), record.value());
            }
            if( true ) {
                break;
            }
        }

        consumer.close(Duration.ofSeconds(10));
        return map;
    }

    public Map<TopicPartition, Long> getLasteOffsets(Properties props) {
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "srm-mig-load-key");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");


        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        final int numPartitions = consumer.partitionsFor(topic).size();

        ArrayList<TopicPartition> partitions = new ArrayList<TopicPartition>();
        logger.info("#### Number of Partitions : " + numPartitions);

        for( int i = 0 ; i < numPartitions; i++ ) {
            partitions.add(new TopicPartition(topic, i));
        }
        Map<TopicPartition, Long> map = consumer.endOffsets(partitions, Duration.ofMinutes(1));

        consumer.close();

        return map;
    }

    private boolean isFinished(boolean[] b) {
        for( boolean f : b) {
            if( !f ) return false;
        }
        System.out.printf("-----finished\n");

        return true;
    }

    public void loaddOffsets(Properties props, Map<TopicPartition, Long> offsetMap) {
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "srm-mig-offset2");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");


        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        consumer.subscribe(Arrays.asList(this.topic));
        boolean[] doesFinish = new boolean[offsetMap.size()];

        for( int i = 0 ; i < doesFinish.length; i++ ) {
            if( offsetMap.get(new TopicPartition(this.topic, i)) == 0 ) {
                doesFinish[i] = true;
            }
        }

        boolean needInit = true;
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            if( needInit ) {
                needInit = false;
                for( int i = 0 ; i < doesFinish.length; i++ ) {
                    TopicPartition tp = new TopicPartition(topic, i);
                    consumer.seek(tp, 0);
                }
                continue;
            }
            for (ConsumerRecord<String, String> record : records) {
                String key = record.key();

                LocalTopicPartition ltp = keyToTopicPartition(key);

                if( ltp != null && !keyMap.containsKey(ltp.getTopicPartition()) ) {
                    System.out.printf("Topic Partition %s is new \n", ltp.toString());
                    keyMap.put(ltp.getTopicPartition(), key);
                }
                
                Long offset = offsetMap.get(new TopicPartition(this.topic, record.partition()));
                if( record.offset() + 1 >= offset ) {
                    doesFinish[record.partition()]= true;
                    System.out.printf("--+--- offset = %s, partition = %s\n", record.offset(), offset);
                } else {
                    System.out.printf("------ offset = %s, partition = %s\n", record.offset(), offset);
                }              
            }
            if( isFinished(doesFinish)) {
                break;
            }
        }
        
        consumer.close(Duration.ofSeconds(10));

        for (TopicPartition keySet : keyMap.keySet()) {
            System.out.printf("topic = %s partition = %s : key = %s \n", keySet.topic(), keySet.partition(), keyMap.get(keySet));
        }
    }

    ObjectMapper mapper =  new ObjectMapper();

    // JSONParser parser = new JSONParser();
    private LocalTopicPartition keyToTopicPartition(String key) {        
        try {
            Object[] objs = mapper.readValue(key, Object[].class);
            if( objs.length == 2 ) {
                LinkedHashMap hmap = (LinkedHashMap)objs[1];
                if( hmap != null ) {
                    Object topicObj = hmap.get("topic");
                    Object partitionObj = hmap.get("partition");
                    Object cluster = hmap.get("cluster");
                    if( topicObj != null && partitionObj != null ) {
                        LocalTopicPartition ltp = new LocalTopicPartition((String)topicObj, (int)partitionObj, (cluster==null)?clusterName:null);
                    
                        if ( ltp.getTopic() != null ) {
                            return ltp;
                        }    
                    }
                }
            }
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null; 
    }  

    private String clusterName;
    private String connectorName;
    private String keyFormat;
    private String topic;

    public String getKey(String key)  {
        LocalTopicPartition ltp = keyToTopicPartition(key);
        if ( ltp != null ) {
            String keyStr = keyMap.get(ltp.getTopicPartition());
            if( keyStr == null ) {
                keyStr = "[\"" + connectorName + "\"," + ltp.setClusterName(clusterName).toString() + "]";
                keyMap.put(ltp.getTopicPartition(), keyStr);
            }
            return keyStr;
        }
        return null;
    }
}
