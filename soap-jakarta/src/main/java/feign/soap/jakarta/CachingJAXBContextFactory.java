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

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public final class CachingJAXBContextFactory implements JAXBContextFactory {

  private final ConcurrentHashMap<Class<?>, JAXBContext> jaxbContexts = new ConcurrentHashMap<>(64);
  private Map<String, Object> properties = new HashMap<>(10);

  public CachingJAXBContextFactory() {
  }

  public CachingJAXBContextFactory(Builder builder) {
    this.properties = builder.properties;
    preloadContextCache(builder.preloadedClasses);
  }

  @Override
  public Unmarshaller createUnmarshaller(Class<?> clazz) throws JAXBException {
    return getContext(clazz).createUnmarshaller();
  }

  @Override
  public Marshaller createMarshaller(Class<?> clazz) throws JAXBException {
    Marshaller marshaller = getContext(clazz).createMarshaller();
    for (Entry<String, Object> en : properties.entrySet()) {
      marshaller.setProperty(en.getKey(), en.getValue());
    }
    return marshaller;
  }

  private void preloadContextCache(Class<?>[] classes) {
    if (classes == null) {
      return;
    }
    for (Class<?> clazz : classes) {
      try {
        getContext(clazz);
      } catch (JAXBException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private JAXBContext getContext(Class<?> clazz) throws JAXBException {
    JAXBContext existing = this.jaxbContexts.get(clazz);
    if (existing != null) {
      return existing;
    }
    JAXBContext newInstance = JAXBContext.newInstance(clazz);
    this.jaxbContexts.putIfAbsent(clazz, newInstance);
    return newInstance;
  }

  public static class Builder {

    private final Map<String, Object> properties = new HashMap<>(10);
    private Class<?>[] preloadedClasses;

    public Builder withMarshallerJAXBEncoding(String value) {
      properties.put(Marshaller.JAXB_ENCODING, value);
      return this;
    }

    public Builder withMarshallerSchemaLocation(String value) {
      properties.put(Marshaller.JAXB_SCHEMA_LOCATION, value);
      return this;
    }

    public Builder withMarshallerNoNamespaceSchemaLocation(String value) {
      properties.put(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, value);
      return this;
    }

    public Builder withMarshallerFormattedOutput(Boolean value) {
      properties.put(Marshaller.JAXB_FORMATTED_OUTPUT, value);
      return this;
    }

    public Builder withMarshallerFragment(Boolean value) {
      properties.put(Marshaller.JAXB_FRAGMENT, value);
      return this;
    }

    public Builder withPreloadedClasses(Class<?>... preloadedClasses) {
      this.preloadedClasses = preloadedClasses;
      return this;
    }

    public JAXBContextFactory build() {
      return new CachingJAXBContextFactory(this);
    }

  }
}
