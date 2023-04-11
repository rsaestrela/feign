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

import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public final class SOAPEncoder implements Encoder {

  private static final DocumentBuilder DOCUMENT_BUILDER;
  private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

  static {
    try {
      DOCUMENT_BUILDER = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  private final JAXBContextFactory jaxbContextFactory;
  private final boolean writeXmlDeclaration;
  private final Charset charsetEncoding;
  private final String soapProtocol;
  private final boolean formattedOutput;
  private final SOAPMessageCustomizer soapMessageCustomizer;

  public SOAPEncoder() {
    this(new SOAPEncoder.Builder());
  }

  public SOAPEncoder(Builder builder) {
    this.jaxbContextFactory = builder.jaxbContextFactory;
    this.writeXmlDeclaration = builder.writeXmlDeclaration;
    this.charsetEncoding = builder.charsetEncoding;
    this.soapProtocol = builder.soapProtocol;
    this.formattedOutput = builder.formattedOutput;
    this.soapMessageCustomizer = builder.soapMessageCustomizer;
  }

  @Override
  public void encode(Object object, Type type, RequestTemplate template) {
    if (!(type instanceof Class)) {
      throw new SOAPUnsupportedRawTypeException(type);
    }
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      Document document = DOCUMENT_BUILDER.newDocument();
      Marshaller marshaller = jaxbContextFactory.createMarshaller((Class<?>) type);
      marshaller.marshal(object, document);
      SOAPMessage soapMessage = MessageFactory.newInstance(soapProtocol).createMessage();
      soapMessage.setProperty(SOAPMessage.WRITE_XML_DECLARATION, Boolean.toString(writeXmlDeclaration));
      soapMessage.setProperty(SOAPMessage.CHARACTER_SET_ENCODING, charsetEncoding.displayName());
      soapMessage.getSOAPBody().addDocument(document);
      soapMessageCustomizer.customize(soapMessage);
      if (formattedOutput) {
        formatOutput(soapMessage, outputStream);
      } else {
        soapMessage.writeTo(outputStream);
      }
      template.body(outputStream.toString());
    } catch (SOAPException | JAXBException | IOException | TransformerFactoryConfigurationError |
             TransformerException e) {
      throw new EncodeException(e.toString(), e);
    }
  }

  private void formatOutput(SOAPMessage soapMessage, ByteArrayOutputStream outputStream) throws TransformerException {
    Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
    transformer.transform(new DOMSource(soapMessage.getSOAPPart()), new StreamResult(outputStream));
  }

  /**
   * Creates instances of {@link SOAPEncoder}.
   */
  public static class Builder {

    private JAXBContextFactory jaxbContextFactory = new CachingJAXBContextFactory();
    private boolean writeXmlDeclaration = true;
    private Charset charsetEncoding = StandardCharsets.UTF_8;
    private String soapProtocol = SOAPConstants.SOAP_1_1_PROTOCOL;
    private boolean formattedOutput;
    private SOAPMessageCustomizer soapMessageCustomizer = soapMessage -> {
    };

    /**
     * The {@link JAXBContextFactory} for body part.
     */
    public Builder withJAXBContextFactory(JAXBContextFactory jaxbContextFactory) {
      this.jaxbContextFactory = jaxbContextFactory;
      return this;
    }

    /**
     * Output format indent if true. Default is false
     */
    public Builder withFormattedOutput(boolean formattedOutput) {
      this.formattedOutput = formattedOutput;
      return this;
    }

    /**
     * Write the xml declaration if true. Default is true
     */
    public Builder withWriteXmlDeclaration(boolean writeXmlDeclaration) {
      this.writeXmlDeclaration = writeXmlDeclaration;
      return this;
    }

    /**
     * Specify the charset encoding. Default is {@link Charset#defaultCharset()}.
     */
    public Builder withCharsetEncoding(Charset charsetEncoding) {
      this.charsetEncoding = charsetEncoding;
      return this;
    }

    /**
     * The protocol used to create message factory. Default is "SOAP 1.1 Protocol".
     *
     * @param soapProtocol a string constant representing the MessageFactory protocol.
     * @see SOAPConstants#SOAP_1_1_PROTOCOL
     * @see SOAPConstants#SOAP_1_2_PROTOCOL
     * @see SOAPConstants#DYNAMIC_SOAP_PROTOCOL
     * @see MessageFactory#newInstance(String)
     */
    public Builder withSOAPProtocol(String soapProtocol) {
      this.soapProtocol = soapProtocol;
      return this;
    }

    /**
     * Add a SOAP Message customizer
     *
     * @param soapMessageCustomizer a {@link Consumer} responsible for the customization
     * @see SOAPConstants#SOAP_1_1_PROTOCOL
     * @see SOAPConstants#SOAP_1_2_PROTOCOL
     * @see SOAPConstants#DYNAMIC_SOAP_PROTOCOL
     * @see MessageFactory#newInstance(String)
     */
    public Builder withSOAPMessageCustomizer(SOAPMessageCustomizer soapMessageCustomizer) {
      this.soapMessageCustomizer = soapMessageCustomizer;
      return this;
    }

    public SOAPEncoder build() {
      return new SOAPEncoder(this);
    }

  }

}
