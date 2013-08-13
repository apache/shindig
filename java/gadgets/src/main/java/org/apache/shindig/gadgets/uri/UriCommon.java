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
package org.apache.shindig.gadgets.uri;

/**
 * Common class used for all Uri params.  Makes it very easy to find classes that
 * use an affected parameter and to insure against duplicates.
 */
public interface UriCommon {
  public static final String USER_PREF_PREFIX = "up_";
  public enum Param {
    URL("url"),
    GADGET("gadget"),
    CONTAINER("container"),
    VIEW("view"),
    LANG("lang"),
    COUNTRY("country"),
    DEBUG("debug"),
    NO_CACHE("nocache"),
    VERSION("v"),
    SECURITY_TOKEN("st"),
    OAUTH2_TOKEN("oauth_token"),
    MODULE_ID("mid"),
    REFRESH("refresh"),
    LIBS("libs"),
    JSON("json"),
    TYPE("type"),
    REWRITE_MIME_TYPE("rewriteMime"),
    SANITIZE("sanitize"),
    CAJOLE("caja"),

    // JS request params
    CONTAINER_MODE("c"),
    COMPILE_MODE("compile"),
    JSLOAD("jsload"),
    ONLOAD("onload"),
    LOADED("loaded"),
    NO_HINT("nohint"),
    REPOSITORY_ID("r"),

    // Proxy resize params:
    RESIZE_HEIGHT("resize_h"),
    RESIZE_WIDTH("resize_w"),
    RESIZE_QUALITY("resize_q"),
    NO_EXPAND("no_expand"),
    FALLBACK_URL_PARAM("fallback_url"),

    // proxy authz params:
    OAUTH_SERVICE_NAME("OAUTH_SERVICE_NAME"),
    AUTHZ("authz"),

    RETURN_ORIGINAL_CONTENT_ON_ERROR("rooe"),
    // The html tag which requested this proxy uri. For example, "script" when
    // "<script src='blah.js'></script>" is being proxied.
    HTML_TAG_CONTEXT("html_tag_context"),

    // This is a legacy param, superseded by container.
    @Deprecated
    SYND("synd");

    private final String key;
    private Param(String key) {
      this.key = key;
    }

    public String getKey() {
      return key;
    }
  }
}
