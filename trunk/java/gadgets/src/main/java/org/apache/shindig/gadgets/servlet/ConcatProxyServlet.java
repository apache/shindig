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

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.common.collect.Lists;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.servlet.InjectedServlet;
import org.apache.shindig.common.Pair;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.common.util.CharsetUtil;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.MultipleResourceHttpFetcher;
import org.apache.shindig.gadgets.http.MultipleResourceHttpFetcher.RequestContext;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.rewrite.ResponseRewriterRegistry;
import org.apache.shindig.gadgets.rewrite.RewriterRegistry;
import org.apache.shindig.gadgets.rewrite.ResponseRewriterList.RewriteFlow;
import org.apache.shindig.gadgets.rewrite.RewritingException;
import org.apache.shindig.gadgets.uri.ConcatUriManager;
import org.apache.shindig.gadgets.uri.UriCommon.Param;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet which concatenates the content of several proxied HTTP responses
 */
public class ConcatProxyServlet extends InjectedServlet {

  private static final long serialVersionUID = -4390212150673709895L;

  public static final String JSON_PARAM = Param.JSON.getKey();
  private static final Pattern JSON_PARAM_PATTERN = Pattern.compile("^\\w*$");

  static final Integer LONG_LIVED_REFRESH = (365 * 24 * 60 * 60);  // 1 year
  static final Integer DEFAULT_REFRESH = (60 * 60);                // 1 hour

  //class name for logging purpose
  private static final String classname = ConcatProxyServlet.class.getName();
  private static final Logger LOG = Logger.getLogger(classname,MessageKeys.MESSAGES);

  private transient RequestPipeline requestPipeline;
  private transient ConcatUriManager concatUriManager;
  private transient ResponseRewriterRegistry contentRewriterRegistry;

  // Sequential version of 'execute' by default.
  private transient Executor executor = Executors.newSingleThreadExecutor();

  private Integer longLivedRefreshSec = LONG_LIVED_REFRESH;

  @Inject(optional = true)
  public void setLongLivedRefresh(
      @Named("org.apache.shindig.gadgets.servlet.longLivedRefreshSec") int longLivedRefreshSec) {
    this.longLivedRefreshSec = longLivedRefreshSec;
  }

  @Inject
  public void setRequestPipeline(RequestPipeline requestPipeline) {
    checkInitialized();
    this.requestPipeline = requestPipeline;
  }

  @Inject
  public void setConcatUriManager(ConcatUriManager concatUriManager) {
    checkInitialized();
    this.concatUriManager = concatUriManager;
  }

  @Inject
  public void setContentRewriterRegistry(@RewriterRegistry(rewriteFlow = RewriteFlow.DEFAULT)
                                         ResponseRewriterRegistry contentRewriterRegistry) {
    checkInitialized();
    this.contentRewriterRegistry = contentRewriterRegistry;
  }

  @Inject
  public void setExecutor(@Named("shindig.concat.executor") Executor executor) {
    checkInitialized();
    // Executor is independently named to allow separate configuration of
    // concat fetch parallelism and other Shindig job execution.
    this.executor = executor;
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
      } catch (GadgetException gex) {
      response.sendError(HttpResponse.SC_BAD_REQUEST, formatError("doGet", gex, uri));
      return;
    }

    // Throughout this class, wherever output is generated it's done as a UTF8 String.
    // As such, we affirmatively state that UTF8 is being returned here.
    response.setHeader("Content-Type", concatType.getMimeType() + "; charset=UTF8");
    response.setHeader("Content-Disposition", "attachment;filename=p.txt");

    ConcatOutputStream cos = createConcatOutputStream(response, concatUri);
    if(cos == null) {
      response.setStatus(HttpResponse.SC_BAD_REQUEST);
      response.getOutputStream().println(
              formatHttpError(HttpServletResponse.SC_BAD_REQUEST,
                  "Bad json variable name " + concatUri.getSplitParam(), null));
    } else {
      if (doFetchConcatResources(response, concatUri, uri, cos)) {
        response.setStatus(HttpResponse.SC_OK);
      } else {
        response.setStatus(HttpResponse.SC_BAD_REQUEST);
      }
      IOUtils.closeQuietly(cos);
    }
  }

  /**
   * Creates the correct ConcatOutputStream to use.  Will return null if there
   * is a bad JSON varibale name.
   * @param response HTTP response object.
   * @param concatUri The concat URI.
   * @return The correct ConcatOutputStream to use.
   * @throws IOException thrown when the ConcatOutputStream cannot be created.
   */
  private ConcatOutputStream createConcatOutputStream(HttpServletResponse response,
          ConcatUriManager.ConcatUri concatUri) throws IOException {
    ConcatOutputStream cos;
    String jsonVar = concatUri.getSplitParam();
    if (jsonVar != null) {
      // JSON-concat mode.
      if (JSON_PARAM_PATTERN.matcher(jsonVar).matches()) {
        cos = new JsonConcatOutputStream(response.getOutputStream(), jsonVar);
      } else {
        return null;
      }
    } else {
      // Standard concat output mode.
      cos = new VerbatimConcatOutputStream(response.getOutputStream());
    }
    return cos;
  }

  /**
   * @param response HttpservletResponse.
   * @param concatUri URI representing the concatenated list of resources requested.
   * @param cos The ConcatOutputStream to write the response to.
   * @return false for cases where concat resources could not be fetched, true for success cases.
   * @throws IOException
   */
  private boolean doFetchConcatResources(HttpServletResponse response,
      ConcatUriManager.ConcatUri concatUri, Uri uri, ConcatOutputStream cos) throws IOException {
    // Check for json concat and set output stream.
    Long minCacheTtl = Long.MAX_VALUE;
    boolean isMinCacheTtlSet = false;

    List<HttpRequest> requests = Lists.newArrayList();

    try {
      for (Uri resourceUri : concatUri.getBatch()) {
        try {
          requests.add(concatUri.makeHttpRequest(resourceUri));
        } catch (GadgetException ge) {
          if (cos.outputError(resourceUri, ge)) {
            // True returned from outputError indicates a terminal error.
            return false;
          }
        }
      }

      MultipleResourceHttpFetcher parallelFetcher =
          new MultipleResourceHttpFetcher(requestPipeline, executor);
      List<Pair<Uri, FutureTask<RequestContext>>> futureTasks = parallelFetcher.fetchAll(requests);

      for (Pair<Uri, FutureTask<RequestContext>> futureTask : futureTasks) {
        RequestContext requestCxt;
        try {
          try {
            requestCxt = futureTask.two.get();
          } catch (InterruptedException ie) {
            throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, ie);
          } catch (ExecutionException ee) {
            throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, ee);
          }
          if (requestCxt.getGadgetException() != null) {
            throw requestCxt.getGadgetException();
          }
          HttpResponse httpResp = requestCxt.getHttpResp();
          if (httpResp != null) {
            if (contentRewriterRegistry != null) {
              try {
                httpResp = contentRewriterRegistry.rewriteHttpResponse(requestCxt.getHttpReq(),
                        httpResp, null);
              } catch (RewritingException e) {
                throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e,
                        e.getHttpStatusCode());
              }
            }
            minCacheTtl = Math.min(minCacheTtl, httpResp.getCacheTtl());
            isMinCacheTtlSet = true;
            cos.output(futureTask.one, httpResp);
          } else {
            return false;
          }
        } catch (GadgetException ge) {
          if (cos.outputError(futureTask.one, ge)) {
            return false;
          }
        }
      }
      // TODO: Investigate Chunked Encoding
      minCacheTtl = isMinCacheTtlSet ? (minCacheTtl / 1000) : DEFAULT_REFRESH;
      HttpUtil.setCachingHeaders(response,
          concatUri.translateStatusRefresh(longLivedRefreshSec, minCacheTtl.intValue()), false);
    } catch (GadgetException gex) {
      cos.outputError(uri, gex);
    }
    return true;
  }

  private static String formatHttpError(int status, String errorMessage, Uri uri) {
    StringBuilder err = new StringBuilder();
    err.append("/* ---- Error ");
    err.append(status);
    if (!Strings.isNullOrEmpty(errorMessage)) {
      err.append(", ");
      err.append(errorMessage);
    }
    if (uri != null) {
      err.append(" (").append(uri.toString()).append(')');
    }

    err.append(" ---- */");
    return err.toString();
  }

  private static String formatError(String methodname, GadgetException excep, Uri uri)
      throws IOException {
    StringBuilder err = new StringBuilder();
    err.append("/* ---- Error ");
    err.append(excep.getCode().toString());
    err.append(" concat(");
    err.append(uri.toString());
    err.append(") ");
    err.append(excep.getMessage());
    err.append(" ---- */");

    // Log the errors here for now. We might want different severity levels
    // for different error codes.
    if (LOG.isLoggable(Level.INFO)) {
      LOG.logp(Level.INFO, classname, methodname, MessageKeys.CONCAT_PROXY_REQUEST_FAILED, new Object[] {err.toString()});
    }
    return err.toString();
  }

  private static abstract class ConcatOutputStream extends ServletOutputStream {
    private final ServletOutputStream wrapped;
    private final StringBuilder stringBuilder;

    protected ConcatOutputStream(ServletOutputStream wrapped) {
      this.wrapped = wrapped;
      stringBuilder = new StringBuilder();
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
      println(formatError("outputError", e, uri));
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
      wrapped.write(CharsetUtil.getUtf8Bytes(stringBuilder.toString()));
      wrapped.close();
    }

    @Override
    public void print(String data) throws IOException {
      stringBuilder.append(data);
    }

    private String CRLF = "\r\n";

    @Override
    public void println(String data) throws IOException {
      print(data + CRLF);
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
    private boolean firstEntry;

    public JsonConcatOutputStream(ServletOutputStream wrapped, String tok) throws IOException {
      super(wrapped);
      this.println(tok + "={");
      this.firstEntry = true;
    }

    @Override
    protected void outputJs(Uri uri, String data) throws IOException {
      if (!firstEntry) {
        println(",");
      }
      firstEntry = false;

      print("\"");
      print(uri.toString());
      print("\":\"");
      print(StringEscapeUtils.escapeEcmaScript(data));
      print("\"");
    }

    @Override
    public void close() throws IOException {
      println("};");
      super.close();
    }

  }
}
