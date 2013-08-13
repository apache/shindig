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
package org.apache.shindig.protocol;

import com.google.common.base.Strings;
import com.google.common.base.Joiner;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Common mime content types and utilities
 */
public final class ContentTypes {
  private ContentTypes() {}

  /**
   * Allowed alternatives to application/json, including types listed
   * in JSON-RPC spec.
   */
  public static final Set<String> ALLOWED_JSON_CONTENT_TYPES =
      ImmutableSet.of("application/json", "text/x-json", "application/javascript",
          "application/x-javascript", "text/javascript", "text/ecmascript",
          "application/json-rpc", "application/jsonrequest");

  /**
   * Allowed alternatives to application/xml
   */
  public static final Set<String> ALLOWED_XML_CONTENT_TYPES =
      ImmutableSet.of("text/xml", "application/xml");

  /**
   * Allowed alternatives to application/atom+xml
   */
  public static final Set<String> ALLOWED_ATOM_CONTENT_TYPES =
      ImmutableSet.of("application/atom+xml");

  /**
   * Content types that are forbidden for REST & RPC calls
   */
  public static final Set<String> FORBIDDEN_CONTENT_TYPES =
      ImmutableSet.of(
          "application/x-www-form-urlencoded" // Not allowed because of OAuth body signing issues
      );

  public static final String MULTIPART_FORM_CONTENT_TYPE = "multipart/form-data";

  public static final Set<String> ALLOWED_MULTIPART_CONTENT_TYPES =
      ImmutableSet.of(MULTIPART_FORM_CONTENT_TYPE);

  public static final String OUTPUT_JSON_CONTENT_TYPE = "application/json";
  public static final String OUTPUT_XML_CONTENT_TYPE = "application/xml";
  public static final String OUTPUT_ATOM_CONTENT_TYPE = "application/atom+xml";

  /**
   * Extract the mime part from an Http Content-Type header
   */
  public static String extractMimePart(String contentType) {
    contentType = contentType.trim();
    int separator = contentType.indexOf(';');
    if (separator != -1) {
      contentType = contentType.substring(0, separator);
    }
    return contentType.trim().toLowerCase();
  }

  public static void checkContentTypes(Set<String> allowedContentTypes,
      String contentType) throws InvalidContentTypeException {

    if (Strings.isNullOrEmpty(contentType)) {
      throw new InvalidContentTypeException(
          "No Content-Type specified. One of "
              + Joiner.on(", ").join(allowedContentTypes) + " is required");
    }

    contentType = ContentTypes.extractMimePart(contentType);

    if (ContentTypes.FORBIDDEN_CONTENT_TYPES.contains(contentType)) {
      throw new InvalidContentTypeException(
          "Cannot use disallowed Content-Type " + contentType);
    }
    if (allowedContentTypes.contains(contentType)) {
      return;
    }
    throw new InvalidContentTypeException(
        "Unsupported Content-Type "
            + contentType
            + ". One of "
            + Joiner.on(", ").join(allowedContentTypes)
            + " is required");
  }

  public static class InvalidContentTypeException extends Exception {
    public InvalidContentTypeException(String message) {
      super(message);
    }
  }
}
