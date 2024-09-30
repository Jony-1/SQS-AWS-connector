package org.example.kafkaSQS;

import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SqsSourceTask extends SourceTask {

    private SqsClient sqsClient;
    private String queueUrl;
    private String kafkaTopics;

    @Override
    public String version() {
        return "1.0";
    }

    @Override
    public void start(Map<String, String> props) {
        String accessKey = props.get(SqsConnectorConfig.AWS_ACCESS_KEY);
        String secretKey = props.get(SqsConnectorConfig.AWS_SECRET_KEY);
        String region = props.get(SqsConnectorConfig.AWS_REGION);
        String queueName = props.get(SqsConnectorConfig.SQS_QUEUE_NAME);
        kafkaTopics = props.get(SqsConnectorConfig.KAFKA_TOPICS);  // Lista de tópicos de Kafka

        if (accessKey != null && secretKey != null) {
            sqsClient = SqsClient.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                    .build();
        } else {
            sqsClient = SqsClient.builder()
                    .region(Region.of(region))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();
        }

        queueUrl = sqsClient.getQueueUrl(builder -> builder.queueName(queueName)).queueUrl();
    }

    @Override
    public List<SourceRecord> poll() throws InterruptedException {
        List<SourceRecord> records = new ArrayList<>();

        ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(20)
                .build();

        List<Message> messages = sqsClient.receiveMessage(receiveMessageRequest).messages();

        for (Message message : messages) {
            SourceRecord record = new SourceRecord(
                    null,
                    null,
                    kafkaTopics,  // Usar los tópicos de Kafka configurados
                    null,
                    null,
                    null,
                    message.body()
            );
            records.add(record);

            sqsClient.deleteMessage(builder -> builder.queueUrl(queueUrl).receiptHandle(message.receiptHandle()));
        }

        return records;
    }

    @Override
    public void stop() {
        if (sqsClient != null) {
            sqsClient.close();
        }
    }
}
