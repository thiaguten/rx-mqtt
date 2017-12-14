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

import static java.util.Objects.requireNonNull;

import br.com.thiaguten.rx.mqtt.api.RxMqttCallback;
import br.com.thiaguten.rx.mqtt.api.RxMqttClient;
import br.com.thiaguten.rx.mqtt.api.RxMqttClientBuilder;
import br.com.thiaguten.rx.mqtt.api.RxMqttMessage;
import br.com.thiaguten.rx.mqtt.api.RxMqttQoS;
import br.com.thiaguten.rx.mqtt.api.RxMqttToken;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class PahoRxMqttClient implements RxMqttClient {

  private final IMqttAsyncClient client;
  private final RxMqttCallback callback;
  private final MqttConnectOptions connectOptions;
  private final BackpressureStrategy globalBackpressureStrategy;

  private PahoRxMqttClient(final Builder builder) {
    this.client = builder.client;
    this.callback = builder.pahoCallback;
    this.connectOptions = builder.connectOptions;
    this.globalBackpressureStrategy = builder.backpressureStrategy;
  }

  @Override
  public Single<String> getClientId() {
    return Single.fromCallable(client::getClientId);
  }

  @Override
  public Single<String> getServerUri() {
    return Single.fromCallable(client::getServerURI);
  }

  @Override
  public Single<Boolean> isConnected() {
    return Single.fromCallable(client::isConnected);
  }

  @Override
  public Completable close() {
    return Completable.fromAction(() -> {
      try {
        client.close();
      } catch (MqttException me) {
        throw new PahoRxMqttException(me);
      }
    });
  }

  @Override
  public Single<RxMqttToken> connect() {
    return Single.create(emitter ->
        client.connect(connectOptions, null, newActionListener(emitter)));
  }

  @Override
  public Single<RxMqttToken> disconnect() {
    return Single.create(emitter -> client.disconnect(null, newActionListener(emitter)));
  }

  @Override
  public Completable disconnectForcibly() {
    return Completable.fromAction(() -> {
      try {
        client.disconnectForcibly();
      } catch (MqttException me) {
        throw new PahoRxMqttException(me);
      }
    });
  }

  @Override
  public Single<RxMqttToken> publish(String topic, RxMqttMessage message) {
    return Single.create(emitter ->
        client.publish(topic, message.getPayload(), message.getQoS().value(), message.isRetained(),
            null, newActionListener(emitter)));
  }

  @Override
  public Flowable<RxMqttMessage> on(String[] topics, RxMqttQoS[] qos) {
    return on(topics, qos, globalBackpressureStrategy);
  }

  @Override
  public Flowable<RxMqttMessage> on(String topic, RxMqttQoS qos) {
    return on(new String[]{topic}, new RxMqttQoS[]{qos});
  }

  @Override
  public Flowable<RxMqttMessage> on(String topic) {
    return on(topic, RxMqttQoS.EXACTLY_ONCE);
  }

  @Override
  public Flowable<RxMqttMessage> on(
      String[] topics, RxMqttQoS[] qos, BackpressureStrategy strategy) {
    return Flowable.create(emitter -> {

      // Create message listeners for each topic.
      IMqttMessageListener[] messageListeners = Stream
          .generate(() -> (IMqttMessageListener) (t, m) -> {
            PahoRxMqttMessage pahoRxMqttMessage = PahoRxMqttMessage.create(m);
            pahoRxMqttMessage.setTopic(t);
            emitter.onNext(pahoRxMqttMessage);
          })
          .limit(topics.length)
          .toArray(IMqttMessageListener[]::new);

      int[] intQos = Arrays.stream(qos).mapToInt(RxMqttQoS::value).toArray();
      client.subscribe(
          topics, intQos, null, newActionListener(emitter), messageListeners);
    }, strategy);
  }

  @Override
  public Flowable<RxMqttMessage> on(String topic, RxMqttQoS qos, BackpressureStrategy strategy) {
    return on(new String[]{topic}, new RxMqttQoS[]{qos}, strategy);
  }

  @Override
  public Flowable<RxMqttMessage> on(String topic, BackpressureStrategy strategy) {
    return on(topic, RxMqttQoS.EXACTLY_ONCE, strategy);
  }

  @Override
  public Single<RxMqttToken> off(String... topics) {
    return Single.create(emitter ->
        client.unsubscribe(topics, null, newActionListener(emitter)));
  }

  // inner builder class

  public static final class Builder implements RxMqttClientBuilder<PahoRxMqttClient> {

    // required
    private final IMqttAsyncClient client;

    // default
    private PahoRxMqttCallback pahoCallback = null;
    private MqttConnectOptions connectOptions = new MqttConnectOptions();
    private BackpressureStrategy backpressureStrategy = BackpressureStrategy.BUFFER;

    // constructor

    Builder(IMqttAsyncClient client) {
      this.client = requireNonNull(client, "client must not be null");
    }

    public Builder setCallbackListener(PahoRxMqttCallback callback) {
      this.pahoCallback = requireNonNull(callback, "callback must not be null");
      this.client.setCallback(pahoCallback);
      return this;
    }

    public Builder setConnectOptions(MqttConnectOptions options) {
      this.connectOptions = requireNonNull(options, "options must not be null");
      return this;
    }

    public Builder setBackpressureStrategy(BackpressureStrategy backpressure) {
      this.backpressureStrategy = requireNonNull(backpressure, "backpressure must not be null");
      return this;
    }

    @Override
    public PahoRxMqttClient build() {
      return new PahoRxMqttClient(this);
    }
  }

  // convenient methods

  public static Builder builder(String brokerUri) {
    return builder(brokerUri, MqttAsyncClient.generateClientId());
  }

  public static Builder builder(String brokerUri, String clientId) {
    return builder(brokerUri, clientId, new MemoryPersistence());
  }

  public static Builder builder(
      String brokerUri, String clientId, MqttClientPersistence persistence) {
    try {
      return builder(new MqttAsyncClient(brokerUri, clientId, persistence));
    } catch (MqttException me) {
      throw new PahoRxMqttException(me);
    }
  }

  public static Builder builder(IMqttAsyncClient client) {
    return new Builder(client);
  }

  public static PahoRxMqttClient create(
      MqttConnectOptions connectOptions, Function<MqttConnectOptions, PahoRxMqttClient> f) {
    return f.apply(connectOptions);
  }

  public static PahoRxMqttClient create(
      MqttConnectOptions connectOptions, PahoRxMqttCallback callback,
      BiFunction<MqttConnectOptions, PahoRxMqttCallback, PahoRxMqttClient> f) {
    requireNonNull(connectOptions);
    requireNonNull(callback);
    return f.apply(connectOptions, callback);
  }

  // internal methods

  static IMqttActionListener newActionListener(FlowableEmitter<RxMqttMessage> emitter) {
    Consumer<IMqttToken> onNext = token -> { /* NOP */ };
    BiConsumer<IMqttToken, Throwable> onFailure = (token, exception) ->
        emitter.onError(new PahoRxMqttException(exception, token));
    return newActionListener(onNext, onFailure);
  }

  static IMqttActionListener newActionListener(SingleEmitter<RxMqttToken> emitter) {
    Consumer<IMqttToken> onSuccess = token -> emitter.onSuccess(new PahoRxMqttToken(token));
    BiConsumer<IMqttToken, Throwable> onFailure = (token, exception) ->
        emitter.onError(new PahoRxMqttException(exception, token));
    return newActionListener(onSuccess, onFailure);
  }

  static IMqttActionListener newActionListener(
      Consumer<IMqttToken> onSuccess, BiConsumer<IMqttToken, Throwable> onFailure) {
    return new PahoActionListener(onSuccess, onFailure);
  }

  // internal inner class implementation

  private static class PahoActionListener implements IMqttActionListener {

    private final Consumer<IMqttToken> onSuccess;
    private final BiConsumer<IMqttToken, Throwable> onFailure;

    PahoActionListener(
        Consumer<IMqttToken> onSuccess, BiConsumer<IMqttToken, Throwable> onFailure) {
      this.onSuccess = onSuccess;
      this.onFailure = onFailure;
    }

    @Override
    public void onSuccess(IMqttToken asyncActionToken) {
      onSuccess.accept(asyncActionToken);
    }

    @Override
    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
      onFailure.accept(asyncActionToken, exception);
    }
  }

  // getters

  //@VisibleForTesting - using the same package in test  for test visibility
  IMqttAsyncClient getClient() {
    return client;
  }

  //@VisibleForTesting - using the same package in test for test visibility
  RxMqttCallback getCallback() {
    return callback;
  }

  //@VisibleForTesting - using the same package in test  for test visibility
  MqttConnectOptions getConnectOptions() {
    return connectOptions;
  }

  //@VisibleForTesting - using the same package in test  for test visibility
  BackpressureStrategy getBackpressureStrategy() {
    return globalBackpressureStrategy;
  }
}
