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
    private String kafkaTopic;  // El tema de Kafka será configurable

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
        kafkaTopic = props.get("topic");  // Lee el tema de Kafka desde las propiedades de configuración

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
    public List<SourceRecord> poll() throws InterruptedException {
        List<SourceRecord> records = new ArrayList<>();

        // Recibir mensajes desde la cola SQS
        ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)  // El número máximo de mensajes a recibir
                .waitTimeSeconds(20)       // Espera hasta 20 segundos para recibir un mensaje
                .build();

        List<Message> messages = sqsClient.receiveMessage(receiveMessageRequest).messages();

        for (Message message : messages) {
            // Crear un registro de Kafka con los datos del mensaje de SQS
            SourceRecord record = new SourceRecord(
                    null,  // No necesitamos particionar
                    null,  // No hay un "offset" para la cola SQS
                    kafkaTopic,  // Usamos el tema de Kafka configurado
                    null,  // Tipo de partición
                    null,  // Tipo de clave
                    null,  // Clave (si es necesario)
                    message.body()       // El cuerpo del mensaje será el valor del registro en Kafka
            );
            records.add(record);

            // Eliminar el mensaje de la cola una vez que ha sido procesado
            sqsClient.deleteMessage(builder -> builder.queueUrl(queueUrl).receiptHandle(message.receiptHandle()));
        }

        return records;
    }

    @Override
    public void stop() {
        // Cierra el cliente de SQS
        if (sqsClient != null) {
            sqsClient.close();
        }
    }
}
