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
import java.util.Optional;
import org.eclipse.paho.client.mqttv3.IMqttToken;

public class PahoRxMqttException extends RxMqttException {

  private static final long serialVersionUID = 4713713128270173625L;

  private final IMqttToken token;

  public PahoRxMqttException(IMqttToken token) {
    this(token.getException(), token);
  }

  public PahoRxMqttException(Throwable cause) {
    this(cause, null);
  }

  public PahoRxMqttException(Throwable cause, IMqttToken token) {
    this(Optional.ofNullable(cause).map(Throwable::getMessage).orElse(null), cause, token);
  }

  public PahoRxMqttException(String message, Throwable cause, IMqttToken token) {
    super(message, cause);
    this.token = token;
  }

  public IMqttToken getToken() {
    return token;
  }
}
