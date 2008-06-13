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
package org.apache.shindig.social.opensocial.util;

import org.apache.commons.betwixt.io.BeanReader;
import org.apache.commons.betwixt.io.BeanWriter;
import org.xml.sax.SAXException;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BeanXmlConverter implements BeanConverter {
  private static Logger logger =
      Logger.getLogger(BeanXmlConverter.class.getName());

  public String convertToString(Object pojo) {
    return convertToXml(pojo);
  }

  public String convertToXml(Object obj) {
    StringWriter outputWriter = new StringWriter();
    BeanWriter writer = new BeanWriter(outputWriter);
    writer.getXMLIntrospector()
        .getConfiguration()
        .setAttributesForPrimitives(false);
    writer.getBindingConfiguration().setMapIDs(false);
    // Print no line endings to match Abdera.
    writer.setEndOfLine("");
    writer.setWriteEmptyElements(false);
    String toReturn = null;
    try {
      writer.write(obj.getClass().getSimpleName().toLowerCase(), obj);
      toReturn = outputWriter.toString();
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
      } catch(IOException e) {
        // ignore this exception. it won't matter
      }
    }

    return toReturn;
  }

  public <T> T convertToObject(String xml, Class<T> className) {
    String errorMessage = "Could not convert " + xml + " to " + className;

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
