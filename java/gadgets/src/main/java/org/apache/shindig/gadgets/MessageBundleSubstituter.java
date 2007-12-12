/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.shindig.gadgets;

import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adds message bundle hangman variable substitution and programmatic JavaScript
 * access to message bundle strings.
 */
public class MessageBundleSubstituter implements GadgetFeature {
  private static final MessageBundleParser parser
      = new MessageBundleParser();

  private MessageBundle bundle;

  /**
   * Fetches a message bundle from the spec for the given locale.
   *
   * @param spec Specification of gadget with messages to potentially retrieve
   * @param locale Locale corresponding to the request
   * @return The message bundle, or null if not found
   */
  private GadgetSpec.MessageBundle getBundle(GadgetSpec spec,
                                             Locale locale) {
    List<GadgetSpec.MessageBundle> bundles = spec.getMessageBundles();
    for (GadgetSpec.MessageBundle bundle : bundles) {
      if (bundle.getLocale().equals(locale)) {
        return bundle;
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public void prepare(GadgetView gadget, GadgetContext context,
                      Map<String, String> params) throws GadgetException {
    Locale locale = context.getLocale();
    GadgetSpec.MessageBundle bundleData = getBundle(gadget, locale);
    if (null == bundleData) {
      // en-all
      bundleData = getBundle(gadget, new Locale(locale.getLanguage(), "all"));
    }
    if (null == bundleData) {
      // all-all
      bundleData = getBundle(gadget, new Locale("all", "all"));
    }

    if (null != bundleData) {
      URL url = bundleData.getURL();
      if (null != url) {
        // We definitely need a bundle, now we need to fetch it.
        bundle = context.getMessageBundleCache().get(url.toString());
        if (null == bundle) {
          byte[] data = context.getHttpFetcher().fetch(url).getByteArray();
          if (data.length > 0) {
            bundle = parser.parse(data);
            context.getMessageBundleCache().put(url.toString(), bundle);
          }
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void process(Gadget gadget, GadgetContext context,
                      Map<String, String> params) {
    if (null != bundle) {
      gadget.getSubstitutions().addSubstitutions(Substitutions.Type.MESSAGE,
                                                 bundle.getMessages());
    }
  }

}
