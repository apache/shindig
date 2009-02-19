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
package org.apache.shindig.protocol;

import org.apache.shindig.common.JsonAssert;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.protocol.conversion.BeanConverter;
import org.apache.shindig.protocol.conversion.BeanJsonConverter;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;

import org.easymock.IMocksControl;
import org.easymock.classextension.EasyMock;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Collections;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class JsonRpcServletTest extends TestCase {

  private static final FakeGadgetToken FAKE_GADGET_TOKEN = new FakeGadgetToken()
      .setOwnerId("john.doe").setViewerId("john.doe");

  private HttpServletRequest req;
  private HttpServletResponse res;
  private JsonRpcServlet servlet;

  private BeanJsonConverter jsonConverter;
  private BeanConverter xmlConverter;
  protected BeanConverter atomConverter;

  private final IMocksControl mockControl = EasyMock.createNiceControl();

  private final ByteArrayOutputStream stream = new ByteArrayOutputStream();
  private final PrintWriter writer = new PrintWriter(stream);
  private final TestHandler handler = new TestHandler();

  @Override protected void setUp() throws Exception {
    servlet = new JsonRpcServlet();
    req = mockControl.createMock(HttpServletRequest.class);
    res = mockControl.createMock(HttpServletResponse.class);
    jsonConverter = new BeanJsonConverter(Guice.createInjector());
    xmlConverter = mockControl.createMock(BeanConverter.class);
    atomConverter = mockControl.createMock(BeanConverter.class);

    HandlerRegistry registry = new DefaultHandlerRegistry(null,
        Collections.<Object>singleton(handler), jsonConverter,
        new HandlerExecutionListener.NoOpHandlerExecutionListener());

    servlet.setHandlerRegistry(registry);
    servlet.setBeanConverters(jsonConverter, xmlConverter, atomConverter);
    handler.setMock(new TestHandler() {
      @Override
      public Object get(RequestItem req) {
        return ImmutableMap.of("foo", "bar");
      }
    });
  }

  private String getOutput() throws IOException {
    writer.close();
    return stream.toString("UTF-8");
  }

  public void testMethodRecognition() throws Exception {
    setupRequest("{method:test.get,id:id,params:{userId:5,groupId:@self}}");

    EasyMock.expect(res.getWriter()).andReturn(writer);
    EasyMock.expectLastCall();

    mockControl.replay();
    servlet.service(req, res);
    mockControl.verify();

    JsonAssert.assertJsonEquals("{id: 'id', data: {foo:'bar'}}", getOutput());
  }

  public void testInvalidService() throws Exception {
    setupRequest("{method:junk.get,id:id,params:{userId:5,groupId:@self}}");

    EasyMock.expect(res.getWriter()).andReturn(writer);
    EasyMock.expectLastCall();

    mockControl.replay();
    servlet.service(req, res);
    mockControl.verify();

    JsonAssert.assertJsonEquals(
        "{id:id,error:{message:'notImplemented: The method junk.get is not implemented',code:501}}",
        getOutput());
  }


  /**
   * Tests a data handler that returns a failed Future.
   * @throws Exception on failure
   */
  public void testFailedRequest() throws Exception {
    setupRequest("{id:id,method:test.futureException}");

    EasyMock.expect(res.getWriter()).andReturn(writer);
    EasyMock.expectLastCall();

    mockControl.replay();
    servlet.service(req, res);

    JsonAssert.assertJsonEquals(
        "{id:id,error:{message:'badRequest: FAILURE_MESSAGE',code:400}}", getOutput());
  }

  public void testBasicBatch() throws Exception {
    setupRequest("[{method:test.get,id:'1'},{method:test.get,id:'2'}]");

    EasyMock.expect(res.getWriter()).andReturn(writer);
    EasyMock.expectLastCall();

    mockControl.replay();
    servlet.service(req, res);

    JsonAssert.assertJsonEquals("[{id:'1',data:{foo:'bar'}},{id:'2',data:{foo:'bar'}}]",
        getOutput());
  }

  public void testGetExecution() throws Exception {
    EasyMock.expect(req.getParameterMap()).andStubReturn(
        ImmutableMap.of("method", new String[]{"test.get"}, "id", new String[]{"1"}));
    EasyMock.expect(req.getMethod()).andStubReturn("GET");
    EasyMock.expect(req.getAttribute(EasyMock.isA(String.class))).andReturn(FAKE_GADGET_TOKEN);
    EasyMock.expect(req.getCharacterEncoding()).andStubReturn("UTF-8");
    res.setCharacterEncoding("UTF-8");

    EasyMock.expect(res.getWriter()).andReturn(writer);
    EasyMock.expectLastCall();

    mockControl.replay();
    servlet.service(req, res);
    mockControl.verify();

    JsonAssert.assertJsonEquals("{id:'1',data:{foo:'bar'}}", getOutput());
  }

  private void setupRequest(String json) throws IOException {
    final InputStream in = new ByteArrayInputStream(json.getBytes());
    ServletInputStream stream = new ServletInputStream() {
      @Override
      public int read() throws IOException {
        return in.read();
      }
    };

    EasyMock.expect(req.getInputStream()).andStubReturn(stream);
    EasyMock.expect(req.getMethod()).andStubReturn("POST");
    EasyMock.expect(req.getAttribute(EasyMock.isA(String.class))).andReturn(FAKE_GADGET_TOKEN);
    EasyMock.expect(req.getCharacterEncoding()).andStubReturn("UTF-8");
    res.setCharacterEncoding("UTF-8");
    res.setContentType("application/json");
  }

}
