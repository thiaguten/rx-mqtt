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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import br.com.thiaguten.rx.mqtt.api.RxMqttClient;
import io.reactivex.BackpressureStrategy;
import io.reactivex.observers.TestObserver;
import java.io.File;
import java.nio.file.Paths;
import java.util.Optional;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PahoRxMqttClientTest {

  @Test
  public void whenClientCreateIsCalledThenCreateSuccessfully() {
    String brokerUri = "tcp://localhost:1883";
    BackpressureStrategy backpressureStrategy = BackpressureStrategy.LATEST;
    MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
    PahoRxMqttCallback pahoCallback =
        PahoRxMqttCallback.create(cause -> {/*NOP*/}, (reconnect, serverUri) -> {/*NOP*/});

    // create by mqtt connect options only
    PahoRxMqttClient pahoRxMqttClient = PahoRxMqttClient.create(mqttConnectOptions,
        connectOptions -> {
          PahoRxMqttClient.Builder builder = PahoRxMqttClient.builder(brokerUri)
              .setBackpressureStrategy(backpressureStrategy);
          Optional.ofNullable(connectOptions).ifPresent(builder::setConnectOptions);
          return builder.build();
        });
    assertThat(pahoRxMqttClient).isNotNull();
    assertThat(pahoRxMqttClient.getCallback()).isNotEqualTo(pahoCallback);
    assertThat(pahoRxMqttClient.getServerUri().blockingGet()).isEqualTo(brokerUri);
    assertThat(pahoRxMqttClient.getConnectOptions()).isEqualTo(mqttConnectOptions);
    assertThat(pahoRxMqttClient.getBackpressureStrategy()).isEqualTo(backpressureStrategy);

    // create by mqtt connect options and paho mqtt callback
    PahoRxMqttClient pahoRxMqttClient2 = PahoRxMqttClient.create(mqttConnectOptions, pahoCallback,
        (connectOptions, callback) -> {
          PahoRxMqttClient.Builder builder = PahoRxMqttClient.builder(brokerUri)
              .setBackpressureStrategy(backpressureStrategy);
          Optional.ofNullable(connectOptions).ifPresent(builder::setConnectOptions);
          Optional.ofNullable(callback).ifPresent(builder::setCallbackListener);
          return builder.build();
        });
    assertThat(pahoRxMqttClient2).isNotNull();
    assertThat(pahoRxMqttClient2.getCallback()).isEqualTo(pahoCallback);
    assertThat(pahoRxMqttClient2.getServerUri().blockingGet()).isEqualTo(brokerUri);
    assertThat(pahoRxMqttClient2.getConnectOptions()).isEqualTo(mqttConnectOptions);
    assertThat(pahoRxMqttClient2.getBackpressureStrategy()).isEqualTo(backpressureStrategy);
  }

  @Test(expected = NullPointerException.class)
  public void whenNullClientIsPassedThenThrowsAnError() {
    PahoRxMqttClient.builder((IMqttAsyncClient) null).build();
  }

  @Test(expected = NullPointerException.class)
  public void whenNullBrokerUriIsPassedThenThrowsAnError() {
    PahoRxMqttClient.builder((String) null).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void whenNullBrokerUriAndClientIdIsPassedThenThrowsAnError() {
    PahoRxMqttClient.builder(null, null).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void whenNullBrokerUriAndClientIdAndMqttClientPersistenceIsPassedThenThrowsAnError() {
    PahoRxMqttClient.builder(null, null, null).build();
  }

  @Test
  public void whenClientBuilderBuildIsCalledThenBuildSuccessfully() {
    String clientId = "pahoClientId";
    String brokerUri = "tcp://localhost:1883";

    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertThat(client).isNotNull();
    when(client.getClientId()).thenReturn(clientId);
    when(client.getServerURI()).thenReturn(brokerUri);

    // build by client instance
    RxMqttClient rxClient = PahoRxMqttClient.builder(client).build();
    assertThat(rxClient).isNotNull();
    assertThat(rxClient.isConnected().blockingGet()).isFalse();

    TestObserver<String> testObserverClientId = rxClient.getClientId().test();
    testObserverClientId.assertSubscribed();
    testObserverClientId.assertNoErrors();
    assertThat(rxClient.getClientId().blockingGet()).isEqualTo(clientId);

    TestObserver<String> testObserverServerUri = rxClient.getServerUri().test();
    testObserverServerUri.assertSubscribed();
    testObserverServerUri.assertNoErrors();
    assertThat(rxClient.getServerUri().blockingGet()).isEqualTo(brokerUri);

    verify(client, times(2)).getClientId();
    verify(client, times(2)).getServerURI();
    verify(client).isConnected();

    verifyNoMoreInteractions(client);
  }

  @Test
  public void whenClientBrokerUriBuilderBuildIsCalledThenBuildSuccessfully() {
    String clientId = "pahoClientId";
    String brokerUri = "tcp://localhost:1883";

    // build by broker uri and the client id is dynamically generated
    RxMqttClient rxClient = PahoRxMqttClient.builder(brokerUri).build();
    assertThat(rxClient).isNotNull();
    assertThat(rxClient.isConnected().blockingGet()).isFalse();

    TestObserver<String> testObserverClientId = rxClient.getClientId().test();
    testObserverClientId.assertSubscribed();
    testObserverClientId.assertNoErrors();
    assertThat(rxClient.getClientId().blockingGet()).isNotEqualTo(clientId);

    TestObserver<String> testObserverServerUri = rxClient.getServerUri().test();
    testObserverServerUri.assertSubscribed();
    testObserverServerUri.assertNoErrors();
    assertThat(rxClient.getServerUri().blockingGet()).isEqualTo(brokerUri);
  }

  @Test
  public void whenClientBrokerUriAndClientIdBuilderBuildIsCalledThenBuildSuccessfully() {
    String clientId = "pahoClientId";
    String brokerUri = "tcp://localhost:1883";

    // build by broker uri and client id
    RxMqttClient rxClient = PahoRxMqttClient.builder(brokerUri, clientId).build();
    assertThat(rxClient).isNotNull();
    assertThat(rxClient.isConnected().blockingGet()).isFalse();

    TestObserver<String> testObserverClientId = rxClient.getClientId().test();
    testObserverClientId.assertSubscribed();
    testObserverClientId.assertNoErrors();
    assertThat(rxClient.getClientId().blockingGet()).isEqualTo(clientId);

    TestObserver<String> testObserverServerUri = rxClient.getServerUri().test();
    testObserverServerUri.assertSubscribed();
    testObserverServerUri.assertNoErrors();
    assertThat(rxClient.getServerUri().blockingGet()).isEqualTo(brokerUri);
  }

  @Test
  public void whenClientBrokerUriAndClientIdAndMqttClientPersistenceBuilderBuildIsCalledThenBuildSuccessfully() {
    String clientId = "pahoClientId";
    String brokerUri = "tcp://localhost:1883";

    File tempDir = Paths.get(System.getProperty("user.dir"), "temp").toFile();
    tempDir.deleteOnExit();

    MqttClientPersistence clientPersistence =
        new MqttDefaultFilePersistence(tempDir.getAbsolutePath());

    // build by broker uri and client id
    PahoRxMqttClient rxClient = PahoRxMqttClient.builder(brokerUri, clientId, clientPersistence).build();
    assertThat(rxClient).isNotNull();
    assertThat(rxClient.isConnected().blockingGet()).isFalse();

    TestObserver<String> testObserverClientId = rxClient.getClientId().test();
    testObserverClientId.assertSubscribed();
    testObserverClientId.assertNoErrors();
    assertThat(rxClient.getClientId().blockingGet()).isEqualTo(clientId);

    TestObserver<String> testObserverServerUri = rxClient.getServerUri().test();
    testObserverServerUri.assertSubscribed();
    testObserverServerUri.assertNoErrors();
    assertThat(rxClient.getServerUri().blockingGet()).isEqualTo(brokerUri);
  }
}
