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

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Provides static hangman substitutions for bidirectional language support.
 * Useful for generating internationalized layouts using CSS.
 */
public class BidiSubstituter implements GadgetFeature {

  /**
   * Fetches a message bundle spec from the {@code GadgetSpec} for the
   * provided locale.
   * @param spec Gadget spec from which to retrieve message bundle spec
   * @param locale Locale of message bundle to retrieve
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

  /** {@inheritDoc} */
  public void prepare(GadgetView spec,
                      GadgetContext context,
                      Map<String, String> params) {
    // Nothing here.
  }

  /**
   * Populates bidi substitutions.
   * @param gadget Gadget object to process
   * @param context Context in which Gadget is being processed
   */
  public void process(Gadget gadget,
                      GadgetContext context,
                      Map<String, String> params) {
    Substitutions subst = gadget.getSubstitutions();
    Locale locale = context.getLocale();
    // Find an appropriate bundle for the ltr flag.
    GadgetSpec.MessageBundle bundle = getBundle(gadget, locale);
    if (null == bundle) {
      bundle = getBundle(gadget, new Locale(locale.getLanguage(), "all"));
    }
    if (null == bundle) {
      bundle = getBundle(gadget, new Locale("all", "all"));
    }
    boolean rtl = false;
    if (bundle != null) {
      rtl = bundle.isRightToLeft();
    }
    subst.addSubstitution(Substitutions.Type.BIDI,
                          "START_EDGE",
                          rtl ? "right" : "left");
    subst.addSubstitution(Substitutions.Type.BIDI,
                          "END_EDGE",
                          rtl ? "left" : "right");
    subst.addSubstitution(Substitutions.Type.BIDI,
                          "DIR",
                          rtl ? "rtl" : "ltr");
    subst.addSubstitution(Substitutions.Type.BIDI,
                          "REVERSE_DIR",
                          rtl ? "ltr" : "rtl");
  }
}
