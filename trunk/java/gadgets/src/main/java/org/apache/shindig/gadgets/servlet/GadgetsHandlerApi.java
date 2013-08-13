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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.protocol.conversion.BeanFilter.Unfiltered;

import com.google.common.collect.Multimap;

/**
 * Gadget Handler Interface data.
 * Classes in here specify the API data.
 * Please do not reference run time classes, instead create new interface (keep imports clean!).
 * Please avoid changes if possible, you might break external system that depend on the API.
 *
 * @since 2.0.0
 */
public class GadgetsHandlerApi {

  public interface BaseRequest {
    public String getContainer();
    public List<String> getFields();
    public Uri getUrl();
  }

  public interface Error {
    public int getCode();
    public String getMessage();
  }

  public interface BaseResponse {
    /** Url of the request, optional (for example for bad url error) */
    public Uri getUrl();
    /** Error response (optional) */
    @Unfiltered
    public Error getError();
    /** The response expiration time (miliseconds since epoch), -1 for no caching */
    @Unfiltered
    public Long getExpireTimeMs();
    /** The response time (miliseconds since epoch) - usefull for misconfigured client time */
    @Unfiltered
    public Long getResponseTimeMs();
  }

  public interface MetadataRequest extends BaseRequest {
    public Locale getLocale();
    public boolean getIgnoreCache();
    public boolean getDebug();
    public String getView();
    public AuthContext getAuthContext();
    public RenderingType getRenderingType();
  }

  public enum RenderingType {
    DEFAULT,
    SANITIZED,
    INLINE_CAJOLED,
    IFRAME_CAJOLED
  }

  public interface AuthContext {
    public String getOwnerId();
    public String getViewerId();
    public String getDomain();
    public long getModuleId();
    public String getAuthenticationMode();
    public Long getExpiresAt();
    public String getTrustedJson();
  }

  public interface MetadataResponse extends BaseResponse {
    public Map<String, String> getIframeUrls();
    public String getChecksum();
    public ModulePrefs getModulePrefs();
    public Map<String, UserPref> getUserPrefs();
    public Map<String, View> getViews();
    public Boolean getNeedsTokenRefresh();
    public Set<String> getRpcServiceIds();
    public Integer getTokenTTL();
  }

  public enum ViewContentType {
    HTML("html"), URL("url"), HTML_SANITIZED("x-html-sanitized");

    private final String name;
    private ViewContentType(String name) {
      this.name = name;
    }
    @Override
    public String toString() {
      return name;
    }
  }

  public interface View {
    public String getName();
    public ViewContentType getType();
    public Uri getHref();
    public boolean getQuirks();
    public int getPreferredHeight(); // Default to 0
    public int getPreferredWidth();  // Default to 0
  }

  public enum UserPrefDataType {
    STRING, HIDDEN, BOOL, ENUM, LIST, NUMBER
  }

  public interface UserPref {
    public String getName();
    public String getDisplayName();
    public String getDefaultValue();
    public boolean getRequired();
    public UserPrefDataType getDataType();
    public List<EnumValuePair> getOrderedEnumValues();
  }

  public interface EnumValuePair {
    public String getValue();
    public String getDisplayValue();
  }

  public interface ModulePrefs {
    public String getTitle();
    public Uri getTitleUrl();
    public String getDescription();
    public String getAuthor();
    public String getAuthorEmail();
    public Uri getScreenshot();
    public Uri getThumbnail();
    public String getDirectoryTitle();
    public String getAuthorAffiliation();
    public String getAuthorLocation();
    public Uri getAuthorPhoto();
    public String getAuthorAboutme();
    public String getAuthorQuote();
    public Uri getAuthorLink();
    public boolean getScaling();
    public boolean getScrolling();
    public int getWidth();
    public int getHeight();
    public List<String> getCategories();
    public Map<String, Feature> getFeatures();
    public Map<String, LinkSpec> getLinks();
    public OAuthSpec getOAuthSpec();
    public OAuth2Spec getOAuth2Spec();
    // TODO: Provide better interface for locale if needed
    // public Map<Locale, LocaleSpec> getLocales();
  }

  public interface Feature {
    public String getName();
    public boolean getRequired();
    public Multimap<String, String> getParams();
  }

  public interface LinkSpec {
    public String getRel();
    public Uri getHref();
    public String getMethod();
  }

  public interface OAuthSpec {
    public Map<String, OAuthService> getServices();
  }

  public interface OAuth2Spec {
	    public Map<String, OAuth2Service> getServices();
  }

  public interface OAuthService {
    public EndPoint getRequestUrl();
    public EndPoint getAccessUrl();
    public Uri getAuthorizationUrl();
    public String getName();
  }

  public interface EndPoint {
    public Uri getUrl();
    public Method getMethod();
    public Location getLocation();
  }

  public interface OAuth2Service {
	  public EndPoint getAuthorizationUrl();
	  public EndPoint getTokenUrl();
	  public String getScope();
	  public String getName();
  }
  public interface TokenRequest extends BaseRequest {
    public AuthContext getAuthContext();
    public Long getModuleId();
  }

  public interface TokenResponse extends BaseResponse {
    public String getToken();
    public Integer getTokenTTL();
    public Long getModuleId();
  }

  // TODO(jasvir): Support getRefresh and noCache
  public interface CajaRequest extends BaseRequest {
    public String getMimeType();
    public boolean getDebug();
  }

  public interface CajaResponse extends BaseResponse {
    public String getHtml();
    public String getJs();
    public List<Message> getMessages();
  }

  public interface Message {
    public MessageLevel getLevel();
    public String getName();
    public String getMessage();
  }

  public enum MessageLevel {
    UNKNOWN,
    // Fine grained info about internal progress
    LOG,
    // Broad info about internal progress
    SUMMARY,
    // Information inferred about source files
    INFERENCE,
    // Indicative of a possible problem in an input source file
    LINT,
    // Indicative of a probable problem in an input source file
    WARNING,
    // Indicative of a problem which prevents production of usable output
    // but progress should continue in case further messages shed more info
    ERROR,
    // Indicative of a problem that prevents usable further processing
    FATAL_ERROR
  }

  public enum Method {
	    GET,
	    POST
  }

  public enum Location {
    HEADER("auth-header"),
    URL("uri-query"),
    BODY("post-body");

    private String locationString;
    private Location(String locationString) {
      this.locationString = locationString;
    }

    @Override
    public String toString() {
      return locationString;
    }
  }

  public interface ProxyRequest extends BaseRequest {
    // The BaseRequest.url store the resource to proxy
    public String getGadget();
    public Integer getRefresh();
    public boolean getDebug();
    public boolean getIgnoreCache();
    public String getFallbackUrl();
    public String getRewriteMimeType();
    public boolean getSanitize();
    public ImageParams getImageParams();
  }

  public interface ImageParams {
    public Integer getHeight();
    public Integer getWidth();
    public Integer getQuality();
    public Boolean getDoNotExpand();
  }

  public interface ProxyResponse extends BaseResponse {
    public Uri getProxyUrl();
    public HttpResponse getProxyContent();
  }

  public interface HttpResponse {
    public int getCode();
    public String getEncoding();
    public String getContentBase64();
    public List<NameValuePair> getHeaders();
  }

  public interface NameValuePair {
    public String getName();
    public String getValue();
  }

  public interface JsRequest extends BaseRequest {
    public String getGadget();
    public Integer getRefresh();
    public boolean getDebug();
    public boolean getIgnoreCache();
    public List<String> getFeatures();
    public List<String> getLoadedFeatures();
    public String getOnload();
    public RenderingContext getContext();
    public String getRepository();
  }

  public enum RenderingContext {
    GADGET, CONTAINER, CONFIGURED_GADGET
  }

  public interface JsResponse extends BaseResponse {
    public Uri getJsUrl();
    public String getJsContent();
  }
}
