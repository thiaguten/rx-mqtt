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
import java.util.function.Consumer;

public interface RxMqttClient {

  Single<String> getClientId();

  Single<String> getServerUri();

  Single<Boolean> isConnected();

  Completable close();

  Single<RxMqttToken> connect();

  Single<RxMqttToken> publish(String topic, RxMqttMessage message);

  Flowable<RxMqttMessage> on(String topic);

  Flowable<RxMqttMessage> on(String topic, Consumer<RxMqttToken> doOnComplete);

  Flowable<RxMqttMessage> on(String[] topics);

  Flowable<RxMqttMessage> on(String[] topics, Consumer<RxMqttToken> doOnComplete);

  Flowable<RxMqttMessage> on(String topic, RxMqttQoS qos);

  Flowable<RxMqttMessage> on(String topic, RxMqttQoS qos, Consumer<RxMqttToken> doOnComplete);

  Flowable<RxMqttMessage> on(String[] topics, RxMqttQoS[] qos);

  Flowable<RxMqttMessage> on(String[] topics, RxMqttQoS[] qos, Consumer<RxMqttToken> doOnComplete);

  Flowable<RxMqttMessage> on(String topic, BackpressureStrategy strategy);

  Flowable<RxMqttMessage> on(String topic, BackpressureStrategy strategy,
      Consumer<RxMqttToken> doOnComplete);

  Flowable<RxMqttMessage> on(String topic, RxMqttQoS qos, BackpressureStrategy strategy);

  Flowable<RxMqttMessage> on(String topic, RxMqttQoS qos, BackpressureStrategy strategy,
      Consumer<RxMqttToken> doOnComplete);

  Flowable<RxMqttMessage> on(String[] topics, RxMqttQoS[] qos, BackpressureStrategy strategy);

  Flowable<RxMqttMessage> on(String[] topics, RxMqttQoS[] qos, BackpressureStrategy strategy,
      Consumer<RxMqttToken> doOnComplete);

  Single<RxMqttToken> off(String... topic);

  Single<RxMqttToken> disconnect();

  Completable disconnectForcibly();

  default Completable offAndClose(String... topics) {
    Completable close = close();
    Completable disconnForcibly = disconnectForcibly().onErrorResumeNext(e -> close);
    Completable disconnect = disconnect().ignoreElement();
    Completable off = off(topics).ignoreElement().onErrorResumeNext(e -> disconnect);

    return isConnected()
        .flatMapCompletable(connected -> connected ? off : close)
        .andThen(disconnect).onErrorResumeNext(e -> disconnForcibly)
        .andThen(close);
  }

}
