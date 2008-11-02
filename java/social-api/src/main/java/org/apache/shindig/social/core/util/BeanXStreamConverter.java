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
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.xml.XppDriver;
import com.thoughtworks.xstream.mapper.DefaultMapper;
import com.thoughtworks.xstream.mapper.Mapper;

import org.apache.shindig.social.core.util.xstream.InterfaceClassMapper;
import org.apache.shindig.social.core.util.xstream.MapConverter;
import org.apache.shindig.social.core.util.xstream.StackDriver;
import org.apache.shindig.social.core.util.xstream.ThreadSafeWriterStack;
import org.apache.shindig.social.core.util.xstream.WriterStack;
import org.apache.shindig.social.core.util.xstream.XStreamConfiguration;
import org.apache.shindig.social.opensocial.service.BeanConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BeanXStreamConverter implements BeanConverter {
  public static final String XML_DECL = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
  private static Log log = LogFactory.getLog(BeanXStreamConverter.class);
  private Converter mapConverter;
  private InterfaceClassMapper icmapper;
  private ReflectionProvider rp;
  private HierarchicalStreamDriver driver;
  private WriterStack writerStack;
  private XStream xstream;

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
     * Create an interface class mapper that understands class hierarchy
     */
    icmapper = new InterfaceClassMapper(writerStack, dmapper, configuration
        .getElementMappingList(), configuration.getListElementMappingList(),
        configuration.getOmitMap(), configuration.getElementClassMap());
    /*
     * Create a map converter to ensure that maps are converted in the
     * compressed form.
     */
    mapConverter = new MapConverter(icmapper);

    xstream = new XStream(rp, icmapper, driver);
    xstream.registerConverter(mapConverter);
    xstream.setMode(XStream.NO_REFERENCES);

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
    icmapper.setBaseObject(obj); // thread safe method

    String result = xstream.toXML(obj);
    log.debug("Result is " + result);
    return "<response>" + result + "</response>";
  }

  @SuppressWarnings("unchecked")
  public <T> T convertToObject(String xml, Class<T> className) {
    return (T) xstream.fromXML(xml);
  }

}
