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

import org.apache.shindig.gadgets.http.HttpResponse;

/**
 * Base class for all Gadget exceptions. The bulk of the code uses
 * this class directly, differentiating between error conditions by
 * the Code enumeration.
 */
public class GadgetException extends Exception {
  public enum Code {
    // Catch-all for internal errors
    INTERNAL_SERVER_ERROR,

    // Configuration errors
    INVALID_PATH,
    INVALID_CONFIG,

    // User-data related errors.
    INVALID_USER_DATA,
    INVALID_SECURITY_TOKEN,

    // General xml
    EMPTY_XML_DOCUMENT,
    MALFORMED_XML_DOCUMENT,

    // HTTP errors
    FAILED_TO_RETRIEVE_CONTENT,

    // Feature-related errors
    UNSUPPORTED_FEATURE,

    // General error, should be accompanied by message
    INVALID_PARAMETER,
    MISSING_PARAMETER,
    UNRECOGNIZED_PARAMETER,
    POST_TOO_LARGE,

    // Interface component errors.
    MISSING_FEATURE_REGISTRY,
    MISSING_MESSAGE_BUNDLE_CACHE,
    MISSING_REMOTE_OBJECT_FETCHER,
    MISSING_SPEC_CACHE,

    // Caja error
    MALFORMED_FOR_SAFE_INLINING,

    // Parsing errors
    CSS_PARSE_ERROR,
    HTML_PARSE_ERROR,
    IMAGE_PARSE_ERROR,
    JS_PARSE_ERROR,

    // View errors
    UNKNOWN_VIEW_SPECIFIED,

    // Blacklisting
    NON_WHITELISTED_GADGET,

    // OAuth
    OAUTH_STORAGE_ERROR,
    OAUTH_APPROVAL_NEEDED,

    // Signed fetch
    REQUEST_SIGNING_FAILURE,

    // Error in the JavaScript processing pipeline
    JS_PROCESSING_ERROR,

    // Error validating that the gadget supplied is correct for the locked domain the request came from.
    GADGET_HOST_MISMATCH,

    //Gadget Admin Error
    GADGET_ADMIN_STORAGE_ERROR,
    GADGET_ADMIN_FEATURE_NOT_ALLOWED
  }

  private final Code code;
  private final int httpStatusCode;

  public GadgetException(Code code, int httpStatusCode) {
    this.code = code;
    this.httpStatusCode = httpStatusCode;
  }

  public GadgetException(Code code, Throwable cause, int httpStatusCode) {
    super(cause);
    this.code = code;
    this.httpStatusCode = httpStatusCode;
  }

  public GadgetException(Code code, String msg, Throwable cause, int httpStatusCode) {
    super(msg, cause);
    this.code = code;
    this.httpStatusCode = httpStatusCode;
  }

  public GadgetException(Code code, String msg, int httpStatusCode) {
    super(msg);
    this.code = code;
    this.httpStatusCode = httpStatusCode;
  }

  public GadgetException(Code code) {
    this(code, HttpResponse.SC_INTERNAL_SERVER_ERROR);
  }

  public GadgetException(Code code, Throwable cause) {
    this(code, cause, HttpResponse.SC_INTERNAL_SERVER_ERROR);
  }

  public GadgetException(Code code, String msg, Throwable cause) {
    this(code, msg, cause, HttpResponse.SC_INTERNAL_SERVER_ERROR);
  }

  public GadgetException(Code code, String msg) {
    this(code, msg, HttpResponse.SC_INTERNAL_SERVER_ERROR);
  }

  public Code getCode() {
    return code;
  }

  public int getHttpStatusCode() {
    return httpStatusCode;
  }
}
