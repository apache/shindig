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
package org.apache.shindig.gadgets.servlet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.auth.SecurityTokenCodec;
import org.apache.shindig.auth.SecurityTokenException;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.process.ProcessingException;
import org.apache.shindig.gadgets.process.Processor;
import org.apache.shindig.gadgets.spec.Feature;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.LinkSpec;
import org.apache.shindig.gadgets.spec.ModulePrefs;
import org.apache.shindig.gadgets.spec.UserPref;
import org.apache.shindig.gadgets.spec.View;
import org.apache.shindig.gadgets.spec.UserPref.EnumValuePair;
import org.apache.shindig.gadgets.uri.IframeUriManager;
import org.apache.shindig.protocol.conversion.BeanDelegator;
import org.apache.shindig.protocol.conversion.BeanFilter;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Service that interfaces with the system to provide information about gadgets.
 *
 * @since 2.0.0
 */
public class GadgetsHandlerService {

  // Map shindig data class to API interfaces
  @VisibleForTesting
  static final Map<Class<?>, Class<?>> apiClasses =
      new ImmutableMap.Builder<Class<?>, Class<?>>()
          .put(View.class, GadgetsHandlerApi.View.class)
          .put(UserPref.class, GadgetsHandlerApi.UserPref.class)
          .put(EnumValuePair.class, GadgetsHandlerApi.EnumValuePair.class)
          .put(ModulePrefs.class, GadgetsHandlerApi.ModulePrefs.class)
          .put(Feature.class, GadgetsHandlerApi.Feature.class)
          .put(LinkSpec.class, GadgetsHandlerApi.LinkSpec.class)
          // Enums
          .put(View.ContentType.class, GadgetsHandlerApi.ViewContentType.class)
          .put(UserPref.DataType.class, GadgetsHandlerApi.UserPrefDataType.class)
          .build();

  // Provide mapping for internal enums to api enums
  @VisibleForTesting
  static final Map<Enum<?>, Enum<?>> enumConversionMap =
      new ImmutableMap.Builder<Enum<?>, Enum<?>>()
          // View.ContentType mapping
          .putAll(BeanDelegator.createDefaultEnumMap(View.ContentType.class,
              GadgetsHandlerApi.ViewContentType.class))
          // UserPref.DataType mapping
          .putAll(BeanDelegator.createDefaultEnumMap(UserPref.DataType.class,
              GadgetsHandlerApi.UserPrefDataType.class))
          .build();

  protected final Processor processor;
  protected final IframeUriManager iframeUriManager;
  protected final SecurityTokenCodec securityTokenCodec;

  protected final BeanDelegator beanDelegator;
  protected final BeanFilter beanFilter;



  @Inject
  public GadgetsHandlerService(Processor processor,
      IframeUriManager iframeUriManager, SecurityTokenCodec securityTokenCodec,
      BeanFilter beanFilter) {
    this.processor = processor;
    this.iframeUriManager = iframeUriManager;
    this.securityTokenCodec = securityTokenCodec;
    this.beanFilter = beanFilter;

    this.beanDelegator = new BeanDelegator(apiClasses, enumConversionMap);
  }

  /**
   * Get gadget metadata information and iframe url. Support filtering of fields
   * @param request request parameters
   * @return gadget metadata nd iframe url
   * @throws ProcessingException
   */
  public GadgetsHandlerApi.MetadataResponse getMetadata(GadgetsHandlerApi.MetadataRequest request)
      throws ProcessingException {
    if (request.getUrl() == null) {
      throw new ProcessingException("Missing url paramater", HttpResponse.SC_BAD_REQUEST);
    }
    if (request.getContainer() == null) {
      throw new ProcessingException("Missing container paramater", HttpResponse.SC_BAD_REQUEST);
    }
    if (request.getFields() == null) {
      throw new ProcessingException("Missing fields paramater", HttpResponse.SC_BAD_REQUEST);
    }
    Set<String> fields = beanFilter.processBeanFields(request.getFields());

    GadgetContext context = new MetadataGadgetContext(request);
    Gadget gadget = processor.process(context);
    String iframeUrl =
        (fields.contains("iframeurl") || fields.contains(BeanFilter.ALL_FIELDS)) ?
            iframeUriManager.makeRenderingUri(gadget).toString() : null;
    return createMetadataResponse(context.getUrl(), gadget.getSpec(), iframeUrl, fields);
  }

  /**
   * Create security token
   * @param request token paramaters (gadget, owner and viewer)
   * @return Security token
   * @throws SecurityTokenException
   */
  public GadgetsHandlerApi.TokenResponse getToken(GadgetsHandlerApi.TokenRequest request)
      throws SecurityTokenException, ProcessingException {
    if (request.getUrl() == null) {
      throw new ProcessingException("Missing url paramater", HttpResponse.SC_BAD_REQUEST);
    }
    if (request.getContainer() == null) {
      throw new ProcessingException("Missing container paramater", HttpResponse.SC_BAD_REQUEST);
    }
    if (request.getFields() == null) {
      throw new ProcessingException("Missing fields paramater", HttpResponse.SC_BAD_REQUEST);
    }
    Set<String> fields = beanFilter.processBeanFields(request.getFields());

    SecurityToken tokenData = convertToken(request.getToken(), request.getContainer(),
        request.getUrl().toString());
    String token = securityTokenCodec.encodeToken(tokenData);
    return createTokenResponse(request.getUrl(), token, fields);
  }

  /**
   * GadgetContext for metadata request. Used by the gadget processor
   */
  protected class MetadataGadgetContext extends GadgetContext {

    private final GadgetsHandlerApi.MetadataRequest request;
    private final SecurityToken token;

    public MetadataGadgetContext(GadgetsHandlerApi.MetadataRequest request) {
      this.request = request;
      this.token = convertToken(
          request.getToken(), request.getContainer(), request.getUrl().toString());
    }

    @Override
    public Uri getUrl() {
      return request.getUrl();
    }

    @Override
    public String getContainer() {
      return request.getContainer();
    }

    @Override
    public RenderingContext getRenderingContext() {
      return RenderingContext.METADATA;
    }

    @Override
    public int getModuleId() {
      return 1;
    }

    @Override
    public Locale getLocale() {
      return request.getLocale();
    }

    @Override
    public boolean getIgnoreCache() {
      return request.getIgnoreCache();
    }

    @Override
    public boolean getDebug() {
      return request.getDebug();
    }

    @Override
    public String getView() {
      return request.getView();
    }

    @Override
    public SecurityToken getToken() {
      return token;
    }
  }

  private SecurityToken convertToken(GadgetsHandlerApi.TokenData token,
      String container, String url) {
    if (token == null) {
      return null;
    }
    return beanDelegator.createDelegator(token, SecurityToken.class,
        ImmutableMap.<String, Object>of("container", container,
            "appid", url, "appurl", url));
  }

  public GadgetsHandlerApi.BaseResponse createBaseResponse(Uri url, String error) {
    return beanDelegator.createDelegator(error, GadgetsHandlerApi.BaseResponse.class,
        ImmutableMap.<String, Object>of("url", url, "error", error));
  }

  private GadgetsHandlerApi.MetadataResponse createMetadataResponse(
      Uri url, GadgetSpec spec, String iframeUrl, Set<String> fields) {
    return (GadgetsHandlerApi.MetadataResponse) beanFilter.createFilteredBean(
        beanDelegator.createDelegator(spec, GadgetsHandlerApi.MetadataResponse.class,
            ImmutableMap.<String, Object>of(
                "url", url, "error", BeanDelegator.NULL,
                "iframeurl", BeanDelegator.nullable(iframeUrl))),
        fields);
  }

  private GadgetsHandlerApi.TokenResponse createTokenResponse(
      Uri url, String token, Set<String> fields) {
    return (GadgetsHandlerApi.TokenResponse) beanFilter.createFilteredBean(
        beanDelegator.createDelegator("empty", GadgetsHandlerApi.TokenResponse.class,
            ImmutableMap.<String, Object>of("url", url, "error", BeanDelegator.NULL,
                "token", BeanDelegator.nullable(token))),
        fields);
  }
}
