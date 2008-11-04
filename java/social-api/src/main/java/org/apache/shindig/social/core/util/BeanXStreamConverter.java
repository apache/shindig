/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.social.core.util;

import com.google.inject.Inject;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.xml.XppDriver;
import com.thoughtworks.xstream.mapper.DefaultMapper;
import com.thoughtworks.xstream.mapper.Mapper;

import org.apache.shindig.social.core.util.xstream.InterfaceClassMapper;
import org.apache.shindig.social.core.util.xstream.StackDriver;
import org.apache.shindig.social.core.util.xstream.ThreadSafeWriterStack;
import org.apache.shindig.social.core.util.xstream.WriterStack;
import org.apache.shindig.social.core.util.xstream.XStreamConfiguration;
import org.apache.shindig.social.opensocial.service.BeanConverter;
import org.apache.shindig.social.opensocial.spi.DataCollection;
import org.apache.shindig.social.opensocial.spi.RestfulCollection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;

public class BeanXStreamConverter implements BeanConverter {
  public static final String XML_DECL = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
  private static final XStreamConfiguration.ConverterSet[] MAPPER_SCOPES = new XStreamConfiguration.ConverterSet[] {
      XStreamConfiguration.ConverterSet.MAP,
      XStreamConfiguration.ConverterSet.COLLECTION,
      XStreamConfiguration.ConverterSet.DEFAULT };
  private static Log log = LogFactory.getLog(BeanXStreamConverter.class);
  private ReflectionProvider rp;
  private HierarchicalStreamDriver driver;
  private WriterStack writerStack;

  protected class ConverterConfig {
    protected InterfaceClassMapper mapper;
    protected XStream xstream;

    protected ConverterConfig(InterfaceClassMapper mapper, XStream xstream) {
      this.mapper = mapper;
      this.xstream = xstream;
    }
  }

  private Map<XStreamConfiguration.ConverterSet, ConverterConfig> converterMap = new HashMap<XStreamConfiguration.ConverterSet, ConverterConfig>();

  @Inject
  public BeanXStreamConverter(XStreamConfiguration configuration) {
    rp = new PureJavaReflectionProvider();
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
    driver = new StackDriver(new XppDriver(), writerStack);
    /*
     * Create an interface class mapper that understands class hierarchy for
     * single items
     */
    for (XStreamConfiguration.ConverterSet c : MAPPER_SCOPES) {
      InterfaceClassMapper mapper = configuration.getMapper(c,dmapper,writerStack);
      XStream xstream = configuration.getXStream(c,rp,mapper,driver);
      converterMap.put(c, new ConverterConfig(mapper, xstream));
    }
  }

  public String getContentType() {
    return "application/xml";
  }

  public String convertToString(Object pojo) {
    return convertToXml(pojo);
  }

  /**
   * convert an Object to XML, but make certain that only one of these is run on
   * a thread at any one time. This only matters if this class is extended.
   * 
   * @param obj
   * @return
   */
  public String convertToXml(Object obj) {

    writerStack.reset();
    if (obj instanceof Map) {
      Map<?, ?> m = (Map<?, ?>) obj;
      ConverterConfig cc = converterMap
          .get(XStreamConfiguration.ConverterSet.MAP);
      if (m.size() == 1) {
        Object s = m.values().iterator().next();
        cc.mapper.setBaseObject(s); // thread safe method
        String result = cc.xstream.toXML(s);
        log.debug("Result is " + result);
        return "<response>" + result + "</response>";
      }
    } else if (obj instanceof RestfulCollection) {
      ConverterConfig cc = converterMap
          .get(XStreamConfiguration.ConverterSet.COLLECTION);
      cc.mapper.setBaseObject(obj); // thread safe method
      String result = cc.xstream.toXML(obj);
      log.debug("Result is " + result);
      return result;
    } else if (obj instanceof DataCollection) {
      ConverterConfig cc = converterMap
          .get(XStreamConfiguration.ConverterSet.MAP);
      cc.mapper.setBaseObject(obj); // thread safe method
      String result = cc.xstream.toXML(obj);
      log.debug("Result is " + result);
      return result;
    }
    ConverterConfig cc = converterMap
        .get(XStreamConfiguration.ConverterSet.DEFAULT);

    cc.mapper.setBaseObject(obj); // thread safe method
    String result = cc.xstream.toXML(obj);
    log.debug("Result is " + result);
    return "<response>" + result + "</response>";
  }

  @SuppressWarnings("unchecked")
  public <T> T convertToObject(String xml, Class<T> className) {
    ConverterConfig cc = converterMap.get(XStreamConfiguration.ConverterSet.DEFAULT);
    return (T) cc.xstream.fromXML(xml);
  }

}
