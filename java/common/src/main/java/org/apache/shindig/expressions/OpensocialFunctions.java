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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import javax.el.ELException;

/**
 * Default functions in the "os:" namespace prefix.
 */
public class OpensocialFunctions {
  private OpensocialFunctions() {
  }
  
  /**
   * Convert a string to a JSON Object or JSON Array.
   */
  @Functions.Expose(prefix = "os", names = {"xParseJson"})
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
  @Functions.Expose(prefix = "os", names = {"xDecodeBase64"})
  public static String decodeBase64(String text) {
    try {
      // TODO: allow a charset to be passed in?
      return new String(Base64.decodeBase64(text.getBytes("UTF-8")),
          "UTF-8");
    } catch (UnsupportedEncodingException uee) {
      // UTF-8 will be supported everywhere
      throw new RuntimeException(uee);
    }
  }
}
