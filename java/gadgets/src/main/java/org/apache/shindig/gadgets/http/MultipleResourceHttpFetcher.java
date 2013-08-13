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
package org.apache.shindig.gadgets.http;

import com.google.common.collect.Maps;
import com.google.common.collect.Lists;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.Pair;
import org.apache.shindig.gadgets.GadgetException;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.List;
import java.util.Map;


/**
 * This class provides simple way for doing parallel fetches for multiple
 * resources using FutureTask's.
 */
public class MultipleResourceHttpFetcher {
  private final RequestPipeline requestPipeline;
  private final Executor executor;

  public MultipleResourceHttpFetcher(RequestPipeline requestPipeline, Executor executor) {
    this.requestPipeline = requestPipeline;
    this.executor = executor;
  }

  /**
   * Issue parallel requests to all resources that are needed.
   *
   * @param requests list of requests for which we want the resourses
   * @return futureTasks List of Pairs of url,futureTask for all the requests
   *    in same order as specified.
   */
  public List<Pair<Uri, FutureTask<RequestContext>>> fetchAll(List<HttpRequest> requests) {
    List<Pair<Uri, FutureTask<RequestContext>>> futureTasks = Lists.newArrayList();
    for (HttpRequest request : requests) {
      futureTasks.add(Pair.of(request.getUri(), createHttpFetcher(request)));
    }

    return futureTasks;
  }

  /**
   * Issue parallel requests to all the resources that are needed ignoring
   * duplicates.
   *
   * @param requests list of urls for which we want the image resourses
   * @return futureTasks map of url -> futureTask for all the requests sent.
   */
  public Map<Uri, FutureTask<RequestContext>> fetchUnique(List<HttpRequest> requests) {
    Map<Uri, FutureTask<RequestContext>> futureTasks = Maps.newHashMap();
    for (HttpRequest request : requests) {
      Uri uri = request.getUri();
      if (!futureTasks.containsKey(uri)) {
        futureTasks.put(uri, createHttpFetcher(request));
      }
    }

    return futureTasks;
  }

  // Fetch the content of the requested uri.
  private FutureTask<RequestContext> createHttpFetcher(HttpRequest request) {
    // Fetch the content of the requested uri.
    FutureTask<RequestContext> httpFetcher =
        new FutureTask<RequestContext>(new HttpFetchCallable(request, requestPipeline));
    executor.execute(httpFetcher);
    return httpFetcher;
  }

  private static class HttpFetchCallable implements Callable<RequestContext> {
    private final HttpRequest httpReq;
    private final RequestPipeline requestPipeline;

    public HttpFetchCallable(HttpRequest httpReq, RequestPipeline requestPipeline) {
      this.httpReq = httpReq;
      this.requestPipeline = requestPipeline;
    }

    public RequestContext call() {
      HttpResponse httpResp = null;
      GadgetException gadgetException = null;
      try {
        httpResp = requestPipeline.execute(httpReq);
      } catch (GadgetException e){
        gadgetException = e;
      }
      return new RequestContext(httpReq, httpResp, gadgetException);
    }
  }

  // Encapsulates the response context of a single resource fetch.
  public static class RequestContext {
    private final HttpRequest httpReq;
    private final HttpResponse httpResp;
    private final GadgetException gadgetException;

    public HttpRequest getHttpReq() {
      return httpReq;
    }

    public HttpResponse getHttpResp() {
      return httpResp;
    }

    public GadgetException getGadgetException() {
      return gadgetException;
    }

    public RequestContext(HttpRequest httpReq, HttpResponse httpResp, GadgetException ge) {
      this.httpReq = httpReq;
      this.httpResp = httpResp;
      this.gadgetException = ge;
    }

    @Override
    public int hashCode() {
      return httpReq.hashCode()
        ^ httpResp.hashCode()
        ^ gadgetException.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof RequestContext)) {
        return false;
      }
      RequestContext reqCxt = (RequestContext)obj;
      return httpReq.equals(reqCxt.httpReq) &&
          (httpResp != null ? httpResp.equals(reqCxt.httpResp) : reqCxt.httpResp == null) &&
          (gadgetException != null ? gadgetException.equals(reqCxt.gadgetException) :
              reqCxt.gadgetException == null);
    }
  }
}
