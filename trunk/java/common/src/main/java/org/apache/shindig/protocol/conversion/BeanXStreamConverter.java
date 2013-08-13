/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.protocol.conversion;

import org.apache.shindig.protocol.ContentTypes;
import org.apache.shindig.protocol.DataCollection;
import org.apache.shindig.protocol.RestfulCollection;
import org.apache.shindig.protocol.conversion.xstream.StackDriver;
import org.apache.shindig.protocol.conversion.xstream.ThreadSafeWriterStack;
import org.apache.shindig.protocol.conversion.xstream.WriterStack;
import org.apache.shindig.protocol.conversion.xstream.XStreamConfiguration;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.xml.XppDriver;
import com.thoughtworks.xstream.mapper.DefaultMapper;
import com.thoughtworks.xstream.mapper.Mapper;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Converts to/from XML format using XStream
 */
public class BeanXStreamConverter implements BeanConverter {
  public static final String XML_DECL = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
  private static final XStreamConfiguration.ConverterSet[] MAPPER_SCOPES = {
      XStreamConfiguration.ConverterSet.MAP,
      XStreamConfiguration.ConverterSet.COLLECTION,
      XStreamConfiguration.ConverterSet.DEFAULT };
  private static final Logger LOG = Logger.getLogger(BeanXStreamConverter.class.getName());

  protected WriterStack writerStack;


  protected Map<XStreamConfiguration.ConverterSet, XStreamConfiguration.ConverterConfig> converterMap = Maps.newHashMap();

  @Inject
  public BeanXStreamConverter(XStreamConfiguration configuration) {
    ReflectionProvider rp = new PureJavaReflectionProvider();
    Mapper dmapper = new DefaultMapper(this.getClass().getClassLoader());
    /*
     * Putting this here means only one conversion per thread may be active at
     * any one time, but since the conversion process is atomic this will not
     * matter unless the class is extended.
     */
    writerStack = new ThreadSafeWriterStack();


    /*
     * create a driver that wires into a standard driver, and updates the stack
     * position.
     */
    HierarchicalStreamDriver driver = new StackDriver(new XppDriver(), writerStack, configuration.getNameSpaces());
    /*
     * Create an interface class mapper that understands class hierarchy for
     * single items
     */
    for (XStreamConfiguration.ConverterSet c : MAPPER_SCOPES) {
      converterMap.put(c, configuration.getConverterConfig(c, rp,dmapper, driver,writerStack));
    }
  }

  public String getContentType() {
    return ContentTypes.OUTPUT_XML_CONTENT_TYPE;
  }

  public String convertToString(Object pojo) {
    return convertToXml(pojo);
  }

  /**
   * convert an Object to XML, but make certain that only one of these is run on
   * a thread at any one time. This only matters if this class is extended.
   *
   * @param obj
   * @return The XML as a string
   */
  private String convertToXml(Object obj) {

    writerStack.reset();
    if (obj instanceof RestfulCollection) {
      XStreamConfiguration.ConverterConfig cc = converterMap
          .get(XStreamConfiguration.ConverterSet.COLLECTION);
      cc.mapper.setBaseObject(obj); // thread safe method
      String result = cc.xstream.toXML(obj);

      if (LOG.isLoggable(Level.FINE))
        LOG.fine("Result is " + result);

      return XML_DECL + result;
    } else if (obj instanceof Map<?, ?>) {
      Map<?, ?> m = (Map<?, ?>) obj;
      XStreamConfiguration.ConverterConfig cc = converterMap
          .get(XStreamConfiguration.ConverterSet.MAP);
      if (m.size() == 1) {
        Object s = m.values().iterator().next();
        cc.mapper.setBaseObject(s); // thread safe method
        String result = cc.xstream.toXML(s);

        if (LOG.isLoggable(Level.FINE))
          LOG.fine("Result is " + result);

        return XML_DECL + "<response xmlns=\"http://ns.opensocial.org/2008/opensocial\">" + result + "</response>";
      }
    } else if (obj instanceof DataCollection) {
      XStreamConfiguration.ConverterConfig cc = converterMap
          .get(XStreamConfiguration.ConverterSet.MAP);
      cc.mapper.setBaseObject(obj); // thread safe method
      String result = cc.xstream.toXML(obj);

      if (LOG.isLoggable(Level.FINE))
        LOG.fine("Result is " + result);

      return XML_DECL + result;
    }
    XStreamConfiguration.ConverterConfig cc = converterMap
        .get(XStreamConfiguration.ConverterSet.DEFAULT);

    cc.mapper.setBaseObject(obj); // thread safe method
    String result = cc.xstream.toXML(obj);

    if (LOG.isLoggable(Level.FINE))
      LOG.fine("Result is " + result);
    return XML_DECL + "<response xmlns=\"http://ns.opensocial.org/2008/opensocial\">" + result + "</response>";
  }

  @SuppressWarnings("unchecked")
  public <T> T convertToObject(String xml, Class<T> className) {
    XStreamConfiguration.ConverterConfig cc = converterMap.get(XStreamConfiguration.ConverterSet.DEFAULT);
    return (T) cc.xstream.fromXML(xml);
  }

  public void append(Appendable buf, Object pojo) throws IOException {
    buf.append(convertToString(pojo));
  }
}
