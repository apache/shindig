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
package org.apache.shindig.protocol.conversion;

import org.apache.commons.lang.StringUtils;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.protocol.model.Model;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.examples.RecursiveElementNameAndTextQualifier;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

/**
 * Basic test for betwixt based XML conversion
 */
public class BeanXmlConverterTest extends Assert {
  private Model.Car car;
  private BeanXmlConverter beanXmlConverter;

  @Before
  public void setUp() throws Exception {
    car = new Model.Car();
    beanXmlConverter = new BeanXmlConverter();
  }

  @Test
  public void testCarToXml() throws Exception {
    String xml = beanXmlConverter.convertToXml(car);
    XMLUnit.setIgnoreWhitespace(true);
    Diff diff;
    diff = new Diff(Model.Car.DEFAULT_XML, xml);
    diff.overrideElementQualifier(new RecursiveElementNameAndTextQualifier());
    XMLAssert.assertXMLEqual(diff, true);
  }

  @Test
  //@Ignore("unknown why this is disabled")
  public void testMapsToXml() throws Exception {
    // This is the structure our app data currently takes
    Map<String, Map<String, String>> map = Maps.newTreeMap();
    Map<String, String> item1Map = ImmutableMap.of("value","1");
    map.put("item1", item1Map);

    Map<String, String> item2Map = ImmutableMap.of("value", "2");
    map.put("item2", item2Map);

    String xml = beanXmlConverter.convertToXml(map);

    // TODO: Change this test to use parsing once we have the right format
    XmlUtil.parse(xml);

    // TODO: I don't believe this is the output we are looking for for app
    // data... we will probably have to tweak this.
    String expectedXml =
        "<response>" +
        "<empty>false</empty>" +
        "<entry>" +
          "<key>item1</key>" +
          "<value>" +
            "<empty>false</empty>" +
            "<entry>" +
              "<key>value</key>" +
              "<value>1</value>" +
            "</entry>" +
          "</value>" +
        "</entry>" +
        "<entry>" +
          "<key>item2</key>" +
          "<value>" +
            "<empty>false</empty>" +
            "<entry>" +
              "<key>value</key>" +
              "<value>2</value>" +
            "</entry>" +
          "</value>" +
        "</entry>" +
        "</response>";
    assertEquals(expectedXml, StringUtils.deleteWhitespace(xml));
  }

}
