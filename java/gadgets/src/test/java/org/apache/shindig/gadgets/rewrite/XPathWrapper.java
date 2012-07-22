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
package org.apache.shindig.gadgets.rewrite;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/**
 * Utility functions to test documents via XPath
 */
public class XPathWrapper {
  private static final XPathFactory FACTORY = XPathFactory.newInstance();
  private final Document doc;

  public XPathWrapper(Document doc) {
    this.doc = doc;
  }

  public String getValue(String pathExpr) throws Exception {
    XPath xPath = FACTORY.newXPath();
    return xPath.evaluate(pathExpr, doc);
  }

  public Node getNode(String pathExpr) throws Exception {
    XPath xPath = FACTORY.newXPath();
    return (Node)xPath.evaluate(pathExpr, doc, XPathConstants.NODE);
  }

  public NodeList getNodeList(String pathExpr) throws Exception {
    XPath xPath = FACTORY.newXPath();
    return (NodeList)xPath.evaluate(pathExpr, doc, XPathConstants.NODESET);
  }
}
