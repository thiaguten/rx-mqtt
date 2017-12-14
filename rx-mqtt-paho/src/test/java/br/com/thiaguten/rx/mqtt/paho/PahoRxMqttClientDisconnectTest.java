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
import static org.junit.Assert.assertNotNull;
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
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PahoRxMqttClientDisconnectTest {

  @Test
  public void whenClientDisconnectIsCalledThenDisconnectSuccessfully() throws MqttException {
    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertNotNull(client);

    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertNotNull(rxClient);

    TestObserver<RxMqttToken> testObserver = rxClient.disconnect().test();
    testObserver.assertSubscribed();
    testObserver.assertNoErrors();

    @SuppressWarnings("unchecked")
    SingleEmitter<RxMqttToken> emitter = spy(SingleEmitter.class);
    assertThat(emitter).isNotNull();

    IMqttActionListener disconnectActionListener = spy(PahoRxMqttClient.newActionListener(emitter));
    assertThat(disconnectActionListener).isNotNull();

    IMqttToken token = mock(IMqttToken.class);
    assertThat(token).isNotNull();

    RxMqttToken rxToken = mock(RxMqttToken.class);
    assertThat(rxToken).isNotNull();

    emitter.onSuccess(rxToken);
    disconnectActionListener.onSuccess(token);

    when(client.isConnected()).thenReturn(false);
    verify(client).disconnect(isNull(), any(IMqttActionListener.class));
    assertFalse(rxClient.isConnected().blockingGet());
    verify(client).isConnected();
    verifyNoMoreInteractions(client);

    ArgumentCaptor<RxMqttToken> disconnectRxTokenArgumentCaptor = ArgumentCaptor.forClass(RxMqttToken.class);
    verify(emitter, times(2)).onSuccess(disconnectRxTokenArgumentCaptor.capture());
    assertThat(disconnectRxTokenArgumentCaptor).isNotNull();
    assertThat(disconnectRxTokenArgumentCaptor.getValue()).isExactlyInstanceOf(PahoRxMqttToken.class);
    verifyNoMoreInteractions(emitter);

    ArgumentCaptor<IMqttToken> disconnectTokenArgumentCaptor = ArgumentCaptor.forClass(IMqttToken.class);
    verify(disconnectActionListener).onSuccess(disconnectTokenArgumentCaptor.capture());
    assertThat(disconnectTokenArgumentCaptor).isNotNull();
    assertThat(disconnectTokenArgumentCaptor.getValue()).isInstanceOf(IMqttToken.class);
    assertThat(disconnectTokenArgumentCaptor.getValue()).isEqualTo(token);
    verifyNoMoreInteractions(disconnectActionListener);
  }

  @Test
  public void whenClientDisconnectForciblyIsCalledThenDisconnectSuccessfully() throws MqttException {
    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertNotNull(client);
    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertNotNull(rxClient);
    TestObserver<Void> testObserver = rxClient.disconnectForcibly().test();
    testObserver.assertSubscribed();
    testObserver.assertNoErrors();
    when(client.isConnected()).thenReturn(false);
    verify(client).disconnectForcibly();
    assertFalse(rxClient.isConnected().blockingGet());
    verify(client).isConnected();
    verifyNoMoreInteractions(client);
  }

  @Test
  public void whenClientDisconnectIsCalledAndClientAlreadyDisconnectedThenThrowException() throws MqttException {
    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertNotNull(client);

    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertNotNull(rxClient);

    IMqttToken token = mock(IMqttToken.class);
    assertThat(token).isNotNull();

    when(token.getException()).thenReturn(new MqttException(MqttException.REASON_CODE_CLIENT_ALREADY_DISCONNECTED));

    PahoRxMqttException exception = new PahoRxMqttException(token);

    doThrow(exception).when(client).disconnect(isNull(), any(IMqttActionListener.class));

    TestObserver<RxMqttToken> testObserver = rxClient.disconnect().test();
    testObserver.assertSubscribed();
    testObserver.assertError(PahoRxMqttException.class);

    @SuppressWarnings("unchecked")
    SingleEmitter<RxMqttToken> emitter = mock(SingleEmitter.class);

    IMqttActionListener disconnectActionListener = spy(PahoRxMqttClient.newActionListener(emitter));
    assertThat(disconnectActionListener).isNotNull();

    emitter.onError(exception);
    disconnectActionListener.onFailure(token, exception);

    when(client.isConnected()).thenReturn(false);
    verify(client).disconnect(isNull(), any(IMqttActionListener.class));
    assertFalse(rxClient.isConnected().blockingGet());
    verify(client).isConnected();
    verifyNoMoreInteractions(client);

    ArgumentCaptor<Throwable> disconnectThrowableArgumentCaptor = ArgumentCaptor.forClass(Throwable.class);
    verify(disconnectActionListener).onFailure(eq(token), disconnectThrowableArgumentCaptor.capture());
    assertThat(disconnectThrowableArgumentCaptor).isNotNull();
    assertThat(disconnectThrowableArgumentCaptor.getValue()).isExactlyInstanceOf(PahoRxMqttException.class);
    assertThat(disconnectThrowableArgumentCaptor.getValue()).hasCauseExactlyInstanceOf(MqttException.class);
    verifyNoMoreInteractions(disconnectActionListener);

    ArgumentCaptor<PahoRxMqttException> emitterThrowableArgumentCaptor = ArgumentCaptor.forClass(PahoRxMqttException.class);
    verify(emitter, times(2)).onError(emitterThrowableArgumentCaptor.capture());
    assertThat(emitterThrowableArgumentCaptor).isNotNull();
    assertThat(emitterThrowableArgumentCaptor.getValue()).isExactlyInstanceOf(PahoRxMqttException.class);
    assertThat(emitterThrowableArgumentCaptor.getValue()).hasCauseExactlyInstanceOf(PahoRxMqttException.class);
    assertThat(emitterThrowableArgumentCaptor.getValue().getToken()).isEqualTo(token);
    verifyNoMoreInteractions(emitter);
  }

  @Test
  public void whenClientDisconnectForciblyIsCalledAndClientAlreadyDisconnectedThenThrowException() throws MqttException {
    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertNotNull(client);
    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertNotNull(rxClient);
    doThrow(new MqttException(MqttException.REASON_CODE_UNEXPECTED_ERROR))
        .when(client).disconnectForcibly();
    TestObserver<Void> testObserver = rxClient.disconnectForcibly().test();
    testObserver.assertSubscribed();
    testObserver.assertError(PahoRxMqttException.class);
    when(client.isConnected()).thenReturn(false);
    verify(client).disconnectForcibly();
    assertFalse(rxClient.isConnected().blockingGet());
    verify(client).isConnected();
    verifyNoMoreInteractions(client);
  }

}
