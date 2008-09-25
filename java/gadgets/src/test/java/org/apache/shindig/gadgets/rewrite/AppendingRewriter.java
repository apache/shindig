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
package org.apache.shindig.gadgets.rewrite;

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.MutableContent;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;

/**
 * Simple ContentRewriter implementation that appends
 * some particular String to the given input content.
 * Used for testing.
 */
class AppendingRewriter implements ContentRewriter {
  private String appender;
  private final long cacheTtl;

  AppendingRewriter(String appender) {
    this.appender = appender;
    this.cacheTtl = 0;
  }

  AppendingRewriter(String appender, long cacheTtl) {
    this.appender = appender;
    this.cacheTtl = cacheTtl;
  }

  public RewriterResults rewrite(HttpRequest request, HttpResponse original,
      MutableContent c) {
    // Appends appender to the end of the content string.
    c.setContent(c.getContent() + appender);
    return RewriterResults.cacheable(cacheTtl);
  }

  public RewriterResults rewrite(Gadget gadget, MutableContent content) {
    // Appends appender to the end of the input string.
	gadget.setContent(content.getContent() + appender);
	return RewriterResults.cacheable(cacheTtl);
  }

  void setAppender(String newAppender) {
    // This can be used to simulate a rewriter that returns different
    // results per run for the same input content.
    this.appender = newAppender;
  }
}