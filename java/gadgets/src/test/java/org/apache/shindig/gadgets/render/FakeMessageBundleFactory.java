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

import org.apache.shindig.gadgets.MessageBundleFactory;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.LocaleSpec;
import org.apache.shindig.gadgets.spec.MessageBundle;

import java.util.Locale;

/**
 * Simple message bundle factory -- only honors inline bundles.
 */
public class FakeMessageBundleFactory implements MessageBundleFactory {
  public MessageBundle getBundle(GadgetSpec spec, Locale locale, boolean ignoreCache, String container, String view) {
    LocaleSpec localeSpec = spec.getModulePrefs().getLocale(locale, view);
    if (localeSpec == null) {
      return MessageBundle.EMPTY;
    }
    return spec.getModulePrefs().getLocale(locale, view).getMessageBundle();
  }
}
