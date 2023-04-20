package io.confluent.spark;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class StreamExecutionContext {
    private static Map<String,String> serdeConfig;

    public static void setSerdesConfig(Properties config){
        serdeConfig = new HashMap<String,String>();
        for (final String name: config.stringPropertyNames())
            serdeConfig.put(name, config.getProperty(name));
    }

    public static Map<String,String> getSerdesConfig(){
        return serdeConfig;
    }
}
