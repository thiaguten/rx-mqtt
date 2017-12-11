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

import br.com.thiaguten.rx.mqtt.api.RxMqttCallback;
import br.com.thiaguten.rx.mqtt.api.RxMqttToken;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public abstract class PahoRxMqttCallback implements RxMqttCallback, MqttCallbackExtended {

  @Override
  public final void messageArrived(String topic, MqttMessage message) throws Exception {
    // NOP
  }

  @Override
  public final void deliveryComplete(IMqttDeliveryToken token) {
    deliveryComplete(new PahoRxMqttToken(token));
  }

  // convenient methods

  public static PahoRxMqttCallback create(
      Consumer<Throwable> onConnectionLost,
      BiConsumer<Boolean, String> onConnectComplete) {
    return create(
        onConnectionLost,
        onConnectComplete,
        (t) -> { /*NOP*/ });
  }

  public static PahoRxMqttCallback create(
      Consumer<Throwable> onConnectionLost,
      BiConsumer<Boolean, String> onConnectComplete,
      Consumer<RxMqttToken> onDeliveryComplete) {
    return new PahoRxMqttCallback() {
      @Override
      public void connectionLost(Throwable cause) {
        onConnectionLost.accept(cause);
      }

      @Override
      public void connectComplete(boolean reconnect, String serverUri) {
        onConnectComplete.accept(reconnect, serverUri);
      }

      @Override
      public final void deliveryComplete(RxMqttToken token) {
        onDeliveryComplete.accept(token);
      }
    };
  }
}
