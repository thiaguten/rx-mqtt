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

package br.com.thiaguten.rx.mqtt.api;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.util.Arrays;
import java.util.Optional;

public interface RxMqttClient {

  Single<String> getClientId();

  Single<String> getServerUri();

  Single<Boolean> isConnected();

  Completable close();

  Single<RxMqttToken> connect();

  Single<RxMqttToken> publish(String topic, RxMqttMessage message);

  Single<RxMqttToken> publish(String topic, byte[] message);

  Single<RxMqttToken> publish(String topic, byte[] message, int qos);

  Single<RxMqttToken> publish(String topic, String message);

  Single<RxMqttToken> publish(String topic, String message, int qos);

  Flowable<RxMqttMessage> on(String[] topics, RxMqttQoS[] qos);

  default Flowable<RxMqttMessage> on(String[] topics, int[] qos) {
    return on(topics, Arrays.stream(qos).boxed().map(RxMqttQoS::valueOf).toArray(RxMqttQoS[]::new));
  }

  default Flowable<RxMqttMessage> on(String topic, RxMqttQoS qos) {
    return on(new String[]{topic}, new RxMqttQoS[]{qos});
  }

  default Flowable<RxMqttMessage> on(String topic, int qos) {
    return on(topic, RxMqttQoS.valueOf(qos));
  }

  default Flowable<RxMqttMessage> on(String topic) {
    return on(topic, RxMqttQoS.EXACTLY_ONCE);
  }

  Flowable<RxMqttMessage> on(
      String[] topics, RxMqttQoS[] qos, BackpressureStrategy backpressureStrategy);

  default Flowable<RxMqttMessage> on(
      String[] topics, int[] qos, BackpressureStrategy backpressureStrategy) {
    return on(
        topics,
        Arrays.stream(qos).boxed().map(RxMqttQoS::valueOf).toArray(RxMqttQoS[]::new),
        backpressureStrategy);
  }

  default Flowable<RxMqttMessage> on(
      String topic, RxMqttQoS qos, BackpressureStrategy backpressureStrategy) {
    return on(new String[]{topic}, new RxMqttQoS[]{qos}, backpressureStrategy);
  }

  default Flowable<RxMqttMessage> on(
      String topic, int qos, BackpressureStrategy backpressureStrategy) {
    return on(topic, RxMqttQoS.valueOf(qos), backpressureStrategy);
  }

  default Flowable<RxMqttMessage> on(String topic, BackpressureStrategy backpressureStrategy) {
    return on(topic, RxMqttQoS.EXACTLY_ONCE, backpressureStrategy);
  }

  Single<RxMqttToken> off(String... topic);

  Single<RxMqttToken> disconnect();

  Completable disconnectForcibly();

  default Completable offAndClose(String... topics) {
    Optional.ofNullable(topics)
        .filter(tps -> isConnected().blockingGet())
        .ifPresent(tps -> {
          off(tps).blockingGet();
          disconnect()
              .doOnError(disconnectError -> disconnectForcibly().blockingAwait())
              .blockingGet();
        });
    return close();
  }

}
