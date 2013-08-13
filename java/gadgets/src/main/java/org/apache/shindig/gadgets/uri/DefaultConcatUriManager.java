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
import com.google.inject.name.Named;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.shindig.common.servlet.Authority;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.uri.UriCommon.Param;


// Temporary replacement of javax.annotation.Nullable
import org.apache.shindig.common.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Default implementation of a ConcatUriManager
 *
 * @since 2.0.0
 */
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
  private Authority authority;
  private static int DEFAULT_URL_MAX_LENGTH = 2048;
  private int urlMaxLength = DEFAULT_URL_MAX_LENGTH;
  private static final float URL_LENGTH_BUFFER_MARGIN = .8f;

  @Inject
  public DefaultConcatUriManager(ContainerConfig config, @Nullable Versioner versioner) {
    this.config = config;
    this.versioner = versioner;
  }

  @Inject(optional = true)
  public void setUseStrictParsing(
      @Named("shindig.uri.concat.use-strict-parsing") boolean useStrict) {
    this.strictParsing = useStrict;
  }

  @Inject(optional = true)
  public void setUrlMaxLength(
      @Named("org.apache.shindig.gadgets.uri.urlMaxLength") int urlMaxLength) {
    this.urlMaxLength = urlMaxLength;
  }

  @Inject(optional = true)
  public void setAuthority(Authority authority) {
    this.authority = authority;
  }

  public int getUrlMaxLength() {
    return this.urlMaxLength;
  }

  public List<ConcatData> make(List<ConcatUri> resourceUris,
      boolean isAdjacent) {
    List<ConcatData> concatUris = Lists.newArrayListWithCapacity(resourceUris.size());

    if (resourceUris.isEmpty()) {
      return concatUris;
    }

    ConcatUri exemplar = resourceUris.get(0);
    String container = exemplar.getContainer();

    for (ConcatUri ctx : resourceUris) {
      concatUris.add(makeConcatUri(ctx, isAdjacent, container));
    }
    return concatUris;
  }

  private ConcatData makeConcatUri(ConcatUri ctx, boolean isAdjacent, String container) {
    // TODO: Consider per-bundle isAdjacent plus first-bundle direct evaluation

    if (!isAdjacent && ctx.getType() != Type.JS) {
      // Split-concat is only supported for JS at the moment.
      // This situation should never occur due to ConcatLinkRewriter's implementation.
      throw new UnsupportedOperationException("Split concatenation only supported for JS");
    }

    String concatHost = getReqVal(ctx.getContainer(), CONCAT_HOST_PARAM);
    String concatPath = getReqVal(ctx.getContainer(), CONCAT_PATH_PARAM);

    List<Uri> resourceUris = ctx.getBatch();
    Map<Uri, String> snippets = Maps.newHashMapWithExpectedSize(resourceUris.size());

    String splitParam = config.getString(ctx.getContainer(), CONCAT_JS_SPLIT_PARAM);

    boolean doSplit = false;
    if (!isAdjacent && splitParam != null && !"false".equalsIgnoreCase(splitParam)) {
      doSplit = true;
    }

    UriBuilder uriBuilder = makeUriBuilder(ctx, concatHost, concatPath);

    // Allowed Max Url length is .80 times of actual max length. So, Split will
    // happen whenever Concat url length crosses this value. Here, buffer also assumes
    // version length.
    int injectedMaxUrlLength = (int) (this.getUrlMaxLength() * URL_LENGTH_BUFFER_MARGIN);

    // batchUris holds uris for the current batch of uris being concatenated.
    List<Uri> batchUris = Lists.newArrayList();

    // uris holds the concatenated uris formed from batches which satisfy the
    // GET URL limit constraint.
    List<Uri> uris = Lists.newArrayList();

    Integer i = START_INDEX;
    for (Uri resource : resourceUris) {
      uriBuilder.addQueryParameter(i.toString(), resource.toString());
      if (uriBuilder.toString().length() > injectedMaxUrlLength) {
        uriBuilder.removeQueryParameter(i.toString());

        addVersionAndSplitParam(uriBuilder, splitParam, doSplit, batchUris, container,
            ctx.getType());
        uris.add(uriBuilder.toUri());

        uriBuilder = makeUriBuilder(ctx, concatHost, concatPath);
        batchUris = Lists.newArrayList();
        i = START_INDEX;
        uriBuilder.addQueryParameter(i.toString(), resource.toString());
      }
      i++;
      batchUris.add(resource);
    }

    if (batchUris != null && uriBuilder != ctx.makeQueryParams(null, null)) {
      addVersionAndSplitParam(uriBuilder, splitParam, doSplit, batchUris, container, ctx.getType());
      uris.add(uriBuilder.toUri());
    }

    if (doSplit) {
      snippets = createSnippets(uris);
    }
   return new ConcatData(uris, snippets);
  }

  private void addVersionAndSplitParam(UriBuilder uriBuilder, String splitParam, boolean doSplit,
                                       List<Uri> batchUris, String container, Type type) {
    // HashCode is used to differentiate splitParam paramter across ConcatUris
    // within single page/url. This value is appended to the splitParam value which
    // is recieved from config container.
    int hashCode = uriBuilder.hashCode();
    if (doSplit) {
      uriBuilder.addQueryParameter(Param.JSON.getKey(),
          (splitParam + String.valueOf(Math.abs(hashCode))));
    }

    if (versioner != null) {
      List<List<Uri>> batches = Lists.newArrayList();
      List<String> resourceTags = Lists.newArrayList();

      batches.add(batchUris);
      resourceTags.add(type.getTagName().toLowerCase());

      List<String> versions = versioner.version(batches, container, resourceTags);

      if (versions != null && versions.size() == 1) {
        String version = versions.get(0);
        if (version != null) {
          uriBuilder.addQueryParameter(Param.VERSION.getKey(), version);
        }
      }
    }
  }

  private Map<Uri, String> createSnippets(List<Uri> uris) {
    Map<Uri, String> snippets = Maps.newHashMap();
    for (Uri uri : uris) {
      Integer i = START_INDEX;
      String splitParam = uri.getQueryParameter(Param.JSON.getKey());
      String resourceUri;
      while ((resourceUri = uri.getQueryParameter(i.toString())) != null) {
        Uri resource = Uri.parse(resourceUri);
        snippets.put(resource, getJsSnippet(splitParam, resource));
        i++;
      }
    }
    return snippets;
  }

  private UriBuilder makeUriBuilder(ConcatUri ctx, String authority, String path) {
    UriBuilder uriBuilder = ctx.makeQueryParams(null, null);
    uriBuilder.setAuthority(authority);
    uriBuilder.setPath(path);
    uriBuilder.addQueryParameter(Param.TYPE.getKey(), ctx.getType().getType());
    return uriBuilder;
  }

  static String getJsSnippet(String splitParam, Uri resource) {
    return String.format(CONCAT_JS_EVAL_TPL, splitParam,
        StringEscapeUtils.escapeEcmaScript(resource.toString()));
  }

  private String getReqVal(String container, String key) {
    String val = config.getString(container, key);
    if (val == null) {
      throw new RuntimeException(
          "Missing required config '" + key + "' for container: " + container);
    }
    if (authority != null) {
      val = val.replace("%authority%", authority.getAuthority());
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

    Integer i = START_INDEX;
    String uriStr;
    while ((uriStr = uri.getQueryParameter(i.toString())) != null) {
      try {
        Uri concatUri = Uri.parse(uriStr);
        if (concatUri.getScheme() == null) {
          // For non schema url, use the request schema:
          concatUri = new UriBuilder(concatUri).setScheme(uri.getScheme()).toUri();
        }
        uris.add(concatUri);
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
