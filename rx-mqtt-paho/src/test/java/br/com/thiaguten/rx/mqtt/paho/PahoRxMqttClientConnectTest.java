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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.isNull;
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
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class PahoRxMqttClientConnectTest {

  @Test
  public void whenConnectIsCalledThenConnectSuccessfully() throws MqttException {
    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertNotNull(client);

    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertNotNull(rxClient);

    TestObserver<RxMqttToken> testObserver = rxClient.connect().test();
    testObserver.assertSubscribed();
    testObserver.assertNoErrors();

    @SuppressWarnings("unchecked")
    SingleEmitter<RxMqttToken> emitter = spy(SingleEmitter.class);
    assertThat(emitter).isNotNull();

    IMqttActionListener connectActionListener = spy(PahoRxMqttClient.newActionListener(emitter));
    assertThat(connectActionListener).isNotNull();

    IMqttToken token = mock(IMqttToken.class);
    assertThat(token).isNotNull();

    RxMqttToken rxToken = mock(RxMqttToken.class);
    assertThat(rxToken).isNotNull();

    emitter.onSuccess(rxToken);
    connectActionListener.onSuccess(token);

    when(client.isConnected()).thenReturn(true);
    verify(client).connect(any(MqttConnectOptions.class), isNull(), any(IMqttActionListener.class));
    assertTrue(rxClient.isConnected().blockingGet());
    verify(client).isConnected();
    verifyNoMoreInteractions(client);

    ArgumentCaptor<RxMqttToken> connectRxTokenArgumentCaptor = ArgumentCaptor.forClass(RxMqttToken.class);
    verify(emitter, times(2)).onSuccess(connectRxTokenArgumentCaptor.capture());
    assertThat(connectRxTokenArgumentCaptor).isNotNull();
    assertThat(connectRxTokenArgumentCaptor.getValue()).isExactlyInstanceOf(PahoRxMqttToken.class);
    verifyNoMoreInteractions(emitter);

    ArgumentCaptor<IMqttToken> connectTokenArgumentCaptor = ArgumentCaptor.forClass(IMqttToken.class);
    verify(connectActionListener).onSuccess(connectTokenArgumentCaptor.capture());
    assertThat(connectTokenArgumentCaptor).isNotNull();
    assertThat(connectTokenArgumentCaptor.getValue()).isInstanceOf(IMqttToken.class);
    assertThat(connectTokenArgumentCaptor.getValue()).isEqualTo(token);
    verifyNoMoreInteractions(connectActionListener);
  }

  @Test
  public void whenAlreadyConnectAndConnectIsCalledThrowException() throws MqttException {
    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertNotNull(client);

    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertNotNull(rxClient);

    IMqttToken token = mock(IMqttToken.class);
    assertThat(token).isNotNull();

    when(token.getException()).thenReturn(new MqttException(MqttException.REASON_CODE_CLIENT_CONNECTED));

    PahoRxMqttException exception = new PahoRxMqttException(token);

    doThrow(exception)
        .when(client).connect(any(MqttConnectOptions.class), isNull(), any(IMqttActionListener.class));

    TestObserver<RxMqttToken> testObserver = rxClient.connect().test();
    testObserver.assertSubscribed();
    testObserver.assertError(PahoRxMqttException.class);

    @SuppressWarnings("unchecked")
    SingleEmitter<RxMqttToken> emitter = mock(SingleEmitter.class);

    IMqttActionListener connectActionListener = spy(PahoRxMqttClient.newActionListener(emitter));
    assertThat(connectActionListener).isNotNull();

    emitter.onError(exception);
    connectActionListener.onFailure(token, exception);

    when(client.isConnected()).thenReturn(true);
    verify(client).connect(any(MqttConnectOptions.class), isNull(), any(IMqttActionListener.class));
    assertTrue(rxClient.isConnected().blockingGet());
    verify(client).isConnected();
    verifyNoMoreInteractions(client);

    ArgumentCaptor<Throwable> connectThrowableArgumentCaptor = ArgumentCaptor.forClass(Throwable.class);
    verify(connectActionListener).onFailure(eq(token), connectThrowableArgumentCaptor.capture());
    assertThat(connectThrowableArgumentCaptor).isNotNull();
    assertThat(connectThrowableArgumentCaptor.getValue()).isExactlyInstanceOf(PahoRxMqttException.class);
    assertThat(connectThrowableArgumentCaptor.getValue()).hasCauseExactlyInstanceOf(MqttException.class);
    verifyNoMoreInteractions(connectActionListener);

    ArgumentCaptor<PahoRxMqttException> emitterThrowableArgumentCaptor = ArgumentCaptor.forClass(PahoRxMqttException.class);
    verify(emitter, times(2)).onError(emitterThrowableArgumentCaptor.capture());
    assertThat(emitterThrowableArgumentCaptor).isNotNull();
    assertThat(emitterThrowableArgumentCaptor.getValue()).isExactlyInstanceOf(PahoRxMqttException.class);
    assertThat(emitterThrowableArgumentCaptor.getValue()).hasCauseExactlyInstanceOf(PahoRxMqttException.class);
    //assertThat(emitterThrowableArgumentCaptor.getValue().getToken()).isEqualTo(token);
    assertThat(emitterThrowableArgumentCaptor.getValue().getToken()).isNull();
    verifyNoMoreInteractions(emitter);
  }

}
