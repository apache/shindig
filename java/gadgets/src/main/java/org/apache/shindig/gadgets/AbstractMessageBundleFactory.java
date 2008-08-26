/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets;

import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.LocaleSpec;
import org.apache.shindig.gadgets.spec.MessageBundle;

import java.util.Locale;
import java.net.URI;

/**
 * Core implementation of MessageBundleFactory that ensures proper MessageBundle creation and
 * delegates caching and network retrieval to concreate implementations.
 */
public abstract class AbstractMessageBundleFactory implements MessageBundleFactory {
  private static final Locale ALL_ALL = new Locale("all", "ALL");

  public MessageBundle getBundle(GadgetSpec spec, Locale locale, boolean ignoreCache)
      throws GadgetException {
    MessageBundle parent = getParentBundle(spec, locale, ignoreCache);
    MessageBundle child = null;
    LocaleSpec localeSpec = spec.getModulePrefs().getLocale(locale);
    if (localeSpec == null) {
      return parent == null ? MessageBundle.EMPTY : parent;
    }
    URI messages = localeSpec.getMessages();
    if (messages == null || messages.toString().length() == 0) {
      child = localeSpec.getMessageBundle();
    } else {
      child = fetchBundle(localeSpec, ignoreCache);
    }

    return new MessageBundle(parent, child);
  }

  private MessageBundle getParentBundle(GadgetSpec spec, Locale locale, boolean ignoreCache)
      throws GadgetException {
    if (locale.getLanguage().equalsIgnoreCase("all")) {
      // Top most locale already.
      return null;
    }

    if (locale.getCountry().equalsIgnoreCase("ALL")) {
      return getBundle(spec, ALL_ALL, ignoreCache);
    }

    return getBundle(spec, new Locale(locale.getLanguage(), "ALL"), ignoreCache);
  }

  /**
   * Retrieve the MessageBundle for the given LocaleSpec from the network or cache.
   */
  protected abstract MessageBundle fetchBundle(LocaleSpec locale, boolean ignoreCache)
      throws GadgetException;
}
