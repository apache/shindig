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

import java.util.Collection;
import java.util.Collections;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.JsCompileMode;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.uri.UriCommon.Param;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

/**
 * Interface defining methods used to generate Uris for the /js servlet.
 */
public interface JsUriManager {
  /**
   * @param ctx The js parameters.
   * @return The uri for the externed javascript that includes all listed extern libraries.
   */
  Uri makeExternJsUri(JsUri ctx);

  /**
   * Processes the inbound URL, for use by serving code in determining which JS to serve
   * and with what caching properties.
   *
   * @param uri Generated extern JS Uri
   * @return Processed status of the provided Uri.
   */
  JsUri processExternJsUri(Uri uri) throws GadgetException;

  public static class JsUri extends ProxyUriBase {
    private final static Collection<String> EMPTY_COLL = Collections.emptyList();
    private final Collection<String> libs;
    private final Collection<String> loadedLibs;
    private final String onload;
    private final RenderingContext context;
    private final Uri origUri;
    private JsCompileMode compileMode;
    private boolean jsload;
    private boolean nohint;
    private String repository;

    public JsUri(UriStatus status, Uri origUri, Collection<String> libs, Collection<String> have) {
      super(status, origUri);
      if (origUri != null) {
        String contextParam = origUri.getQueryParameter(Param.CONTAINER_MODE.getKey());
        this.context = RenderingContext.valueOfParam(contextParam);
        String compileParam = origUri.getQueryParameter(Param.COMPILE_MODE.getKey());
        this.compileMode = JsCompileMode.valueOfParam(compileParam);
        this.jsload = "1".equals(origUri.getQueryParameter(Param.JSLOAD.getKey()));
        this.onload = origUri.getQueryParameter(Param.ONLOAD.getKey());
        this.nohint = "1".equals(origUri.getQueryParameter(Param.NO_HINT.getKey()));
        this.repository = origUri.getQueryParameter(Param.REPOSITORY_ID.getKey());
      } else {
        this.context = RenderingContext.getDefault();
        this.compileMode = JsCompileMode.getDefault();
        this.jsload = false;
        this.onload = null;
        this.nohint = false;
        this.repository = null;
      }
      this.libs = nonNullLibs(libs);
      this.loadedLibs = nonNullLibs(have);
      this.origUri = origUri;
    }

    public JsUri(UriStatus status) {
      this(status, null, EMPTY_COLL, EMPTY_COLL);
    }

    public JsUri(UriStatus status, Collection<String> libs, RenderingContext context,
        String onload, boolean jsload, boolean nohint, String repository) {
      super(status, null);
      this.compileMode = JsCompileMode.getDefault();
      this.onload = onload;
      this.jsload = jsload;
      this.nohint = nohint;
      this.context = context;
      this.libs = nonNullLibs(libs);
      this.loadedLibs = EMPTY_COLL;
      this.origUri = null;
      this.repository = repository;
    }

    public JsUri(Gadget gadget, Collection<String> libs) {
      super(gadget);
      this.compileMode = JsCompileMode.getDefault();
      this.onload = null;
      this.jsload = false;
      this.nohint = false;
      this.context = RenderingContext.getDefault();
      this.libs = nonNullLibs(libs);
      this.loadedLibs = EMPTY_COLL;
      this.origUri = null;
      this.setCajoleContent(gadget.requiresCaja());
    }

    public JsUri(Integer refresh, boolean debug, boolean noCache, String container, String gadget,
        Collection<String> libs, Collection<String> loadedLibs, String onload, boolean jsload,
        boolean nohint, RenderingContext context, Uri origUri, String repository) {
      super(null, refresh, debug, noCache, container, gadget);
      this.compileMode = JsCompileMode.getDefault();
      this.onload = onload;
      this.jsload = jsload;
      this.nohint = nohint;
      this.context = context;
      this.libs = nonNullLibs(libs);
      this.loadedLibs = nonNullLibs(loadedLibs);
      this.origUri = origUri;
      this.repository = repository;
    }

    public JsUri(JsUri origJsUri) {
      this(origJsUri.getStatus(), origJsUri);
    }

    public JsUri(UriStatus status, JsUri origJsUri) {
      super(status, origJsUri.getRefresh(),
          origJsUri.isDebug(),
          origJsUri.isNoCache(),
          origJsUri.getContainer(),
          origJsUri.getGadget());
      this.setCajoleContent(origJsUri.cajoleContent());
      this.libs = origJsUri.getLibs();
      this.loadedLibs = origJsUri.getLoadedLibs();
      this.onload = origJsUri.getOnload();
      this.jsload = origJsUri.isJsload();
      this.nohint = origJsUri.isNohint();
      this.compileMode = origJsUri.getCompileMode();
      this.context = origJsUri.getContext();
      this.origUri = origJsUri.getOrigUri();
      this.repository = origJsUri.getRepository();
      this.extensionParams = origJsUri.getExtensionParams();
    }

    public Collection<String> getLibs() {
      return libs;
    }

    public Collection<String> getLoadedLibs() {
      return loadedLibs;
    }

    private Collection<String> nonNullLibs(Collection<String> in) {
      return in != null ? Collections.unmodifiableList(Lists.newArrayList(in)) : EMPTY_COLL;
    }

    public RenderingContext getContext() {
      return context;
    }

    public JsCompileMode getCompileMode() {
      return compileMode;
    }

    public void setCompileMode(JsCompileMode mode) {
      this.compileMode = mode;
    }

    public String getOnload() {
      return onload;
    }

    public boolean isJsload() {
      return jsload;
    }

    public void setJsload(boolean jsload) {
      this.jsload = jsload;
    }

    public boolean isNohint() {
      return nohint;
    }

    public void setNohint(boolean nohint) {
      this.nohint = nohint;
    }

    public Uri getOrigUri() {
      return origUri;
    }

    public void setRepository(String repository) {
      this.repository = repository;
    }

    public String getRepository() {
      return repository;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof JsUri)) {
        return false;
      }
      JsUri objUri = (JsUri) obj;
      return (super.equals(obj)
          && Objects.equal(this.libs, objUri.libs)
          && Objects.equal(this.loadedLibs, objUri.loadedLibs)
          && Objects.equal(this.onload, objUri.onload)
          && Objects.equal(this.jsload, objUri.jsload)
          && Objects.equal(this.nohint, objUri.nohint)
          && Objects.equal(this.compileMode, objUri.compileMode)
          && Objects.equal(this.context, objUri.context)
          && Objects.equal(this.origUri, objUri.origUri)
          && Objects.equal(this.repository, objUri.repository));
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(this.libs, this.loadedLibs, this.onload, this.jsload,
                              this.nohint, this.context, this.origUri,
                              this.compileMode, this.repository);
    }
  }

  public interface Versioner {
    /**
     * @param jsUri js request to create version for
     * @return Version string for the Uri.
     */
    String version(JsUri jsUri);

    /**
     * @param jsUri js request to validate
     * @param version Version string generated by the Versioner.
     * @return Validation status of the version.
     */
    UriStatus validate(JsUri jsUri, String version);
  }
}
