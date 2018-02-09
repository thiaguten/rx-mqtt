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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import br.com.thiaguten.rx.mqtt.api.RxMqttClient;
import br.com.thiaguten.rx.mqtt.api.RxMqttMessage;
import br.com.thiaguten.rx.mqtt.api.RxMqttQoS;
import br.com.thiaguten.rx.mqtt.api.RxMqttToken;
import io.reactivex.SingleEmitter;
import io.reactivex.observers.TestObserver;
import java.nio.charset.StandardCharsets;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
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
    rxClient.publish("topic", (RxMqttMessage) null).blockingGet();
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

    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertThat(rxClient).isNotNull();

    RxMqttMessage mqttMessage = PahoRxMqttMessage.create(payload, RxMqttQoS.valueOf(qos), retain);
    assertThat(mqttMessage).isNotNull();

    TestObserver<RxMqttToken> testObserver = rxClient.publish(topic, mqttMessage).test();
    testObserver.assertSubscribed();
    testObserver.assertNoErrors();

    @SuppressWarnings("unchecked")
    SingleEmitter<RxMqttToken> publishSingleEmitter = spy(SingleEmitter.class);
    assertThat(publishSingleEmitter).isNotNull();

    IMqttActionListener publishActionListener = spy(PahoRxMqttClient.newActionListener(publishSingleEmitter));
    assertThat(publishActionListener).isNotNull();

    IMqttDeliveryToken publishDeliveryToken = mock(IMqttDeliveryToken.class);
    assertThat(publishDeliveryToken).isNotNull();
    when(publishDeliveryToken.isComplete()).thenReturn(true);
    when(publishDeliveryToken.getMessage()).thenReturn(null);
    when(publishDeliveryToken.getMessageId()).thenReturn(1);
    when(publishDeliveryToken.getClient()).thenReturn(client);
    when(publishDeliveryToken.getException()).thenReturn(null);
    when(publishDeliveryToken.getUserContext()).thenReturn(null);
    when(publishDeliveryToken.getGrantedQos()).thenReturn(new int[] {qos});
    when(publishDeliveryToken.getTopics()).thenReturn(new String[] {topic});
    when(publishDeliveryToken.getActionCallback()).thenReturn(publishActionListener);
    when(publishDeliveryToken.getClient().getClientId()).thenReturn(clientId);
    when(publishDeliveryToken.getSessionPresent()).thenReturn(false);

    RxMqttToken rxToken = mock(RxMqttToken.class);
    assertThat(rxToken).isNotNull();
    when(rxToken.isComplete()).thenReturn(true);
    when(rxToken.getMessage()).thenReturn(null);
    when(rxToken.getMessageId()).thenReturn(1);
    when(rxToken.getException()).thenReturn(null);
    when(rxToken.getGrantedQos()).thenReturn(new int[] {qos});
    when(rxToken.getTopics()).thenReturn(new String[] {topic});
    when(rxToken.getClientId()).thenReturn(clientId);
    when(rxToken.getSessionPresent()).thenReturn(false);

    publishSingleEmitter.onSuccess(rxToken);
    publishActionListener.onSuccess(publishDeliveryToken);

    when(client.isConnected()).thenReturn(true);
    when(client.getClientId()).thenReturn(clientId);
    verify(client).publish(eq(topic), eq(payload), eq(qos), eq(retain), isNull(), any(IMqttActionListener.class));
    assertTrue(rxClient.isConnected().blockingGet());
    verify(client).isConnected();
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

    ArgumentCaptor<RxMqttToken> rxPublishDeliveryTokenArgumentCaptor = ArgumentCaptor.forClass(RxMqttToken.class);
    verify(publishSingleEmitter, times(2)).onSuccess(rxPublishDeliveryTokenArgumentCaptor.capture());
    assertThat(rxPublishDeliveryTokenArgumentCaptor).isNotNull();
    assertThat(rxPublishDeliveryTokenArgumentCaptor.getValue()).isExactlyInstanceOf(PahoRxMqttToken.class);
    assertThat(rxPublishDeliveryTokenArgumentCaptor.getValue().isComplete()).isEqualTo(publishDeliveryToken.isComplete());
    assertThat(rxPublishDeliveryTokenArgumentCaptor.getValue().isComplete()).isEqualTo(rxToken.isComplete());
    assertThat(rxPublishDeliveryTokenArgumentCaptor.getValue().getMessage()).isEqualTo(publishDeliveryToken.getMessage());
    assertThat(rxPublishDeliveryTokenArgumentCaptor.getValue().getMessage()).isEqualTo(rxToken.getMessage());
    assertThat(rxPublishDeliveryTokenArgumentCaptor.getValue().getMessageId()).isEqualTo(publishDeliveryToken.getMessageId());
    assertThat(rxPublishDeliveryTokenArgumentCaptor.getValue().getMessageId()).isEqualTo(rxToken.getMessageId());
    assertThat(rxPublishDeliveryTokenArgumentCaptor.getValue().getException()).isEqualTo(publishDeliveryToken.getException());
    assertThat(rxPublishDeliveryTokenArgumentCaptor.getValue().getException()).isEqualTo(rxToken.getException());
    assertThat(rxPublishDeliveryTokenArgumentCaptor.getValue().getGrantedQos()).isEqualTo(publishDeliveryToken.getGrantedQos());
    assertThat(rxPublishDeliveryTokenArgumentCaptor.getValue().getGrantedQos()).isEqualTo(rxToken.getGrantedQos());
    assertThat(rxPublishDeliveryTokenArgumentCaptor.getValue().getTopics()).isEqualTo(publishDeliveryToken.getTopics());
    assertThat(rxPublishDeliveryTokenArgumentCaptor.getValue().getTopics()).isEqualTo(rxToken.getTopics());
    assertThat(rxPublishDeliveryTokenArgumentCaptor.getValue().getClientId()).isEqualTo(publishDeliveryToken.getClient().getClientId());
    assertThat(rxPublishDeliveryTokenArgumentCaptor.getValue().getClientId()).isEqualTo(rxToken.getClientId());
    assertThat(rxPublishDeliveryTokenArgumentCaptor.getValue().getSessionPresent()).isEqualTo(publishDeliveryToken.getSessionPresent());
    assertThat(rxPublishDeliveryTokenArgumentCaptor.getValue().getSessionPresent()).isEqualTo(rxToken.getSessionPresent());
    verifyNoMoreInteractions(publishActionListener);
  }

  @Test
  public void whenPublishIsCalledAndClientIsNotConnectedThenThrowException() throws MqttException {
    int qos = 2;
    String topic = "foo";
    boolean retain = false;
    byte[] payload = "test".getBytes(StandardCharsets.UTF_8);

    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertThat(client).isNotNull();

    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertThat(rxClient).isNotNull();

    IMqttToken token = mock(IMqttToken.class);
    assertThat(token).isNotNull();

    when(token.getException()).thenReturn(new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED));

    PahoRxMqttException exception = new PahoRxMqttException(token);

    doThrow(exception).when(client).publish(eq(topic), eq(payload), eq(qos), eq(retain), isNull(), any(IMqttActionListener.class));

    RxMqttMessage message = PahoRxMqttMessage.create(payload, RxMqttQoS.valueOf(qos), retain);
    assertThat(message).isNotNull();

    TestObserver<RxMqttToken> testObserver = rxClient.publish(topic, message).test();
    testObserver.assertSubscribed();
    testObserver.assertError(PahoRxMqttException.class);

    @SuppressWarnings("unchecked")
    SingleEmitter<RxMqttToken> emitter = mock(SingleEmitter.class);

    IMqttActionListener publishActionListener = spy(PahoRxMqttClient.newActionListener(emitter));
    assertThat(publishActionListener).isNotNull();

    emitter.onError(exception);
    publishActionListener.onFailure(token, exception);

    when(client.isConnected()).thenReturn(false);
    verify(client).publish(eq(topic), eq(payload), eq(qos), eq(retain), isNull(), any(IMqttActionListener.class));
    assertThat(rxClient.isConnected().blockingGet()).isFalse();
    verify(client).isConnected();
    verifyNoMoreInteractions(client);

    ArgumentCaptor<Throwable> publishThrowableArgumentCaptor = ArgumentCaptor.forClass(Throwable.class);
    verify(publishActionListener).onFailure(eq(token), publishThrowableArgumentCaptor.capture());
    assertThat(publishThrowableArgumentCaptor).isNotNull();
    assertThat(publishThrowableArgumentCaptor.getValue()).isExactlyInstanceOf(PahoRxMqttException.class);
    assertThat(publishThrowableArgumentCaptor.getValue()).hasCauseExactlyInstanceOf(MqttException.class);
    verifyNoMoreInteractions(publishActionListener);

    ArgumentCaptor<PahoRxMqttException> emitterThrowableArgumentCaptor = ArgumentCaptor.forClass(PahoRxMqttException.class);
    verify(emitter, times(2)).onError(emitterThrowableArgumentCaptor.capture());
    assertThat(emitterThrowableArgumentCaptor).isNotNull();
    assertThat(emitterThrowableArgumentCaptor.getValue()).isExactlyInstanceOf(PahoRxMqttException.class);
    assertThat(emitterThrowableArgumentCaptor.getValue()).hasCauseExactlyInstanceOf(PahoRxMqttException.class);
    assertThat(emitterThrowableArgumentCaptor.getValue().getToken()).isEqualTo(token);
    verifyNoMoreInteractions(emitter);
  }

}
