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

import java.net.MalformedURLException;
import java.net.URI;
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
  private GadgetSpec.LocaleSpec getLocaleSpec(GadgetSpec spec,
                                              Locale locale) {
    List<GadgetSpec.LocaleSpec> localeSpecs = spec.getLocaleSpecs();
    for (GadgetSpec.LocaleSpec locSpec : localeSpecs) {
      if (locSpec.getLocale().equals(locale)) {
        return locSpec;
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
    GadgetSpec.LocaleSpec localeData = getLocaleSpec(gadget, locale);
    if (localeData == null) {
      // en-all
      localeData = getLocaleSpec(gadget,
                                 new Locale(locale.getLanguage(), "all"));
    }
    if (localeData == null) {
      // all-all
      localeData = getLocaleSpec(gadget, new Locale("all", "all"));
    }

    if (localeData != null) {
      URI uri = localeData.getURI();
      if (uri != null) {
        // We definitely need a bundle, now we need to fetch it.
        bundle = context.getMessageBundleCache().get(uri.toString());
        if (bundle == null) {
          byte[] data = null;
          try {
            data = context.getHttpFetcher().fetch(uri.toURL()).getByteArray();
          } catch (MalformedURLException e) {
            throw new GadgetException(
                GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT,
                String.format("Malformed message bundle URL: %s",
                              uri.toString()));
          }
          if (data.length > 0) {
            bundle = parser.parse(data);
            context.getMessageBundleCache().put(uri.toString(), bundle);
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
      gadget.setCurrentMessageBundle(bundle);
      gadget.getSubstitutions().addSubstitutions(Substitutions.Type.MESSAGE,
                                                 bundle.getMessages());
    }
  }
}
