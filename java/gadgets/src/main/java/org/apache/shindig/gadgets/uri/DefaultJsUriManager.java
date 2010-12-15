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

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.GadgetException.Code;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.uri.UriCommon.Param;

import java.util.Arrays;
import java.util.Collection;

/**
 * Generates and validates URLs serviced by a gadget JavaScript service (JsServlet).
 */
public class DefaultJsUriManager implements JsUriManager {
  static final String JS_HOST_PARAM = "gadgets.uri.js.host";
  static final String JS_PATH_PARAM = "gadgets.uri.js.path";
  static final JsUri INVALID_URI = new JsUri(UriStatus.BAD_URI, Lists.<String>newArrayList());
  protected static final String JS_SUFFIX = ".js";
  protected static final String JS_DELIMITER = ":";

  private final ContainerConfig config;
  private final Versioner versioner;

  @Inject
  public DefaultJsUriManager(ContainerConfig config, Versioner versioner) {
    this.config = config;
    this.versioner = versioner;
  }

  public Uri makeExternJsUri(JsUri ctx) {
    String container = ctx.getContainer();
    String jsHost = getReqConfig(container, JS_HOST_PARAM);
    String jsPathBase = getReqConfig(container, JS_PATH_PARAM);

    // We somewhat cheat in that jsHost may contain protocol/scheme as well.
    UriBuilder uri = new UriBuilder(Uri.parse(jsHost));

    // Add JS info to path and set it in URI.
    StringBuilder jsPath = new StringBuilder(jsPathBase);
    if (!jsPathBase.endsWith("/")) {
      jsPath.append('/');
    }
    jsPath.append(addJsLibs(ctx.getLibs()));
    jsPath.append(JS_SUFFIX);
    uri.setPath(jsPath.toString());

    // Standard container param, as JS may be container-specific.
    uri.addQueryParameter(Param.CONTAINER.getKey(), container);

    // Pass through nocache param for dev purposes.
    uri.addQueryParameter(Param.NO_CACHE.getKey(),
        ctx.isNoCache() ? "1" : "0");

    // Pass through debug param for debugging use.
    uri.addQueryParameter(Param.DEBUG.getKey(),
        ctx.isDebug() ? "1" : "0");

    uri.addQueryParameter(Param.CONTAINER_MODE.getKey(),
        ctx.getContext().getParamValue());

    // Pass through gadget Uri
    if (addGadgetUri()) {
      uri.addQueryParameter(Param.URL.getKey(), ctx.getGadget());
    }

    if (ctx.getOnload() != null) {
      uri.addQueryParameter(Param.ONLOAD.getKey(), ctx.getOnload());
    }

    if (ctx.isJsload()) {
      uri.addQueryParameter(Param.JSLOAD.getKey(), "1");
    }

    // Finally, version it, but only if !nocache.
    if (versioner != null && !ctx.isNoCache()) {
      uri.addQueryParameter(Param.VERSION.getKey(),
          versioner.version(ctx.getGadget(), container, ctx.getLibs()));
    }
    if (ctx.getExtensionParams() != null) {
      uri.addQueryParameters(ctx.getExtensionParams());
    }

    return uri.toUri();
  }

  /**
   * Essentially pulls apart a Uri created by makeExternJsUri, validating its
   * contents, especially the version key.
   */
  public JsUri processExternJsUri(Uri uri) throws GadgetException {
    // Validate basic Uri structure and params
    String container = uri.getQueryParameter(Param.CONTAINER.getKey());
    if (container == null) {
      container = ContainerConfig.DEFAULT_CONTAINER;
    }

    // Get config values up front.
    getReqConfig(container, JS_HOST_PARAM); // validate that it exists
    String jsPrefix = getReqConfig(container, JS_PATH_PARAM);

    String host = uri.getAuthority();
    if (host == null) {
      issueUriFormatError("Unexpected: Js Uri has no host");
      return INVALID_URI;
    }

    // Pull out the collection of features referenced by the Uri.
    String path = uri.getPath();
    if (path == null) {
      issueUriFormatError("Unexpected: Js Uri has no path");
      return INVALID_URI;
    }
    if (!path.startsWith(jsPrefix)) {
      issueUriFormatError("Js Uri path invalid, expected prefix: " + jsPrefix + ", is: " + path);
      return INVALID_URI;
    }
    path = path.substring(jsPrefix.length());

    // Convenience suffix: pull off .js if present; leave alone otherwise.
    if (path.endsWith(JS_SUFFIX)) {
      path = path.substring(0, path.length() - JS_SUFFIX.length());
    }

    while (path.startsWith("/")) {
      path = path.substring(1);
    }

    Collection<String> libs = getJsLibs(path);
    UriStatus status = UriStatus.VALID_UNVERSIONED;
    String version = uri.getQueryParameter(Param.VERSION.getKey());
    if (version != null && versioner != null) {
      String gadgetParam = uri.getQueryParameter(Param.URL.getKey());
      status = versioner.validate(gadgetParam, container, libs, version);
    }

    return new JsUri(status, uri, libs);
  }

  static String addJsLibs(Collection<String> extern) {
    return StringUtils.join(extern, JS_DELIMITER);
  }

  static Collection<String> getJsLibs(String path) {
    return Arrays.asList(StringUtils.split(path, JS_DELIMITER));
  }

  private String getReqConfig(String container, String key) {
    String ret = config.getString(container, key);
    if (ret == null) {
      ret = config.getString(ContainerConfig.DEFAULT_CONTAINER, key);
      if (ret == null) {
        throw new RuntimeException("Container '" + container +
            "' missing config for required param: " + key);
      }
    }
    return ret;
  }

  // May be overridden to report errors in an alternate way to the user.
  protected void issueUriFormatError(String err) throws GadgetException {
    throw new GadgetException(Code.INVALID_PARAMETER, err, HttpResponse.SC_BAD_REQUEST);
  }

  // Overridable in the event that a Versioner implementation is injected
  // that uses the gadget itself to perform intelligent optimization and versioning.
  // This isn't the cleanest logic, so should be cleaned up when better concrete
  // examples of this behavior exist.
  protected boolean addGadgetUri() {
    return false;
  }
}
