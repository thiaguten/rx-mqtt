# RxMQTT

[![Build Status](https://travis-ci.org/thiaguten/rx-mqtt.svg?branch=master)](https://travis-ci.org/thiaguten/rx-mqtt)
[![Coverage Status](https://coveralls.io/repos/github/thiaguten/rx-mqtt/badge.svg?branch=master)](https://coveralls.io/github/thiaguten/rx-mqtt?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/br.com.thiaguten/rx-mqtt-paho/badge.svg)](http://search.maven.org/#search|gav|1|g:"br.com.thiaguten"%20AND%20a:"rx-mqtt-paho")
[![Javadocs](http://www.javadoc.io/badge/br.com.thiaguten/rx-mqtt-paho.svg)](http://www.javadoc.io/doc/br.com.thiaguten/rx-mqtt-paho)
[![License](https://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

Basically, this project has two modules:

 - RxMQTT API
 - RxMQTT Paho

RxMQTT API is a set of interfaces that was built on top of RxJava for composing asynchronous and event-based programs by using observable sequences. These interfaces define a higher abstraction for implementations.

RxMQTT Paho is the implementation of RxMQTT API that was built on top of Eclipse Paho framework to handle MQTT messages.

---

### Creating the client

It all starts by using one of the methods below to create an instance of PahoRxMqttClient class.

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
You can achieve this by using the following method:

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
You can achieve this by using one of the following methods:

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
You can achieve this by using the following method:

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
You can achieve this by using the following method:

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
You can achieve this by using one of the following methods:

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
You can achieve this by using the following method:

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
You can achieve this by using the following method:

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
<dependency>
    <groupId>br.com.thiaguten</groupId>
    <artifactId>rx-mqtt-paho</artifactId>
    <version>0.1.0</version>
</dependency>
```

Gradle:

```gradle
compile 'br.com.thiaguten:rx-mqtt-paho:0.1.0'
```

### Todos

 - Write more tests
 - Write javadocs
