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
package org.apache.shindig.gadgets;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.spec.View;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

/**
 * Retrieves the content pointed to by the href in a type=html view and inserts
 * it into the view.
 */
public class ViewContentFetcher implements Runnable {
  private static final Logger logger = Logger.getLogger(ViewContentFetcher.class.getName());
  private final View view;
  private final CountDownLatch latch;
  private final HttpFetcher httpFetcher;
  private final boolean ignoreCache;

  public ViewContentFetcher(View view,
                            CountDownLatch latch,
                            HttpFetcher httpFetcher,
                            boolean ignoreCache) {
    this.view = view;
    this.latch = latch;
    this.httpFetcher = httpFetcher;
    this.ignoreCache = ignoreCache;
  }

  /**
   * Retrieves the remote view content.
   */
  public void run() {
    HttpRequest request = new HttpRequest(Uri.fromJavaUri(view.getHref()))
        .setIgnoreCache(ignoreCache);
    try {
      HttpResponse response = httpFetcher.fetch(request);
      if (response.getHttpStatusCode() != HttpResponse.SC_OK) {
        throw new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT,
                                  "Unable to retrieve gadget content. HTTP error " +
                                  response.getHttpStatusCode());
      } else {
        view.setContent(response.getResponseAsString());

        // Reset the href since the content is now inline; a non-null href will
        // indicate a failed retrieval attempt.
        view.setHref(null);
      }
    } catch (GadgetException e) {
      logger.info("Failed to retrieve content at "  + view.getHref());
    }
    latch.countDown();
  }
}
