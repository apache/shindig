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

import org.apache.shindig.auth.AuthInfo;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.servlet.InjectedServlet;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.core.util.BeanJsonConverter;
import org.apache.shindig.social.opensocial.spi.SocialSpiException;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Common base class for API servlets.
 */
public abstract class ApiServlet extends InjectedServlet {
  protected static final String DEFAULT_ENCODING = "UTF-8";

  private HandlerDispatcher dispatcher;
  protected BeanJsonConverter jsonConverter;
  protected BeanConverter xmlConverter;
  protected BeanConverter atomConverter;

  @Inject
  public void setHandlerDispatcher(HandlerDispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }

  @Inject
  public void setBeanConverters(
      @Named("shindig.bean.converter.json") BeanConverter jsonConverter,
      @Named("shindig.bean.converter.xml") BeanConverter xmlConverter,
      @Named("shindig.bean.converter.atom") BeanConverter atomConverter) {
    // fix this
    this.jsonConverter = (BeanJsonConverter) jsonConverter;
    this.xmlConverter = xmlConverter;
    this.atomConverter = atomConverter;
  }

  protected SecurityToken getSecurityToken(HttpServletRequest servletRequest) {
    return new AuthInfo(servletRequest).getSecurityToken();
  }

  protected abstract void sendError(HttpServletResponse servletResponse, ResponseItem responseItem)
      throws IOException;

  protected void sendSecurityError(HttpServletResponse servletResponse) throws IOException {
    sendError(servletResponse, new ResponseItem(ResponseError.UNAUTHORIZED,
        "The request did not have a proper security token nor oauth message and unauthenticated "
            + "requests are not allowed"));
  }

  /**
   * Delivers a request item to the appropriate DataRequestHandler.
   */
  protected Future<?> handleRequestItem(RequestItem requestItem,
      HttpServletRequest servletRequest) {
    DataRequestHandler handler = dispatcher.getHandler(requestItem.getService());

    if (handler == null) {
      return ImmediateFuture.errorInstance(new SocialSpiException(ResponseError.NOT_IMPLEMENTED,
          "The service " + requestItem.getService() + " is not implemented"));
    }

    return handler.handleItem(requestItem);
  }

  protected ResponseItem getResponseItem(Future<?> future) {
    ResponseItem response;
    try {
      // TODO: use timeout methods?
      Object result = future != null ? future.get() : null;
      // TODO: null is now a supported return value for post/delete, but
      // is bad for get().
      response = new ResponseItem(result != null ? result : Collections.emptyMap());
    } catch (InterruptedException ie) {
      response = responseItemFromException(ie);
    } catch (ExecutionException ee) {
      response = responseItemFromException(ee.getCause());
    }

    return response;
  }

  protected ResponseItem responseItemFromException(Throwable t) {
    if (t instanceof SocialSpiException) {
      SocialSpiException spe = (SocialSpiException) t;
      return new ResponseItem(spe.getError(), spe.getMessage());
    }

    return new ResponseItem(ResponseError.INTERNAL_ERROR, t.getMessage());
  }

  protected void setCharacterEncodings(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse) throws IOException {
    if (servletRequest.getCharacterEncoding() == null) {
      servletRequest.setCharacterEncoding(DEFAULT_ENCODING);
    }
    servletResponse.setCharacterEncoding(DEFAULT_ENCODING);
  }
}
