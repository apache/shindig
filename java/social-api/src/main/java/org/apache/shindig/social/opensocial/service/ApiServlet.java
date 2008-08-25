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

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.servlet.InjectedServlet;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.core.oauth.AuthenticationServletFilter;
import org.apache.shindig.social.core.util.BeanJsonConverter;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;

/**
 * Common base class for API servlets.
 */
public class ApiServlet extends InjectedServlet {
  private Map<String, Class<? extends DataRequestHandler>> handlers;
  protected BeanJsonConverter jsonConverter;
  protected BeanConverter xmlConverter;

  @Inject
  public void setHandlers(HandlerProvider handlers) {
    this.handlers = handlers.get();
  }

  @Inject
  public void setBeanConverters(
      @Named("shindig.bean.converter.json")BeanConverter jsonConverter,
      @Named("shindig.bean.converter.xml")BeanConverter xmlConverter) {
    // fix this
    this.jsonConverter = (BeanJsonConverter) jsonConverter;
    this.xmlConverter = xmlConverter;
  }

  // Only for testing use. Do not override the injector.
  public void setInjector(Injector injector) {
    this.injector = injector;
  }

  protected SecurityToken getSecurityToken(HttpServletRequest servletRequest) {
    return ((AuthenticationServletFilter.SecurityTokenRequest) servletRequest).getToken();
  }

  /**
   * Delivers a request item to the appropriate DataRequestHandler.
   */
  protected Future<? extends ResponseItem> handleRequestItem(RequestItem requestItem) {
    Class<? extends DataRequestHandler> handlerClass = handlers.get(requestItem.getService());

    if (handlerClass == null) {
      return ImmediateFuture.newInstance(new ResponseItem(ResponseError.NOT_IMPLEMENTED,
          "The service " + requestItem.getService() + " is not implemented"));
    }

    DataRequestHandler handler = injector.getInstance(handlerClass);
    return handler.handleItem(requestItem);
  }

  protected ResponseItem getResponseItem(Future<? extends ResponseItem> future) {
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

  protected ResponseItem responseItemFromException(Throwable t) {
    return new ResponseItem(ResponseError.INTERNAL_ERROR, t.getMessage());
  }
}
