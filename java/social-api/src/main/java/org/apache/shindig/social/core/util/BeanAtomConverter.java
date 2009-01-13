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

import org.apache.shindig.social.opensocial.service.BeanConverter;

import org.apache.commons.betwixt.IntrospectionConfiguration;
import org.apache.commons.betwixt.io.BeanReader;
import org.apache.commons.betwixt.io.BeanWriter;
import org.xml.sax.SAXException;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO: This does not produce valid atom sytnax yet
public class BeanAtomConverter implements BeanConverter {
  private static Logger logger =
      Logger.getLogger(BeanAtomConverter.class.getName());


  public String getContentType() {
    return "application/atom+xml";
  }

  public String convertToString(Object pojo) {
    return convertToXml(pojo);
  }

  public String convertToXml(Object obj) {
    String xmlHead="<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    StringWriter outputWriter = new StringWriter();
    BeanWriter writer = new BeanWriter(outputWriter);
    IntrospectionConfiguration configuration = writer.getXMLIntrospector().getConfiguration();
    configuration.setAttributesForPrimitives(false);
    configuration.setWrapCollectionsInElement(true);

    writer.getBindingConfiguration().setMapIDs(false);
    // Print no line endings
    writer.setEndOfLine("");
    writer.setWriteEmptyElements(false);

    // Still left to do:
    //
    // Fix map output with custom outputter:
    // for a map with {key : value, key2 : value2} we need:
    // <key>value</key> <key2>value2</key2>

    // Supress empty lists

    // Within a list the items need to be renamed - this probably means with need a .betwixt file

    String toReturn = xmlHead;
    try {
      writer.write("response", obj);
      toReturn =toReturn+ outputWriter.toString();
      logger.finest("XML is: " + toReturn + "\n **** \n\n");

    } catch (SAXException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    } catch (IOException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    } catch (IntrospectionException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        writer.close();
      } catch (IOException e) {
        logger.log(Level.FINEST, e.getMessage(), e);
      }
    }

    return toReturn;
  }

  @SuppressWarnings("unchecked")
  public <T> T convertToObject(String xml, Class<T> className) {
    String errorMessage = "Could not convert " + xml + " to " + className;
    xml=xml.substring(xml.indexOf("?>") + 2);
    BeanReader reader = new BeanReader();
    try {
      reader.registerBeanClass("activity", className);
      StringReader rd = new StringReader(xml);
      return (T) reader.parse(rd);
    } catch (IntrospectionException e) {
      throw new RuntimeException(errorMessage, e);
    } catch (IOException e) {
      throw new RuntimeException(errorMessage, e);
    } catch (SAXException e) {
      throw new RuntimeException(errorMessage, e);
    }
  }
}
