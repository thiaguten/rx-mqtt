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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import br.com.thiaguten.rx.mqtt.api.RxMqttClient;
import br.com.thiaguten.rx.mqtt.api.RxMqttMessage;
import br.com.thiaguten.rx.mqtt.api.RxMqttQoS;
import br.com.thiaguten.rx.mqtt.api.RxMqttToken;
import io.reactivex.observers.TestObserver;
import java.nio.charset.StandardCharsets;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class PahoRxMqttClientPublishTest {

  @Test(expected = NullPointerException.class)
  public void whenNullTopicIsPassedThenThrowsAnError() {
    RxMqttMessage mqttMessage = PahoRxMqttMessage.create("payload");
    assertThat(mqttMessage).isNotNull();
    PahoRxMqttClient rxClient = PahoRxMqttClient.builder("tcp://localhost:1883").build();
    rxClient.publish(null, mqttMessage).blockingGet();
  }

  @Test(expected = NullPointerException.class)
  public void whenNullMessageIsPassedThenThrowsAnError() {
    PahoRxMqttClient rxClient = PahoRxMqttClient.builder("tcp://localhost:1883").build();
    rxClient.publish("topic", null).blockingGet();
  }

  @Test(expected = IllegalArgumentException.class)
  public void whenInvalidQosIsPassedThenThrowsAnError() {
    RxMqttMessage mqttMessage = PahoRxMqttMessage.create("payload", RxMqttQoS.valueOf(10));
    assertThat(mqttMessage).isNotNull();
    PahoRxMqttClient rxClient = PahoRxMqttClient.builder("tcp://localhost:1883").build();
    rxClient.publish("topic", mqttMessage).blockingGet();
  }

  @Test
  public void whenPublishIsCalledThenPublishSuccessfully() throws MqttException {
    int qos = 2;
    String topic = "foo";
    boolean retain = false;
    String clientId = "clientId";
    byte[] payload = "test".getBytes(StandardCharsets.UTF_8);

    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertThat(client).isNotNull();
    when(client.isConnected()).thenReturn(true);
    when(client.getClientId()).thenReturn(clientId);

    IMqttActionListener publishActionListener = mock(IMqttActionListener.class);
    assertThat(publishActionListener).isNotNull();

    IMqttDeliveryToken publishDeliveryToken = mock(IMqttDeliveryToken.class);
    assertThat(publishDeliveryToken).isNotNull();
    when(publishDeliveryToken.isComplete()).thenReturn(true);
    when(publishDeliveryToken.getMessage()).thenReturn(null);
    when(publishDeliveryToken.getClient()).thenReturn(client);
    when(publishDeliveryToken.getException()).thenReturn(null);
    when(publishDeliveryToken.getUserContext()).thenReturn(null);
    when(publishDeliveryToken.getGrantedQos()).thenReturn(new int[] {qos});
    when(publishDeliveryToken.getTopics()).thenReturn(new String[] {topic});
    when(publishDeliveryToken.getActionCallback()).thenReturn(publishActionListener);
    when(publishDeliveryToken.getClient().getClientId()).thenReturn(clientId);

    publishActionListener.onSuccess(publishDeliveryToken);

    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertThat(rxClient).isNotNull();
    assertTrue(rxClient.isConnected().blockingGet());

    RxMqttMessage mqttMessage = PahoRxMqttMessage.create(payload, RxMqttQoS.valueOf(qos), retain);
    assertThat(mqttMessage).isNotNull();

    TestObserver<RxMqttToken> testObserver = rxClient.publish(topic, mqttMessage).test();
    testObserver.assertSubscribed();
    testObserver.assertNoErrors();

    verify(client).isConnected();
    verify(client).publish(eq(topic), eq(payload), eq(qos), eq(retain), isNull(), any(IMqttActionListener.class));
    verifyNoMoreInteractions(client);

    ArgumentCaptor<IMqttDeliveryToken> publishDeliveryTokenArgumentCaptor = ArgumentCaptor.forClass(IMqttDeliveryToken.class);
    verify(publishActionListener).onSuccess(publishDeliveryTokenArgumentCaptor.capture());
    assertThat(publishDeliveryTokenArgumentCaptor).isNotNull();
    assertThat(publishDeliveryTokenArgumentCaptor.getValue()).isInstanceOf(IMqttDeliveryToken.class);
    assertThat(publishDeliveryTokenArgumentCaptor.getValue().isComplete()).isEqualTo(publishDeliveryToken.isComplete());
    assertThat(publishDeliveryTokenArgumentCaptor.getValue().getMessage()).isEqualTo(publishDeliveryToken.getMessage());
    assertThat(publishDeliveryTokenArgumentCaptor.getValue().getClient()).isEqualTo(publishDeliveryToken.getClient());
    assertThat(publishDeliveryTokenArgumentCaptor.getValue().getException()).isEqualTo(publishDeliveryToken.getException());
    assertThat(publishDeliveryTokenArgumentCaptor.getValue().getUserContext()).isEqualTo(publishDeliveryToken.getUserContext());
    assertThat(publishDeliveryTokenArgumentCaptor.getValue().getGrantedQos()).isEqualTo(publishDeliveryToken.getGrantedQos());
    assertThat(publishDeliveryTokenArgumentCaptor.getValue().getTopics()).isEqualTo(publishDeliveryToken.getTopics());
    assertThat(publishDeliveryTokenArgumentCaptor.getValue().getActionCallback()).isEqualTo(publishDeliveryToken.getActionCallback());
    assertThat(publishDeliveryTokenArgumentCaptor.getValue().getClient().getClientId()).isEqualTo(publishDeliveryToken.getClient().getClientId());
    verifyNoMoreInteractions(publishActionListener);
  }

  @Test
  public void whenPublishIsCalledAndClientIsNotConnectedThenThrowException() throws MqttException {
    int qos = 2;
    String topic = "foo";
    boolean retain = false;
    byte[] payload = "test".getBytes(StandardCharsets.UTF_8);

    RxMqttMessage message = PahoRxMqttMessage.create(payload, RxMqttQoS.valueOf(qos), retain);
    assertThat(message).isNotNull();

    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertThat(client).isNotNull();
    when(client.isConnected()).thenReturn(false);

    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertThat(rxClient).isNotNull();
    assertThat(rxClient.isConnected().blockingGet()).isFalse();
    when(client.publish(eq(topic), eq(payload), eq(qos), eq(retain), isNull(), any(IMqttActionListener.class)))
        .thenThrow(new PahoRxMqttException(new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED)));

    TestObserver<RxMqttToken> testObserver = rxClient.publish(topic, message).test();
    testObserver.assertSubscribed();
    testObserver.assertError(PahoRxMqttException.class);

    verify(client).publish(eq(topic), eq(payload), eq(qos), eq(retain), isNull(), any(IMqttActionListener.class));
    verify(client).isConnected();
    verifyNoMoreInteractions(client);
  }
}
