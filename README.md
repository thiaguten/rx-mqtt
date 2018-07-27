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

RxMQTT Paho is the implementation of RxMQTT API that was built on top of Eclipse Paho Java Client framework to handle MQTT messages.

For more informations, see the [Wiki Page](https://github.com/thiaguten/rx-mqtt/wiki).

---

### Installation

Maven:

```xml
<dependency>
    <groupId>br.com.thiaguten</groupId>
    <artifactId>rx-mqtt-paho</artifactId>
    <version>${version}</version>
</dependency>
```

Gradle:

```gradle
compile "br.com.thiaguten:rx-mqtt-paho:$version"
```

### Todos

 - Write javadocs
