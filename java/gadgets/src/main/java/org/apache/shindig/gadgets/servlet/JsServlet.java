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
package org.apache.shindig.gadgets.servlet;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.servlet.InjectedServlet;
import org.apache.shindig.common.util.CharsetUtil;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.js.JsException;
import org.apache.shindig.gadgets.js.JsRequest;
import org.apache.shindig.gadgets.js.JsRequestBuilder;
import org.apache.shindig.gadgets.js.JsResponse;
import org.apache.shindig.gadgets.js.JsServingPipeline;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple servlet serving up JavaScript files by their registered aliases.
 * Used by type=URL gadgets in loading JavaScript resources.
 */
public class JsServlet extends InjectedServlet {
  private static final long serialVersionUID = 6255917470412008175L;

  private JsServingPipeline jsServingPipeline;
  private CachingSetter cachingSetter;

  private JsRequestBuilder jsRequestBuilder;

  @VisibleForTesting
  static class CachingSetter {
    public void setCachingHeaders(HttpServletResponse resp, int ttl, boolean noProxy) {
      if (ttl < 0) {
        HttpUtil.setCachingHeaders(resp, noProxy);
      } else if (ttl == 0) {
        HttpUtil.setNoCache(resp);
      } else {
        HttpUtil.setCachingHeaders(resp, ttl, noProxy);
      }
    }
  }

  @Inject
  public void setJsRequestBuilder(JsRequestBuilder jsRequestBuilder) {
    checkInitialized();
    this.jsRequestBuilder = jsRequestBuilder;
  }

  @Inject
  public void setCachingSetter(CachingSetter cachingSetter) {
    this.cachingSetter = cachingSetter;
  }

  @Inject
  public void setJsServingPipeline(JsServingPipeline jsServingPipeline) {
    this.jsServingPipeline = jsServingPipeline;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {

    JsRequest jsRequest;
    try {
      jsRequest = jsRequestBuilder.build(req);
    } catch (GadgetException e) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    JsResponse jsResponse;
    try {
      jsResponse = jsServingPipeline.execute(jsRequest);
    } catch (JsException e) {
      resp.sendError(e.getStatusCode(), e.getMessage());
      return;
    }

    emitJsResponse(jsResponse, req, resp);
  }

  protected void emitJsResponse(JsResponse jsResponse, HttpServletRequest req,
      HttpServletResponse resp) throws IOException {
    if (jsResponse.getStatusCode() == HttpServletResponse.SC_NOT_MODIFIED) {
      resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      cachingSetter.setCachingHeaders(
          resp, jsResponse.getCacheTtlSecs(), !jsResponse.isProxyCacheable());
      return;
    }
    if (jsResponse.getStatusCode() == HttpServletResponse.SC_OK && jsResponse.toJsString().length() == 0) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    cachingSetter.setCachingHeaders(
        resp, jsResponse.getCacheTtlSecs(), !jsResponse.isProxyCacheable());

    resp.setStatus(jsResponse.getStatusCode());
    resp.setContentType("text/javascript; charset=utf-8");
    byte[] response = CharsetUtil.getUtf8Bytes(jsResponse.toJsString());
    resp.setContentLength(response.length);
    resp.getOutputStream().write(response);
  }
}
