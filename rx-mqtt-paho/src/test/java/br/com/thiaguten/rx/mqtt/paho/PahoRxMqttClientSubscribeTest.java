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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import br.com.thiaguten.rx.mqtt.api.RxMqttClient;
import br.com.thiaguten.rx.mqtt.api.RxMqttException;
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
    rxClient.on(topics, null).blockingFirst();
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

    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertThat(client).isNotNull();
    when(client.isConnected()).thenReturn(true);

    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertThat(rxClient).isNotNull();

    RxMqttQoS[] rxQos = Arrays.stream(qos).boxed().map(RxMqttQoS::valueOf).toArray(RxMqttQoS[]::new);
    TestSubscriber<RxMqttMessage> testSubscriber = rxClient.on(topics, rxQos).test();
    testSubscriber.assertSubscribed();
    testSubscriber.assertNoErrors();

    ArgumentCaptor<IMqttMessageListener[]> messageListenersArgumentCaptor = ArgumentCaptor.forClass(IMqttMessageListener[].class);

    verify(client).subscribe(same(topics), eq(qos), isNull(), any(IMqttActionListener.class), messageListenersArgumentCaptor.capture());
    assertThat(rxClient.isConnected().blockingGet()).isTrue();
    verify(client).isConnected();
    verifyNoMoreInteractions(client);
    assertThat(messageListenersArgumentCaptor.getValue()).isNotEmpty().hasSize(3);
  }

  @Test
  public void whenClientOnWithOnlyTopicIsCalledThenSubscribeSuccessfully() throws Exception {
    String[] topics = {"topic"};
    int[] qos = {2};

    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertThat(client).isNotNull();
    when(client.isConnected()).thenReturn(true);

    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertThat(rxClient).isNotNull();

    TestSubscriber<RxMqttMessage> testSubscriber = rxClient.on(topics[0]).test();
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
    int id = 1;
    int qos = 2;
    boolean retain = false;
    String topic = "topic";
    byte[] payload = "payload".getBytes();

    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertThat(client).isNotNull();
    when(client.isConnected()).thenReturn(true);

    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertThat(rxClient).isNotNull();

    TestSubscriber<RxMqttMessage> testSubscriber = rxClient.on(topic, RxMqttQoS.valueOf(qos)).test();
    testSubscriber.assertSubscribed();
    testSubscriber.assertNoErrors();

    MqttMessage expectedMessage = new MqttMessage(payload);
    expectedMessage.setId(id);
    expectedMessage.setQos(qos);
    expectedMessage.setRetained(retain);

    PahoRxMqttMessage expectedRxMqttMessage = PahoRxMqttMessage.create(expectedMessage);
    expectedRxMqttMessage.setTopic(topic);

    @SuppressWarnings("unchecked")
    FlowableEmitter<RxMqttMessage> flowableEmitter = mock(FlowableEmitter.class);
    assertThat(flowableEmitter).isNotNull();

    flowableEmitter.onNext(expectedRxMqttMessage);

    IMqttMessageListener messageListener = mock(IMqttMessageListener.class);
    ArgumentCaptor<MqttMessage> mqttMessageArgumentCaptor = ArgumentCaptor.forClass(MqttMessage.class);
    doNothing().when(messageListener).messageArrived(eq(topic), mqttMessageArgumentCaptor.capture());
    messageListener.messageArrived(topic, expectedMessage);
    verify(messageListener).messageArrived(topic, expectedMessage);
    assertThat(mqttMessageArgumentCaptor.getValue()).isEqualTo(expectedMessage);

    assertThat(mqttMessageArgumentCaptor.getValue().getId()).isEqualTo(id);
    assertThat(mqttMessageArgumentCaptor.getValue().getQos()).isEqualTo(qos);
    assertThat(mqttMessageArgumentCaptor.getValue().isRetained()).isFalse();
    assertThat(mqttMessageArgumentCaptor.getValue().getPayload()).isEqualTo(payload);

    assertThat(mqttMessageArgumentCaptor.getValue().getPayload()).isEqualTo(expectedRxMqttMessage.getPayload());

    verify(messageListener).messageArrived(eq(topic), mqttMessageArgumentCaptor.capture());
    assertThat(rxClient.isConnected().blockingGet()).isTrue();
    verify(client).isConnected();

    ArgumentCaptor<PahoRxMqttMessage> rxMqttMessageTokenArgumentCaptor = ArgumentCaptor.forClass(PahoRxMqttMessage.class);
    verify(flowableEmitter).onNext(rxMqttMessageTokenArgumentCaptor.capture());
    assertThat(rxMqttMessageTokenArgumentCaptor).isNotNull();
    assertThat(rxMqttMessageTokenArgumentCaptor.getValue()).isExactlyInstanceOf(PahoRxMqttMessage.class);
    assertThat(rxMqttMessageTokenArgumentCaptor.getValue().getQoS()).isEqualTo(expectedRxMqttMessage.getQoS());
    assertThat(rxMqttMessageTokenArgumentCaptor.getValue().getQoS().value()).isEqualTo(expectedMessage.getQos());
    assertThat(rxMqttMessageTokenArgumentCaptor.getValue().getId()).isEqualTo(expectedRxMqttMessage.getId());
    assertThat(rxMqttMessageTokenArgumentCaptor.getValue().getId()).isEqualTo(expectedMessage.getId());
    assertThat(rxMqttMessageTokenArgumentCaptor.getValue().getPayload()).isEqualTo(expectedRxMqttMessage.getPayload());
    assertThat(rxMqttMessageTokenArgumentCaptor.getValue().getPayload()).isEqualTo(expectedMessage.getPayload());
    assertThat(rxMqttMessageTokenArgumentCaptor.getValue().getTopic()).isEqualTo(expectedRxMqttMessage.getTopic());
    assertThat(rxMqttMessageTokenArgumentCaptor.getValue().getTopic()).isEqualTo(topic);
    assertThat(rxMqttMessageTokenArgumentCaptor.getValue().getMqttMessage()).isEqualTo(expectedMessage);
    assertThat(rxMqttMessageTokenArgumentCaptor.getValue().getMqttMessage().toString()).isEqualTo(expectedRxMqttMessage.toString());
    assertThat(rxMqttMessageTokenArgumentCaptor.getValue().getMqttMessage().toString()).isEqualTo(expectedMessage.toString());

    verifyNoMoreInteractions(flowableEmitter);
  }

  //@Test
  public void whenClientOonIsCalledAndClientAlreadyDisconnectedThenThrowException() throws MqttException {
    int[] qos = { 0, 1, 2 };
    String[] topics = {"topic1", "topic2", "topic3"};

    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertThat(client).isNotNull();

    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertThat(rxClient).isNotNull();

    ArgumentCaptor<IMqttActionListener> actionListener = ArgumentCaptor.forClass(IMqttActionListener.class);
    ArgumentCaptor<IMqttMessageListener[]> messageListener = ArgumentCaptor.forClass(IMqttMessageListener[].class);

    when(client.subscribe(same(topics), same(qos), isNull(), actionListener.capture(), messageListener.capture()))
        .thenThrow(new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED));

    RxMqttQoS[] rxQos = Arrays.stream(qos).boxed().map(RxMqttQoS::valueOf).toArray(RxMqttQoS[]::new);
    TestSubscriber<RxMqttMessage> testSubscriber = rxClient.on(topics, rxQos).test();
    testSubscriber.assertSubscribed();
    testSubscriber.assertError(PahoRxMqttException.class);

    when(client.isConnected()).thenReturn(false);
    assertFalse(rxClient.isConnected().blockingGet());

    verify(client).subscribe(same(topics), same(qos), isNull(), same(actionListener.getValue()), same(messageListener.getValue()));
    verify(client).isConnected();

    verifyNoMoreInteractions(client);
  }

  //@Test
  public void whenClientOnIsCalledThenSubscribeFail() throws MqttException {
    int qos = 0;
    String topic = "topic";

    IMqttToken mqttToken = mock(IMqttToken.class);
    assertThat(mqttToken).isNotNull();

    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertThat(client).isNotNull();
    when(client.isConnected()).thenReturn(false);

    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertThat(rxClient).isNotNull();

    IMqttActionListener mqttActionListener = mock(IMqttActionListener.class);
    assertThat(mqttActionListener).isNotNull();
    RxMqttException expectedException = new PahoRxMqttException(new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED));
    ArgumentCaptor<RxMqttException> mqttExceptionArgumentCaptor = ArgumentCaptor.forClass(RxMqttException.class);
    doNothing().when(mqttActionListener).onFailure(eq(mqttToken), mqttExceptionArgumentCaptor.capture());
    mqttActionListener.onFailure(mqttToken, expectedException);
    verify(mqttActionListener).onFailure(mqttToken, expectedException);
    assertThat(mqttExceptionArgumentCaptor.getValue()).isEqualTo(expectedException);

    TestSubscriber<RxMqttMessage> testSubscriber = rxClient.on(topic, RxMqttQoS.valueOf(qos)).test();
    testSubscriber.assertSubscribed();
    testSubscriber.assertError(PahoRxMqttException.class);

    verify(mqttActionListener).onFailure(any(IMqttToken.class), same(expectedException));
    verify(client).subscribe(same(topic), same(qos), isNull(), same(mqttActionListener), any(IMqttMessageListener.class));
    assertThat(rxClient.isConnected().blockingGet()).isFalse();
    verify(client).isConnected();
    verifyNoMoreInteractions(client);
  }

}
