/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.spec;

import com.google.common.collect.Maps;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.XmlException;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.variables.Substitutions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Collections;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Parsing code for <os:*> elements.
 */
public class SocialData {

  private final Map<String, Object> socialPreloads;
  private final Map<String, Preload> httpPreloads;

  private boolean needsViewer;
  private boolean needsOwner;

  public static final String OPENSOCIAL_NAMESPACE = "http://ns.opensocial.org/2008/markup";


  public SocialData(Element element, Uri base) throws SpecParserException {
    Map<String, Object> socialPreloads = Maps.newHashMap();
    Map<String, Preload> httpPreloads = Maps.newHashMap();

    // TODO: extract this loop into XmlUtils.getChildrenWithNamespace
    for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (!(node instanceof Element)) {
        continue;
      }

      Element child = (Element) node;
      // Ignore elements not in the namesapce
      if (!OPENSOCIAL_NAMESPACE.equals(child.getNamespaceURI())) {
        continue;
      }

      String elementName = child.getLocalName();

      String key = child.getAttribute("key");
      if (key == null) {
        throw new SpecParserException("Missing key attribute on os:" + elementName);
      }

      try {
        if ("PeopleRequest".equals(elementName)) {
          socialPreloads.put(key, createPeopleRequest(child));
        } else if ("ViewerRequest".equals(elementName)) {
          socialPreloads.put(key, createViewerRequest(child));
        } else if ("OwnerRequest".equals(elementName)) {
          socialPreloads.put(key, createOwnerRequest(child));
        } else if ("PersonAppDataRequest".equals(elementName)) {
          socialPreloads.put(key, createPersonAppDataRequest(child));
        } else if ("ActivitiesRequest".equals(elementName)) {
          socialPreloads.put(key, createActivityRequest(child));
        } else if ("MakeRequest".equals(elementName)) {
          httpPreloads.put(key, createMakeRequest(child, base));
        } else {
          // TODO: This is wrong - the spec should parse, but should preload notImplemented
          throw new SpecParserException("Unknown element <os:" + elementName + ">");
        }
        // TODO: implement DataRequest
      } catch (JSONException je) {
        throw new SpecParserException(new XmlException(je));
      }
    }

    this.socialPreloads = Collections.unmodifiableMap(socialPreloads);
    this.httpPreloads = Collections.unmodifiableMap(httpPreloads);
  }

  private SocialData(SocialData socialData, Substitutions substituter) {
    Map<String, Object> socialPreloads = Maps.newHashMap();
    Map<String, Preload> httpPreloads = Maps.newHashMap();

    // TODO: support hangman substitutions for social preloads?
    socialPreloads.putAll(socialData.socialPreloads);
    
    for (Map.Entry<String, Preload> httpPreload : socialData.httpPreloads.entrySet()) {
      httpPreloads.put(httpPreload.getKey(), httpPreload.getValue().substitute(substituter));
    }

    this.socialPreloads = Collections.unmodifiableMap(socialPreloads);
    this.httpPreloads = Collections.unmodifiableMap(httpPreloads);

  }
  
  /**
   * Allows the creation of a view from an existing view so that localization
   * can be performed.
   */
  public SocialData substitute(Substitutions substituter) {
    return new SocialData(this, substituter);
  }

  public Map<String, Object> getSocialPreloads() {
    return socialPreloads;
  }

  public Map<String, Preload> getHttpPreloads() {
    return httpPreloads;
  }

  public boolean needsViewer() {
    return needsViewer;
  }

  public boolean needsOwner() {
    return needsOwner;
  }

  /** Handle the os:PeopleRequest element */
  private Object createPeopleRequest(Element child) throws JSONException {
    JSONObject params = new JSONObject();
    copyAttribute("groupId", child, params);
    copyArrayAttribute("userId", child, params, true);
    copyArrayAttribute("personId", child, params, true);

    copyIntegerAttribute("startIndex", child, params);
    copyIntegerAttribute("count", child, params);
    copyAttribute("sortBy", child, params);
    copyAttribute("sortOrder", child, params);
    copyAttribute("filterBy", child, params);
    copyAttribute("filterOperation", child, params);
    copyAttribute("filterValue", child, params);
    copyArrayAttribute("fields", child, params, false);

    JSONObject peopleRequest = new JSONObject();
    peopleRequest.put("method", "people.get");
    peopleRequest.put("params", params);
    peopleRequest.put("id", child.getAttribute("key"));
    return peopleRequest;
  }

  /** Handle the os:ViewerRequest element */
  private Object createViewerRequest(Element child) throws JSONException {
    return createPersonRequest(child, "@viewer");
  }

  /** Handle the os:OwnerRequest element */
  private Object createOwnerRequest(Element child) throws JSONException {
    return createPersonRequest(child, "@owner");
  }

  private Object createPersonRequest(Element child, String userId) throws JSONException {
    JSONObject params = new JSONObject();

    params.put("userId", new JSONArray().put(userId));
    updateUserState(userId);
    copyArrayAttribute("fields", child, params, false);

    JSONObject peopleRequest = new JSONObject();
    peopleRequest.put("method", "people.get");
    peopleRequest.put("params", params);
    peopleRequest.put("id", child.getAttribute("key"));
    return peopleRequest;
  }

  /** Handle the os:PersonAppDataRequest element */
  private Object createPersonAppDataRequest(Element child) throws JSONException {
    JSONObject params = new JSONObject();
    copyAttribute("groupId", child, params);
    copyArrayAttribute("userId", child, params, true);
    copyAttribute("appId", child, params);
    copyArrayAttribute("fields", child, params, false);

    JSONObject appDataRequest = new JSONObject();
    appDataRequest.put("method", "appdata.get");
    appDataRequest.put("params", params);
    appDataRequest.put("id", child.getAttribute("key"));
    return appDataRequest;
  }

  /** Handle the os:ActivitiesRequest element */
  private Object createActivityRequest(Element child) throws JSONException {
    JSONObject params = new JSONObject();
    copyAttribute("groupId", child, params);
    copyArrayAttribute("userId", child, params, true);
    copyAttribute("appId", child, params);
    copyArrayAttribute("activitityId", child, params, false);
    copyArrayAttribute("fields", child, params, false);

    JSONObject appDataRequest = new JSONObject();
    appDataRequest.put("method", "activities.get");
    appDataRequest.put("params", params);
    appDataRequest.put("id", child.getAttribute("key"));
    return appDataRequest;
  }

  /** Handle an os:MakeRequest element */
  private Preload createMakeRequest(Element child, Uri base) throws SpecParserException {
    Preload preload = new Preload(child, base);
    if (preload.getAuthType() != AuthType.NONE) {
      if (preload.isSignOwner()) {
        needsOwner = true;
      }

      if (preload.isSignViewer()) {
        needsViewer = true;
      }
    }

    return preload;
  }

  private void copyAttribute(String name, Element element, JSONObject jsonObject)
      throws JSONException {
    if (element.hasAttribute(name)) {
      jsonObject.put(name, element.getAttribute(name));
    }
  }

  private void copyIntegerAttribute(String name, Element element, JSONObject jsonObject)
      throws JSONException {
    if (element.hasAttribute(name)) {
      String valueStr = element.getAttribute(name);
      try {
        int value = Integer.parseInt(valueStr);
        jsonObject.put(name, value);
      } catch (NumberFormatException nfe) {
        throw new JSONException(String.format("Value \"%s\" of attribute \"%s\" is not an integer",
            valueStr, name));
      }
    }
  }

  private void copyArrayAttribute(String name, Element element, JSONObject jsonObject,
      boolean isUserAttribute) throws JSONException {
    if (element.hasAttribute(name)) {
      StringTokenizer tokenizer = new StringTokenizer(element.getAttribute(name), ",");
      JSONArray array = new JSONArray();
      while (tokenizer.hasMoreTokens()) {
        String token = tokenizer.nextToken();
        array.put(token);
        if (isUserAttribute) {
          updateUserState(token);
        }
      }

      jsonObject.put(name, array);
    }
  }

  /** Updates whether this batch of SocialData needs owner or viewer data */
  private void updateUserState(String userId) {
    if ("@owner".equals(userId)) {
      needsOwner = true;
    } else if ("@viewer".equals(userId) || "@me".equals(userId)) {
      needsViewer = true;
    }
  }
}
