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
package org.apache.shindig.gadgets;

import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.MessageBundle;

import com.google.inject.ImplementedBy;

import java.util.Locale;

/**
 * Factory of message bundles
 */
@ImplementedBy(DefaultMessageBundleFactory.class)
public interface MessageBundleFactory {
  /**
   * Retrieves a messagMessageBundle for the provided GadgetSpec and Locale. Implementations must be
   * sure to perform proper merging of message bundles of lower specifity with exact matches
   * (exact &gt; lang only &gt; country only &gt; all / all)
   *
   * @param spec The gadget to inspect for Locales.
   * @param locale The language and country to get a message bundle for.
   * @param ignoreCache  True to bypass any caching of message bundles for debugging purposes.
   * @param container The container that is requesting this message bundle
   * @param view The view for which to return the Locale appropriate message bundle.  To retrieve only globally scoped bundles pass 'null'.
   * @return The newly created MesageBundle.
   * @throws GadgetException if retrieval fails for any reason.
   */
  MessageBundle getBundle(GadgetSpec spec, Locale locale, boolean ignoreCache, String container, String view)
      throws GadgetException;
}
