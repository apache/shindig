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
package org.apache.shindig.gadgets.http;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.util.Base32;
import org.apache.shindig.common.util.CharsetUtil;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class to update HttpResponse metadata value.
 *
 * @since 2.0.0
 */
public class HttpResponseMetadataHelper {
  public static final String DATA_HASH = "DataHash";
  public static final String IMAGE_HEIGHT = "ImageHeight";
  public static final String IMAGE_WIDTH = "ImageWidth";

  //class name for logging purpose
  private static final String classname = HttpResponseMetadataHelper.class.getName();
  private static final Logger LOG = Logger.getLogger(classname,MessageKeys.MESSAGES);

  /**
   * Return a copy of input response with additional metadata values.
   * @param response source response
   * @param values added metadata values
   * @return copy of source response with updated metadata
   */
  public static HttpResponse updateMetadata(HttpResponse response, Map<String, String> values) {
    Map<String, String> metadata = Maps.newHashMap(response.getMetadata());
    // metadata.putAll(values);
    for (Map.Entry<String, String> val : values.entrySet()) {
      metadata.put(val.getKey(), val.getValue());
    }
    return new HttpResponseBuilder(response).setMetadata(metadata).create();
  }

  /**
   * Calculate hash value for response and update metadata value (DATA_HASH)
   * @return hash value
   */
  public String getHash(HttpResponse response) {
    try {
      MessageDigest md5 = MessageDigest.getInstance("MD5");
      md5.update(response.getResponseAsBytes());
      byte[] md5val = md5.digest();
      return CharsetUtil.newUtf8String(Base32.encodeBase32(md5val));
    } catch (NoSuchAlgorithmException e) {
      // Should not happen
      if (LOG.isLoggable(Level.INFO)) {
        LOG.logp(Level.INFO, classname, "getHash", MessageKeys.ERROR_GETTING_MD5);
      }
    }
    return null;
  }

  public static HttpResponse updateHash(HttpResponse response, HttpResponseMetadataHelper helper) {
    if (helper != null) {
      String hash = helper.getHash(response);
      if (hash != null) {
        return updateMetadata(response, ImmutableMap.<String, String>of(DATA_HASH, hash));
      }
    }
    return response;
  }
}
