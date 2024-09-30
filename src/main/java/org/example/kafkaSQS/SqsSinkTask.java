package org.example.kafkaSQS;

import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class SqsSinkTask extends SinkTask {

    private SqsClient sqsClient;
    private String queueUrl;
    private String topics;

    @Override
    public String version() {
        return "1.0";
    }

    @Override
    public void start(Map<String, String> props) {
        String accessKey = props.get(SqsConnectorConfig.AWS_ACCESS_KEY);
        String secretKey = props.get(SqsConnectorConfig.AWS_SECRET_KEY);
        String region = props.get(SqsConnectorConfig.AWS_REGION);
        String queueUrlFromConfig = props.get(SqsConnectorConfig.SQS_QUEUE_URL);
        String queueName = props.get(SqsConnectorConfig.SQS_QUEUE_NAME);
        topics = props.get(SqsConnectorConfig.KAFKA_TOPICS);  // Lista de tópicos de Kafka

        if (topics == null || topics.isEmpty()) {
            throw new IllegalArgumentException("Debe proporcionar al menos un tópico de Kafka.");
        }

        // Configurar el cliente SQS con credenciales opcionales
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

        // Si la URL de la cola SQS está en la configuración, úsala directamente. De lo contrario, busca la URL con el nombre de la cola.
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
                // Añadir MessageGroupId, requerido para colas FIFO de SQS
                String messageGroupId = "default-group"; // Puedes personalizar este valor según tu lógica

                // Añadir MessageDeduplicationId, requerido si ContentBasedDeduplication no está habilitado
                String messageDeduplicationId = record.key() != null ? record.key().toString() : UUID.randomUUID().toString();

                SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(messageBody)
                        .messageGroupId(messageGroupId)  // Incluye el MessageGroupId
                        .messageDeduplicationId(messageDeduplicationId)  // Incluye el MessageDeduplicationId
                        .build();

                // Enviar el mensaje a SQS
                sqsClient.sendMessage(sendMsgRequest);
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
