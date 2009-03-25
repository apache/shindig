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

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.ImmutableSet;

import java.util.Set;
import java.util.logging.Logger;

/**
 * Common mime content types and utilities
 */
public class ContentTypes {

  private static final Logger logger = Logger.getLogger(ContentTypes.class.getName());

  /**
   * Allowed alternatives to application/json
   */
  public static final Set<String> ALLOWED_JSON_CONTENT_TYPES =
      ImmutableSet.of("application/json", "text/x-json", "application/javascript",
          "application/x-javascript", "text/javascript", "text/ecmascript");

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
      String contentType, boolean disallowUnknownContentTypes) throws InvalidContentTypeException {

    if (StringUtils.isEmpty(contentType)) {
       if (disallowUnknownContentTypes) {
        throw new InvalidContentTypeException(
            "No Content-Type specified. One of "
                + StringUtils.join(allowedContentTypes, ", ") + " is required");
       } else {
         // No content type specified, we can fail in other ways later.
         return;
       }
    }

    contentType = ContentTypes.extractMimePart(contentType);

    if (ContentTypes.FORBIDDEN_CONTENT_TYPES.contains(contentType)) {
      throw new InvalidContentTypeException(
          "Cannot use disallowed Content-Type " + contentType);
    }
    if (allowedContentTypes.contains(contentType)) {
      return;
    }
    if (disallowUnknownContentTypes) {
      throw new InvalidContentTypeException(
          "Unsupported Content-Type "
              + contentType
              + ". One of "
              + StringUtils.join(allowedContentTypes, ", ")
              + " is required");
    } else {
      logger.warning("Unsupported Content-Type "
          + contentType
          + ". One of "
          + StringUtils.join(allowedContentTypes, ", ")
          + " is expected");
    }
  }

  public static class InvalidContentTypeException extends Exception {
    public InvalidContentTypeException(String message) {
      super(message);
    }
  }
}
