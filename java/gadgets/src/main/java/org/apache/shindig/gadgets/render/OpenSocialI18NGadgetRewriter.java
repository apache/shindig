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
package org.apache.shindig.gadgets.render;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.shindig.common.util.ResourceLoader;
import org.apache.shindig.common.xml.DomUtil;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.rewrite.GadgetRewriter;
import org.apache.shindig.gadgets.rewrite.MutableContent;
import org.apache.shindig.gadgets.rewrite.RewritingException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Produce data constants that are needed by the opensocial-i18n
 * feature based on user locale.
 */
public class OpenSocialI18NGadgetRewriter implements GadgetRewriter {
  private static final String I18N_FEATURE_NAME = "opensocial-i18n";
  private static final String DATA_PATH = "features/i18n/data/";
  private Map<Locale, String> i18nConstantsCache = new ConcurrentHashMap<Locale, String>();

  public void rewrite(Gadget gadget, MutableContent mutableContent) throws RewritingException {
    // Don't touch sanitized gadgets.
    if (gadget.sanitizeOutput()) {
      return;
    }
    // Quickly return if opensocial-i18n feature is not needed.
    if (!gadget.getAllFeatures().contains(I18N_FEATURE_NAME)) {
      return;
    }

    try {
      Document document = mutableContent.getDocument();
      Element head = (Element)DomUtil.getFirstNamedChildNode(document.getDocumentElement(), "head");
      injectI18NConstants(gadget, head);
      mutableContent.documentChanged();
    } catch (GadgetException e) {
      throw new RewritingException(e, e.getHttpStatusCode());
    }
  }

  private void injectI18NConstants(Gadget gadget, Node headTag) throws GadgetException {
    StringBuilder inlineJs = new StringBuilder();
    Locale locale = gadget.getContext().getLocale();
    if (i18nConstantsCache.containsKey(locale)) {
      inlineJs.append(i18nConstantsCache.get(locale));
    } else {
      // load gadgets.i18n.DateTimeConstants and gadgets.i18n.NumberFormatConstants
      String localeName = getLocaleNameForLoadingI18NConstants(locale);
      String dateTimeConstantsResource = "DateTimeConstants__" + localeName + ".js";
      String numberConstantsResource = "NumberFormatConstants__" + localeName + ".js";
      try {
        inlineJs.append(attemptToLoadResource(dateTimeConstantsResource))
            .append('\n').append(attemptToLoadResource(numberConstantsResource));
        i18nConstantsCache.put(locale, inlineJs.toString());
      } catch (IOException e) {
        throw new GadgetException(GadgetException.Code.INVALID_CONFIG,
            "Unexpected inability to load i18n data for locale: " + localeName,
            HttpResponse.SC_INTERNAL_SERVER_ERROR);
      }
    }
    Element inlineTag = headTag.getOwnerDocument().createElement("script");
    headTag.appendChild(inlineTag);
    inlineTag.appendChild(headTag.getOwnerDocument().createTextNode(inlineJs.toString()));
  }

  String getLocaleNameForLoadingI18NConstants(Locale locale) {
    String localeName = "en";
    String language = locale.getLanguage();
    String country = locale.getCountry();
    if (!language.equalsIgnoreCase("ALL")) {
      try {
        attemptToLoadDateConstants(language);
        localeName = language;
      } catch (IOException e) {
        // ignore
      }
    }

    if (!country.equalsIgnoreCase("ALL")) {
      try {
        attemptToLoadDateConstants(localeName + '_' + country);
        localeName += '_' + country;
      } catch (IOException e) {
        // ignore
      }
    }
    return localeName;
  }

  protected String attemptToLoadDateConstants(String localeName) throws IOException {
    return attemptToLoadResource("DateTimeConstants__" + localeName + ".js");
  }

  private String attemptToLoadResource(String i18nRes) throws IOException {
    return attemptToLoadResourceFullyQualified(DATA_PATH + i18nRes);
  }

  protected String attemptToLoadResourceFullyQualified(String resource) throws IOException {
    return ResourceLoader.getContent(resource);
  }
}
