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
package org.apache.shindig.expressions;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.shindig.common.util.CharsetUtil;
import org.apache.shindig.common.util.Utf8UrlCoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.el.ELException;

/**
 * Default functions in the OpenSocial-Templating spec are prefixed with "os:"
 * All other functions are prefixed with "osx:"
 */
public final class OpensocialFunctions {
  private OpensocialFunctions() {
  }

  /**
   * Convert a string to a JSON Object or JSON Array.
   */
  @Functions.Expose(prefix = "osx", names = {"parseJson"})
  public static Object parseJson(String text) {
    if ((text == null) || "".equals(text)) {
      return null;
    }

    try {
      if (text.startsWith("[")) {
        return new JSONArray(text);
      } else {
        return new JSONObject(text);
      }
    } catch (JSONException je) {
      throw new ELException(je);
    }
  }

  /**
   * Decode a base-64 encoded string.
   */
  @Functions.Expose(prefix = "osx", names = {"decodeBase64"})
  public static String decodeBase64(String text) {
    if (text == null) {
      return null;
    }

    // TODO: allow a charset to be passed in?
    return CharsetUtil.newUtf8String(Base64.decodeBase64(CharsetUtil.getUtf8Bytes(text)));
  }

  /**
   * Form encode a string
   */
  @Functions.Expose(prefix = "os", names = {"urlEncode"})
  public static String formEncode(String text) {
    if (text == null) {
      return null;
    }

    return Utf8UrlCoder.encode(text);
  }

  /**
   * Form decode a string
   * @param text
   * @return
   */
  @Functions.Expose(prefix = "os", names = {"urlDecode"})
  public static String formDecode(String text) {
    if (text == null) {
      return null;
    }

    return Utf8UrlCoder.decode(text);
  }

  /**
   * Escape HTML entities in a string
   */
  @Functions.Expose(prefix = "os", names = {"htmlEncode"})
  public static String htmlEncode(String text) {
    if (text == null) {
      return null;
    }

    return StringEscapeUtils.escapeHtml4(text);
  }

  /**
   * Unescape HTML entities in a string
   * @param text
   * @return
   */
  @Functions.Expose(prefix = "os", names = {"htmlDecode"})
  public static String htmlDecode(String text) {
    if (text == null) {
      return null;
    }

    return StringEscapeUtils.unescapeHtml4(text);
  }
}
