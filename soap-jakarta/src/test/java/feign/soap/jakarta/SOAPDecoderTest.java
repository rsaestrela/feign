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

import feign.FeignException;
import feign.Request;
import feign.Request.HttpMethod;
import feign.RequestTemplate;
import feign.Response;
import feign.Util;
import feign.codec.Encoder;
import feign.soap.jakarta.mocks.Box;
import feign.soap.jakarta.mocks.GetPrice;
import feign.soap.jakarta.mocks.Item;
import feign.soap.jakarta.mocks.ParameterizedHolder;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPFactory;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Collections;

import static feign.Util.UTF_8;
import static feign.assertj.FeignAssertions.assertThat;
import static org.junit.Assert.*;

public class SOAPDecoderTest {

  private static final RequestTemplate REQUEST_TEMPLATE = new RequestTemplate();

  @Test
  public void shouldDecodeSoap() throws IOException {

    GetPrice mock = new GetPrice();
    mock.setItem(new Item("Apples"));

    String mockSoapEnvelop = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
        + "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<SOAP-ENV:Header/>"
        + "<SOAP-ENV:Body>"
        + "<GetPrice xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://apihost/schema.xsd\">"
        + "<Item>Apples</Item>"
        + "</GetPrice>"
        + "</SOAP-ENV:Body>"
        + "</SOAP-ENV:Envelope>";

    Response response = Response.builder()
        .status(200)
        .reason("OK")
        .request(Request.create(HttpMethod.GET, "/api", Collections.emptyMap(), null, Util.UTF_8, REQUEST_TEMPLATE))
        .headers(Collections.emptyMap())
        .body(mockSoapEnvelop, UTF_8)
        .build();

    SOAPDecoder decoder = new SOAPDecoder();

    assertEquals(mock, decoder.decode(response, GetPrice.class));
  }

  @Test
  public void shouldDecodeSoapWithSchemaOnEnvelope() throws IOException {

    GetPrice mock = new GetPrice();
    mock.setItem(new Item("Apples"));

    String mockSoapEnvelop = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
        + "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" "
        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://apihost/schema.xsd\" "
        + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">"
        + "<SOAP-ENV:Header/>"
        + "<SOAP-ENV:Body>"
        + "<GetPrice>"
        + "<Item xsi:type=\"xsd:string\">Apples</Item>"
        + "</GetPrice>"
        + "</SOAP-ENV:Body>"
        + "</SOAP-ENV:Envelope>";

    Response response = Response.builder()
        .status(200)
        .reason("OK")
        .request(Request.create(HttpMethod.GET, "/api", Collections.emptyMap(), null, Util.UTF_8, REQUEST_TEMPLATE))
        .headers(Collections.emptyMap())
        .body(mockSoapEnvelop, UTF_8)
        .build();

    SOAPDecoder decoder = new SOAPDecoder.Builder()
        .useFirstChild()
        .build();

    assertEquals(mock, decoder.decode(response, GetPrice.class));

  }

  @Test
  public void shouldDecodeSoap1_2Protocol() throws IOException {

    GetPrice mock = new GetPrice();
    mock.setItem(new Item("Apples"));

    String mockSoapEnvelop = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
        + "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<SOAP-ENV:Header/>"
        + "<SOAP-ENV:Body>"
        + "<GetPrice xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://apihost/schema.xsd\">"
        + "<Item>Apples</Item>"
        + "</GetPrice>"
        + "</SOAP-ENV:Body>"
        + "</SOAP-ENV:Envelope>";

    Response response = Response.builder()
        .status(200)
        .reason("OK")
        .request(Request.create(HttpMethod.GET, "/api", Collections.emptyMap(), null, Util.UTF_8, REQUEST_TEMPLATE))
        .headers(Collections.emptyMap())
        .body(mockSoapEnvelop, UTF_8)
        .build();

    assertEquals(mock, new SOAPDecoder().decode(response, GetPrice.class));

  }

  @Test
  public void shouldNotDecodeParameterizedTypes() throws Exception {

    Type parameterized = ParameterizedHolder.class.getDeclaredField("field").getGenericType();

    try (Response response = Response.builder()
        .status(200)
        .reason("OK")
        .request(Request.create(HttpMethod.GET, "/api", Collections.emptyMap(), null, Util.UTF_8, new RequestTemplate()))
        .headers(Collections.emptyMap())
        .body("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
            + "<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<Header/>"
            + "<Body>"
            + "<GetPrice>"
            + "<Item>Apples</Item>"
            + "</GetPrice>"
            + "</Body>"
            + "</Envelope>", UTF_8)
        .build()) {
      assertThrows(
          "java.util.Map is an interface, and JAXB can't handle interfaces.\n"
              + "\tthis problem is related to the following location:\n"
              + "\t\tat java.util.Map",
          feign.codec.DecodeException.class,
          () -> new SOAPDecoder().decode(response, parameterized)
      );
    }

  }

  @Test
  public void shouldDecodeAnnotatedParameterizedTypes() throws IOException {

    JAXBContextFactory jaxbContextFactory = new CachingJAXBContextFactory.Builder()
        .withMarshallerFormattedOutput(true)
        .build();

    Encoder encoder = new SOAPEncoder.Builder()
        .withJAXBContextFactory(jaxbContextFactory)
        .build();

    Box<String> boxStr = new Box<>();
    boxStr.set("hello");
    Box<Box<String>> boxBoxStr = new Box<>();
    boxBoxStr.set(boxStr);
    RequestTemplate template = new RequestTemplate();
    encoder.encode(boxBoxStr, Box.class, template);

    Response response = Response.builder()
        .status(200)
        .reason("OK")
        .request(Request.create(HttpMethod.GET, "/api", Collections.emptyMap(), null, Util.UTF_8, REQUEST_TEMPLATE))
        .headers(Collections.emptyMap())
        .body(template.body())
        .build();

    new SOAPDecoder().decode(response, Box.class);

  }

  @Test
  public void notFoundShouldDecodeToNull() throws IOException {
    Response response = Response.builder()
        .status(404)
        .reason("NOT FOUND")
        .request(Request.create(HttpMethod.GET, "/api", Collections.emptyMap(), null, Util.UTF_8, REQUEST_TEMPLATE))
        .headers(Collections.emptyMap())
        .build();
    assertThat((byte[]) new SOAPDecoder().decode(response, byte[].class)).isEmpty();
  }

  @Test
  public void shouldChangeSoapProtocolAndSetHeader() {

    Encoder encoder = new SOAPEncoder.Builder()
        .withSOAPProtocol("SOAP 1.2 Protocol")
        .withSOAPMessageCustomizer(soapMessage -> {
          SOAPFactory soapFactory = SOAPFactory.newInstance();
          String uri = "http://schemas.xmlsoap.org/ws/2002/12/secext";
          String prefix = "wss";
          SOAPElement security = soapFactory.createElement("Security", prefix, uri);
          SOAPElement usernameToken = soapFactory.createElement("UsernameToken", prefix, uri);
          usernameToken.addChildElement("Username", prefix, uri).setValue("test");
          usernameToken.addChildElement("Password", prefix, uri).setValue("test");
          security.addChildElement(usernameToken);
          soapMessage.getSOAPHeader().addChildElement(security);
        })
        .build();

    GetPrice mock = new GetPrice();
    mock.setItem(new Item("Apples"));

    RequestTemplate template = new RequestTemplate();
    encoder.encode(mock, GetPrice.class, template);

    String soapEnvelop = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" +
        "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\">" +
        "<env:Header>" +
        (System.getProperty("java.version").startsWith("1.8")
            ? "<wss:Security xmlns:wss=\"http://schemas.xmlsoap.org/ws/2002/12/secext\">"
            : "<wss:Security xmlns=\"http://schemas.xmlsoap.org/ws/2002/12/secext\" xmlns:wss=\"http://schemas.xmlsoap.org/ws/2002/12/secext\">")
        +
        "<wss:UsernameToken>" +
        "<wss:Username>test</wss:Username>" +
        "<wss:Password>test</wss:Password>" +
        "</wss:UsernameToken>" +
        "</wss:Security>" +
        "</env:Header>" +
        "<env:Body>" +
        "<GetPrice>" +
        "<Item>Apples</Item>" +
        "</GetPrice>" +
        "</env:Body>" +
        "</env:Envelope>";

    assertThat(template).hasBody(soapEnvelop);

  }

  @Test
  public void shouldThrowSOAPFaultException() throws IOException {

    try (Response response = Response.builder()
        .status(200)
        .reason("OK")
        .request(Request.create(HttpMethod.GET, "/api", Collections.emptyMap(), null, Util.UTF_8, REQUEST_TEMPLATE))
        .headers(Collections.emptyMap())
        .body(getResourceBytes("/samples/SOAP_1_2_FAULT.xml"))
        .build()) {
      assertThrows("Processing error", SOAPFaultException.class, () -> new SOAPDecoder.Builder()
          .withSOAPProtocol(SOAPConstants.SOAP_1_2_PROTOCOL).build()
          .decode(response, Object.class));
    }

  }

  @Test
  public void shouldReturnSOAPFaultException() throws IOException {

    Response response = Response.builder()
        .status(400)
        .reason("BAD REQUEST")
        .request(Request.create(HttpMethod.GET, "/api", Collections.emptyMap(), null, Util.UTF_8, REQUEST_TEMPLATE))
        .headers(Collections.emptyMap())
        .body(getResourceBytes("/samples/SOAP_1_1_FAULT.xml"))
        .build();

    Exception error = new SOAPErrorDecoder().decode("Service#foo()", response);
    Assertions.assertThat(error).isInstanceOf(SOAPFaultException.class)
        .hasMessage("Message was not SOAP 1.1 compliant");

  }

  @Test
  public void shouldReturnFeignExceptionOn503Status() {

    Response response = Response.builder()
        .status(503)
        .reason("Service Unavailable")
        .request(Request.create(HttpMethod.GET, "/api", Collections.emptyMap(), null, Util.UTF_8, REQUEST_TEMPLATE))
        .headers(Collections.emptyMap())
        .body("Service Unavailable", UTF_8)
        .build();

    Exception error = new SOAPErrorDecoder().decode("Service#foo()", response);
    Assertions.assertThat(error).isInstanceOf(FeignException.class)
        .hasMessage(
            "[503 Service Unavailable] during [GET] to [/api] [Service#foo()]: [Service Unavailable]");
  }

  @Test
  public void shouldReturnFeignExceptionOnEmptyFault() {

    String responseBody = "<?xml version = '1.0' encoding = 'UTF-8'?>\n" +
        "<SOAP-ENV:Envelope\n" +
        "   xmlns:SOAP-ENV = \"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
        "   xmlns:xsi = \"http://www.w3.org/1999/XMLSchema-instance\"\n" +
        "   xmlns:xsd = \"http://www.w3.org/1999/XMLSchema\">\n" +
        "   <SOAP-ENV:Body>\n" +
        "   </SOAP-ENV:Body>\n" +
        "</SOAP-ENV:Envelope>";

    Response response = Response.builder()
        .status(500)
        .reason("Internal Server Error")
        .request(Request.create(HttpMethod.GET, "/api", Collections.emptyMap(), null, Util.UTF_8, REQUEST_TEMPLATE))
        .headers(Collections.emptyMap())
        .body(responseBody, UTF_8)
        .build();

    Exception error = new SOAPErrorDecoder().decode("Service#foo()", response);
    Assertions.assertThat(error).isInstanceOf(FeignException.class)
        .hasMessage("[500 Internal Server Error] during [GET] to [/api] [Service#foo()]: ["
            + responseBody + "]");

  }

  private static byte[] getResourceBytes(String resourcePath) throws IOException {
    InputStream resourceAsStream = SOAPDecoderTest.class.getResourceAsStream(resourcePath);
    assertNotNull(resourceAsStream);
    byte[] bytes = new byte[resourceAsStream.available()];
    new DataInputStream(resourceAsStream).readFully(bytes);
    return bytes;
  }

}
