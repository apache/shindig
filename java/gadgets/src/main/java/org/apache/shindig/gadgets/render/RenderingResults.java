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
package org.apache.shindig.gadgets.render;

import org.apache.shindig.common.uri.Uri;

import com.google.common.base.Preconditions;

import javax.servlet.http.HttpServletResponse;

/**
 * Contains the results of a rendering operation.
 */
public final class RenderingResults {
  private final Status status;
  private final String content;
  private final String errorMessage;
  private final int httpStatusCode;

  private final Uri redirect;

  private RenderingResults(Status status, String content, String errorMessage,
      int httpStatusCode, Uri redirect) {
    this.status = status;
    this.content = content;
    this.errorMessage = errorMessage;
    this.httpStatusCode = httpStatusCode;

    this.redirect = redirect;
  }

  public static RenderingResults ok(String content) {
    return new RenderingResults(Status.OK, content, null, HttpServletResponse.SC_OK, null);
  }

  public static RenderingResults error(String errorMessage, int httpStatusCode) {
    return new RenderingResults(Status.ERROR, null, errorMessage, httpStatusCode, null);
  }

  public static RenderingResults mustRedirect(Uri redirect) {
    Preconditions.checkNotNull(redirect);
    return new RenderingResults(Status.MUST_REDIRECT, null, null, HttpServletResponse.SC_FOUND,
        redirect);
  }

  /**
   * @return The status of the rendering operation.
   */
  public Status getStatus() {
    return status;
  }

  /**
   * @return The content to render. Only available when status is OK.
   */
  public String getContent() {
    Preconditions.checkState(status == Status.OK, "Only available when status is OK.");
    return content;
  }

  /**
   * @return The error message for rendering. Only available when status is ERROR.
   */
  public String getErrorMessage() {
    Preconditions.checkState(status == Status.ERROR, "Only available when status is ERROR.");
    return errorMessage;
  }

  /**
   * @return The HTTP status code for rendering. Only available when status is ERROR.
   */
  public int getHttpStatusCode() {
    Preconditions.checkState(status == Status.ERROR, "Only available when status is ERROR.");
    return httpStatusCode;
  }

  /**
   * @return The error message for rendering. Only available when status is ERROR.
   */
  public Uri getRedirect() {
    Preconditions.checkState(status == Status.MUST_REDIRECT, "Only available when status is MUST_REDIRECT.");
    return redirect;
  }

  public enum Status {
    OK, MUST_REDIRECT, ERROR
  }
}
