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

import org.apache.shindig.common.BasicSecurityTokenDecoder;
import org.apache.shindig.social.GadgetDataServletFetcher;
import org.apache.shindig.social.SocialApiTestsGuiceModule;
import org.apache.shindig.social.dataservice.ActivityHandler;
import org.apache.shindig.social.dataservice.AppDataHandler;
import org.apache.shindig.social.dataservice.DataRequestHandler;
import org.apache.shindig.social.dataservice.DataServiceServlet;
import org.apache.shindig.social.dataservice.DataServiceServletFetcher;
import org.apache.shindig.social.dataservice.HandlerProvider;
import org.apache.shindig.social.dataservice.PersonHandler;
import org.apache.shindig.social.opensocial.util.BeanJsonConverter;
import org.apache.shindig.social.opensocial.util.BeanXmlConverter;

import com.google.common.collect.Maps;
import com.google.inject.Guice;
import junit.framework.TestCase;
import org.easymock.classextension.EasyMock;
import org.json.JSONObject;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Vector;

public abstract class AbstractLargeRestfulTests extends TestCase {
  private HttpServletRequest req;
  private HttpServletResponse res;
  private DataServiceServlet servlet;

  @Override
  protected void setUp() throws Exception {
    Map<String, Class<? extends DataRequestHandler>> handlers = Maps.newHashMap();
    handlers.put(DataServiceServlet.PEOPLE_ROUTE, PersonHandler.class);
    handlers.put(DataServiceServlet.ACTIVITY_ROUTE, ActivityHandler.class);
    handlers.put(DataServiceServlet.APPDATA_ROUTE, AppDataHandler.class);

    servlet = new DataServiceServlet();
    servlet.setHandlers(new HandlerProvider(handlers));
    servlet.setInjector(Guice.createInjector(new SocialApiTestsGuiceModule()));
    servlet.setBeanConverters(new BeanJsonConverter(
        Guice.createInjector(new SocialApiTestsGuiceModule())), new BeanXmlConverter());
    servlet.setSecurityTokenDecoder(new BasicSecurityTokenDecoder());
    servlet.setParameterFetcher(new DataServiceServletFetcher());

    req = EasyMock.createMock(HttpServletRequest.class);
    res = EasyMock.createMock(HttpServletResponse.class);
  }

  protected String getJsonResponse(String path, String method) throws Exception {
    return getJsonResponse(path, method, Maps.<String, String>newHashMap(), "");
  }

  protected String getJsonResponse(String path, String method,
      Map<String, String> extraParams) throws Exception {
    return getJsonResponse(path, method, extraParams, "");
  }

  protected String getJsonResponse(String path, String method, String postData) throws Exception {
    return getJsonResponse(path, method, Maps.<String, String>newHashMap(), postData);
  }

  protected String getJsonResponse(String path, String method, Map<String, String> extraParams,
      String postData) throws Exception {
    req.setCharacterEncoding("UTF-8");

    EasyMock.expect(req.getPathInfo()).andStubReturn(path);
    EasyMock.expect(req.getMethod()).andStubReturn(method);
    EasyMock.expect(req.getParameter("format")).andStubReturn(null);
    EasyMock.expect(req.getParameter("X-HTTP-Method-Override")).andStubReturn(method);
    EasyMock.expect(req.getParameter("st")).andStubReturn(
        "john.doe:john.doe:app:container.com:foo:bar");

    Vector<String> vector = new Vector<String>(extraParams.keySet());
    EasyMock.expect(req.getParameterNames()).andStubReturn(vector.elements());

    for (Map.Entry<String, String> entry : extraParams.entrySet()) {
      EasyMock.expect(req.getParameter(entry.getKey())).andStubReturn(entry.getValue());
    }

    if (postData == null) {
      postData = "";
    }

    final InputStream stream = new ByteArrayInputStream(postData.getBytes());
    ServletInputStream servletStream = new ServletInputStream () {
      public int read() throws IOException {
        return stream.read();
      }
    };
    EasyMock.expect(req.getInputStream()).andReturn(servletStream);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PrintWriter writer = new PrintWriter(outputStream);
    EasyMock.expect(res.getWriter()).andReturn(writer);

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
   * @param str input content string
   * @return the map<> of <name, value> pairs from the content xml
   * @throws javax.xml.stream.XMLStreamException If the str is not valid xml
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
        int eventType =  parser.next();
        if (eventType == XMLStreamConstants.CHARACTERS) {
          String value = parser.getText();
          columns.put(name, value);
        }
      }
    }
    return columns;
  }

}
