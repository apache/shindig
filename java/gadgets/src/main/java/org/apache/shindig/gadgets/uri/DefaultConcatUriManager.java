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
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.internal.Nullable;
import com.google.inject.name.Named;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.uri.UriCommon.Param;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DefaultConcatUriManager implements ConcatUriManager {
  public static final String CONCAT_HOST_PARAM = "gadgets.uri.concat.host";
  public static final String CONCAT_PATH_PARAM = "gadgets.uri.concat.path";
  public static final String CONCAT_JS_SPLIT_PARAM = "gadgets.uri.concat.js.splitToken";
  public static final String CONCAT_JS_EVAL_TPL = "eval(%s['%s']);";
  
  private static final ConcatUri BAD_URI =
      new ConcatUri(UriStatus.BAD_URI, null, null, null, null);
  private static final Integer START_INDEX = 1;
  
  private final ContainerConfig config;
  private final Versioner versioner;
  private boolean strictParsing;
  
  @Inject
  public DefaultConcatUriManager(ContainerConfig config, @Nullable Versioner versioner) {
    this.config = config;
    this.versioner = versioner;
  }
  
  @Inject(optional = true)
  public void setUseStrictParsing(
      @Named("shindig.uri.concat.use-strict-parsing") String useStrict) {
    this.strictParsing = Boolean.parseBoolean(useStrict);
  }

  public List<ConcatData> make(List<ConcatUri> resourceUris,
      boolean isAdjacent) {
    List<ConcatData> concatUris = Lists.newArrayListWithCapacity(resourceUris.size());
    
    if (resourceUris.size() == 0) {
      return concatUris;
    }

    ConcatUri exemplar = resourceUris.get(0);
    String container = exemplar.getContainer();
    String concatHost = getReqVal(container, CONCAT_HOST_PARAM);
    String concatPath = getReqVal(container, CONCAT_PATH_PARAM);
    
    UriBuilder uriBuilder = new UriBuilder();
    uriBuilder.setAuthority(concatHost);
    uriBuilder.setPath(concatPath);
    
    uriBuilder.addQueryParameter(Param.CONTAINER.getKey(), container);
    uriBuilder.addQueryParameter(Param.GADGET.getKey(), exemplar.getGadget());
    uriBuilder.addQueryParameter(Param.DEBUG.getKey(), exemplar.isDebug() ? "1" : "0");
    uriBuilder.addQueryParameter(Param.NO_CACHE.getKey(), exemplar.isNoCache() ? "1" : "0");
    
    // Above params are common for all generated Uris.
    // Use as a base for specific ConcatUri instances.
    Uri uriBase = uriBuilder.toUri();
    
    List<String> versions = null;
    List<List<Uri>> batches = Lists.newArrayListWithCapacity(resourceUris.size());
    for (ConcatUri ctx : resourceUris) {
      batches.add(ctx.getBatch());
    }
    
    if (versioner != null) {
      versions = versioner.version(batches, container);
    }
    
    Iterator<String> versionIt = versions != null ? versions.iterator() : null;
    for (ConcatUri ctx : resourceUris) {
      String version = versionIt != null ? versionIt.next() : null;
      concatUris.add(
          makeConcatUri(uriBase, ctx.getBatch(), ctx.getType(), container, isAdjacent, version));
    }
    
    return concatUris;
  }
  
  private ConcatData makeConcatUri(Uri uriBase, List<Uri> resourceUris, Type contentType,
      String container, boolean isAdjacent, String version) {
    // TODO: Consider per-bundle isAdjacent plus first-bundle direct evaluation
    
    if (!isAdjacent && contentType != Type.JS) {
      // Split-concat is only supported for JS at the moment.
      // This situation should never occur due to ConcatLinkRewriter's implementation.
      throw new UnsupportedOperationException("Split concatenation only supported for JS");
    }
    
    UriBuilder uriBuilder = new UriBuilder(uriBase);
    uriBuilder.addQueryParameter(Param.TYPE.getKey(), contentType.getType());
    Map<Uri, String> snippets = Maps.newHashMapWithExpectedSize(resourceUris.size());
    
    if (version != null) {
      uriBuilder.addQueryParameter(Param.VERSION.getKey(), version);
    }
    
    String splitParam = getReqVal(container, CONCAT_JS_SPLIT_PARAM);
    if (!isAdjacent) {
      uriBuilder.addQueryParameter(Param.JSON.getKey(), splitParam);
    }

    Integer i = new Integer(START_INDEX);
    for (Uri resource : resourceUris) {
      uriBuilder.addQueryParameter(i.toString(), resource.toString());
      i++;
      if (!isAdjacent) {
        snippets.put(resource, getJsSnippet(splitParam, resource));
      }
    }
    
    return new ConcatData(uriBuilder.toUri(), snippets);
  }
  
  static String getJsSnippet(String splitParam, Uri resource) {
    return String.format(CONCAT_JS_EVAL_TPL, splitParam,
        StringEscapeUtils.escapeJavaScript(resource.toString()));
  }
  
  private String getReqVal(String container, String key) {
    String val = config.getString(container, key);
    if (val == null) {
      throw new RuntimeException(
          "Missing required config '" + key + "' for container: " + container);
    }
    return val;
  }

  public ConcatUri process(Uri uri) {
    String container = uri.getQueryParameter(Param.CONTAINER.getKey());
    if (strictParsing && container == null) {
      return BAD_URI;
    }
    
    if (strictParsing) {
      String concatHost = getReqVal(container, CONCAT_HOST_PARAM);
      String concatPath = getReqVal(container, CONCAT_PATH_PARAM);
      if (!uri.getAuthority().equalsIgnoreCase(concatHost) ||
          !uri.getPath().equals(concatPath)) {
        return BAD_URI;
      }
    }
    
    // At this point the Uri is at least concat.
    UriStatus status = UriStatus.VALID_UNVERSIONED;
    List<Uri> uris = Lists.newLinkedList();
    Type type = Type.fromType(uri.getQueryParameter(Param.TYPE.getKey()));
    if (type == null) {
      // try "legacy" method
      type = Type.fromMime(uri.getQueryParameter("rewriteMime"));
      if (type == null) {
        return BAD_URI;
      }
    }
    String splitParam = type == Type.JS ? uri.getQueryParameter(Param.JSON.getKey()) : null;
    
    Integer i = new Integer(START_INDEX);
    String uriStr = null;
    while ((uriStr = uri.getQueryParameter(i.toString())) != null) {
      try {
        uris.add(Uri.parse(uriStr));
      } catch (IllegalArgumentException e) {
        // Malformed inbound Uri. Don't process.
        return BAD_URI;
      }
      i++;
    }
    
    if (versioner != null) {
      String version = uri.getQueryParameter(Param.VERSION.getKey());
      if (version != null) {
        status = versioner.validate(uris, container, version);
      }
    }
    
    return new ConcatUri(status, uris, splitParam, type, uri);
  }

}
