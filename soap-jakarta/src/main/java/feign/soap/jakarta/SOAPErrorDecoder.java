/*
 * Copyright 2012-2023 The Feign Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package feign.soap.jakarta;

import feign.Response;
import feign.codec.ErrorDecoder;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.soap.SOAPFaultException;

import java.io.IOException;

public class SOAPErrorDecoder implements ErrorDecoder {

  private final String soapProtocol;

  public SOAPErrorDecoder() {
    this.soapProtocol = SOAPConstants.DEFAULT_SOAP_PROTOCOL;
  }

  public SOAPErrorDecoder(String soapProtocol) {
    this.soapProtocol = soapProtocol;
  }

  @Override
  public Exception decode(String methodKey, Response response) {
    if (response.body() == null || response.status() == 503) {
      return defaultErrorDecoder(methodKey, response);
    }
    try {
      SOAPMessage message = MessageFactory.newInstance(soapProtocol).createMessage(null,
          response.body().asInputStream());
      if (message.getSOAPBody() != null && message.getSOAPBody().hasFault()) {
        return new SOAPFaultException(message.getSOAPBody().getFault());
      }
    } catch (SOAPException | IOException e) {
      // ignored
    }
    return defaultErrorDecoder(methodKey, response);
  }

  private Exception defaultErrorDecoder(String methodKey, Response response) {
    return new ErrorDecoder.Default().decode(methodKey, response);
  }

}
