package io.confluent.spark;
import java.util.Objects;

import org.apache.kafka.common.TopicPartition;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true) 
public class LocalTopicPartition  {
    private String topic;
    private int partition;
    private String cluster;
    private int hash = 0;


    public LocalTopicPartition() {}
    public LocalTopicPartition(String topic, int partition, String cluster) {
        this.topic = topic;
        this.partition = partition;
        this.cluster = cluster;
    }

    public String getTopic() {
        return topic;
    }

    public int getPartition() {
        return partition;
    }

    public  String cluster() {
        return cluster;
    }

    public String toString() {
        if( cluster == null ) {
            return String.format("{\"topic\"=\"%s\",\"partition\"=%s}", this.topic, "" + this.partition);
        } else {
            return String.format("{\"cluster\"=\"%s\",\"topic\"=\"%s\",\"partition\"=%s}", this.cluster, this.topic, "" + this.partition);
        }
    }

    public TopicPartition getTopicPartition(){
        return new TopicPartition(this.topic, this.partition);
    }
    public Object setClusterName(String clusterName) {
        this.cluster = clusterName;
        return this;
    }

    @Override
    public int hashCode() {
        if (hash != 0)
            return hash;
        final int prime = 31;
        int result = prime + partition;
        result = prime * result + Objects.hashCode(topic);
        this.hash = result;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
            LocalTopicPartition other = (LocalTopicPartition) obj;
        return partition == other.partition && Objects.equals(topic, other.topic);
    }

}
