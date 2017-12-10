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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import br.com.thiaguten.rx.mqtt.api.RxMqttClient;
import br.com.thiaguten.rx.mqtt.api.RxMqttToken;
import io.reactivex.observers.TestObserver;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.Test;

public class PahoRxMqttClientConnectTest {

  //@Rule
  //public MockitoRule rule = MockitoJUnit.rule();

  @Test
  public void whenConnectIsCalledThenConnectSuccessfully() throws MqttException {
    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertNotNull(client);
    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertNotNull(rxClient);
    TestObserver<RxMqttToken> testObserver = rxClient.connect().test();
    testObserver.assertSubscribed();
    testObserver.assertNoErrors();
    when(client.isConnected()).thenReturn(true);
    verify(client).connect(any(MqttConnectOptions.class), isNull(), any(IMqttActionListener.class));
    assertTrue(rxClient.isConnected().blockingGet());
    verify(client).isConnected();
    verifyNoMoreInteractions(client);
  }

  @Test
  public void whenAlreadyConnectAndConnectIsCalledThrowException() throws MqttException {
    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertNotNull(client);
    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertNotNull(rxClient);
    when(client.connect(any(MqttConnectOptions.class), isNull(), any(IMqttActionListener.class)))
        .thenThrow(new PahoRxMqttException(new MqttException(MqttException.REASON_CODE_CLIENT_CONNECTED)));
    TestObserver<RxMqttToken> testObserver = rxClient.connect().test();
    testObserver.assertSubscribed();
    testObserver.assertError(PahoRxMqttException.class);
    when(client.isConnected()).thenReturn(true);
    verify(client).connect(any(MqttConnectOptions.class), isNull(), any(IMqttActionListener.class));
    assertTrue(rxClient.isConnected().blockingGet());
    verify(client).isConnected();
    verifyNoMoreInteractions(client);
  }

}
