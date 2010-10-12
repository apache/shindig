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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.protocol.conversion.BeanFilter.Unfiltered;
// Keep imports clean, so it is clear what is used by API

import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    public Uri getUrl();
    public String getContainer();
    public List<String> getFields();
  }

  public interface MetadataRequest extends BaseRequest {
    public Locale getLocale();
    public boolean getIgnoreCache();
    public boolean getDebug();
    public String getView();
    public TokenData getToken();
  }

  public interface TokenData {
    public String getOwnerId();
    public String getViewerId();
  }

  public interface TokenRequest extends BaseRequest {
    public TokenData getToken();
  }

  public interface BaseResponse {
    @Unfiltered
    public Uri getUrl();
    @Unfiltered
    public String getError();
  }

  public interface MetadataResponse extends BaseResponse {
    public String getIframeUrl();
    public String getChecksum();
    public ModulePrefs getModulePrefs();
    public Map<String, UserPref> getUserPrefs();
    public Map<String, View> getViews();
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
    public String getContent();
    public int getPreferredHeight();
    public int getPreferredWidth();
    public boolean needsUserPrefSubstitution();
    public Map<String, String> getAttributes();
  }

  public enum UserPrefDataType {
    STRING, HIDDEN, BOOL, ENUM, LIST, NUMBER;
  }

  public interface UserPref {
    public String getName();
    public String getDisplayName();
    public String getDefaultValue();
    public boolean getRequired();
    public UserPrefDataType getDataType();
    public Map<String, String> getEnumValues();
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
    // TODO: Provide better interface for locale if needed
    // public Map<Locale, LocaleSpec> getLocales();
  }

  public interface Feature {
    public String getName();
    public boolean getRequired();
    // TODO: Handle multi map if params are needed
    // public Multimap<String, String> getParams();
  }

  public interface LinkSpec {
    public String getRel();
    public Uri getHref();
  }

  public interface TokenResponse extends BaseResponse {
    public String getToken();
  }
}
