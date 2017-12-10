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

import java.util.HashMap;
import java.util.Map;

public enum RxMqttQoS {

  /**
   * RxMqttQoS 0 - At most once delivery: With this setting, messages are delivered according to the
   * best effort of the underlying network. A response is not expected and no retry semantics are
   * defined in the protocol. This is the least level of Quality of Service and from a performance
   * perspective, adds value as it’s the fastest way to send a message using MQTT. A RxMqttQoS 0
   * message can get lost if the client unexpectedly disconnects or if the server fails.
   */
  AT_MOST_ONCE(0),

  /**
   * RxMqttQoS 1 - At least Once Delivery: For this level of service, the MQTT client or the server
   * would attempt to deliver the message at-least once. But there can be a duplicate message.
   */
  AT_LEAST_ONCE(1),

  /**
   * RxMqttQoS 2 - Exactly once delivery: This is the highest level of Quality of Service.
   * Additional protocol flows ensure that duplicate messages are not delivered to the receiving
   * application. The message is delivered once and only once when RxMqttQoS 2 is used.
   */
  EXACTLY_ONCE(2),

  FAILURE(0x80);

  private static final class Holder {

    static final Map<Integer, RxMqttQoS> map = new HashMap<>();
  }

  private final int value;

  RxMqttQoS(int value) {
    this.value = value;
    Holder.map.put(value, this);
  }

  public int value() {
    return value;
  }

  public static RxMqttQoS valueOf(int value) {
    return Holder.map.getOrDefault(value, FAILURE);
  }

}
