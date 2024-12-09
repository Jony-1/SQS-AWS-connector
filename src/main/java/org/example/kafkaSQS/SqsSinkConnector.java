package org.example.kafkaSQS;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SqsSinkConnector extends SinkConnector {

    private SqsConnectorConfig config;

    @Override
    public String version() {
        return "1.2";
    }

    @Override
    public void start(Map<String, String> props) {
        config = new SqsConnectorConfig(props);
    }

    @Override
    public Class<? extends Task> taskClass() {
        return SqsSinkTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        // Pasar la configuración para las tareas
        List<Map<String, String>> configs = new ArrayList<>();
        Map<String, String> originals = config.originalsStrings();
        for (int i = 0; i < maxTasks; i++) {
            configs.add(originals);
        }
        return configs;
    }

    @Override
    public void stop() {
        // Lógica para detener el conector
    }

    @Override
    public ConfigDef config() {
        return SqsConnectorConfig.config();
    }
}
