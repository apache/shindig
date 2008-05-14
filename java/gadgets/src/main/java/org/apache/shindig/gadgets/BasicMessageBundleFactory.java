/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.gadgets;

import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.spec.LocaleSpec;
import org.apache.shindig.gadgets.spec.MessageBundle;

import com.google.inject.Inject;

import java.net.URI;

/**
 * Basic implementation of a message bundle factory
 */
public class BasicMessageBundleFactory implements MessageBundleFactory {

  private HttpFetcher bundleFetcher;

  public MessageBundle getBundle(LocaleSpec localeSpec, GadgetContext context)
      throws GadgetException {
    return getBundle(localeSpec.getMessages(), context.getIgnoreCache());
  }

  public MessageBundle getBundle(URI bundleUrl, boolean ignoreCache)
      throws GadgetException {
    HttpRequest request
        = HttpRequest.getRequest(bundleUrl, ignoreCache);
    HttpResponse response = bundleFetcher.fetch(request);
    if (response.getHttpStatusCode() != HttpResponse.SC_OK) {
      throw new GadgetException(
          GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT,
          "Unable to retrieve message bundle xml. HTTP error " +
          response.getHttpStatusCode());
    }
    MessageBundle bundle
        = new MessageBundle(bundleUrl, response.getResponseAsString());
    return bundle;
  }

  @Inject
  public BasicMessageBundleFactory(HttpFetcher bundleFetcher) {
    this.bundleFetcher = bundleFetcher;
  }
}
