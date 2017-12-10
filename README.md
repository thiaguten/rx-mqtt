# RxMQTT

Basically, This project has for now, has two modules:
 - RxMQTT API
 - RxMQTT Paho

RxMQTT API is a set of interfaces that was built in top of RxJava for composing asynchronous and event-based programs by using observable sequences. These interfaces define a higher abstraction for implementations.

RxMQTT Paho is the implementation of RxMQTT API that was built in top of Eclipse Paho framework to handle MQTT messages.

---

### Creating the client

The game begins by creating an instance of the PahoRxMqttClient class through some of the building methods:

```java
String clientId = "clientId";
String brokerUri = "tcp://localhost:1883";
MqttClientPersistence clientPersistence = new MemoryPersistence();
IMqttAsyncClient mqttAsyncClient = new MqttAsyncClient(brokerUri, clientId, clientPersistence);

// Simple example:

RxMqttClient client = PahoRxMqttClient.builder(brokerUri).build();
RxMqttClient client = PahoRxMqttClient.builder(brokerUri, clientId).build();
RxMqttClient client = PahoRxMqttClient.builder(brokerUri, clientId, clientPersistence).build();
RxMqttClient client = PahoRxMqttClient.builder(mqttAsyncClient).build();

// More complete example:

MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
mqttConnectOptions.setAutomaticReconnect(true);

RxMqttClient client = PahoRxMqttClient.builder(brokerUri)
  // set a custom mqtt connection options
  .setConnectOptions(mqttConnectOptions)
  // set a different top level backpressure strategy
  .setBackpressureStrategy(BackpressureStrategy.BUFFER)
  // set a client callback listener
  .setCallbackListener(PahoRxMqttCallback.create(
    cause -> {
      System.err.println("connection lost");
      cause.printStackTrace();
    },
    (reconnect, serverUri) ->
      System.out.printf(
        "connect complete to [%s] by reconnection [%s]\n", serverUri, reconnect ? "yes" : "no")))
  .build();
```

### Connect
You can archive this by calling the method:

```java
Single<RxMqttToken> connect();
```

For example:

```java
client.connect().subscribe(connectToken -> {
    // connected
    System.out.printf("client id [%s]\n", connectToken.getClientId())
  }, e -> {
    // ops! something goes wrong
  });
```

### Subscribe
You can archive this by calling some of the methods:

```java
Flowable<RxMqttMessage> on(String[] topics, RxMqttQoS[] qos);
Flowable<RxMqttMessage> on(String topic, RxMqttQoS qos);
Flowable<RxMqttMessage> on(String topic);
Flowable<RxMqttMessage> on(String[] topics, RxMqttQoS[] qos, BackpressureStrategy backpressureStrategy);
Flowable<RxMqttMessage> on(String topic, RxMqttQoS qos, BackpressureStrategy backpressureStrategy);
Flowable<RxMqttMessage> on(String topic, BackpressureStrategy backpressureStrategy);
```

For example:

```java
client.on(topic).subscribe(message -> {
    // arrived message
  }, e -> {
      // ops! something goes wrong
  });
```

### Publish
You can archive this by calling the method:

```java
Single<RxMqttToken> publish(String topic, RxMqttMessage message);
```

For example:

```java
client.publish(topic, PahoRxMqttMessage.create("message")).subscribe(publishToken -> {
    // success
  }, e -> {
    // error
  });
```

### Unsubscribe
You can archive this by calling the method:

```java
Single<RxMqttToken> off(String... topic);
```

For example:

```java
client.off(topic).subscribe(unsubscribeToken -> {
    // success
  }, e -> {
    // error
  });
```

### Disconnect
You can archive this by calling some of the methods:

```java
Single<RxMqttToken> disconnect();
Completable disconnectForcibly();
```

For example:

```java
client.disconnect().subscribe(disconnectToken -> {
    // success
  }, e -> {
    // error... try forcibly

    client.disconnectForcibly().subscribe(() -> {
      // success
    }, e1 -> {
      // :(
    });

  });
```

### Close
You can archive this by calling the method:

```java
Completable close();
```

For example:

```java
client.close().subscribe(() -> {
      // success
    }, e -> {
      // error
    });
```

### Unsubscribe, Disconnect and Close
You can archive this by calling the method:

```java
Completable offAndClose(String... topics);
```

For example:

```java
client.offAndClose(topic).subscribe(() -> {
      // success
    }, e -> {
      // error
    });
```

---

### Installation

Maven:

```xml
<!-- api -->
<dependency>
    <groupId>br.com.thiaguten</groupId>
    <artifactId>rx-mqtt-api</artifactId>
    <version>0.1.0</version>
</dependency>

<!-- implementation -->
<dependency>
    <groupId>br.com.thiaguten</groupId>
    <artifactId>rx-mqtt-paho</artifactId>
    <version>0.1.0</version>
</dependency>
```

Gradle:

```
// api
compile 'br.com.thiaguten:rx-mqtt-api:0.1.0'

// implementation
compile 'br.com.thiaguten:rx-mqtt-paho:0.1.0'
```
