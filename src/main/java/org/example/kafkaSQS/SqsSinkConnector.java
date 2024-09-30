package org.example.kafkaSQS;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;

import java.util.List;
import java.util.Map;

public class SqsSinkConnector extends SinkConnector {

    private SqsConnectorConfig config;

    @Override
    public String version() {
        return "1.0";
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
        return List.of(config.originalsStrings());
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
