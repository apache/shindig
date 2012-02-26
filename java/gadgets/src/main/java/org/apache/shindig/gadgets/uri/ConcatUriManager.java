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

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Generates concat servlet specific uris.
 *
 * @since 2.0.0
 */
public interface ConcatUriManager {
  public enum Type {
    JS("text/javascript", "src", "js", "script"), // JavaScript
    CSS("text/css", "href", "css", "link");     // CSS/styling

    private final String mimeType;
    private final String srcAttrib;
    private final String type;
    private final String tagName;

    private Type(String mimeType, String srcAttrib, String type, String tagName) {
      this.mimeType = mimeType;
      this.srcAttrib = srcAttrib;
      this.type = type;
      this.tagName = tagName;
    }

    public String getMimeType() {
      return mimeType;
    }

    public String getSrcAttrib() {
      return srcAttrib;
    }

    public String getType() {
      return type;
    }

    public String getTagName() {
      return tagName;
    }

    public static Type fromType(String type) {
      for (Type val : Type.values()) {
        if (val.getType().equalsIgnoreCase(type)) {
          return val;
        }
      }
      return null;
    }

    public static Type fromMime(String mime) {
      for (Type val : Type.values()) {
        if (val.getMimeType().equals(mime)) {
          return val;
        }
      }
      return null;
    }
  }

  /**
   * Generate Uris that concatenate all given resources together.
   * @param batches List of batches to concatenate
   * @param isAdjacent True if Uris are adjacent in the source DOM
   * @return List of proxied-concatenated Uris (or null if unable to generate)
   *     in index-correlated order, one per input.
   */
  List<ConcatData> make(List<ConcatUri> batches, boolean isAdjacent);

  /**
   * Represents a single concatenated Uri. This must include a Uri for
   * loading the given resource(s), and may optionally include a
   * Map from Uri to String of Snippets, each of which provides a
   * piece of JavaScript, assumed to be executed after the resource Uri
   * is loaded, which causes the given Uri's content to be loaded. In
   * practice, this supports split-JS, where multiple chunks of
   * (non-contiguous) JS are included as Strings (once) and evaluated
   * in their correct original position.
   */
  public static class ConcatData {
    private final List<Uri> uris;
    private final Map<Uri, String> snippets;

    public ConcatData(List<Uri> uris, Map<Uri, String> snippets) {
      this.uris = Collections.unmodifiableList(uris);
      this.snippets = snippets;
    }

    public List<Uri> getUris() {
      return uris;
    }

    public String getSnippet(Uri orig) {
      return snippets == null || !snippets.containsKey(orig) ?
          null : snippets.get(orig);
    }
  }

  public static class ConcatUri extends ProxyUriBase {
    private final List<Uri> batch;
    private final Type type;
    private final String splitParam;

    public ConcatUri(Gadget gadget, List<Uri> batch, Type type) {
      super(gadget);
      this.batch = batch;
      this.type = type;
      this.splitParam = null;
    }

    public ConcatUri(
        UriStatus status, List<Uri> uris, String splitParam, Type type, Uri origUri) {
      super(status, origUri);
      this.batch = uris;
      this.splitParam = splitParam;
      this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof ConcatUri)) {
        return false;
      }
      ConcatUri objUri = (ConcatUri) obj;
      return (super.equals(obj)
          && Objects.equal(this.batch, objUri.batch)
          && Objects.equal(this.splitParam, objUri.splitParam)
          && Objects.equal(this.type, objUri.type));
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(super.hashCode(), batch, splitParam, type);
    }

    public List<Uri> getBatch() {
      return batch;
    }

    public Type getType() {
      return type;
    }

    public String getSplitParam() {
      return splitParam;
    }

    public static List<ConcatUri> fromList(Gadget gadget, List<List<Uri>> batches, Type type) {
      List<ConcatUri> ctx = Lists.newArrayListWithCapacity(batches.size());
      for (List<Uri> batch : batches) {
        ctx.add(new ConcatUri(gadget, batch, type));
      }
      return ctx;
    }
  }

  /**
   * Parses a given Uri indicating whether it's a concat Uri and if so,
   * whether it's valid.
   * @param uri Uri to validate for concat-ness
   * @return Uri validation status
   */
  ConcatUri process(Uri uri);

  public interface Versioner {
    /**
     * Generates a version for each of the provided resources.
     * @param resourceUris List of resource "batches" to version.
     * @param container Container making the request
     * @param resourceTags Index-correlated list of html tags, one per list of resouceUris as only
     * similar tags can be concat. Each entry in resourceTags corresponds to html tag of resources
     * uris. Any older implementations can just ignore.
     * @return Index-correlated list of version strings, one per input.
     */
    List<String> version(List<List<Uri>> resourceUris, String container,
                         List<String> resourceTags);

    /**
     * Validate the version of the resource list.
     * @param resourceUris Uris of a proxied resource
     * @param container Container requesting the resource
     * @param value Version value to validate.
     * @return Status of the version.
     */
    UriStatus validate(List<Uri> resourceUris, String container, String value);
  }
}
