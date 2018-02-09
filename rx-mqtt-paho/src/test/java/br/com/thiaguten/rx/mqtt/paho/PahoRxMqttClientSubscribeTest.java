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
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
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
import io.reactivex.BackpressureStrategy;
import io.reactivex.FlowableEmitter;
import io.reactivex.subscribers.TestSubscriber;
import java.util.Arrays;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PahoRxMqttClientSubscribeTest {

  @Test(expected = NullPointerException.class)
  public void whenANullTopicsIsSuppliedThenAnExceptionIsThrown() {
    int[] qos = { 0, 1, 2 };
    RxMqttClient rxClient = PahoRxMqttClient.builder("tcp://localhost:1883").build();
    RxMqttQoS[] rxQos = Arrays.stream(qos).boxed().map(RxMqttQoS::valueOf).toArray(RxMqttQoS[]::new);
    rxClient.on(null, rxQos).blockingFirst();
  }

  @Test(expected = NullPointerException.class)
  public void whenANullQoSIsSuppliedThenAnExceptionIsThrown() {
    String[] topics = { "topic1", "topic2", "topic2" };
    RxMqttClient rxClient = PahoRxMqttClient.builder("tcp://localhost:1883").build();
    rxClient.on(topics, (RxMqttQoS[]) null).blockingFirst();
  }

  @Test(expected = NullPointerException.class)
  public void whenANullBackpressureStrategyIsSuppliedThenAnExceptionIsThrown() {
    int[] qos = { 0, 1, 2 };
    String[] topics = { "topic1", "topic2", "topic2" };
    RxMqttClient rxClient = PahoRxMqttClient.builder("tcp://localhost:1883").build();
    RxMqttQoS[] rxQos = Arrays.stream(qos).boxed().map(RxMqttQoS::valueOf).toArray(RxMqttQoS[]::new);
    rxClient.on(topics, rxQos, null).blockingFirst();
  }

  @Test
  public void whenClientOnIsCalledThenSubscribeSuccessfully() throws Exception {
    int[] qos = { 0, 1, 2 };
    String[] topics = { "topic1", "topic2", "topic3" };
    String message = "test";

    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertThat(client).isNotNull();

    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertThat(rxClient).isNotNull();

    RxMqttQoS[] rxQos = Arrays.stream(qos).boxed().map(RxMqttQoS::valueOf).toArray(RxMqttQoS[]::new);

    TestSubscriber<RxMqttMessage> testSubscriber = rxClient.on(topics, rxQos).test();
    testSubscriber.assertSubscribed();
    testSubscriber.assertNoErrors();

    @SuppressWarnings("unchecked")
    FlowableEmitter<RxMqttMessage> emitter = spy(FlowableEmitter.class);
    assertThat(emitter).isNotNull();

    IMqttActionListener subscribeActionListener = spy(PahoRxMqttClient.newActionListener(emitter));
    assertThat(subscribeActionListener).isNotNull();

    IMqttToken token = mock(IMqttToken.class);
    assertThat(token).isNotNull();

    RxMqttMessage rxMessage = PahoRxMqttMessage.create(message);
    assertThat(rxMessage).isNotNull();

    emitter.onNext(rxMessage);
    subscribeActionListener.onSuccess(token);

    ArgumentCaptor<IMqttMessageListener[]> messageListenersArgumentCaptor = ArgumentCaptor.forClass(IMqttMessageListener[].class);

    when(client.isConnected()).thenReturn(true);
    verify(client).subscribe(same(topics), eq(qos), isNull(), any(IMqttActionListener.class), messageListenersArgumentCaptor.capture());
    assertThat(rxClient.isConnected().blockingGet()).isTrue();
    verify(client).isConnected();
    verifyNoMoreInteractions(client);

    assertThat(messageListenersArgumentCaptor.getValue()).isNotEmpty().hasSize(3);

    ArgumentCaptor<RxMqttMessage> subscribeRxMessageArgumentCaptor = ArgumentCaptor.forClass(RxMqttMessage.class);
    verify(emitter).onNext(subscribeRxMessageArgumentCaptor.capture());
    assertThat(subscribeRxMessageArgumentCaptor).isNotNull();
    assertThat(subscribeRxMessageArgumentCaptor.getValue()).isExactlyInstanceOf(PahoRxMqttMessage.class);
    assertThat(subscribeRxMessageArgumentCaptor.getValue()).isEqualTo(rxMessage);
    verifyNoMoreInteractions(emitter);

    ArgumentCaptor<IMqttToken> subscribeTokenArgumentCaptor = ArgumentCaptor.forClass(IMqttToken.class);
    verify(subscribeActionListener).onSuccess(subscribeTokenArgumentCaptor.capture());
    assertThat(subscribeTokenArgumentCaptor).isNotNull();
    assertThat(subscribeTokenArgumentCaptor.getValue()).isInstanceOf(IMqttToken.class);
    assertThat(subscribeTokenArgumentCaptor.getValue()).isEqualTo(token);
    verifyNoMoreInteractions(subscribeActionListener);
  }

  @Test
  public void whenClientOnWithOnlyTopicIsCalledThenSubscribeSuccessfully() throws Exception {
    String[] topics = {"topic"};
    int[] qos = {2};
    String message = "test";

    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertThat(client).isNotNull();

    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertThat(rxClient).isNotNull();

    TestSubscriber<RxMqttMessage> testSubscriber = rxClient.on(topics[0]).test();
    testSubscriber.assertSubscribed();
    testSubscriber.assertNoErrors();

    @SuppressWarnings("unchecked")
    FlowableEmitter<RxMqttMessage> emitter = spy(FlowableEmitter.class);
    assertThat(emitter).isNotNull();

    IMqttActionListener subscribeActionListener = spy(PahoRxMqttClient.newActionListener(emitter));
    assertThat(subscribeActionListener).isNotNull();

    IMqttToken token = mock(IMqttToken.class);
    assertThat(token).isNotNull();

    RxMqttMessage rxMessage = PahoRxMqttMessage.create(message);
    assertThat(rxMessage).isNotNull();

    emitter.onNext(rxMessage);
    subscribeActionListener.onSuccess(token);

    ArgumentCaptor<IMqttMessageListener[]> messageListenersArgumentCaptor = ArgumentCaptor.forClass(IMqttMessageListener[].class);

    when(client.isConnected()).thenReturn(true);
    verify(client).subscribe(eq(topics), eq(qos), isNull(), any(IMqttActionListener.class), messageListenersArgumentCaptor.capture());
    assertThat(rxClient.isConnected().blockingGet()).isTrue();
    verify(client).isConnected();
    verifyNoMoreInteractions(client);

    assertThat(messageListenersArgumentCaptor.getValue()).isNotEmpty().hasSize(1);

    ArgumentCaptor<RxMqttMessage> subscribeRxMessageArgumentCaptor = ArgumentCaptor.forClass(RxMqttMessage.class);
    verify(emitter).onNext(subscribeRxMessageArgumentCaptor.capture());
    assertThat(subscribeRxMessageArgumentCaptor).isNotNull();
    assertThat(subscribeRxMessageArgumentCaptor.getValue()).isExactlyInstanceOf(PahoRxMqttMessage.class);
    assertThat(subscribeRxMessageArgumentCaptor.getValue()).isEqualTo(rxMessage);
    verifyNoMoreInteractions(emitter);

    ArgumentCaptor<IMqttToken> subscribeTokenArgumentCaptor = ArgumentCaptor.forClass(IMqttToken.class);
    verify(subscribeActionListener).onSuccess(subscribeTokenArgumentCaptor.capture());
    assertThat(subscribeTokenArgumentCaptor).isNotNull();
    assertThat(subscribeTokenArgumentCaptor.getValue()).isInstanceOf(IMqttToken.class);
    assertThat(subscribeTokenArgumentCaptor.getValue()).isEqualTo(token);
    verifyNoMoreInteractions(subscribeActionListener);
  }

  @Test
  public void whenClientOnWithTopicAndBackpressureIsCalledThenSubscribeSuccessfully() throws Exception {
    String[] topics = {"topic"};
    int[] qos = {2};
    BackpressureStrategy backpressureStrategy = BackpressureStrategy.ERROR;

    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertThat(client).isNotNull();
    when(client.isConnected()).thenReturn(true);

    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertThat(rxClient).isNotNull();

    TestSubscriber<RxMqttMessage> testSubscriber = rxClient.on(topics[0], backpressureStrategy).test();
    testSubscriber.assertSubscribed();
    testSubscriber.assertNoErrors();

    ArgumentCaptor<IMqttMessageListener[]> messageListenersArgumentCaptor = ArgumentCaptor.forClass(IMqttMessageListener[].class);

    verify(client).subscribe(eq(topics), eq(qos), isNull(), any(IMqttActionListener.class), messageListenersArgumentCaptor.capture());
    assertThat(rxClient.isConnected().blockingGet()).isTrue();
    verify(client).isConnected();
    verifyNoMoreInteractions(client);
    assertThat(messageListenersArgumentCaptor.getValue()).isNotEmpty().hasSize(1);
  }

  @Test
  public void whenClientOnWithTopicAndQosAndBackpressureIsCalledThenSubscribeSuccessfully() throws Exception {
    String[] topics = {"topic"};
    int[] qos = {2};
    BackpressureStrategy backpressureStrategy = BackpressureStrategy.ERROR;

    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertThat(client).isNotNull();
    when(client.isConnected()).thenReturn(true);

    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertThat(rxClient).isNotNull();

    TestSubscriber<RxMqttMessage> testSubscriber = rxClient.on(topics[0], RxMqttQoS.valueOf(qos[0]), backpressureStrategy).test();
    testSubscriber.assertSubscribed();
    testSubscriber.assertNoErrors();

    ArgumentCaptor<IMqttMessageListener[]> messageListenersArgumentCaptor = ArgumentCaptor.forClass(IMqttMessageListener[].class);

    verify(client).subscribe(eq(topics), eq(qos), isNull(), any(IMqttActionListener.class), messageListenersArgumentCaptor.capture());
    assertThat(rxClient.isConnected().blockingGet()).isTrue();
    verify(client).isConnected();
    verifyNoMoreInteractions(client);
    assertThat(messageListenersArgumentCaptor.getValue()).isNotEmpty().hasSize(1);
  }

  @Test
  public void whenClientOnIsCalledThenMessageArrives() throws Exception {
    int[] ids = {1};
    int[] qos = {2};
    String[] topics = {"topic"};
    byte[] payload = "payload".getBytes();

    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertThat(client).isNotNull();

    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertThat(rxClient).isNotNull();

    RxMqttQoS[] rxQos = Arrays.stream(qos).boxed().map(RxMqttQoS::valueOf).toArray(RxMqttQoS[]::new);

    TestSubscriber<RxMqttMessage> testSubscriber = rxClient.on(topics[0], rxQos[0]).test();
    testSubscriber.assertSubscribed();
    testSubscriber.assertNoErrors();

    MqttMessage expectedMessage = new MqttMessage(payload);
    expectedMessage.setId(ids[0]);
    expectedMessage.setQos(qos[0]);
    expectedMessage.setRetained(false);

    PahoRxMqttMessage expectedRxMqttMessage = PahoRxMqttMessage.create(expectedMessage);
    expectedRxMqttMessage.setTopic(topics[0]);

    @SuppressWarnings("unchecked")
    FlowableEmitter<RxMqttMessage> emitter = spy(FlowableEmitter.class);
    assertThat(emitter).isNotNull();

    IMqttMessageListener messageListener = spy(PahoRxMqttClient.newMessageListener(emitter));
    assertThat(messageListener).isNotNull();

    emitter.onNext(expectedRxMqttMessage);
    messageListener.messageArrived(topics[0], expectedMessage);

    when(client.isConnected()).thenReturn(true);
    verify(client).subscribe(eq(topics), eq(qos), isNull(), any(IMqttActionListener.class), any(IMqttMessageListener[].class));
    assertThat(rxClient.isConnected().blockingGet()).isTrue();
    verify(client).isConnected();
    verifyNoMoreInteractions(client);

    ArgumentCaptor<String> mqttMessageArrivedTopicArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<MqttMessage> mqttMessageArrivedMessageArgumentCaptor = ArgumentCaptor.forClass(MqttMessage.class);
    verify(messageListener).messageArrived(mqttMessageArrivedTopicArgumentCaptor.capture(), mqttMessageArrivedMessageArgumentCaptor.capture());
    assertThat(mqttMessageArrivedTopicArgumentCaptor.getValue()).isExactlyInstanceOf(String.class);
    assertThat(mqttMessageArrivedTopicArgumentCaptor.getValue()).isEqualTo(topics[0]);
    assertThat(mqttMessageArrivedMessageArgumentCaptor.getValue()).isExactlyInstanceOf(MqttMessage.class);
    assertThat(mqttMessageArrivedMessageArgumentCaptor.getValue()).isEqualTo(expectedMessage);
    assertThat(mqttMessageArrivedMessageArgumentCaptor.getValue().getId()).isEqualTo(expectedMessage.getId());
    assertThat(mqttMessageArrivedMessageArgumentCaptor.getValue().getQos()).isEqualTo(expectedMessage.getQos());
    assertThat(mqttMessageArrivedMessageArgumentCaptor.getValue().isRetained()).isFalse();
    assertThat(mqttMessageArrivedMessageArgumentCaptor.getValue().getPayload()).isEqualTo(payload);
    verifyNoMoreInteractions(messageListener);

    ArgumentCaptor<PahoRxMqttMessage> rxMqttMessageTokenArgumentCaptor = ArgumentCaptor.forClass(PahoRxMqttMessage.class);
    verify(emitter, times(2)).onNext(rxMqttMessageTokenArgumentCaptor.capture());
    assertThat(rxMqttMessageTokenArgumentCaptor.getValue()).isExactlyInstanceOf(PahoRxMqttMessage.class);
    assertThat(rxMqttMessageTokenArgumentCaptor.getValue().getQoS()).isEqualTo(expectedRxMqttMessage.getQoS());
    assertThat(rxMqttMessageTokenArgumentCaptor.getValue().getQoS().value()).isEqualTo(expectedMessage.getQos());
    assertThat(rxMqttMessageTokenArgumentCaptor.getValue().getId()).isEqualTo(expectedRxMqttMessage.getId());
    assertThat(rxMqttMessageTokenArgumentCaptor.getValue().getId()).isEqualTo(expectedMessage.getId());
    assertThat(rxMqttMessageTokenArgumentCaptor.getValue().getPayload()).isEqualTo(expectedRxMqttMessage.getPayload());
    assertThat(rxMqttMessageTokenArgumentCaptor.getValue().getPayload()).isEqualTo(expectedMessage.getPayload());
    assertThat(rxMqttMessageTokenArgumentCaptor.getValue().getTopic()).isEqualTo(expectedRxMqttMessage.getTopic());
    assertThat(rxMqttMessageTokenArgumentCaptor.getValue().getTopic()).isEqualTo(topics[0]);
    assertThat(rxMqttMessageTokenArgumentCaptor.getValue().getMqttMessage()).isEqualTo(expectedMessage);
    assertThat(rxMqttMessageTokenArgumentCaptor.getValue().getMqttMessage().toString()).isEqualTo(expectedRxMqttMessage.toString());
    assertThat(rxMqttMessageTokenArgumentCaptor.getValue().getMqttMessage().toString()).isEqualTo(expectedMessage.toString());
    verifyNoMoreInteractions(emitter);
  }

  @Test
  public void whenClientOnIsCalledAndClientAlreadyDisconnectedThenThrowException() throws MqttException {
    int[] qos = { 0, 1, 2 };
    String[] topics = {"topic1", "topic2", "topic3"};

    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertThat(client).isNotNull();

    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertThat(rxClient).isNotNull();

    IMqttToken token = mock(IMqttToken.class);
    assertThat(token).isNotNull();

    when(token.getException()).thenReturn(new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED));

    PahoRxMqttException exception = new PahoRxMqttException(token);

    doThrow(exception)
        .when(client).subscribe(eq(topics), eq(qos), isNull(), any(IMqttActionListener.class), any(IMqttMessageListener[].class));

    RxMqttQoS[] rxQos = Arrays.stream(qos).boxed().map(RxMqttQoS::valueOf).toArray(RxMqttQoS[]::new);

    TestSubscriber<RxMqttMessage> testSubscriber = rxClient.on(topics, rxQos).test();
    testSubscriber.assertSubscribed();
    testSubscriber.assertError(PahoRxMqttException.class);

    @SuppressWarnings("unchecked")
    FlowableEmitter<RxMqttMessage> emitter = mock(FlowableEmitter.class);

    IMqttActionListener subscribeActionListener = spy(PahoRxMqttClient.newActionListener(emitter));
    assertThat(subscribeActionListener).isNotNull();

    emitter.onError(exception);
    subscribeActionListener.onFailure(token, exception);

    when(client.isConnected()).thenReturn(false);
    verify(client).subscribe(eq(topics), eq(qos), isNull(), any(IMqttActionListener.class), any(IMqttMessageListener[].class));
    assertFalse(rxClient.isConnected().blockingGet());
    verify(client).isConnected();
    verifyNoMoreInteractions(client);

    ArgumentCaptor<Throwable> subscribeThrowableArgumentCaptor = ArgumentCaptor.forClass(Throwable.class);
    verify(subscribeActionListener).onFailure(eq(token), subscribeThrowableArgumentCaptor.capture());
    assertThat(subscribeThrowableArgumentCaptor).isNotNull();
    assertThat(subscribeThrowableArgumentCaptor.getValue()).isExactlyInstanceOf(PahoRxMqttException.class);
    assertThat(subscribeThrowableArgumentCaptor.getValue()).hasCauseExactlyInstanceOf(MqttException.class);
    verifyNoMoreInteractions(subscribeActionListener);

    ArgumentCaptor<PahoRxMqttException> emitterThrowableArgumentCaptor = ArgumentCaptor.forClass(PahoRxMqttException.class);
    verify(emitter, times(2)).onError(emitterThrowableArgumentCaptor.capture());
    assertThat(emitterThrowableArgumentCaptor).isNotNull();
    assertThat(emitterThrowableArgumentCaptor.getValue()).isExactlyInstanceOf(PahoRxMqttException.class);
    assertThat(emitterThrowableArgumentCaptor.getValue()).hasCauseExactlyInstanceOf(PahoRxMqttException.class);
    assertThat(emitterThrowableArgumentCaptor.getValue().getToken()).isEqualTo(token);
    verifyNoMoreInteractions(emitter);
  }

}
