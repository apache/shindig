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

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.protocol.conversion.BeanConverter;

import com.google.common.collect.ImmutableMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataServiceServlet extends ApiServlet {

  protected static final String FORMAT_PARAM = "format";
  protected static final String ATOM_FORMAT = "atom";
  protected static final String XML_FORMAT = "xml";

  public static final String CONTENT_TYPE = "CONTENT_TYPE";

  private static final Logger logger = Logger.getLogger("org.apache.shindig.social.opensocial.spi");

  protected static final String X_HTTP_METHOD_OVERRIDE = "X-HTTP-Method-Override";

  @Override
  protected void doGet(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse)
      throws ServletException, IOException {
    doPost(servletRequest, servletResponse);
  }

  @Override
  protected void doPut(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse)
      throws ServletException, IOException {
    doPost(servletRequest, servletResponse);
  }

  @Override
  protected void doDelete(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse)
      throws ServletException, IOException {
    doPost(servletRequest, servletResponse);
  }

  @Override
  protected void doPost(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse)
      throws ServletException, IOException {
    if (logger.isLoggable(Level.FINEST)) logger.finest("Handling restful request for " + servletRequest.getPathInfo());

    setCharacterEncodings(servletRequest, servletResponse);

    SecurityToken token = getSecurityToken(servletRequest);
    if (token == null) {
      sendSecurityError(servletResponse);
      return;
    }

    BeanConverter converter = getConverterForRequest(servletRequest);

    handleSingleRequest(servletRequest, servletResponse, token, converter);
  }

  @Override
  protected void sendError(HttpServletResponse servletResponse, ResponseItem responseItem)
      throws IOException {
    servletResponse.sendError(responseItem.getError().getHttpErrorCode(),
        responseItem.getErrorMessage());
  }

  /**
   * Handler for non-batch requests.
   */
  private void handleSingleRequest(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse, SecurityToken token,
      BeanConverter converter) throws IOException {

    // TODO Rework to allow sub-services
    String path = servletRequest.getPathInfo();

    // TODO - This shouldnt be on BaseRequestItem
    String method = servletRequest.getParameter(X_HTTP_METHOD_OVERRIDE);
    if (method == null) {
      method = servletRequest.getMethod();
    }

    // Always returns a non-null handler.
    RestHandler handler = dispatcher.getRestHandler(path, method.toUpperCase());

    Reader bodyReader = null;
    if (!servletRequest.getMethod().equals("GET") && !servletRequest.getMethod().equals("HEAD")) {
      String contentType = servletRequest.getContentType();
      if (contentType == null || !contentType.startsWith("application/x-www-form-urlencoded")) {
        bodyReader = servletRequest.getReader();
      }
    }

    // Execute the request
    Future future = handler.execute(path, servletRequest.getParameterMap(),
        bodyReader, token, converter);

    ResponseItem responseItem = getResponseItem(future);

    servletResponse.setContentType(converter.getContentType());
    if (responseItem.getError() == null) {
      PrintWriter writer = servletResponse.getWriter();
      Object response = responseItem.getResponse();
      // TODO: ugliness resulting from not using RestfulItem
      if (!(response instanceof DataCollection) && !(response instanceof RestfulCollection)) {
        response = ImmutableMap.of("entry", response);
      }

      writer.write(converter.convertToString(response));
    } else {
      sendError(servletResponse, responseItem);
    }
  }


  BeanConverter getConverterForRequest(HttpServletRequest servletRequest) {
    String formatString = null;
    BeanConverter converter = null;
    String contentType = null;

    try {
      formatString = servletRequest.getParameter(FORMAT_PARAM);
    } catch (Throwable t) {
      // this happens while testing
      if (logger.isLoggable(Level.FINE)) logger.fine("Unexpected error : format param is null " + t.toString());
    }
    try {
      contentType = servletRequest.getHeader(CONTENT_TYPE);
    } catch (Throwable t) {
      //this happens while testing
      if (logger.isLoggable(Level.FINE)) logger.fine("Unexpected error : content type is null " + t.toString());
    }

    if (contentType != null) {
      if (contentType.equals("application/json")) {
        converter = jsonConverter;
      } else if (contentType.equals("application/atom+xml")) {
        converter = atomConverter;
      } else if (contentType.equals("application/xml")) {
        converter = xmlConverter;
      } else if (formatString == null) {
        // takes care of cases where content!= null but is ""
        converter = jsonConverter;
      }
    } else if (formatString != null) {
      if (formatString.equals(ATOM_FORMAT)) {
        converter = atomConverter;
      } else if (formatString.equals(XML_FORMAT)) {
        converter = xmlConverter;
      } else {
        converter = jsonConverter;
      }
    } else {
      converter = jsonConverter;
    }
    return converter;
  }
}
