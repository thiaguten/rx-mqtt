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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PahoRxMqttTokenTest {

  @Test(expected = NullPointerException.class)
  public void whenANullMqttTokenIsSuppliedThenAnExceptionIsThrown() {
    new PahoRxMqttToken((IMqttToken) null);
  }

  @Test
  public void whenAMqttTokenIsSuppliedThenCreateSuccessfully() {
    String clientId = "clientId";

    IMqttAsyncClient client = mock(IMqttAsyncClient.class);
    assertThat(client).isNotNull();
    when(client.getClientId()).thenReturn(clientId);

    IMqttToken mqttToken = mock(IMqttToken.class);
    assertThat(mqttToken).isNotNull();
    when(mqttToken.getClient()).thenReturn(client);
    when(mqttToken.isComplete()).thenReturn(true);
    when(mqttToken.getTopics()).thenReturn(new String[]{"topic"});
    when(mqttToken.getMessageId()).thenReturn(1);
    when(mqttToken.getGrantedQos()).thenReturn(new int[]{2});
    when(mqttToken.getSessionPresent()).thenReturn(false);
    when(mqttToken.getException()).thenReturn(null);

    PahoRxMqttToken rxMqttToken = new PahoRxMqttToken(mqttToken);
    assertThat(rxMqttToken).isNotNull();
    assertThat(rxMqttToken.getClientId()).isEqualTo(mqttToken.getClient().getClientId());
    assertThat(rxMqttToken.isComplete()).isEqualTo(mqttToken.isComplete());
    assertThat(rxMqttToken.getTopics()).isEqualTo(mqttToken.getTopics());
    assertThat(rxMqttToken.getMessageId()).isEqualTo(mqttToken.getMessageId());
    assertThat(rxMqttToken.getGrantedQos()).isEqualTo(mqttToken.getGrantedQos());
    assertThat(rxMqttToken.getSessionPresent()).isEqualTo(mqttToken.getSessionPresent());
    assertThat(rxMqttToken.getException()).isEqualTo(mqttToken.getException());
    assertThat(rxMqttToken.getMessage()).isNull();

    verify(mqttToken, times(2)).getClient();
    verify(mqttToken.getClient(), times(2)).getClientId();
    verify(mqttToken, times(2)).isComplete();
    verify(mqttToken, times(2)).getTopics();
    verify(mqttToken, times(2)).getMessageId();
    verify(mqttToken, times(2)).getGrantedQos();
    verify(mqttToken, times(2)).getSessionPresent();
    verify(mqttToken, times(2)).getException();
  }

  @Test
  public void whenAMqttDeliveryTokenIsSuppliedThenCreateSuccessfully() throws MqttException {
    MqttException mqttException = mock(MqttException.class);
    assertThat(mqttException).isNotNull();

    MqttMessage mqttMessage = mock(MqttMessage.class);
    assertThat(mqttMessage).isNotNull();

    IMqttDeliveryToken mqttToken = mock(IMqttDeliveryToken.class);
    assertThat(mqttToken).isNotNull();
    when(mqttToken.getClient()).thenReturn(null);
    when(mqttToken.isComplete()).thenReturn(true);
    when(mqttToken.getTopics()).thenReturn(new String[]{"topic"});
    when(mqttToken.getMessageId()).thenReturn(1);
    when(mqttToken.getGrantedQos()).thenReturn(new int[]{2});
    when(mqttToken.getSessionPresent()).thenReturn(false);
    when(mqttToken.getException()).thenReturn(mqttException);
    when(mqttToken.getMessage()).thenReturn(mqttMessage);

    PahoRxMqttToken rxMqttToken = new PahoRxMqttToken(mqttToken);
    assertThat(rxMqttToken).isNotNull();
    assertThat(rxMqttToken.getClientId()).isNull();
    assertThat(rxMqttToken.isComplete()).isEqualTo(mqttToken.isComplete());
    assertThat(rxMqttToken.getTopics()).isEqualTo(mqttToken.getTopics());
    assertThat(rxMqttToken.getMessageId()).isEqualTo(mqttToken.getMessageId());
    assertThat(rxMqttToken.getGrantedQos()).isEqualTo(mqttToken.getGrantedQos());
    assertThat(rxMqttToken.getSessionPresent()).isEqualTo(mqttToken.getSessionPresent());
    assertThat(rxMqttToken.getException()).isExactlyInstanceOf(PahoRxMqttException.class);
    assertThat(rxMqttToken.getException()).hasCauseInstanceOf(MqttException.class);
    assertThat(rxMqttToken.getMessage()).isExactlyInstanceOf(PahoRxMqttMessage.class);

    verify(mqttToken).getClient();
    verify(mqttToken, times(2)).isComplete();
    verify(mqttToken, times(2)).getTopics();
    verify(mqttToken, times(2)).getMessageId();
    verify(mqttToken, times(2)).getGrantedQos();
    verify(mqttToken, times(2)).getSessionPresent();
    verify(mqttToken, times(4)).getException();
    verify(mqttToken).getMessage();
  }

  @Test
  public void whenAMqttDeliveryTokenIsSuppliedThenGetMessageFails() throws MqttException {
    IMqttDeliveryToken mqttToken = mock(IMqttDeliveryToken.class);
    assertThat(mqttToken).isNotNull();
    doThrow(new MqttException(MqttException.REASON_CODE_UNEXPECTED_ERROR)).when(mqttToken).getMessage();

    PahoRxMqttToken rxMqttToken = new PahoRxMqttToken(mqttToken);
    assertThat(rxMqttToken).isNotNull();

    try {
      rxMqttToken.getMessage();
    } catch (Exception e) {
      assertThat(e).isExactlyInstanceOf(PahoRxMqttException.class);
      assertThat(e).hasCauseExactlyInstanceOf(MqttException.class);
      assertThat(e).hasMessageContaining("Erro inesperado");
    }

    verify(mqttToken).getMessage();
  }

}
