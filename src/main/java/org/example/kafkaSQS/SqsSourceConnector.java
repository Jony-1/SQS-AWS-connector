package org.example.kafkaSQS;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;

import java.util.List;
import java.util.Map;

public class SqsSourceConnector extends SourceConnector {

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
        return SqsSourceTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
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
