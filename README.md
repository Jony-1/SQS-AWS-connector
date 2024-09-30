
# Kafka-SQS Connector

Este proyecto es un **conector de Kafka para AWS SQS** que permite el envío de mensajes desde un tópico de Kafka hacia una cola FIFO de AWS SQS. Utiliza el SDK de AWS (versión 2.x) para interactuar con SQS y se integra con Kafka Connect para manejar la lógica de flujo de datos.

## Características

- **Kafka a SQS (Sink Connector)**: Consume mensajes de un tópico de Kafka y los envía a una cola FIFO de AWS SQS.
- Soporta colas FIFO de SQS con **MessageGroupId** y **MessageDeduplicationId**.
- Compatible con Kafka Connect para despliegue en modo distribuido o independiente.
- Compatible con deduplicación basada en contenido o deduplicación explícita.

## Requisitos

Antes de ejecutar el proyecto, asegúrate de tener lo siguiente instalado:

- **Java 11 o superior**: El proyecto requiere Java 11 para compilar y ejecutarse.
- **Apache Kafka**: Se necesita un clúster de Kafka con Kafka Connect habilitado.
- **AWS Account**: Necesitas una cuenta de AWS con acceso a Amazon SQS y las credenciales adecuadas.

## Configuración

### Dependencias

Este proyecto utiliza Maven para la gestión de dependencias. Asegúrate de tener las siguientes dependencias incluidas en el archivo `pom.xml`:

```xml
<dependencies>
    <!-- Kafka Connect API -->
    <dependency>
      <groupId>org.apache.kafka</groupId>
      <artifactId>connect-api</artifactId>
      <version>3.8.0</version>
    </dependency>

    <!-- AWS SDK para SQS -->
    <dependency>
      <groupId>software.amazon.awssdk</groupId>
      <artifactId>sqs</artifactId>
      <version>2.28.6</version>
    </dependency>

    <!-- AWS SDK para autenticación -->
    <dependency>
      <groupId>software.amazon.awssdk</groupId>
      <artifactId>auth</artifactId>
      <version>2.28.6</version>
    </dependency>

    <!-- SLF4J para logging -->
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-api</artifactId>
      <version>1.7.30</version>
    </dependency>
</dependencies>
```

### Compilación del proyecto

Para compilar el proyecto, asegúrate de tener Maven instalado. Luego, ejecuta el siguiente comando en la raíz del proyecto:

```bash
mvn clean package
```

Esto generará un **JAR sombreado** (`uber-jar`) que contendrá todas las dependencias necesarias para ejecutar el conector.

### Configuración de AWS SQS

1. **Crear una cola FIFO en AWS SQS**: Ve a la consola de AWS y crea una cola FIFO. Asegúrate de que la cola tenga el nombre que deseas utilizar, y toma nota de la **URL** de la cola.

2. **Habilitar `ContentBasedDeduplication` (opcional)**: Si prefieres que AWS SQS maneje automáticamente la deduplicación de mensajes, habilita esta opción en la configuración de la cola.

3. **Obtener credenciales de AWS**: Asegúrate de tener las credenciales de acceso (`aws.access.key` y `aws.secret.key`) que tengan permisos para interactuar con SQS.

### Configuración del Conector Kafka

1. **Agregar el JAR a Kafka Connect**: Una vez que el proyecto esté empaquetado, copia el JAR generado a la carpeta de plugins de Kafka Connect.

2. **Crear un archivo de configuración para el conector** (`sink-connector.json`):

```json
{
  "name": "SqsSinkConnector",
  "config": {
    "connector.class": "org.example.kafkaSQS.SqsSinkConnector",
    "tasks.max": "1",
    "topics": "your-kafka-topic",  
    "sqs.queue.url": "https://sqs.us-east-2.amazonaws.com/your-account-id/your-queue-name.fifo",  
    "aws.access.key": "your-aws-access-key",
    "aws.secret.key": "your-aws-secret-key",
    "aws.region": "us-east-2",
    "sqs.queue.name": "your-queue-name.fifo",
    "message.group.id": "default-group",  // Personaliza según tu lógica
    "message.deduplication.id": "kafka-key" // O usa ContentBasedDeduplication si está habilitado
  }
}
```

3. **Desplegar el conector**:

   Usa el siguiente comando para registrar el conector en Kafka Connect:

```bash
curl -X POST -H "Content-Type: application/json" --data @sink-connector.json http://localhost:8083/connectors
```

### Opciones de Configuración Adicionales

- **message.group.id**: Identificador para agrupar mensajes en colas FIFO. Los mensajes dentro del mismo grupo se entregan en orden.
- **message.deduplication.id**: Si `ContentBasedDeduplication` no está habilitado en la cola, este campo debe ser proporcionado.

### Ejecución

Una vez que el conector esté desplegado, comenzará a consumir mensajes desde el tópico Kafka especificado y los enviará a la cola FIFO de SQS. Los registros y errores se pueden monitorear desde Kafka Connect o la consola de AWS.

## Ejemplo de Mensajes

A continuación, un ejemplo de mensaje JSON que puede ser enviado desde Kafka:

```json
{
  "id": "123",
  "name": "John Doe",
  "email": "john.doe@example.com",
  "timestamp": "2023-09-24T12:34:56Z"
}
```

Este mensaje será enviado a la cola FIFO de SQS y deduplicado según la configuración que elijas.

## Errores Comunes

1. **Error: The request must contain the parameter MessageGroupId**:
   - Asegúrate de que estás proporcionando el parámetro `MessageGroupId` en el conector o que estás configurando correctamente el `ContentBasedDeduplication` en SQS.

2. **Error: MessageDeduplicationId is required**:
   - Si `ContentBasedDeduplication` no está habilitado, asegúrate de que cada mensaje tiene un `MessageDeduplicationId`.

## Licencia

Este proyecto está licenciado bajo los términos de la licencia MIT.
