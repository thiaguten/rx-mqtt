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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import br.com.thiaguten.rx.mqtt.api.RxMqttClient;
import io.reactivex.observers.TestObserver;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PahoRxMqttClientCloseTest {

  @Test
  public void whenClientCloseIsCalledThenCloseSuccessfully() throws MqttException {
    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertNotNull(client);
    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertNotNull(rxClient);
    TestObserver<Void> testObserver = rxClient.close().test();
    testObserver.assertSubscribed();
    testObserver.assertNoErrors();
    when(client.isConnected()).thenReturn(false);
    verify(client).close();
    assertFalse(rxClient.isConnected().blockingGet());
    verify(client).isConnected();
    verifyNoMoreInteractions(client);
  }

  @Test
  public void whenClientCloseIsCalledAndClientStillConnectThenThrowException() throws MqttException {
    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertNotNull(client);
    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertNotNull(rxClient);
    doThrow(new MqttException(MqttException.REASON_CODE_CLIENT_CONNECTED)).when(client).close();
    TestObserver<Void> testObserver = rxClient.close().test();
    testObserver.assertSubscribed();
    testObserver.assertError(PahoRxMqttException.class);
    when(client.isConnected()).thenReturn(true);
    verify(client).close();
    assertTrue(rxClient.isConnected().blockingGet());
    verify(client).isConnected();
    verifyNoMoreInteractions(client);
  }

}
