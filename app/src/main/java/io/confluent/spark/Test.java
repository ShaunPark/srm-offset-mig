package io.confluent.spark;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.connect.storage.Converter;
import org.apache.kafka.connect.storage.OffsetStorageReader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.utils.Utils;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.json.JsonConverter;
import org.apache.kafka.connect.json.JsonConverterConfig;
import org.apache.kafka.connect.runtime.isolation.DelegatingClassLoader;

public class Test {
    // protected DelegatingClassLoader newDelegatingClassLoader(final List<String> paths) {
    //     return new DelegatingClassLoader(paths);
    // }

    // public Converter newInternalConverter(boolean isKey, String className, Map<String, String> converterConfig) {
    //     Converter plugin;
    //     plugin = new JsonConverter();
    //     plugin.configure(converterConfig, isKey);
    //     return plugin;
    // }
    Map<String, String> internalConverterConfig = Collections.singletonMap(JsonConverterConfig.SCHEMAS_ENABLE_CONFIG, "false");

    String convertToString(Map<String, Object> m, String namespace) {
        try (JsonConverter c = new JsonConverter();){
            c.configure(internalConverterConfig, true);    
            return new String(c.fromConnectData(namespace, null, Arrays.asList(namespace, m)));
        }   
        
        // ConnectorOffsetBackingStore a;
    }

    public static void main(String[] args) {
        // String key = "{\"cluster\":\"Cluster\",\"topic\":\"london\",\"partition\":2}";
        String key = "{\"topic\":\"london\",\"partition\":2}";
        ObjectMapper mapper =  new ObjectMapper();
        try {
            LocalTopicPartition tp = mapper.readValue(key, LocalTopicPartition.class);
            System.out.println(tp.toString());
        } catch (JsonMappingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // Test t = new Test();
        // Map<String, Object> t1 = Test.wrapPartition("migtest.m16.topic", "0", "m16cdc");
        // Map<String, Object> t2 = Test.wrapPartition("migtest3.m16.topic", "230", "m16cdc");

        // System.out.println(t.convertToString(t1, "cp-replicator"));ne
        // System.out.println(t.convertToString(t2, "MMMMMMDMDMFMDFMD"));
    }

    static Map<String, Object> wrapPartition(String topic,  String partition, String sourceClusterAlias) {
        Map<String, Object> wrapped = new HashMap<>();
        wrapped.put("partition", partition);
        wrapped.put("topic", topic);
        wrapped.put("cluster", sourceClusterAlias);
        return wrapped;
    }


}
