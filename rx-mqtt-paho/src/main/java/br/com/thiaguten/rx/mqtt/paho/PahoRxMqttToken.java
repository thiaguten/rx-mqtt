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

import br.com.thiaguten.rx.mqtt.api.RxMqttException;
import br.com.thiaguten.rx.mqtt.api.RxMqttMessage;
import br.com.thiaguten.rx.mqtt.api.RxMqttToken;
import java.util.Objects;
import java.util.Optional;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;

public class PahoRxMqttToken implements RxMqttToken {

  private final IMqttToken token;

  public PahoRxMqttToken(IMqttToken token) {
    this.token = Objects.requireNonNull(token);
  }

  @Override
  public String getClientId() {
    return Optional.ofNullable(token.getClient()).map(IMqttAsyncClient::getClientId).orElse(null);
  }

  @Override
  public boolean isComplete() {
    return token.isComplete();
  }

  @Override
  public String[] getTopics() {
    return token.getTopics();
  }

  @Override
  public int getMessageId() {
    return token.getMessageId();
  }

  @Override
  public int[] getGrantedQos() {
    return token.getGrantedQos();
  }

  @Override
  public boolean getSessionPresent() {
    return token.getSessionPresent();
  }

  @Override
  public RxMqttMessage getMessage() {
    return Optional.of(token)
        .filter(t -> IMqttDeliveryToken.class.isAssignableFrom(t.getClass()))
        .map(t -> {
          try {
            return IMqttDeliveryToken.class.cast(t).getMessage();
          } catch (MqttException me) {
            throw new PahoRxMqttException(me, t);
          }
        })
        .map(PahoRxMqttMessage::create)
        .orElse(null);
  }

  @Override
  public RxMqttException getException() {
    return Optional.of(token)
        .filter(t -> t.getException() != null)
        .map(PahoRxMqttException::new)
        .orElse(null);
  }
}
