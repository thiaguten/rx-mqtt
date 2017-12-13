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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import br.com.thiaguten.rx.mqtt.api.RxMqttToken;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PahoRxMqttCallbackTest {

  @Test
  public void whenConnectionLostOccurs() {
    PahoRxMqttCallback rxMqttCallback = spy(PahoRxMqttCallback.create(cause -> {}, (recon, uri) -> {}, t -> {}));

    PahoRxMqttException exception = new PahoRxMqttException(
        new MqttException(MqttException.REASON_CODE_CONNECTION_LOST));

    ArgumentCaptor<Throwable> onConnectionLostCauseArgumentCaptor = ArgumentCaptor.forClass(Throwable.class);

    rxMqttCallback.connectionLost(exception);

    verify(rxMqttCallback).connectionLost(onConnectionLostCauseArgumentCaptor.capture());

    assertThat(onConnectionLostCauseArgumentCaptor.getValue()).isNotNull();
    assertThat(onConnectionLostCauseArgumentCaptor.getValue()).isInstanceOf(PahoRxMqttException.class);
    assertThat(onConnectionLostCauseArgumentCaptor.getValue()).hasCauseInstanceOf(MqttException.class);
    assertThat(onConnectionLostCauseArgumentCaptor.getValue()).isEqualTo(exception);
  }

  @Test
  public void whenConnectCompleteOccurs() {
    PahoRxMqttCallback rxMqttCallback = spy(PahoRxMqttCallback.create(cause -> {}, (r, u) -> {}, t -> {}));

    boolean reconnect = true;
    String brokerUri = "tcp://localhost:1883";

    ArgumentCaptor<Boolean> onConnectCompleteReconnectArgumentCaptor = ArgumentCaptor.forClass(Boolean.class);
    ArgumentCaptor<String> onConnectCompleteServerUriArgumentCaptor = ArgumentCaptor.forClass(String.class);

    rxMqttCallback.connectComplete(reconnect, brokerUri);

    verify(rxMqttCallback).connectComplete(
        onConnectCompleteReconnectArgumentCaptor.capture(),
        onConnectCompleteServerUriArgumentCaptor.capture());

    assertThat(onConnectCompleteReconnectArgumentCaptor.getValue()).isNotNull();
    assertThat(onConnectCompleteReconnectArgumentCaptor.getValue()).isEqualTo(reconnect);
    assertThat(onConnectCompleteServerUriArgumentCaptor.getValue()).isNotNull();
    assertThat(onConnectCompleteServerUriArgumentCaptor.getValue()).isEqualTo(brokerUri);
  }

  @Test
  public void whenDeliveryCompleteOccurs() {
    PahoRxMqttCallback rxMqttCallback = spy(PahoRxMqttCallback.create(cause -> {}, (r, u) -> {}));

    IMqttDeliveryToken deliveryToken = new MqttDeliveryToken();

    RxMqttToken rxMqttToken = new PahoRxMqttToken(deliveryToken);

    ArgumentCaptor<IMqttDeliveryToken> onDeliveryCompleteTokenArgumentCaptor = ArgumentCaptor.forClass(IMqttDeliveryToken.class);
    ArgumentCaptor<RxMqttToken> onDeliveryCompleteRxTokenArgumentCaptor = ArgumentCaptor.forClass(RxMqttToken.class);

    rxMqttCallback.deliveryComplete(deliveryToken);
    rxMqttCallback.deliveryComplete(rxMqttToken);

    verify(rxMqttCallback).deliveryComplete(onDeliveryCompleteTokenArgumentCaptor.capture());
    verify(rxMqttCallback, times(2)).deliveryComplete(onDeliveryCompleteRxTokenArgumentCaptor.capture());

    assertThat(onDeliveryCompleteTokenArgumentCaptor.getValue()).isNotNull();
    assertThat(onDeliveryCompleteTokenArgumentCaptor.getValue()).isExactlyInstanceOf(MqttDeliveryToken.class);
    assertThat(onDeliveryCompleteTokenArgumentCaptor.getValue()).isEqualTo(deliveryToken);

    assertThat(onDeliveryCompleteRxTokenArgumentCaptor.getValue()).isNotNull();
    assertThat(onDeliveryCompleteRxTokenArgumentCaptor.getValue()).isExactlyInstanceOf(PahoRxMqttToken.class);
    assertThat(onDeliveryCompleteRxTokenArgumentCaptor.getValue()).isEqualTo(rxMqttToken);
  }

  @Test
  public void whenMessageArrived() throws Exception {
    PahoRxMqttCallback rxMqttCallback = spy(PahoRxMqttCallback.create(cause -> {}, (r, u) -> {}, t -> {}));

    String topic = "topic";
    MqttMessage message = new MqttMessage();

    ArgumentCaptor<String> onMessageArrivedTopicArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<MqttMessage> onMessageArrivedMessageArgumentCaptor = ArgumentCaptor.forClass(MqttMessage.class);

    rxMqttCallback.messageArrived(topic, message);

    verify(rxMqttCallback).messageArrived(onMessageArrivedTopicArgumentCaptor.capture(), onMessageArrivedMessageArgumentCaptor.capture());

    assertThat(onMessageArrivedTopicArgumentCaptor.getValue()).isNotNull();
    assertThat(onMessageArrivedTopicArgumentCaptor.getValue()).isEqualTo(topic);

    assertThat(onMessageArrivedMessageArgumentCaptor.getValue()).isNotNull();
    assertThat(onMessageArrivedMessageArgumentCaptor.getValue()).isEqualTo(message);
  }

}
