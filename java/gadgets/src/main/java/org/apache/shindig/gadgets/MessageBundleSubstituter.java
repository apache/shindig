/*
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

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adds message bundle hangman variable substitution and programmatic JavaScript
 * access to message bundle strings.
 */
public class MessageBundleSubstituter implements GadgetFeatureFactory {
  /**
   * {@inheritDoc}
   */
  public GadgetFeature create() {
    return new MessageBundleSubstituterFeature();
  }
}

class MessageBundleSubstituterFeature extends GadgetFeature {
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
  @Override
  public void prepare(GadgetView gadget, GadgetContext context,
                      Map<String, String> params) throws GadgetException {
    super.prepare(gadget, context, params);
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

    GadgetServerConfigReader config = context.getServerConfig();

    if (localeData != null) {
      URI uri = localeData.getURI();
      if (uri != null) {
        // We definitely need a bundle, now we need to fetch it.
        bundle = config.getMessageBundleCache().get(uri.toString());
        if (bundle == null) {
          RemoteContent data = null;
          data = config.getContentFetcher().fetch(new RemoteContentRequest(uri),
                                                  context.getOptions());
          if (data.getHttpStatusCode() != RemoteContent.SC_OK) {
            throw new GadgetException(
                GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT,
                String.format("Malformed message bundle URL: %s",
                              uri.toString()));
          }
          bundle = parser.parse(data.getResponseAsString());
          config.getMessageBundleCache().put(uri.toString(), bundle);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void process(Gadget gadget, GadgetContext context,
                      Map<String, String> params) throws GadgetException {
    super.process(gadget, context, params);
    StringBuilder js = new StringBuilder();
    int moduleId = gadget.getId().getModuleId();
    Locale locale = context.getLocale();

    String setLangFmt = "gadgets.prefs_.setLanguage(%d, \"%s\");";
    String setCountryFmt = "gadgets.prefs_.setCountry(%d, \"%s\");";

    js.append(String.format(setLangFmt, moduleId, locale.getLanguage()));
    js.append(String.format(setCountryFmt, moduleId, locale.getCountry()));

    if (null != bundle) {
      gadget.setCurrentMessageBundle(bundle);
      gadget.getSubstitutions().addSubstitutions(Substitutions.Type.MESSAGE,
                                                 bundle.getMessages());

      RenderingContext rc = context.getRenderingContext();
      if (rc == RenderingContext.GADGET) {
        Map<String, String> msgs = bundle.getMessages();
        JSONObject json = new JSONObject();
        try {
          for (Map.Entry<String, String> entry : msgs.entrySet()) {
            json.put(entry.getKey(), entry.getValue());
          }
        } catch (JSONException e) {
          throw new RuntimeException(e);
        }

        String setMsgFmt = "gadgets.prefs_.setMsg(%d, %s);";
        js.append(String.format(setMsgFmt, moduleId, json.toString()));
      }
    }
    gadget.addJsLibrary(JsLibrary.create(JsLibrary.Type.INLINE, js.toString()));
  }
}
