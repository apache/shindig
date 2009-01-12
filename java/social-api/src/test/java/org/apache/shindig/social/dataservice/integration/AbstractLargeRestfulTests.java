/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.shindig.social.dataservice.integration;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.TestCase;

import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.social.SocialApiTestsGuiceModule;
import org.apache.shindig.social.core.util.BeanJsonConverter;
import org.apache.shindig.social.core.util.BeanXStreamAtomConverter;
import org.apache.shindig.social.core.util.BeanXStreamConverter;
import org.apache.shindig.social.core.util.xstream.XStream081Configuration;
import org.apache.shindig.social.opensocial.service.DataServiceServlet;
import org.apache.shindig.social.opensocial.service.HandlerDispatcher;

import org.easymock.classextension.EasyMock;
import org.json.JSONObject;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public abstract class AbstractLargeRestfulTests extends TestCase {
  protected static final String XMLSCHEMA = " xmlns=\"http://ns.opensocial.org/2008/opensocial\" \n"
    + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n"
    + " xsi:schemaLocation=\"http://ns.opensocial.org/2008/opensocial classpath:opensocial.xsd\" ";
  protected static final String XSDRESOURCE = "opensocial.xsd";

  private HttpServletRequest req;

  private HttpServletResponse res;

  private DataServiceServlet servlet;

  private static final FakeGadgetToken FAKE_GADGET_TOKEN = new FakeGadgetToken()
      .setOwnerId("john.doe").setViewerId("john.doe");

  protected HttpServletRequest getRequest() {
    return req;
  }
  protected void setRequest(HttpServletRequest req) {
    this.req = req;
  }

  protected HttpServletResponse getResponse() {
    return res;
  }
  protected void setResponse(HttpServletResponse res) {
    this.res = res;
  }

  protected DataServiceServlet getServlet() {
    return servlet;
  }
  protected void setServlet(DataServiceServlet servlet) {
    this.servlet = servlet;
  }

  @Override
  protected void setUp() throws Exception {
    Injector injector = Guice.createInjector(new SocialApiTestsGuiceModule());

    servlet = new DataServiceServlet();

    servlet.setHandlerDispatcher(injector.getInstance(HandlerDispatcher.class));
    servlet.setBeanConverters(new BeanJsonConverter(injector),
        new BeanXStreamConverter(new XStream081Configuration(injector)),
        new BeanXStreamAtomConverter(new XStream081Configuration(injector)));

    req = EasyMock.createMock(HttpServletRequest.class);
    res = EasyMock.createMock(HttpServletResponse.class);
  }

  protected String getResponse(String path, String method, String format,
      String contentType) throws Exception {
    return getResponse(path, method, Maps.<String, String> newHashMap(), "",
        format, contentType);
  }

  protected String getResponse(String path, String method,
      Map<String, String> extraParams, String format, String contentType)
      throws Exception {
    return getResponse(path, method, extraParams, "", format, contentType);
  }

  protected String getResponse(String path, String method, String postData,
      String format, String contentType) throws Exception {
    return getResponse(path, method, Maps.<String,String> newHashMap(),
        postData, format, contentType);
  }

  protected String getResponse(String path, String method,
      Map<String, String> extraParams, String postData, String format,
      String contentType) throws Exception {
    EasyMock.expect(req.getCharacterEncoding()).andStubReturn("UTF-8");
    EasyMock.expect(req.getPathInfo()).andStubReturn(path);
    EasyMock.expect(req.getMethod()).andStubReturn(method);
    EasyMock.expect(req.getParameter("format")).andStubReturn(format);
    EasyMock.expect(req.getParameter("X-HTTP-Method-Override")).andStubReturn(
        method);

    EasyMock.expect(req.getAttribute(EasyMock.isA(String.class))).andReturn(
        FAKE_GADGET_TOKEN);

    Vector<String> vector = new Vector<String>(extraParams.keySet());
    EasyMock.expect(req.getParameterNames()).andStubReturn(vector.elements());

    for (Map.Entry<String, String> entry : extraParams.entrySet()) {
      if (entry.getValue() != null) {
        EasyMock.expect(req.getParameterValues(entry.getKey())).andStubReturn(
            new String[] { entry.getValue() });
      } else {
        EasyMock.expect(req.getParameterValues(entry.getKey())).andStubReturn(
            new String[] {});
      }
    }

    if (postData == null) {
      postData = "";
    }

    if (!("GET").equals(method) && !("HEAD").equals(method)) {
      final InputStream stream = new ByteArrayInputStream(postData.getBytes());
      ServletInputStream servletStream = new ServletInputStream() {
        @Override
        public int read() throws IOException {
          return stream.read();
        }
      };
      EasyMock.expect(req.getInputStream()).andReturn(servletStream);
    }

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PrintWriter writer = new PrintWriter(outputStream);
    EasyMock.expect(res.getWriter()).andReturn(writer);
    res.setCharacterEncoding("UTF-8");
    res.setContentType(contentType);

    EasyMock.replay(req, res);
    servlet.service(req, res);
    EasyMock.verify(req, res);
    EasyMock.reset(req, res);

    writer.flush();
    return outputStream.toString();
  }

  protected JSONObject getJson(String json) throws Exception {
    return new JSONObject(json);
  }

  /**
   * parse entry.content xml into a Map<> struct
   *
   * @param str
   *          input content string
   * @return the map<> of <name, value> pairs from the content xml
   * @throws javax.xml.stream.XMLStreamException
   *           If the str is not valid xml
   */
  protected Map<String, String> parseXmlContent(String str)
      throws XMLStreamException {
    ByteArrayInputStream inStr = new ByteArrayInputStream(str.getBytes());
    XMLInputFactory factory = XMLInputFactory.newInstance();
    XMLStreamReader parser = factory.createXMLStreamReader(inStr);
    Map<String, String> columns = Maps.newHashMap();

    while (true) {
      int event = parser.next();
      if (event == XMLStreamConstants.END_DOCUMENT) {
        parser.close();
        break;
      } else if (event == XMLStreamConstants.START_ELEMENT) {
        String name = parser.getLocalName();
        int eventType = parser.next();
        if (eventType == XMLStreamConstants.CHARACTERS) {
          String value = parser.getText();
          columns.put(name, value);
        }
      }
    }
    return columns;
  }

  /**
   * Converts a node which child nodes into a map keyed on element names
   * containing the text inside each child node.
   *
   * @param n
   *          the node to convert.
   * @return a map keyed on element name, containing the contents of each
   *         element.
   */
  protected Map<String, List<String>> childNodesToMap(Node n) {
    Map<String, List<String>> v = Maps.newHashMap();
    NodeList result = n.getChildNodes();
    for (int i = 0; i < result.getLength(); i++) {
      Node nv = result.item(i);
      if (nv.getNodeType() == Node.ELEMENT_NODE) {
        List<String> l = v.get(nv.getLocalName());
        if (l == null) {
          l = Lists.newArrayList();
          v.put(nv.getLocalName(), l);
        }
        l.add(nv.getTextContent());
      }
    }
    return v;
  }

  /**
   * Converts <entry> <key>k</key> <value> <entry> <key>count</key>
   * <value>val</value> </entry> <entry> <key>lastUpdate</key>
   * <value>val</value> </entry> </value> </entry>
   *
   * To map.get("k").get("count")
   *
   * @param result
   * @return
   */
  protected Map<String, Map<String, List<String>>> childNodesToMapofMap(
      NodeList result) {
    Map<String, Map<String, List<String>>> v = Maps.newHashMap();
    for (int i = 0; i < result.getLength(); i++) {
      Map<String, List<Node>> keyValue = childNodesToNodeMap(result.item(i));

      assertEquals(2, keyValue.size());
      assertTrue(keyValue.containsKey("key"));
      assertTrue(keyValue.containsKey("value"));
      Node valueNode = keyValue.get("value").get(0);
      Node key = keyValue.get("key").get(0);
      NodeList entryList = valueNode.getChildNodes();
      Map<String, List<String>> pv = Maps.newHashMap();
      v.put(key.getTextContent(), pv);
      for (int j = 0; j < entryList.getLength(); j++) {
        Node n = entryList.item(j);
        if ("entry".equals(n.getNodeName())) {
          Map<String, List<String>> ve = childNodesToMap(entryList.item(j));
          assertTrue(ve.containsKey("key"));
          List<String> l = pv.get(ve.get("key").get(0));
          if ( l == null ) {
            l = Lists.newArrayList();
            pv.put(ve.get("key").get(0), l);
          }
          l.add(ve.get("value").get(0));
        }
      }
    }
    return v;
  }

  /**
   * @param personNode
   * @return
   */
  protected Map<String, List<Node>> childNodesToNodeMap(Node n) {
    Map<String, List<Node>> v = Maps.newHashMap();
    NodeList result = n.getChildNodes();
    for (int i = 0; i < result.getLength(); i++) {
      Node nv = result.item(i);
      if (nv.getNodeType() == Node.ELEMENT_NODE) {
        List<Node> l = v.get(nv.getLocalName());
        if (l == null) {
          l = Lists.newArrayList();
          v.put(nv.getLocalName(), l);
        }
        l.add(nv);
      }
    }
    return v;
  }

}
