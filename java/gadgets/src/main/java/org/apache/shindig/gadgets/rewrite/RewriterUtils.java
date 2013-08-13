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
package org.apache.shindig.gadgets.rewrite;

import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.uri.UriCommon;

// Temporary replacement of javax.annotation.Nullable
import org.apache.shindig.common.Nullable;

/**
 * Various utility functions used by rewriters
 */
public final class RewriterUtils {
  private RewriterUtils() {}

  public static boolean isHtml(HttpRequest request, HttpResponse original) {
    String mimeType = getMimeType(request, original);
    return mimeType != null && (mimeType.contains("html"));
  }

  public static boolean isHtml(HttpRequest request, HttpResponseBuilder original) {
    String mimeType = getMimeType(request, original);
    return mimeType != null && (mimeType.contains("html")) &&
           maybeAcceptHtml(request.getParam(UriCommon.Param.HTML_TAG_CONTEXT.getKey()));
  }

  /**
   * Returns true if the given html tag can accept text/html data. For now,
   * all html tags other than "script" are treated as html accepting tags.
   *
   * @param htmlTagName The html tag in question.
   * @return True if {@code htmlTagName} accepts text/html data, false
   *   otherwise.
   */
  public static boolean maybeAcceptHtml(@Nullable String htmlTagName) {
    return !"script".equalsIgnoreCase(htmlTagName);
  }

  public static boolean isCss(HttpRequest request, HttpResponse original) {
    String mimeType = getMimeType(request, original);
    return mimeType != null && mimeType.contains("css");
  }

  public static boolean isCss(HttpRequest request, HttpResponseBuilder original) {
    String mimeType = getMimeType(request, original);
    return mimeType != null && mimeType.contains("css");
  }

  // TODO: Also check if the HTML_TAG_CONTEXT is script.
  public static boolean isJavascript(HttpRequest request, HttpResponse original) {
    String mimeType = getMimeType(request, original);
    return mimeType != null && mimeType.contains("javascript");
  }

  public static boolean isJavascript(HttpRequest request, HttpResponseBuilder original) {
    String mimeType = getMimeType(request, original);
    return mimeType != null && mimeType.contains("javascript");
  }

  public static String getMimeType(HttpRequest request, HttpResponse original) {
    String mimeType = request.getRewriteMimeType();
    if (mimeType == null) {
      mimeType = original.getHeader("Content-Type");
    }
    return mimeType != null ? mimeType.toLowerCase() : null;
  }

  public static String getMimeType(HttpRequest request, HttpResponseBuilder original) {
    String mimeType = request.getRewriteMimeType();
    if (mimeType == null) {
      mimeType = original.getHeader("Content-Type");
    }
    return mimeType != null ? mimeType.toLowerCase() : null;
  }
}
