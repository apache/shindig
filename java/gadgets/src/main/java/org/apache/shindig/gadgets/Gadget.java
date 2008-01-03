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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Intermediary representation of all state associated with processing
 * of a single gadget request.
 *
 * This class is constructed by an immutable base {@code GadgetSpec},
 * and is modified in parallel by a number of {@code GadgetFeature}
 * processors, in an order defined by their dependencies, in
 * {@code GadgetServer}.
 *
 * Upon completion of processing, a {@code Gadget} is serialized as appropriate
 * to whatever output format is appropriate (eg. as gadget content in an
 * IFRAME), potentially with post-processing such as HTML whitespace
 * compression or HTML+JS (Caja) rewriting applied.
 *
 * "Hangman" variable substitutions (eg. __MSG_foo__) are performed as needed
 * and transparently for fields that support this functionality.
 */
public class Gadget implements GadgetView {
  private final ID id;
  private final GadgetSpec baseSpec;
  private final Substitutions substitutions;
  private final Map<String, String> userPrefValues;
  private final List<JsLibrary> jsLibraries;

  public static class GadgetId implements GadgetView.ID {
    private final URI uri;
    private final int moduleId;

    public GadgetId(URI uri, int moduleId) {
      this.uri = uri;
      this.moduleId = moduleId;
    }

    @Override
    public boolean equals(Object comp) {
      if (comp instanceof GadgetView.ID) {
        GadgetView.ID id = (GadgetView.ID)comp;
        return id.getURI() == uri &&
               id.getModuleId() == moduleId;
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      int result = 17;
      result = (37 * result) + uri.hashCode();
      result = (37 * result) + moduleId;
      return result;
    }

    public URI getURI() {
      return uri;
    }

    public int getModuleId() {
      return moduleId;
    }

    public String getKey() {
      return uri.toString();
    }
  }

  /**
   * Create a new {@code Gadget} devoid of processing modifications.
   * @param id Identifier used to retrieve {@code baseSpec}
   * @param baseSpec Base (immutable) {@code GadgetSpec} on which this is based
   */
  public Gadget(ID id, GadgetSpec baseSpec) {
    this.id = id;
    this.baseSpec = baseSpec;
    substitutions = new Substitutions();
    userPrefValues = new HashMap<String, String>();
    jsLibraries = new LinkedList<JsLibrary>();
  }

  /**
   * @return Global identifier used to retrieve gadget's spec
   */
  public ID getId() {
    return id;
  }

  /**
   * @return GadgetSpec that backs this Gadget. Package scope for tests.
   */
  GadgetSpec getBaseSpec() {
    return baseSpec;
  }

  /**
   * @return Object containing all hangman substitutions applied to this gadget
   */
  public Substitutions getSubstitutions() {
    return substitutions;
  }

  // GadgetSpec accessors

  /**
   * @return Gadget title with substitutions applied
   */
  public String getTitle() {
    return substitutions.substitute(baseSpec.getTitle());
  }

  /**
   * @return URI used as a target for Gadget's title link, or null if malformed
   */
  public URI getTitleURI() {
    URI ret = null;
    if (baseSpec.getTitleURI() != null) {
      String uriStr = baseSpec.getTitleURI().toString();
      try {
        ret = new URI(substitutions.substitute(uriStr));
      } catch (URISyntaxException e) {
        return null;
      }
    }
    return ret;
  }

  /**
   * @return String used to describe this Gadget in directories, with
   * substitutions applied
   */
  public String getDirectoryTitle() {
    return substitutions.substitute(baseSpec.getDirectoryTitle());
  }

  /**
   * @return Extended description of {@code Gadget}, with substitutions applied
   */
  public String getDescription() {
    return substitutions.substitute(baseSpec.getDescription());
  }

  /**
   * @return Name of this Gadget's author as specified in its spec
   */
  public String getAuthor() {
    return baseSpec.getAuthor();
  }

  /**
   * @return E-mail address of this Gadget's author as specified in its spec
   */
  public String getAuthorEmail() {
    return baseSpec.getAuthorEmail();
  }

  // TODO: make this URI?
  public String getScreenshot() {
    return baseSpec.getScreenshot();
  }

  // TODO: make this URI?
  public String getThumbnail() {
    return baseSpec.getThumbnail();
  }

  public List<LocaleSpec> getLocaleSpecs() {
    return new ArrayList<LocaleSpec>(baseSpec.getLocaleSpecs());
  }

  /**
   * @return List of all {@code FeatureSpec}s declared by this gadget
   */
  public Map<String, FeatureSpec> getRequires() {
    return Collections.unmodifiableMap(baseSpec.getRequires());
  }

  /**
   * @return All JS libraries needed to render this gadget.
   */
  public List<JsLibrary> getJsLibraries() {
    return Collections.unmodifiableList(jsLibraries);
  }

  /**
   * @param library
   */
  public void addJsLibrary(JsLibrary library) {
    jsLibraries.add(library);
  }

  /**
   * Extracts parameters for the given feature.
   *
   * @param gadget
   * @param feature
   * @return The parameters, or an empty map.
   */
  @SuppressWarnings("unchecked")
  public static Map<String, String> getFeatureParams(Gadget gadget,
                                                     String feature) {
    GadgetSpec.FeatureSpec spec = gadget.getRequires().get(feature);
    if (spec == null) {
      return Collections.EMPTY_MAP;
    } else {
      return spec.getParams();
    }
  }

  /**
   * @return List of all preload URIs declared, with substitutions applied
   */
  public List<String> getPreloads() {
    List<String> ret = new LinkedList<String>();
    for (String preload : baseSpec.getPreloads()) {
      ret.add(substitutions.substitute(preload));
    }
    return ret;
  }

  /**
   * @return List of icons defined in gadget spec
   */
  public List<Icon> getIcons() {
    return Collections.unmodifiableList(baseSpec.getIcons());
  }

  /**
   * @return List of all user pref specs defined in gadget spec
   */
  public List<UserPref> getUserPrefs() {
    return Collections.unmodifiableList(baseSpec.getUserPrefs());
  }

  public Map<String, String> getUserPrefValues() {
    return Collections.unmodifiableMap(userPrefValues);
  }

  /**
   * @return Type of gadget to render
   */
  public ContentType getContentType() {
    return baseSpec.getContentType();
  }

  /**
   * @return URI of gadget to render of type == URL; null if malformed/missing
   * @throws IllegalStateException if contentType is not URL.
   */
  public URI getContentHref() {
    URI ret = null;
    String uriStr = baseSpec.getContentHref().toString();
    try {
      ret = new URI(substitutions.substitute(uriStr));
    } catch (URISyntaxException e) {
      return null;
    }
    return ret;
  }

  /**
   * @return Gadget contents with all substitutions applied
   * @throws IllegalStateException if contentType is not HTML.
   */
  public String getContentData() {
    return substitutions.substitute(baseSpec.getContentData());
  }

  private MessageBundle currentMessageBundle = MessageBundle.EMPTY;
  public MessageBundle getCurrentMessageBundle() {
    return currentMessageBundle;
  }
  public void setCurrentMessageBundle(MessageBundle bundle) {
    currentMessageBundle = bundle;
  }

  /**
   * @return Copy of base spec that created this {@code Gadget}
   */
  public GadgetSpec copy() {
    return baseSpec.copy();
  }
}
