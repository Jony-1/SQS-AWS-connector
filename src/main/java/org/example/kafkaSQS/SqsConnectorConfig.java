package org.example.kafkaSQS;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;

import java.util.Map;

public class SqsConnectorConfig extends AbstractConfig {

    public static final String SQS_QUEUE_NAME = "sqs.queue.name";
    public static final String AWS_ACCESS_KEY = "aws.access.key";
    public static final String AWS_SECRET_KEY = "aws.secret.key";
    public static final String AWS_REGION = "aws.region";  // Región de AWS
    public static final String KAFKA_TOPIC = "topic";  // Tópico de Kafka para origen o destino

    public SqsConnectorConfig(Map<String, String> originals) {
        super(config(), originals);
    }

    public static ConfigDef config() {
        return new ConfigDef()
                .define(SQS_QUEUE_NAME, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, "Name of the SQS Queue")
                .define(AWS_ACCESS_KEY, ConfigDef.Type.STRING, null, ConfigDef.Importance.MEDIUM, "AWS Access Key (optional)")
                .define(AWS_SECRET_KEY, ConfigDef.Type.STRING, null, ConfigDef.Importance.MEDIUM, "AWS Secret Key (optional)")
                .define(AWS_REGION, ConfigDef.Type.STRING, "us-east-1", ConfigDef.Importance.MEDIUM, "AWS Region (optional)")
                .define(KAFKA_TOPIC, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, "Kafka topic to write to or read from");
    }
}
