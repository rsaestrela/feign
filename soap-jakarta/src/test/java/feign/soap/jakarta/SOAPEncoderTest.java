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
import feign.codec.Encoder;
import feign.soap.jakarta.mocks.GetPrice;
import feign.soap.jakarta.mocks.Item;
import feign.soap.jakarta.mocks.ParameterizedHolder;
import org.junit.Test;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static feign.assertj.FeignAssertions.assertThat;
import static org.junit.Assert.assertThrows;

public class SOAPEncoderTest {

  @Test
  public void shouldEncodesSoap() {

    Encoder encoder = new SOAPEncoder.Builder()
        .withJAXBContextFactory(new CachingJAXBContextFactory.Builder().build())
        .build();

    GetPrice mock = new GetPrice();
    mock.setItem(new Item("Apples"));

    RequestTemplate template = new RequestTemplate();
    encoder.encode(mock, GetPrice.class, template);

    String soapEnvelop = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" +
        "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
        "<SOAP-ENV:Header/>" +
        "<SOAP-ENV:Body>" +
        "<GetPrice>" +
        "<Item>Apples</Item>" +
        "</GetPrice>" +
        "</SOAP-ENV:Body>" +
        "</SOAP-ENV:Envelope>";
    assertThat(template).hasBody(soapEnvelop);

  }

  @Test
  public void shouldNotEncodeParameterizedTypes() throws Exception {

    Type parameterized = ParameterizedHolder.class.getDeclaredField("field").getGenericType();

    RequestTemplate template = new RequestTemplate();

    assertThrows(
        "SOAP only supports encoding raw types. Found java.util.Map<java.lang.String, ?>",
        SOAPUnsupportedRawTypeException.class,
        () -> new SOAPEncoder().encode(Collections.emptyMap(), parameterized, template)
    );

  }

  @Test
  public void shouldEncodeSoapWithCustomJAXBMarshallerEncoding() {

    JAXBContextFactory jaxbContextFactory =
        new CachingJAXBContextFactory.Builder().withMarshallerJAXBEncoding("UTF-16").build();

    Encoder encoder = new SOAPEncoder.Builder()
        .withJAXBContextFactory(jaxbContextFactory)
        .withCharsetEncoding(StandardCharsets.UTF_16)
        .build();

    GetPrice mock = new GetPrice();
    mock.setItem(new Item("Apples"));

    RequestTemplate template = new RequestTemplate();
    encoder.encode(mock, GetPrice.class, template);

    String soapEnvelop = "<?xml version=\"1.0\" encoding=\"UTF-16\" ?>" +
        "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
        "<SOAP-ENV:Header/>" +
        "<SOAP-ENV:Body>" +
        "<GetPrice>" +
        "<Item>Apples</Item>" +
        "</GetPrice>" +
        "</SOAP-ENV:Body>" +
        "</SOAP-ENV:Envelope>";

    byte[] utf16Bytes = soapEnvelop.getBytes(StandardCharsets.UTF_16LE);
    assertThat(template).hasBody(utf16Bytes);

  }

  @Test
  public void shouldEncodeSoapWithCustomJAXBSchemaLocation() {

    JAXBContextFactory jaxbContextFactory =
        new CachingJAXBContextFactory.Builder()
            .withMarshallerSchemaLocation("http://apihost http://apihost/schema.xsd")
            .build();

    Encoder encoder = new SOAPEncoder(
        new SOAPEncoder.Builder().withJAXBContextFactory(jaxbContextFactory));

    GetPrice mock = new GetPrice();
    mock.setItem(new Item("Apples"));

    RequestTemplate template = new RequestTemplate();
    encoder.encode(mock, GetPrice.class, template);

    assertThat(template).hasBody("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
        + "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<SOAP-ENV:Header/>"
        + "<SOAP-ENV:Body>"
        + "<GetPrice xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://apihost http://apihost/schema.xsd\">"
        + "<Item>Apples</Item>"
        + "</GetPrice>"
        + "</SOAP-ENV:Body>"
        + "</SOAP-ENV:Envelope>");
  }

  @Test
  public void shouldEncodesSoapWithCustomJAXBNoSchemaLocation() {

    JAXBContextFactory jaxbContextFactory =
        new CachingJAXBContextFactory.Builder()
            .withMarshallerNoNamespaceSchemaLocation("http://apihost/schema.xsd")
            .build();

    Encoder encoder = new SOAPEncoder(new SOAPEncoder.Builder().withJAXBContextFactory(jaxbContextFactory));

    GetPrice mock = new GetPrice();
    mock.setItem(new Item("Apples"));

    RequestTemplate template = new RequestTemplate();
    encoder.encode(mock, GetPrice.class, template);

    assertThat(template).hasBody("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
        + "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<SOAP-ENV:Header/>"
        + "<SOAP-ENV:Body>"
        + "<GetPrice xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://apihost/schema.xsd\">"
        + "<Item>Apples</Item>"
        + "</GetPrice>"
        + "</SOAP-ENV:Body>"
        + "</SOAP-ENV:Envelope>");
  }

  @Test
  public void shouldEncodesSoapWithCustomJAXBFormattedOutput() {

    Encoder encoder = new SOAPEncoder.Builder().withFormattedOutput(true)
        .withJAXBContextFactory(new CachingJAXBContextFactory.Builder().build())
        .build();

    GetPrice mock = new GetPrice();
    mock.setItem(new Item("Apples"));

    RequestTemplate template = new RequestTemplate();
    encoder.encode(mock, GetPrice.class, template);

    assertThat(template).hasBody(
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" + System.lineSeparator() +
            "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + System.lineSeparator() +
            "    <SOAP-ENV:Header/>" + System.lineSeparator() +
            "    <SOAP-ENV:Body>" + System.lineSeparator() +
            "        <GetPrice>" + System.lineSeparator() +
            "            <Item>Apples</Item>" + System.lineSeparator() +
            "        </GetPrice>" + System.lineSeparator() +
            "    </SOAP-ENV:Body>" + System.lineSeparator() +
            "</SOAP-ENV:Envelope>" + System.lineSeparator() +
            "");
  }

}
