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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import br.com.thiaguten.rx.mqtt.api.RxMqttClient;
import br.com.thiaguten.rx.mqtt.api.RxMqttToken;
import io.reactivex.observers.TestObserver;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.Test;
import org.junit.runner.RunWith;
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

    when(client.isConnected()).thenReturn(true);
    verify(client).unsubscribe(same(topics), isNull(), any(IMqttActionListener.class));
    assertTrue(rxClient.isConnected().blockingGet());
    verify(client).isConnected();
    verifyNoMoreInteractions(client);
  }

  @Test
  public void whenClientOffIsCalledAndClientAlreadyDisconnectedThenThrowException() throws MqttException {
    String[] topics = {"topic1", "topic2", "topic3"};

    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertThat(client).isNotNull();

    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertThat(rxClient).isNotNull();

    when(client.unsubscribe(same(topics), isNull(), any(IMqttActionListener.class))).thenThrow(
        new PahoRxMqttException(new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED)));

    TestObserver<RxMqttToken> testObserver = rxClient.off(topics).test();
    testObserver.assertSubscribed();
    testObserver.assertError(PahoRxMqttException.class);

    when(client.isConnected()).thenReturn(false);
    assertFalse(rxClient.isConnected().blockingGet());

    verify(client).unsubscribe(same(topics), isNull(), any(IMqttActionListener.class));
    verify(client).isConnected();

    verifyNoMoreInteractions(client);
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
