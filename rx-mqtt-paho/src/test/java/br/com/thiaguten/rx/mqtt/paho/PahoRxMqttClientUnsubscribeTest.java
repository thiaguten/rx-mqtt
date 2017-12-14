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
import static org.junit.Assert.assertTrue;
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
import br.com.thiaguten.rx.mqtt.api.RxMqttToken;
import io.reactivex.SingleEmitter;
import io.reactivex.observers.TestObserver;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PahoRxMqttClientUnsubscribeTest {

  @Test
  public void whenClientOffIsCalledThenUnsubscribeSuccessfully() throws MqttException {
    String[] topics = {"topic1", "topic2", "topic3"};

    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertThat(client).isNotNull();

    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertThat(rxClient).isNotNull();

    TestObserver<RxMqttToken> testObserver = rxClient.off(topics).test();
    testObserver.assertSubscribed();
    testObserver.assertNoErrors();

    @SuppressWarnings("unchecked")
    SingleEmitter<RxMqttToken> emitter = spy(SingleEmitter.class);
    assertThat(emitter).isNotNull();

    IMqttActionListener unsubscribeActionListener = spy(PahoRxMqttClient.newActionListener(emitter));
    assertThat(unsubscribeActionListener).isNotNull();

    IMqttToken token = mock(IMqttToken.class);
    assertThat(token).isNotNull();

    RxMqttToken rxToken = mock(RxMqttToken.class);
    assertThat(rxToken).isNotNull();

    emitter.onSuccess(rxToken);
    unsubscribeActionListener.onSuccess(token);

    when(client.isConnected()).thenReturn(true);
    verify(client).unsubscribe(same(topics), isNull(), any(IMqttActionListener.class));
    assertTrue(rxClient.isConnected().blockingGet());
    verify(client).isConnected();
    verifyNoMoreInteractions(client);

    ArgumentCaptor<RxMqttToken> unsubscribeRxTokenArgumentCaptor = ArgumentCaptor.forClass(RxMqttToken.class);
    verify(emitter, times(2)).onSuccess(unsubscribeRxTokenArgumentCaptor.capture());
    assertThat(unsubscribeRxTokenArgumentCaptor).isNotNull();
    assertThat(unsubscribeRxTokenArgumentCaptor.getValue()).isExactlyInstanceOf(PahoRxMqttToken.class);
    verifyNoMoreInteractions(emitter);

    ArgumentCaptor<IMqttToken> unsubscribeTokenArgumentCaptor = ArgumentCaptor.forClass(IMqttToken.class);
    verify(unsubscribeActionListener).onSuccess(unsubscribeTokenArgumentCaptor.capture());
    assertThat(unsubscribeTokenArgumentCaptor).isNotNull();
    assertThat(unsubscribeTokenArgumentCaptor.getValue()).isInstanceOf(IMqttToken.class);
    assertThat(unsubscribeTokenArgumentCaptor.getValue()).isEqualTo(token);
    verifyNoMoreInteractions(unsubscribeActionListener);
  }

  @Test
  public void whenClientOffIsCalledAndClientAlreadyDisconnectedThenThrowException() throws MqttException {
    String[] topics = {"topic1", "topic2", "topic3"};

    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertThat(client).isNotNull();

    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertThat(rxClient).isNotNull();

    IMqttToken token = mock(IMqttToken.class);
    assertThat(token).isNotNull();

    when(token.getException()).thenReturn(new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED));

    PahoRxMqttException exception = new PahoRxMqttException(token);

    doThrow(exception).when(client).unsubscribe(same(topics), isNull(), any(IMqttActionListener.class));

    TestObserver<RxMqttToken> testObserver = rxClient.off(topics).test();
    testObserver.assertSubscribed();
    testObserver.assertError(PahoRxMqttException.class);

    @SuppressWarnings("unchecked")
    SingleEmitter<RxMqttToken> emitter = mock(SingleEmitter.class);

    IMqttActionListener unsubscribeActionListener = spy(PahoRxMqttClient.newActionListener(emitter));
    assertThat(unsubscribeActionListener).isNotNull();

    emitter.onError(exception);
    unsubscribeActionListener.onFailure(token, exception);

    when(client.isConnected()).thenReturn(false);
    verify(client).unsubscribe(same(topics), isNull(), any(IMqttActionListener.class));
    assertFalse(rxClient.isConnected().blockingGet());
    verify(client).isConnected();
    verifyNoMoreInteractions(client);

    ArgumentCaptor<Throwable> unsubscribeThrowableArgumentCaptor = ArgumentCaptor.forClass(Throwable.class);
    verify(unsubscribeActionListener).onFailure(eq(token), unsubscribeThrowableArgumentCaptor.capture());
    assertThat(unsubscribeThrowableArgumentCaptor).isNotNull();
    assertThat(unsubscribeThrowableArgumentCaptor.getValue()).isExactlyInstanceOf(PahoRxMqttException.class);
    assertThat(unsubscribeThrowableArgumentCaptor.getValue()).hasCauseExactlyInstanceOf(MqttException.class);
    verifyNoMoreInteractions(unsubscribeActionListener);

    ArgumentCaptor<PahoRxMqttException> emitterThrowableArgumentCaptor = ArgumentCaptor.forClass(PahoRxMqttException.class);
    verify(emitter, times(2)).onError(emitterThrowableArgumentCaptor.capture());
    assertThat(emitterThrowableArgumentCaptor).isNotNull();
    assertThat(emitterThrowableArgumentCaptor.getValue()).isExactlyInstanceOf(PahoRxMqttException.class);
    assertThat(emitterThrowableArgumentCaptor.getValue()).hasCauseExactlyInstanceOf(PahoRxMqttException.class);
    assertThat(emitterThrowableArgumentCaptor.getValue().getToken()).isEqualTo(token);
    verifyNoMoreInteractions(emitter);
  }

  @Test
  public void whenClientOffIsCalledMultipleTimesThenDoNotThrowException() throws MqttException {
    String[] topics = {"topic1", "topic2", "topic3"};

    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertThat(client).isNotNull();

    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertThat(rxClient).isNotNull();

    int times = 4;
    Supplier<TestObserver<RxMqttToken>> rxOffTestSupplier = () -> rxClient.off(topics).test();
    try (Stream<TestObserver<RxMqttToken>> stream = Stream.generate(rxOffTestSupplier).limit(times)) {
      stream
          .forEach(testObserver -> {
            testObserver.assertSubscribed();
            testObserver.assertNoErrors();
          });
    }

    when(client.isConnected()).thenReturn(true);
    assertTrue(rxClient.isConnected().blockingGet());

    verify(client, times(times)).unsubscribe(same(topics), isNull(), any(IMqttActionListener.class));
    verify(client).isConnected();
    verifyNoMoreInteractions(client);
  }

  @Test
  public void whenClientOffAndCloseIsCalledThenUnsubscribeAndCloseClientSuccessfully() throws MqttException {
    String[] topics = {"topic1", "topic2", "topic3"};

    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertThat(client).isNotNull();

    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertThat(rxClient).isNotNull();

    TestObserver<Void> testObserver = rxClient.offAndClose(topics).test();
    testObserver.assertSubscribed();
    testObserver.assertNoErrors();

    when(client.isConnected()).thenReturn(false);
    assertFalse(rxClient.isConnected().blockingGet());

    verify(client).close();
    verify(client, times(2)).isConnected();
    verifyNoMoreInteractions(client);
  }

}
