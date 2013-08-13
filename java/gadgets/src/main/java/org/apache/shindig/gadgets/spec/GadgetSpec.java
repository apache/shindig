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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.HashUtil;
import org.apache.shindig.common.util.OpenSocialVersion;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.spec.View.ContentType;
import org.apache.shindig.gadgets.variables.Substitutions;


import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Represents a gadget specification root element (Module).
 *
 * @see <a href="http://www.opensocial.org/Technical-Resources/opensocial-spec-v08/gadget-spec">gadgets spec</a>
 */
public class GadgetSpec {
  public static final String DEFAULT_VIEW = "default";
  public static final Locale DEFAULT_LOCALE = new Locale("all", "ALL");

  private static final String ATTR_SPECIFICATION_VERSION = "specificationVersion";
  public static final String DOCTYPE_QUIRKSMODE = "quirksmode";

  /**
   * Creates a new Module from the given xml input.
   *
   * @param url The original url of the gadget.
   * @param doc The pre-parsed xml document.
   * @param original Unparsed input XML. Used to generate checksums.
   *
   * @throws SpecParserException If xml can not be processed as a valid gadget spec.
   */
  public GadgetSpec(Uri url, Element doc, String original) throws SpecParserException {
    this.url = url;

    // This might not be good enough; should we take message bundle changes into account?
    this.checksum = HashUtil.checksum(original.getBytes());

    NodeList children = doc.getChildNodes();
    //Save specification version of this Gadget
    setAttribute(ATTR_SPECIFICATION_VERSION,doc.getAttribute(ATTR_SPECIFICATION_VERSION));

    ModulePrefs modulePrefs = null;
    // Lets try keep order of user prefs and views
    Map<String,UserPref> prefsBuilder = Maps.newLinkedHashMap();
    Map<String, List<Element>> views = Maps.newLinkedHashMap();
    for (int i = 0, j = children.getLength(); i < j; ++i) {
      Node child = children.item(i);
      if (!(child instanceof Element)) {
        continue;
      }
      Element element = (Element)child;
      String name = element.getTagName();
      if ("ModulePrefs".equals(name)) {
        if (modulePrefs == null) {
          modulePrefs = new ModulePrefs(element, url);
        } else {
          throw new SpecParserException("Only 1 ModulePrefs is allowed.");
        }
      }
      if ("UserPref".equals(name)) {
        UserPref pref = new UserPref(element);
        if (prefsBuilder.containsKey(pref.getName())) {
          throw new SpecParserException("Duplicate value for user pref " + pref.getName());
        }
        prefsBuilder.put(pref.getName(), pref);
      }
      if ("Content".equals(name)) {
        String viewNames = XmlUtil.getAttribute(element, "view", "default");
        for (String view : Splitter.on(',').trimResults().split(viewNames)) {
          List<Element> viewElements = views.get(view);
          if (viewElements == null) {
            viewElements = Lists.newLinkedList();
            views.put(view, viewElements);
          }
          viewElements.add(element);
        }
      }
      if("ExternalServices".equals(name)) {
        // There could be only one ExternalServices tag
        if(externalServices != null) {
          throw new SpecParserException("Only 1 ExternalServices is allowed.");
        }
        externalServices = new ExternalServices(element);
      }
    }

    if (modulePrefs == null) {
      throw new SpecParserException("At least 1 ModulePrefs is required.");
    } else {
      this.modulePrefs = modulePrefs;
    }

    if (views.isEmpty()) {
      throw new SpecParserException("At least 1 Content is required.");
    } else {
      Map<String, View> tmpViews = Maps.newHashMap();
      for (Map.Entry<String, List<Element>> view : views.entrySet()) {
        View v = new View(view.getKey(), view.getValue(), url);
        tmpViews.put(v.getName(), v);
      }
      this.views = ImmutableMap.copyOf(tmpViews);
    }
    this.userPrefs = ImmutableMap.copyOf(prefsBuilder);
  }

  /**
   * Use for testing.
   */
  @VisibleForTesting
  public GadgetSpec(Uri url, String xml) throws SpecParserException {
    this(url, XmlUtil.parseSilent(xml), xml);
  }

  /**
   * Constructs a GadgetSpec for substitute calls.
   * @param spec
   */
  protected GadgetSpec(GadgetSpec spec) {
    url = spec.url;
    checksum = spec.checksum;
    attributes.putAll(spec.attributes);
  }

  /**
   * Returns this Gadget's specification version.  Defaults to 1.0 if attribute not set.
   * @return Version value as String
   */
  public OpenSocialVersion getSpecificationVersion(){
    // 1.0 is default if unspecified as defined in Section 7 of OS 1.1 Core Gadget specification
    String value = (String)attributes.get(ATTR_SPECIFICATION_VERSION);
    if (value == null) {
      return new OpenSocialVersion("1.0");
    } else {
      return new OpenSocialVersion(value);
    }
  }

  /**
   * The url for this gadget spec.
   */
  private final Uri url;
  public Uri getUrl() {
    return url;
  }

  /**
   * A checksum of the gadget's content.
   */
  private final String checksum;
  public String getChecksum() {
    return checksum;
  }

  /**
   * ModulePrefs
   */
  protected ModulePrefs modulePrefs;
  public ModulePrefs getModulePrefs() {
    return modulePrefs;
  }


  /**
   * UserPref
   */
  protected Map<String,UserPref> userPrefs;
  public Map<String,UserPref> getUserPrefs() {
    return userPrefs;
  }

  /**
   * Content
   * Mapping is view -> Content section.
   */
  protected Map<String, View> views;
  public Map<String, View> getViews() {
    return views;
  }

  /**
   * ExternalServices
   */
  protected ExternalServices externalServices;
  public ExternalServices getExternalServices() {
    return externalServices;
  }

  /**
   * Retrieves a single view by name.
   *
   * @param name The name of the view you want to see
   * @return The view object, if it exists, or null.
   */
  public View getView(String name) {
    return views.get(name);
  }

  /**
   * A map of attributes associated with the instance of the spec
   * Used by handler classes to use specs to carry context.
   * Not defined by the specification
   */
  private final Map<String, Object> attributes = new MapMaker().makeMap();
  public Object getAttribute(String key) {
    return attributes.get(key);
  }

  /**
   * Sets an attribute on the gadget spec. This should only be done during a constructing phase, as
   * a GadgetSpec should be effectively immutable after it is constructed.
   *
   * @param key The attribute name.
   * @param o The value of the attribute.
   */
  public void setAttribute(String key, Object o) {
    attributes.put(key, o);
  }

  /**
   * Performs substitutions on the spec. See individual elements for
   * details on what gets substituted.
   *
   * @param substituter
   * @return The substituted spec.
   */
  public GadgetSpec substitute(Substitutions substituter) {
    GadgetSpec spec = new GadgetSpec(this);
    spec.modulePrefs = modulePrefs.substitute(substituter);

    if (userPrefs.isEmpty()) {
      spec.userPrefs = ImmutableMap.of();
    } else {
      ImmutableMap.Builder<String,UserPref> prefs = ImmutableMap.builder();
      for (UserPref pref : this.userPrefs.values()) {
        prefs.put(pref.getName(), pref.substitute(substituter));
      }
      spec.userPrefs = prefs.build();
    }

    ImmutableMap.Builder<String, View> viewMap = ImmutableMap.builder();
    for (View view : views.values()) {
      viewMap.put(view.getName(), view.substitute(substituter));
    }
    spec.views = viewMap.build();

    return spec;
  }

  /**
   * Returns a copy of the spec with all type=url views removed.
   */
  public GadgetSpec removeUrlViews() {
    GadgetSpec spec = new GadgetSpec(this);
    spec.modulePrefs = modulePrefs;
    spec.userPrefs = userPrefs;
    ImmutableMap.Builder<String, View> viewMap = ImmutableMap.builder();
    for (View view : views.values()) {
      if (view.getType() != ContentType.URL) {
        viewMap.put(view.getName(), view);
      }
    }
    spec.views = viewMap.build();
    return spec;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("<Module>\n")
       .append(modulePrefs).append('\n');
    for (UserPref pref : userPrefs.values()) {
      buf.append(pref).append('\n');
    }
    for (Map.Entry<String, View> view : views.entrySet()) {
      buf.append(view.getValue()).append('\n');
    }
    buf.append("</Module>");
    return buf.toString();
  }
}
