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
package org.apache.shindig.gadgets.spec;
import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.XmlUtil;
import org.w3c.dom.Element;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Represents a Locale tag.
 * Generally compatible with java.util.Locale, but with some extra
 * localization data from the spec.
 * Named "LocaleSpec" so as to not conflict with java.util.Locale
 *
 * No localization.
 * No user pref substitution.
 */
public class LocaleSpec {
  private final Locale locale;
  private final String languageDirection;
  private final Uri messages;
  private final MessageBundle messageBundle;

  /**
   * @param specUrl The url that the spec is loaded from. messages is assumed
   *     to be relative to this path.
   * @throws SpecParserException If language_direction is not valid
   */
  public LocaleSpec(Element element, Uri specUrl) throws SpecParserException {
    String language = XmlUtil.getAttribute(element, "lang", "all").toLowerCase();
    String country = XmlUtil.getAttribute(element, "country", "ALL").toUpperCase();
    this.locale = new Locale(language, country);

    languageDirection = XmlUtil.getAttribute(element, "language_direction", "ltr");
    if (!("ltr".equals(languageDirection) || "rtl".equals(languageDirection))) {
      throw new SpecParserException("Locale/@language_direction must be ltr or rtl");
    }
    // Record all the associated views
    String viewNames = XmlUtil.getAttribute(element, "views", "").trim();

    this.views = ImmutableSet.copyOf(Splitter.on(',').omitEmptyStrings().trimResults().split(viewNames));

    String messagesString = XmlUtil.getAttribute(element, "messages");
    if (messagesString == null) {
      this.messages = Uri.parse("");
    } else {
      try {
        this.messages = specUrl.resolve(Uri.parse(messagesString));
      } catch (IllegalArgumentException e) {
        throw new SpecParserException("Locale@messages url is invalid.");
      }
    }
    messageBundle = new MessageBundle(element);
  }

  public Locale getLocale() {
    return locale;
  }

  /**
   * Locale@lang
   */
  public String getLanguage() {
    return locale.getLanguage();
  }

  /**
   * Locale@country
   */
  public String getCountry() {
    return locale.getCountry();
  }

  /**
   * Locale@language_direction
   */
  public String getLanguageDirection() {
    return languageDirection;
  }

  /**
   * Locale@messages
   */
  public Uri getMessages() {
    return messages;
  }

  /**
   * Locale/msg
   */
  public MessageBundle getMessageBundle() {
    return messageBundle;
  }

  /**
   * Locale@views
   *
   * Views associated with this Locale
   */
  private final Set<String> views;
  public Set<String> getViews() {
    return views;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("<Locale").append(" lang='").append(getLanguage()).append('\'')
        .append(" country='").append(getCountry()).append('\'')
        .append(" language_direction='").append(languageDirection).append('\'');
    if (!views.isEmpty()) {
      buf.append(" views=\'").append(StringUtils.join(views, ',')).append('\'');
    }
    buf.append(" messages='").append(messages).append("'>\n");
    for (Map.Entry<String, String> entry : messageBundle.getMessages().entrySet()) {
      buf.append("<msg name='").append(entry.getKey()).append("'>").append(entry.getValue()).append("</msg>\n");
    }
    buf.append("</Locale>");
    return buf.toString();
  }
}
