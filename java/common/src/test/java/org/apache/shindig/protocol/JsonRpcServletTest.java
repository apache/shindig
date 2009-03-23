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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.classextension.EasyMock.reset;

import org.apache.shindig.common.JsonAssert;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.protocol.conversion.BeanJsonConverter;
import org.apache.shindig.protocol.multipart.FormDataItem;
import org.apache.shindig.protocol.multipart.MultipartFormParser;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class JsonRpcServletTest extends TestCase {

  private static final FakeGadgetToken FAKE_GADGET_TOKEN = new FakeGadgetToken()
      .setOwnerId("john.doe").setViewerId("john.doe");

  private static final String IMAGE_FIELDNAME = "profile-photo";
  private static final byte[] IMAGE_DATA = "image data".getBytes();
  private static final String IMAGE_TYPE = "image/jpeg";

  private HttpServletRequest req;
  private HttpServletResponse res;
  private JsonRpcServlet servlet;
  private MultipartFormParser multipartFormParser;

  private final IMocksControl mockControl = EasyMock.createNiceControl();

  private final ByteArrayOutputStream stream = new ByteArrayOutputStream();
  private final PrintWriter writer = new PrintWriter(stream);
  private final TestHandler handler = new TestHandler();

  @Override protected void setUp() throws Exception {
    servlet = new JsonRpcServlet();
    req = mockControl.createMock(HttpServletRequest.class);
    res = mockControl.createMock(HttpServletResponse.class);

    multipartFormParser = mockControl.createMock(MultipartFormParser.class);
    EasyMock.expect(multipartFormParser.isMultipartContent(req)).andStubReturn(false);
    servlet.setMultipartFormParser(multipartFormParser);

    BeanJsonConverter converter = new BeanJsonConverter(Guice.createInjector());

    HandlerRegistry registry = new DefaultHandlerRegistry(null, null,
        new HandlerExecutionListener.NoOpHandler());
    registry.addHandlers(Collections.<Object>singleton(handler));

    servlet.setHandlerRegistry(registry);
    servlet.setBeanConverters(converter, null, null);
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

    expect(res.getWriter()).andReturn(writer);
    expectLastCall();

    mockControl.replay();
    servlet.service(req, res);
    mockControl.verify();

    JsonAssert.assertJsonEquals("{id: 'id', data: {foo:'bar'}}", getOutput());
  }

  public void testPostMultipartFormData() throws Exception {
    reset(multipartFormParser);

    handler.setMock(new TestHandler() {
      @Override
      public Object get(RequestItem req) {
        FormDataItem item = req.getFormMimePart(IMAGE_FIELDNAME);
        return ImmutableMap.of("image-data", new String(item.get()),
            "image-type", item.getContentType(),
            "image-ref", req.getParameter("image-ref"));
      }
    });
    expect(req.getMethod()).andStubReturn("POST");
    expect(req.getAttribute(isA(String.class))).andReturn(FAKE_GADGET_TOKEN);
    expect(req.getCharacterEncoding()).andStubReturn("UTF-8");
    res.setCharacterEncoding("UTF-8");
    res.setContentType("application/json");

    List<FormDataItem> formItems = new ArrayList<FormDataItem>();
    String request = "{method:test.get,id:id,params:" +
        "{userId:5,groupId:@self,image-ref:@" + IMAGE_FIELDNAME + "}}";
    formItems.add(mockFormDataItem(JsonRpcServlet.REQUEST_PARAM, "application/json",
        request.getBytes(), true));
    formItems.add(mockFormDataItem(IMAGE_FIELDNAME, IMAGE_TYPE, IMAGE_DATA, false));
    expect(multipartFormParser.isMultipartContent(req)).andReturn(true);
    expect(multipartFormParser.parse(req)).andReturn(formItems);
    expect(res.getWriter()).andReturn(writer);
    expectLastCall();

    mockControl.replay();
    servlet.service(req, res);
    mockControl.verify();

    JsonAssert.assertJsonEquals("{id: 'id', data: {image-data:'" + new String(IMAGE_DATA) +
        "', image-type:'" + IMAGE_TYPE + "', image-ref:'@" + IMAGE_FIELDNAME + "'}}", getOutput());
  }

  /**
   * Tests whether mime part with content type appliction/json is picked up as
   * request even when its fieldname is not "request".
   */
  public void testPostMultipartFormDataWithNoExplicitRequestField() throws Exception {
    reset(multipartFormParser);

    handler.setMock(new TestHandler() {
      @Override
      public Object get(RequestItem req) {
        FormDataItem item = req.getFormMimePart(IMAGE_FIELDNAME);
        return ImmutableMap.of("image-data", new String(item.get()),
            "image-type", item.getContentType(),
            "image-ref", req.getParameter("image-ref"));
      }
    });
    expect(req.getMethod()).andStubReturn("POST");
    expect(req.getAttribute(isA(String.class))).andReturn(FAKE_GADGET_TOKEN);
    expect(req.getCharacterEncoding()).andStubReturn("UTF-8");
    res.setCharacterEncoding("UTF-8");
    res.setContentType("application/json");

    List<FormDataItem> formItems = new ArrayList<FormDataItem>();
    String request = "{method:test.get,id:id,params:" +
        "{userId:5,groupId:@self,image-ref:@" + IMAGE_FIELDNAME + "}}";
    formItems.add(mockFormDataItem(IMAGE_FIELDNAME, IMAGE_TYPE, IMAGE_DATA, false));
    formItems.add(mockFormDataItem("json", "application/json",
        request.getBytes(), true));
    expect(multipartFormParser.isMultipartContent(req)).andReturn(true);
    expect(multipartFormParser.parse(req)).andReturn(formItems);
    expect(res.getWriter()).andReturn(writer);
    expectLastCall();

    mockControl.replay();
    servlet.service(req, res);
    mockControl.verify();

    JsonAssert.assertJsonEquals("{id: 'id', data: {image-data:'" + new String(IMAGE_DATA) +
        "', image-type:'" + IMAGE_TYPE + "', image-ref:'@" + IMAGE_FIELDNAME + "'}}", getOutput());
  }


  public void testInvalidService() throws Exception {
    setupRequest("{method:junk.get,id:id,params:{userId:5,groupId:@self}}");

    expect(res.getWriter()).andReturn(writer);
    expectLastCall();

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

    expect(res.getWriter()).andReturn(writer);
    expectLastCall();

    mockControl.replay();
    servlet.service(req, res);
    mockControl.verify();

    JsonAssert.assertJsonEquals(
        "{id:id,error:{message:'badRequest: FAILURE_MESSAGE',code:400}}", getOutput());
  }

  public void testBasicBatch() throws Exception {
    setupRequest("[{method:test.get,id:'1'},{method:test.get,id:'2'}]");

    expect(res.getWriter()).andReturn(writer);
    expectLastCall();

    mockControl.replay();
    servlet.service(req, res);
    mockControl.verify();

    JsonAssert.assertJsonEquals("[{id:'1',data:{foo:'bar'}},{id:'2',data:{foo:'bar'}}]",
        getOutput());
  }

  public void testGetExecution() throws Exception {
    expect(req.getParameterMap()).andStubReturn(
        ImmutableMap.of("method", new String[]{"test.get"}, "id", new String[]{"1"}));
    expect(req.getMethod()).andStubReturn("GET");
    expect(req.getAttribute(isA(String.class))).andReturn(FAKE_GADGET_TOKEN);
    expect(req.getCharacterEncoding()).andStubReturn("UTF-8");
    res.setCharacterEncoding("UTF-8");

    expect(res.getWriter()).andReturn(writer);
    expectLastCall();

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

    expect(req.getInputStream()).andStubReturn(stream);
    expect(req.getMethod()).andStubReturn("POST");
    expect(req.getAttribute(isA(String.class))).andReturn(FAKE_GADGET_TOKEN);
    expect(req.getCharacterEncoding()).andStubReturn("UTF-8");
    expect(req.getContentType()).andStubReturn("application/json");
    res.setCharacterEncoding("UTF-8");
    res.setContentType("application/json");
  }

  private FormDataItem mockFormDataItem(String fieldName, String contentType, byte content[],
      boolean isFormField) throws IOException {
    InputStream in = new ByteArrayInputStream(content);
    FormDataItem formDataItem = mockControl.createMock(FormDataItem.class);
    expect(formDataItem.getContentType()).andStubReturn(contentType);
    expect(formDataItem.getSize()).andStubReturn((long) content.length);
    expect(formDataItem.get()).andStubReturn(content);
    expect(formDataItem.getFieldName()).andStubReturn(fieldName);
    expect(formDataItem.isFormField()).andStubReturn(isFormField);
    expect(formDataItem.getInputStream()).andStubReturn(in);
    return formDataItem;
  }

}
