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

public class SqsSinkTask extends SinkTask {

    private SqsClient sqsClient;
    private String queueUrl;

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

        // Configurar el cliente SQS con credenciales opcionales
        if (accessKey != null && secretKey != null) {
            sqsClient = SqsClient.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                    .build();
        } else {
            // Usa el proveedor de credenciales predeterminado de AWS si no se proporcionan las credenciales
            sqsClient = SqsClient.builder()
                    .region(Region.of(region))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();
        }

        queueUrl = sqsClient.getQueueUrl(builder -> builder.queueName(queueName)).queueUrl();
    }

    @Override
    public void put(Collection<SinkRecord> records) {
        for (SinkRecord record : records) {
            String messageBody = record.value() != null ? record.value().toString() : "";

            // Validar que el mensaje no sea vacío antes de enviarlo a SQS
            if (!messageBody.isEmpty()) {
                SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(messageBody)
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
        // Cerrar el cliente de SQS cuando se detenga el conector
        if (sqsClient != null) {
            sqsClient.close();
        }
    }
}
