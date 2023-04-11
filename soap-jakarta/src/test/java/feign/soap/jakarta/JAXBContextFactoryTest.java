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

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.Assert.*;

public class JAXBContextFactoryTest {

  @Test
  public void shouldBuildMarshallerWithJAXBEncodingProperty() throws JAXBException {

    JAXBContextFactory factory = new CachingJAXBContextFactory.Builder()
        .withMarshallerJAXBEncoding("UTF-16")
        .build();

    Marshaller marshaller = factory.createMarshaller(Object.class);
    assertEquals("UTF-16", marshaller.getProperty(Marshaller.JAXB_ENCODING));
  }

  @Test
  public void shouldBuildMarshallerWithSchemaLocationProperty() throws JAXBException {

    JAXBContextFactory factory = new CachingJAXBContextFactory.Builder()
        .withMarshallerSchemaLocation("http://apihost http://apihost/schema.xsd")
        .build();

    Marshaller marshaller = factory.createMarshaller(Object.class);
    assertEquals("http://apihost http://apihost/schema.xsd",
        marshaller.getProperty(Marshaller.JAXB_SCHEMA_LOCATION));
  }

  @Test
  public void shouldBuildMarshallerWithNoNamespaceSchemaLocationProperty() throws JAXBException {

    JAXBContextFactory factory = new CachingJAXBContextFactory.Builder()
        .withMarshallerNoNamespaceSchemaLocation("http://apihost/schema.xsd")
        .build();

    Marshaller marshaller = factory.createMarshaller(Object.class);
    assertEquals("http://apihost/schema.xsd",
        marshaller.getProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION));
  }

  @Test
  public void shouldBuildMarshallerWithFormattedOutputProperty() throws JAXBException {

    JAXBContextFactory factory = new CachingJAXBContextFactory.Builder()
        .withMarshallerFormattedOutput(true)
        .build();

    Marshaller marshaller = factory.createMarshaller(Object.class);
    assertTrue((Boolean) marshaller.getProperty(Marshaller.JAXB_FORMATTED_OUTPUT));
  }

  @Test
  public void shouldBuildMarshallerWithFragmentProperty() throws JAXBException {

    JAXBContextFactory factory = new CachingJAXBContextFactory.Builder()
        .withMarshallerFragment(true)
        .build();

    Marshaller marshaller = factory.createMarshaller(Object.class);
    assertTrue((Boolean) marshaller.getProperty(Marshaller.JAXB_FRAGMENT));
  }

  @Test
  public void shouldPreloadCache() throws Exception {

    Class<?>[] classes = new Class[]{String.class, Integer.class};

    JAXBContextFactory factory = new CachingJAXBContextFactory.Builder()
        .withPreloadedClasses(classes)
        .build();

    Field jaxbContexts = factory.getClass().getDeclaredField("jaxbContexts");
    jaxbContexts.setAccessible(true);

    Map internalCache = (Map) jaxbContexts.get(factory);
    assertFalse(internalCache.isEmpty());
    assertEquals(internalCache.size(), classes.length);
    assertNotNull(internalCache.get(String.class));
    assertNotNull(internalCache.get(Integer.class));

  }

}
