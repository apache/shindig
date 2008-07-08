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
package org.apache.shindig.social.dataservice;

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.SecurityTokenDecoder;
import org.apache.shindig.common.SecurityTokenException;
import org.apache.shindig.common.servlet.InjectedServlet;
import org.apache.shindig.common.servlet.ParameterFetcher;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.opensocial.util.BeanConverter;
import org.apache.shindig.social.opensocial.util.BeanJsonConverter;
import org.apache.shindig.social.opensocial.util.BeanXmlConverter;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;

public class DataServiceServlet extends InjectedServlet {

  protected static final String X_HTTP_METHOD_OVERRIDE = "X-HTTP-Method-Override";
  protected static final String SECURITY_TOKEN_PARAM = "st";
  protected static final String REQUEST_PARAMETER = "request";
  protected static final String FORMAT_PARAM = "format";
  protected static final String ATOM_FORMAT = "atom";

  public static final String PEOPLE_ROUTE = "people";
  public static final String ACTIVITY_ROUTE = "activities";
  public static final String APPDATA_ROUTE = "appdata";

  private static final Logger logger = Logger.getLogger(
      "org.apache.shindig.social.dataservice");

  private SecurityTokenDecoder securityTokenDecoder;
  private Map<String, Class<? extends DataRequestHandler>> handlers;
  private BeanJsonConverter jsonConverter;
  private BeanXmlConverter xmlConverter;
  private ParameterFetcher parameterFetcher;
  private static final String JSON_BATCH_ROUTE = "jsonBatch";

  @Inject
  public void setHandlers(HandlerProvider handlers) {
    this.handlers = handlers.get();
  }

  @Inject
  public void setSecurityTokenDecoder(SecurityTokenDecoder securityTokenDecoder) {
    this.securityTokenDecoder = securityTokenDecoder;
  }

  @Inject
  public void setBeanConverters(BeanJsonConverter jsonConverter, BeanXmlConverter xmlConverter) {
    this.jsonConverter = jsonConverter;
    this.xmlConverter = xmlConverter;
  }

  @Inject
  public void setParameterFetcher(@Named("DataServiceServlet") ParameterFetcher parameterFetcher) {
    this.parameterFetcher = parameterFetcher;
  }

  // Only for testing use. Do not override the injector.
  public void setInjector(Injector injector) {
    this.injector = injector;
  }

  protected void doGet(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse)
      throws ServletException, IOException {
    doPost(servletRequest, servletResponse);
  }

  protected void doPut(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse)
      throws ServletException, IOException {
    doPost(servletRequest, servletResponse);
  }

  protected void doDelete(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse)
      throws ServletException, IOException {
    doPost(servletRequest, servletResponse);
  }

  protected void doPost(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse)
      throws ServletException, IOException {
    logger.finest("Handling restful request for " + servletRequest.getPathInfo());

    servletRequest.setCharacterEncoding("UTF-8");
    SecurityToken token = getSecurityToken(servletRequest);
    BeanConverter converter = getConverterForRequest(servletRequest);

    if (isBatchUrl(servletRequest)) {
      try {
        handleBatchRequest(servletRequest, servletResponse, token, converter);
      } catch (JSONException e) {
        throw new RuntimeException("Bad batch format", e);
      }
    } else {
      handleSingleRequest(servletRequest, servletResponse, token, converter);
    }
  }

  /** Handler for non-batch requests */
  private void handleSingleRequest(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse, SecurityToken token,
      BeanConverter converter) throws IOException {
    String method = getHttpMethodFromParameter(servletRequest.getMethod(),
        servletRequest.getParameter(X_HTTP_METHOD_OVERRIDE));

    RequestItem requestItem = new RequestItem(servletRequest, token, method, converter);
    ResponseItem responseItem = getResponseItem(handleRequestItem(requestItem));

    if (responseItem.getError() == null) {
      PrintWriter writer = servletResponse.getWriter();
      writer.write(converter.convertToString(responseItem.getResponse()));
    } else {
      servletResponse.sendError(responseItem.getError().getHttpErrorCode(),
          responseItem.getErrorMessage());
    }
  }

  private void handleBatchRequest(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse, SecurityToken token,
      BeanConverter converter) throws IOException, JSONException {

    byte[] postedBytes = IOUtils.toByteArray(servletRequest.getInputStream());
    JSONObject requests = new JSONObject(new String(postedBytes));
    Map<String, Future<? extends ResponseItem>> responses = Maps.newHashMap();

    // Gather all Futures.  We do this up front so that
    // the first call to get() comes after all futures are created,
    // which allows for implementations that batch multiple Futures
    // into single requests.
    Iterator keys = requests.keys();
    while (keys.hasNext()) {
      String key = (String) keys.next();
      String request = requests.getString(key);

      RequestItem requestItem = converter.convertToObject(request, RequestItem.class);
      requestItem.setToken(token);
      requestItem.setConverter(converter);

      responses.put(key, handleRequestItem(requestItem));
    }

    // Resolve each Future into a response.
    // TODO: should use shared deadline across each request
    Map<String, ResponseItem> resolvedResponses = Maps.newHashMap();
    for (Map.Entry<String, Future<? extends ResponseItem>> responseEntry : responses.entrySet()) {
      ResponseItem response = getResponseItem(responseEntry.getValue());
      resolvedResponses.put(responseEntry.getKey(), response);
    }

    PrintWriter writer = servletResponse.getWriter();
    writer.write(converter.convertToString(
        Maps.immutableMap("error", false, "responses", resolvedResponses)));
  }

  private ResponseItem getResponseItem(Future<? extends ResponseItem> future) {
    ResponseItem response;
    try {
      // TODO: use timeout methods?
      response = future.get();
    } catch (InterruptedException ie) {
      response = responseItemFromException(ie);
    } catch (ExecutionException ee) {
      response = responseItemFromException(ee.getCause());
    }
    return response;
  }

  /**
   * Delivers a request item to the appropriate DataRequestHandler.
   * 
   * @return the resulting ResponseItem
   */
  Future<? extends ResponseItem> handleRequestItem(RequestItem requestItem) {
    String route = getRouteFromParameter(requestItem.getUrl());
    Class<? extends DataRequestHandler> handlerClass = handlers.get(route);

    if (handlerClass == null) {
      throw new RuntimeException("No handler for route: " + route);
    }

    DataRequestHandler handler = injector.getInstance(handlerClass);
    return handler.handleItem(requestItem);
  }

  private ResponseItem<?> responseItemFromException(Throwable t) {
    return new ResponseItem<Void>(ResponseError.INTERNAL_ERROR, t.getMessage(), null);
  }

  SecurityToken getSecurityToken(HttpServletRequest servletRequest) {
    SecurityToken token;
    try {
      token = securityTokenDecoder.createToken(parameterFetcher.fetch(servletRequest));
    } catch (SecurityTokenException e) {
      throw new RuntimeException("Implement error return for bad security token.", e);
    }
    return token;
  }

  BeanConverter getConverterForRequest(HttpServletRequest servletRequest) {
    String formatString = servletRequest.getParameter(FORMAT_PARAM);
    if (ATOM_FORMAT.equals(formatString)) {
      return xmlConverter;
    }
    return jsonConverter;
  }

  String getHttpMethodFromParameter(String method, String overrideParameter) {
    if (!StringUtils.isBlank(overrideParameter)) {
      return overrideParameter;
    } else {
      return method;
    }
  }

  String getRouteFromParameter(String pathInfo) {
    pathInfo = pathInfo.substring(1);
    int indexOfNextPathSeparator = pathInfo.indexOf("/");
    return indexOfNextPathSeparator != -1 ?
        pathInfo.substring(0, indexOfNextPathSeparator) :
        pathInfo;
  }

  boolean isBatchUrl(HttpServletRequest servletRequest) {
    String[] parts = servletRequest.getPathInfo().split("/");
    return parts[parts.length - 1].equals(JSON_BATCH_ROUTE);
  }
}
