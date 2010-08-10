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

package org.apache.shindig.gadgets.servlet;

import com.google.inject.Inject;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.servlet.InjectedServlet;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.rewrite.RequestRewriterRegistry;
import org.apache.shindig.gadgets.rewrite.RewritingException;
import org.apache.shindig.gadgets.uri.ConcatUriManager;
import org.apache.shindig.gadgets.uri.UriCommon.Param;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet which concatenates the content of several proxied HTTP responses
 *
 * @see org.apache.shindig.gadgets.rewrite.old.HTMLContentRewriter
 */
public class ConcatProxyServlet extends InjectedServlet {

  public static final String JSON_PARAM = Param.JSON.getKey();
  private static final Pattern JSON_PARAM_PATTERN = Pattern.compile("^\\w*$");
  
  // TODO: parameterize these.
  static final Integer LONG_LIVED_REFRESH = (365 * 24 * 60 * 60);  // 1 year
  static final Integer DEFAULT_REFRESH = (60 * 60);                // 1 hour

  private static final Logger logger
      = Logger.getLogger(ConcatProxyServlet.class.getName());

  private RequestPipeline requestPipeline;
  private ConcatUriManager concatUriManager;
  private RequestRewriterRegistry contentRewriterRegistry;

  @Inject
  public void setRequestPipeline(RequestPipeline requestPipeline) {
    this.requestPipeline = requestPipeline;
  }
  
  @Inject
  public void setConcatUriManager(ConcatUriManager concatUriManager) {
    this.concatUriManager = concatUriManager;
  }

  @Inject
  public void setContentRewriterRegistry(RequestRewriterRegistry contentRewriterRegistry) {
    this.contentRewriterRegistry = contentRewriterRegistry;
  }

  @SuppressWarnings("boxing")
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    if (request.getHeader("If-Modified-Since") != null) {
      response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }

    Uri uri = new UriBuilder(request).toUri();
    ConcatUriManager.ConcatUri concatUri = concatUriManager.process(uri);

    ConcatUriManager.Type concatType = concatUri.getType();
    try {
      if (concatType == null) {
        throw new GadgetException(GadgetException.Code.MISSING_PARAMETER, "Missing type",
            HttpResponse.SC_BAD_REQUEST);
      }
      HttpUtil.setCachingHeaders(response,
          concatUri.translateStatusRefresh(LONG_LIVED_REFRESH, DEFAULT_REFRESH), false);
    } catch (GadgetException gex) {
      response.sendError(HttpResponse.SC_BAD_REQUEST, formatError(gex, uri));
      return;
    }
    
    // Throughout this class, wherever output is generated it's done as a UTF8 String.
    // As such, we affirmatively state that UTF8 is being returned here.
    response.setHeader("Content-Type", concatType.getMimeType() + "; charset=UTF8");
    response.setHeader("Content-Disposition", "attachment;filename=p.txt");

    // Check for json concat and set output stream.
    ConcatOutputStream cos = null;
    
    String jsonVar = concatUri.getSplitParam();
    if (jsonVar != null) {
      // JSON-concat mode.
      if (JSON_PARAM_PATTERN.matcher(jsonVar).matches()) {
        cos = new JsonConcatOutputStream(response.getOutputStream(), jsonVar);
      } else {
        response.getOutputStream().println(
            formatHttpError(HttpServletResponse.SC_BAD_REQUEST,
                "Bad json variable name " + jsonVar, null));
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
    } else {
      // Standard concat output mode.
      cos = new VerbatimConcatOutputStream(response.getOutputStream());
    }

    for (Uri resourceUri : concatUri.getBatch()) {
      try {
        HttpRequest httpReq = concatUri.makeHttpRequest(resourceUri);
        HttpResponse httpResp = requestPipeline.execute(httpReq);
        if (contentRewriterRegistry != null) {
          try {
            httpResp = contentRewriterRegistry.rewriteHttpResponse(httpReq, httpResp);
          } catch (RewritingException e) {
            throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e,
                e.getHttpStatusCode());
          }
        }
        cos.output(resourceUri, httpResp);
      } catch (GadgetException ge) {
        response.setStatus(HttpResponse.SC_BAD_REQUEST);
        if (cos.outputError(resourceUri, ge)) {
          // True returned from outputError indicates a terminal error.
          return;
        }
      }
    }
    
    cos.close();
    response.setStatus(HttpResponse.SC_OK);
  }

  private static String formatHttpError(int status, String errorMessage, Uri uri) {
    StringBuilder err = new StringBuilder();
    err.append("/* ---- Error ");
    err.append(status);
    if (!StringUtils.isEmpty(errorMessage)) {
      err.append(", ");
      err.append(errorMessage);
    }
    if (uri != null) {
      err.append(" (").append(uri.toString()).append(')');
    }

    err.append(" ---- */");
    return err.toString();
  }

  private static String formatError(GadgetException excep, Uri uri)
      throws IOException {
    StringBuilder err = new StringBuilder();
    err.append(excep.getCode().toString());
    err.append(" concat(");
    err.append(uri.toString());
    err.append(") ");
    err.append(excep.getMessage());

    // Log the errors here for now. We might want different severity levels
    // for different error codes.
    logger.log(Level.INFO, "Concat proxy request failed", err);
    return err.toString();
  }
  
  private static abstract class ConcatOutputStream extends ServletOutputStream {
    private final ServletOutputStream wrapped;
    
    protected ConcatOutputStream(ServletOutputStream wrapped) {
      this.wrapped = wrapped;
    }
    
    protected abstract void outputJs(Uri uri, String data) throws IOException;
    
    public void output(Uri uri, HttpResponse resp) throws IOException {
      if (resp.getHttpStatusCode() != HttpServletResponse.SC_OK) {
        println(formatHttpError(resp.getHttpStatusCode(), resp.getResponseAsString(), uri));
      } else {
        outputJs(uri, resp.getResponseAsString());
      }
    }
    
    public boolean outputError(Uri uri, GadgetException e)
        throws IOException {
      print(formatError(e, uri));
      return e.getHttpStatusCode() == HttpResponse.SC_INTERNAL_SERVER_ERROR;
    }

    @Override
    public void write(int b) throws IOException {
      wrapped.write(b);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
      wrapped.write(b, off, len);
    }

    @Override
    public void write(byte b[]) throws IOException {
      wrapped.write(b);
    }
    
    @Override
    public void close() throws IOException {
      wrapped.close();
    }
    
    @Override
    public void print(String data) throws IOException {
      write(data.getBytes("UTF8"));
    }
    
    @Override
    public void println(String data) throws IOException {
      print(data);
      write("\r\n".getBytes("UTF8"));
    }
  }
  
  private static class VerbatimConcatOutputStream extends ConcatOutputStream {    
    public VerbatimConcatOutputStream(ServletOutputStream wrapped) {
      super(wrapped);
    }

    @Override
    protected void outputJs(Uri uri, String data) throws IOException {
      println("/* ---- Start " + uri.toString() + " ---- */");
      print(data);
      println("/* ---- End " + uri.toString() + " ---- */");
    }
  }
  
  private static class JsonConcatOutputStream extends ConcatOutputStream {    
    public JsonConcatOutputStream(ServletOutputStream wrapped, String tok) throws IOException {
      super(wrapped);
      this.println(tok + "={");
    }

    @Override
    protected void outputJs(Uri uri, String data) throws IOException {
      print("\"");
      print(uri.toString());
      print("\":\"");
      print(StringEscapeUtils.escapeJavaScript(data));
      println("\",");
    }
    
    @Override
    public void close() throws IOException {
      println("};");
      super.close();
    }
    
  }
}

