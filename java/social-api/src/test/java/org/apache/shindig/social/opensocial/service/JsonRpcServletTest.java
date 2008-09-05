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
package org.apache.shindig.social.opensocial.service;

import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.social.core.util.BeanJsonConverter;
import org.apache.shindig.social.core.util.BeanXmlConverter;

import com.google.common.collect.Maps;
import com.google.inject.Injector;
import junit.framework.TestCase;
import org.easymock.classextension.EasyMock;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.concurrent.Future;
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

  private PersonHandler peopleHandler;
  private ActivityHandler activityHandler;
  private AppDataHandler appDataHandler;

  private Injector injector;

  private BeanJsonConverter jsonConverter;
  private BeanXmlConverter xmlConverter;

  @Override protected void setUp() throws Exception {
    servlet = new JsonRpcServlet();
    req = EasyMock.createMock(HttpServletRequest.class);
    res = EasyMock.createMock(HttpServletResponse.class);
    jsonConverter = EasyMock.createMock(BeanJsonConverter.class);
    xmlConverter = EasyMock.createMock(BeanXmlConverter.class);

    peopleHandler = EasyMock.createMock(PersonHandler.class);
    activityHandler = EasyMock.createMock(ActivityHandler.class);
    appDataHandler = EasyMock.createMock(AppDataHandler.class);

    injector = EasyMock.createMock(Injector.class);
    servlet.setInjector(injector);

    servlet.setHandlers(new HandlerProvider(new PersonHandler(null), new ActivityHandler(null),
        new AppDataHandler(null)));

    servlet.setBeanConverters(jsonConverter, xmlConverter);
  }

  private void setupInjector() {
    EasyMock.expect(injector.getInstance(PersonHandler.class)).andStubReturn(peopleHandler);
    EasyMock.expect(injector.getInstance(ActivityHandler.class)).andStubReturn(activityHandler);
    EasyMock.expect(injector.getInstance(AppDataHandler.class)).andStubReturn(appDataHandler);
  }

  public void testPeopleMethodRecognition() throws Exception {
    verifyHandlerWasFoundForMethod("{method:people.get,id:id,params:{userId:5,groupId:@self}}",
        peopleHandler);
  }

  public void testActivitiesMethodRecognition() throws Exception {
    verifyHandlerWasFoundForMethod("{method:activities.get,id:id,params:{userId:5,groupId:@self}}",
        activityHandler);
  }

  public void testAppDataMethodRecognition() throws Exception {
    verifyHandlerWasFoundForMethod("{method:appdata.get,id:id,params:{userId:5,groupId:@self}}",
        appDataHandler);
  }

  public void testInvalidService() throws Exception {
    String json = "{method:junk.get,id:id,params:{userId:5,groupId:@self}}";
    setupRequest(json);
    setupInjector();

    JSONObject err = new JSONObject(
        "{id:id,error:{message:'notImplemented: The service junk is not implemented',code:501}}");

    PrintWriter writerMock = EasyMock.createMock(PrintWriter.class);
    EasyMock.expect(res.getWriter()).andReturn(writerMock);
    writerMock.write(EasyMock.eq(err.toString()));
    EasyMock.expectLastCall();

    EasyMock.replay(req, res, injector, jsonConverter, writerMock);
    servlet.service(req, res);
    EasyMock.verify(req, res, injector, jsonConverter, writerMock);
    EasyMock.reset(req, res, injector, jsonConverter);
  }


  /**
   * Tests a data handler that returns a failed Future.
   * @throws Exception on failure
   */
  public void testFailedRequest() throws Exception {
    setupRequest("{id:id,method:appdata.get}");
    EasyMock.expect(injector.getInstance(AppDataHandler.class)).andStubReturn(appDataHandler);
    setupInjector();

    EasyMock.expect(appDataHandler.handleItem(EasyMock.isA(RpcRequestItem.class)));
    EasyMock.expectLastCall().andReturn(
        ImmediateFuture.errorInstance(new RuntimeException("FAILED")));

    JSONObject err = new JSONObject(
        "{id:id,error:{message:'internalError: FAILED',code:500}}");

    PrintWriter writerMock = EasyMock.createMock(PrintWriter.class);
    EasyMock.expect(res.getWriter()).andReturn(writerMock);
    writerMock.write(EasyMock.eq(err.toString()));
    EasyMock.expectLastCall();

    EasyMock.replay(req, res, appDataHandler, injector, jsonConverter, writerMock);
    servlet.service(req, res);
    EasyMock.verify(req, res, appDataHandler, injector, jsonConverter, writerMock);
    EasyMock.reset(req, res, appDataHandler, injector, jsonConverter);
  }

  private void verifyHandlerWasFoundForMethod(String json, DataRequestHandler handler)
      throws Exception {
    setupRequest(json);
    setupInjector();

    String resultObject = "my lovely json";

    EasyMock.expect(handler.handleItem(EasyMock.isA(RequestItem.class)));
    EasyMock.expectLastCall().andReturn(ImmediateFuture.newInstance(resultObject));

    EasyMock.expect(jsonConverter.convertToJson(resultObject))
        .andReturn(new JSONObject(Maps.immutableMap("foo", "bar")));

    JSONObject result = new JSONObject();
    result.put("id", "id");
    result.put("data", Maps.immutableMap("foo", "bar"));
    PrintWriter writerMock = EasyMock.createMock(PrintWriter.class);
    EasyMock.expect(res.getWriter()).andReturn(writerMock);
    writerMock.write(EasyMock.eq(result.toString()));
    EasyMock.expectLastCall();

    EasyMock.replay(req, res, handler, injector, jsonConverter, writerMock);
    servlet.service(req, res);
    EasyMock.verify(req, res, handler, injector, jsonConverter, writerMock);
    EasyMock.reset(req, res, handler, injector, jsonConverter);
  }

  public void testBasicBatch() throws Exception {
    String batchJson =
        "[{method:people.get,id:'1'},{method:activities.get,id:'2'}]";
    setupRequest(batchJson);
    setupInjector();

    String resultObject = "my lovely json";
    Future<?> responseItemFuture = ImmediateFuture.newInstance(resultObject);
    EasyMock.expect(peopleHandler.handleItem(EasyMock.isA(RequestItem.class)));
    EasyMock.expectLastCall().andReturn(responseItemFuture);
    EasyMock.expect(activityHandler.handleItem(EasyMock.isA(RequestItem.class)));
    EasyMock.expectLastCall().andReturn(responseItemFuture);

    EasyMock.expect(jsonConverter.convertToJson(resultObject))
        .andStubReturn(new JSONObject(Maps.immutableMap("foo", "bar")));

    JSONArray result = new JSONArray("[{id:'1',data:{foo:'bar'}}," + "{id:'2',data:{foo:'bar'}}]");
    PrintWriter writerMock = EasyMock.createMock(PrintWriter.class);
    EasyMock.expect(res.getWriter()).andReturn(writerMock);
    writerMock.write(EasyMock.eq(result.toString()));
    EasyMock.expectLastCall();

    EasyMock.replay(req, res, peopleHandler, activityHandler, injector, jsonConverter, writerMock);
    servlet.service(req, res);
    EasyMock.verify(req, res, peopleHandler, activityHandler, injector, jsonConverter, writerMock);
    EasyMock.reset(req, res, peopleHandler, activityHandler, injector, jsonConverter);
  }

  public void testGetExecution() throws Exception {
    EasyMock.expect(req.getParameterMap()).andStubReturn(
        Maps.immutableMap("method", new String[]{"people.get"}, "id", new String[]{"1"}));
    EasyMock.expect(req.getMethod()).andStubReturn("GET");
    EasyMock.expect(req.getAttribute(EasyMock.isA(String.class))).andReturn(FAKE_GADGET_TOKEN);
    EasyMock.expect(req.getCharacterEncoding()).andStubReturn("UTF-8");
    res.setCharacterEncoding("UTF-8");
    setupInjector();

    String resultObject = "my lovely json";

    Future<?> responseItemFuture = ImmediateFuture.newInstance(resultObject);
    EasyMock.expect(peopleHandler.handleItem(EasyMock.isA(RequestItem.class)));
    EasyMock.expectLastCall().andReturn(responseItemFuture);

    EasyMock.expect(jsonConverter.convertToJson(resultObject))
        .andReturn(new JSONObject(Maps.immutableMap("foo", "bar")));

    JSONObject result = new JSONObject("{id:'1',data:{foo:'bar'}}");
    PrintWriter writerMock = EasyMock.createMock(PrintWriter.class);
    EasyMock.expect(res.getWriter()).andReturn(writerMock);
    writerMock.write(EasyMock.eq(result.toString()));
    EasyMock.expectLastCall();

    EasyMock.replay(req, res, peopleHandler, activityHandler, injector, jsonConverter, writerMock);
    servlet.service(req, res);
    EasyMock.verify(req, res, peopleHandler, activityHandler, injector, jsonConverter, writerMock);
    EasyMock.reset(req, res, peopleHandler, activityHandler, injector, jsonConverter);
  }

  private void setupRequest(String json)
      throws IOException {
    EasyMock.expect(req.getReader()).andStubReturn(new BufferedReader(new StringReader(json)));
    EasyMock.expect(req.getMethod()).andStubReturn("POST");
    EasyMock.expect(req.getAttribute(EasyMock.isA(String.class))).andReturn(FAKE_GADGET_TOKEN);
    EasyMock.expect(req.getCharacterEncoding()).andStubReturn("UTF-8");
    res.setCharacterEncoding("UTF-8");
  }

}
