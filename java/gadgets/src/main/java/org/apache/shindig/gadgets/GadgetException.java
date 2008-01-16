/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.shindig.gadgets;

/**
 * Base class for all Gadget exceptions. The bulk of the code uses
 * this class directly, differentiating between error conditions by
 * the Code enumeration.
 */
public class GadgetException extends Exception {
  public static enum Code {
    // Catch-all for internal errors
    INTERNAL_SERVER_ERROR,

    // Configuration errors
    INVALID_PATH,

    // User-data related errors.
    INVALID_USER_DATA,

    // General xml
    EMPTY_XML_DOCUMENT,
    MALFORMED_XML_DOCUMENT,

    // HTTP errors
    FAILED_TO_RETRIEVE_CONTENT,

    // Feature-related errors
    UNSUPPORTED_FEATURE,

    // Interface component errors.
    MISSING_FEATURE_REGISTRY,
    MISSING_MESSAGE_BUNDLE_CACHE,
    MISSING_REMOTE_OBJECT_FETCHER,
    MISSING_SPEC_CACHE,

    // Caja error
    MALFORMED_FOR_SAFE_INLINING,

    // Blacklisting
    BLACKLISTED_GADGET
  }

  private final Code code;

  public GadgetException(Code code) {
    this.code = code;
  }

  public GadgetException(Code code, Throwable cause) {
    super(cause);
    this.code = code;
  }

  public GadgetException(Code code, String msg) {
    super(msg);
    this.code = code;
  }

  public Code getCode() {
    return code;
  }
}
