/*
 * Copyright 2017 Thiago Gutenberg Carvalho da Costa <thiaguten@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package br.com.thiaguten.rx.mqtt.paho;

import br.com.thiaguten.rx.mqtt.api.RxMqttMessage;
import br.com.thiaguten.rx.mqtt.api.RxMqttQoS;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class PahoRxMqttMessage implements RxMqttMessage {

  private String topic;
  private final MqttMessage mqttMessage;

  public PahoRxMqttMessage(byte[] payload) {
    this(payload, RxMqttQoS.EXACTLY_ONCE);
  }

  public PahoRxMqttMessage(byte[] payload, RxMqttQoS qos) {
    this(payload, qos, false);
  }

  public PahoRxMqttMessage(byte[] payload, RxMqttQoS qos, boolean retained) {
    MqttMessage mqttMessage = new MqttMessage(payload);
    mqttMessage.setQos(qos.value());
    mqttMessage.setRetained(retained);
    this.mqttMessage = mqttMessage;
  }

  public PahoRxMqttMessage(MqttMessage mqttMessage) {
    this.mqttMessage = Objects.requireNonNull(mqttMessage);
  }

  @Override
  public String getTopic() {
    return topic;
  }

  void setTopic(String topic) {
    this.topic = topic;
  }

  //@VisibleForTesting - using the same package in test  for test visibility
  MqttMessage getMqttMessage() {
    return mqttMessage;
  }

  @Override
  public int getId() {
    return mqttMessage.getId();
  }

  @Override
  public byte[] getPayload() {
    return mqttMessage.getPayload();
  }

  @Override
  public RxMqttQoS getQoS() {
    return RxMqttQoS.valueOf(mqttMessage.getQos());
  }

  @Override
  public boolean isRetained() {
    return mqttMessage.isRetained();
  }

  @Override
  public String toString() {
    return mqttMessage.toString();
  }

  // convenient methods

  public static PahoRxMqttMessage create() {
    return create(new byte[0]);
  }

  public static PahoRxMqttMessage create(MqttMessage message) {
    return new PahoRxMqttMessage(message);
  }

  public static PahoRxMqttMessage create(String payload) {
    return create(payload, StandardCharsets.UTF_8);
  }

  public static PahoRxMqttMessage create(String payload, RxMqttQoS qos) {
    return create(payload, StandardCharsets.UTF_8, qos);
  }

  public static PahoRxMqttMessage create(String payload, RxMqttQoS qos, boolean retained) {
    return create(payload, StandardCharsets.UTF_8, qos, retained);
  }

  public static PahoRxMqttMessage create(String payload, Charset charset) {
    return create(payload.getBytes(charset));
  }

  public static PahoRxMqttMessage create(String payload, Charset charset, RxMqttQoS qos) {
    return create(payload.getBytes(charset), qos);
  }

  public static PahoRxMqttMessage create(
      String payload, Charset charset, RxMqttQoS qos, boolean retained) {
    return create(payload.getBytes(charset), qos, retained);
  }

  public static PahoRxMqttMessage create(byte[] payload) {
    return new PahoRxMqttMessage(payload);
  }

  public static PahoRxMqttMessage create(byte[] payload, RxMqttQoS qos) {
    return new PahoRxMqttMessage(payload, qos);
  }

  public static PahoRxMqttMessage create(byte[] payload, RxMqttQoS qos, boolean retained) {
    return new PahoRxMqttMessage(payload, qos, retained);
  }

}
