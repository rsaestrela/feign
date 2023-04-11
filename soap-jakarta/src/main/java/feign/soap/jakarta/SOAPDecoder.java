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
import feign.Util;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.soap.SOAPFaultException;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public final class SOAPDecoder implements Decoder {

  private final JAXBContextFactory jaxbContextFactory;
  private final String soapProtocol;
  private final boolean useFirstChild;

  public SOAPDecoder() {
    this(new SOAPDecoder.Builder());
  }

  private SOAPDecoder(Builder builder) {
    this.soapProtocol = builder.soapProtocol;
    this.jaxbContextFactory = builder.jaxbContextFactory;
    this.useFirstChild = builder.useFirstChild;
  }

  @Override
  public Object decode(Response response, Type type) throws IOException {
    if (response.status() == 404 || response.status() == 204) {
      return Util.emptyValueOf(type);
    }
    if (response.body() == null) {
      return null;
    }
    if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      type = parameterizedType.getRawType();
    }
    if (!(type instanceof Class)) {
      throw new SOAPUnsupportedRawTypeException(type);
    }
    try (Response.Body body = response.body()) {
      SOAPMessage soapMessage = getSoapMessage(body);
      Unmarshaller unmarshaller = jaxbContextFactory.createUnmarshaller((Class<?>) type);
      return this.useFirstChild ?
          unmarshaller.unmarshal(soapMessage.getSOAPBody().getFirstChild()) :
          unmarshaller.unmarshal(soapMessage.getSOAPBody().extractContentAsDocument());
    } catch (SOAPException | JAXBException e) {
      throw new DecodeException(response.status(), e.toString(), response.request(), e);
    }
  }

  private SOAPMessage getSoapMessage(Response.Body body) throws SOAPException, IOException {
    SOAPMessage soapMessage = MessageFactory.newInstance(soapProtocol).createMessage(null, body.asInputStream());
    if (soapMessage.getSOAPBody() != null && soapMessage.getSOAPBody().hasFault()) {
      throw new SOAPFaultException(soapMessage.getSOAPBody().getFault());
    }
    return soapMessage;
  }

  public static class Builder {

    private JAXBContextFactory jaxbContextFactory = new CachingJAXBContextFactory();
    private String soapProtocol = SOAPConstants.DEFAULT_SOAP_PROTOCOL;
    private boolean useFirstChild = false;

    public Builder withJAXBContextFactory(JAXBContextFactory jaxbContextFactory) {
      this.jaxbContextFactory = jaxbContextFactory;
      return this;
    }

    public Builder withSOAPProtocol(String soapProtocol) {
      this.soapProtocol = soapProtocol;
      return this;
    }

    public Builder useFirstChild() {
      this.useFirstChild = true;
      return this;
    }

    public SOAPDecoder build() {
      return new SOAPDecoder(this);
    }

  }

}
