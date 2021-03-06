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

import static org.assertj.core.api.Assertions.assertThat;

import br.com.thiaguten.rx.mqtt.api.RxMqttQoS;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PahoRxMqttMessageTest {

  @Test(expected = NullPointerException.class)
  public void whenANullMessageIsSuppliedThenAnExceptionIsThrown() {
      PahoRxMqttMessage.create((MqttMessage) null);
  }

  @Test(expected = NullPointerException.class)
  public void whenANullStringMessageIsSuppliedThenAnExceptionIsThrown() {
    PahoRxMqttMessage.create((String) null);
  }

  @Test(expected = NullPointerException.class)
  public void whenANullByteArrayMessageIsSuppliedThenAnExceptionIsThrown() {
    PahoRxMqttMessage.create((byte[]) null);
  }

  @Test
  public void whenANothingIsSuppliedThenCreateSuccessfully() {
    String message = "";
    byte[] payload = new byte[0];

    PahoRxMqttMessage rxMqttMessage = PahoRxMqttMessage.create();
    assertThat(rxMqttMessage).isNotNull();

    assertThat(rxMqttMessage.getPayload()).isEqualTo(payload);
    assertThat(rxMqttMessage.toString()).isEqualTo(message);
  }

  @Test
  public void whenAMessageIsSuppliedThenCreateSuccessfully() {
    String topic = "topic";
    MqttMessage mqttMessage = new MqttMessage("message".getBytes());
    mqttMessage.setQos(0);
    mqttMessage.setRetained(false);
    mqttMessage.setId(1);

    PahoRxMqttMessage rxMqttMessage = PahoRxMqttMessage.create(mqttMessage);
    rxMqttMessage.setTopic(topic);
    assertThat(rxMqttMessage).isNotNull();

    assertThat(rxMqttMessage.getId()).isEqualTo(mqttMessage.getId());
    assertThat(rxMqttMessage.getQoS()).isEqualTo(RxMqttQoS.valueOf(mqttMessage.getQos()));
    assertThat(rxMqttMessage.getTopic()).isEqualTo(topic);
    assertThat(rxMqttMessage.getPayload()).isEqualTo(mqttMessage.getPayload());
    assertThat(rxMqttMessage.isRetained()).isEqualTo(mqttMessage.isRetained());
    assertThat(rxMqttMessage.getMqttMessage()).isEqualTo(mqttMessage);
    assertThat(rxMqttMessage.toString()).isEqualTo(mqttMessage.toString());
  }

  @Test
  public void whenAStringIsSuppliedThenCreateSuccessfully() {
    String message = "message";
    byte[] payload = message.getBytes();

    PahoRxMqttMessage rxMqttMessage = PahoRxMqttMessage.create(message);
    assertThat(rxMqttMessage).isNotNull();

    assertThat(rxMqttMessage.getId()).isZero();
    assertThat(rxMqttMessage.getQoS()).isEqualTo(RxMqttQoS.EXACTLY_ONCE);
    assertThat(rxMqttMessage.getPayload()).isEqualTo(payload);
    assertThat(rxMqttMessage.toString()).isEqualTo(message);
  }

  @Test
  public void whenAStringAndCharsetIsSuppliedThenCreateSuccessfully() {
    String message = "message";
    byte[] payload = message.getBytes();
    Charset charset = StandardCharsets.ISO_8859_1;

    PahoRxMqttMessage rxMqttMessage = PahoRxMqttMessage.create(message, charset);
    assertThat(rxMqttMessage).isNotNull();

    assertThat(rxMqttMessage.getId()).isZero();
    assertThat(rxMqttMessage.getQoS()).isEqualTo(RxMqttQoS.EXACTLY_ONCE);
    assertThat(rxMqttMessage.getPayload()).isEqualTo(payload);
    assertThat(rxMqttMessage.toString()).isEqualTo(message);
  }

  @Test
  public void whenAStringAndQosIsSuppliedThenCreateSuccessfully() {
    String message = "message";
    byte[] payload = message.getBytes();
    RxMqttQoS qos = RxMqttQoS.EXACTLY_ONCE;

    PahoRxMqttMessage rxMqttMessage = PahoRxMqttMessage.create(message, qos);
    assertThat(rxMqttMessage).isNotNull();

    assertThat(rxMqttMessage.getId()).isZero();
    assertThat(rxMqttMessage.getQoS()).isEqualTo(qos);
    assertThat(rxMqttMessage.getPayload()).isEqualTo(payload);
    assertThat(rxMqttMessage.toString()).isEqualTo(message);
  }

  @Test
  public void whenAByteArrayAndQosIsSuppliedThenCreateSuccessfully() {
    String message = "message";
    byte[] payload = message.getBytes();
    RxMqttQoS qos = RxMqttQoS.EXACTLY_ONCE;

    PahoRxMqttMessage rxMqttMessage = PahoRxMqttMessage.create(payload, qos);
    assertThat(rxMqttMessage).isNotNull();

    assertThat(rxMqttMessage.getId()).isZero();
    assertThat(rxMqttMessage.getQoS()).isEqualTo(qos);
    assertThat(rxMqttMessage.getPayload()).isEqualTo(payload);
    assertThat(rxMqttMessage.toString()).isEqualTo(message);
    assertThat(rxMqttMessage.toString()).isEqualTo(message);
  }

  @Test
  public void whenAStringAndQosAndCharsetIsSuppliedThenCreateSuccessfully() {
    String message = "message";
    byte[] payload = message.getBytes();
    RxMqttQoS qos = RxMqttQoS.EXACTLY_ONCE;
    Charset charset = StandardCharsets.UTF_8;

    PahoRxMqttMessage rxMqttMessage = PahoRxMqttMessage.create(message, charset, qos);
    assertThat(rxMqttMessage).isNotNull();

    assertThat(rxMqttMessage.getId()).isZero();
    assertThat(rxMqttMessage.getQoS()).isEqualTo(qos);
    assertThat(rxMqttMessage.getPayload()).isEqualTo(payload);
    assertThat(rxMqttMessage.toString()).isEqualTo(message);
  }

  @Test
  public void whenAStringAndQosAndRetainIsSuppliedThenCreateSuccessfully() {
    boolean retain = false;
    String message = "message";
    byte[] payload = message.getBytes();
    RxMqttQoS qos = RxMqttQoS.EXACTLY_ONCE;

    PahoRxMqttMessage rxMqttMessage = PahoRxMqttMessage.create(message, qos, retain);
    assertThat(rxMqttMessage).isNotNull();

    assertThat(rxMqttMessage.getId()).isZero();
    assertThat(rxMqttMessage.getQoS()).isEqualTo(qos);
    assertThat(rxMqttMessage.getPayload()).isEqualTo(payload);
    assertThat(rxMqttMessage.isRetained()).isEqualTo(retain);
    assertThat(rxMqttMessage.toString()).isEqualTo(message);
  }

  @Test
  public void whenAStringAndQosAndCharsetAndRetainIsSuppliedThenCreateSuccessfully() {
    String topic = "topic";
    boolean retain = false;
    String message = "message";
    byte[] payload = message.getBytes();
    RxMqttQoS qos = RxMqttQoS.EXACTLY_ONCE;
    Charset charset = StandardCharsets.UTF_8;

    PahoRxMqttMessage rxMqttMessage = PahoRxMqttMessage.create(message, charset, qos, retain);
    rxMqttMessage.setTopic(topic);
    assertThat(rxMqttMessage).isNotNull();

    assertThat(rxMqttMessage.getId()).isZero();
    assertThat(rxMqttMessage.getQoS()).isEqualTo(qos);
    assertThat(rxMqttMessage.getTopic()).isEqualTo(topic);
    assertThat(rxMqttMessage.getPayload()).isEqualTo(payload);
    assertThat(rxMqttMessage.isRetained()).isEqualTo(retain);
    assertThat(rxMqttMessage.toString()).isEqualTo(message);
  }
}
