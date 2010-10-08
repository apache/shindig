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

import org.apache.commons.betwixt.IntrospectionConfiguration;
import org.apache.commons.betwixt.io.BeanReader;
import org.apache.commons.betwixt.io.BeanWriter;
import org.apache.shindig.protocol.ContentTypes;

import org.xml.sax.SAXException;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO: This does not produce valid atom sytnax yet
/**
 * Converts a Bean into Atom-like syntax.
 */
public class BeanAtomConverter implements BeanConverter {
  private static final Logger LOG =
      Logger.getLogger(BeanAtomConverter.class.getName());


  public String getContentType() {
    return ContentTypes.OUTPUT_ATOM_CONTENT_TYPE;
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
      if (LOG.isLoggable(Level.FINEST)) LOG.finest("XML is: " + toReturn + "\n **** \n\n");

    } catch (SAXException e) {
      LOG.log(Level.SEVERE, e.getMessage(), e);
    } catch (IOException e) {
      LOG.log(Level.SEVERE, e.getMessage(), e);
    } catch (IntrospectionException e) {
      LOG.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        writer.close();
      } catch (IOException e) {
        if (LOG.isLoggable(Level.FINEST)) LOG.log(Level.FINEST, e.getMessage(), e);
      }
    }

    return toReturn;
  }

  public static final String ERROR_MESSAGE_FMT = "Could not convert %s to %s";

  @SuppressWarnings("unchecked")
  public <T> T convertToObject(String xml, Class<T> className) {
    xml=xml.substring(xml.indexOf("?>") + 2);
    BeanReader reader = new BeanReader();
    try {
      reader.registerBeanClass("activity", className);
      StringReader rd = new StringReader(xml);
      return (T) reader.parse(rd);
    } catch (IntrospectionException e) {
      throw new RuntimeException(String.format(ERROR_MESSAGE_FMT, xml, className), e);
    } catch (IOException e) {
      throw new RuntimeException(String.format(ERROR_MESSAGE_FMT, xml, className), e);
    } catch (SAXException e) {
      throw new RuntimeException(String.format(ERROR_MESSAGE_FMT, xml, className), e);
    }
  }
  
  public void append(Appendable buf, Object pojo) throws IOException {
    buf.append(convertToString(pojo));
  }
}
