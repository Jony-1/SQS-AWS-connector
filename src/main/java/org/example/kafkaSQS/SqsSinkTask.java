package org.example.kafkaSQS;

import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ProxyConfiguration;

import software.amazon.awssdk.regions.Region;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class SqsSinkTask extends SinkTask {

    private SqsClient sqsClient;
    private String queueUrl;

    public SqsSinkTask() {
        super();
    }

    @Override
    public String version() {
        return "1.2";
    }

    @Override
    public void start(Map<String, String> props) {
        String accessKey = props.get(SqsConnectorConfig.AWS_ACCESS_KEY);
        String secretKey = props.get(SqsConnectorConfig.AWS_SECRET_KEY);
        String region = props.get(SqsConnectorConfig.AWS_REGION);
        String queueUrlFromConfig = props.get(SqsConnectorConfig.SQS_QUEUE_URL);
        String queueName = props.get(SqsConnectorConfig.SQS_QUEUE_NAME);
        String proxyHost = props.get(SqsConnectorConfig.PROXY_HOST);
        Integer proxyPort = props.containsKey(SqsConnectorConfig.PROXY_PORT)
                ? Integer.parseInt(props.get(SqsConnectorConfig.PROXY_PORT))
                : null;

        // Configurar el proxy para ApacheHttpClient
        ProxyConfiguration proxyConfig = null;
        if (proxyHost != null && proxyPort != null) {
            proxyConfig = ProxyConfiguration.builder()
                    .endpoint(URI.create("http://" + proxyHost + ":" + proxyPort))
                    .build();
        }

        SdkHttpClient httpClient = ApacheHttpClient.builder()
                .proxyConfiguration(proxyConfig)
                .build();

        SqsClientBuilder sqsClientBuilder = SqsClient.builder()
                .region(Region.of(region))
                .httpClient(httpClient);

        // Configuración de credenciales
        AwsCredentialsProvider credentialsProvider;
        if (accessKey != null && secretKey != null) {
            credentialsProvider = StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
            );
        } else {
            credentialsProvider = DefaultCredentialsProvider.create();
        }

        sqsClientBuilder.credentialsProvider(credentialsProvider);

        sqsClient = sqsClientBuilder.build();

        // Configurar la URL de la cola SQS
        if (queueUrlFromConfig != null && !queueUrlFromConfig.isEmpty()) {
            this.queueUrl = queueUrlFromConfig;
        } else if (queueName != null && !queueName.isEmpty()) {
            this.queueUrl = sqsClient.getQueueUrl(builder -> builder.queueName(queueName)).queueUrl();
        } else {
            throw new IllegalArgumentException("Debe proporcionar una URL de cola SQS o un nombre de cola.");
        }
    }

    @Override
    public void put(Collection<SinkRecord> records) {
        for (SinkRecord record : records) {
            String messageBody = record.value() != null ? record.value().toString() : "";

            if (!messageBody.isEmpty()) {
                String messageGroupId = "default-group"; // Puedes personalizar este valor
                String messageDeduplicationId = record.key() != null ? record.key().toString() : UUID.randomUUID().toString();

                SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(messageBody)
                        .messageGroupId(messageGroupId)
                        .messageDeduplicationId(messageDeduplicationId)
                        .build();

                sqsClient.sendMessage(sendMsgRequest);
                System.out.println("Mensaje enviado: " + messageBody);
            } else {
                System.out.println("Mensaje vacío, omitiendo envío.");
            }
        }
    }

    @Override
    public void stop() {
        if (sqsClient != null) {
            sqsClient.close();
        }
    }
}
