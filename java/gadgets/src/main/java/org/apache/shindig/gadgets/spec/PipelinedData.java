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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.XmlException;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.expressions.ElException;
import org.apache.shindig.gadgets.expressions.Expression;
import org.apache.shindig.gadgets.expressions.ExpressionContext;
import org.apache.shindig.gadgets.expressions.Expressions;
import org.apache.shindig.gadgets.expressions.GadgetExpressionContext;
import org.apache.shindig.gadgets.variables.Substitutions;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Parsing code for <os:*> elements.
 */
public class PipelinedData {
  private final Map<String, SocialData> socialPreloads;
  private final Map<String, HttpData> httpPreloads;

  private boolean needsViewer;
  private boolean needsOwner;

  public static final String OPENSOCIAL_NAMESPACE = "http://ns.opensocial.org/2008/markup";

  public PipelinedData(Element element, Uri base) throws SpecParserException {
    Map<String, SocialData> socialPreloads = Maps.newHashMap();
    Map<String, HttpData> httpPreloads = Maps.newHashMap();

    // TODO: extract this loop into XmlUtils.getChildrenWithNamespace
    for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (!(node instanceof Element)) {
        continue;
      }

      Element child = (Element) node;
      // Ignore elements not in the namespace
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
          // TODO: delete when 0.9 app data retrieval is supported
          socialPreloads.put(key, createPersonAppDataRequest(child));
        } else if ("ActivitiesRequest".equals(elementName)) {
          socialPreloads.put(key, createActivityRequest(child));
        } else if ("MakeRequest".equals(elementName)) {
          httpPreloads.put(key, createMakeRequest(child, base));
        } else {
          // TODO: This is wrong - the spec should parse, but should preload
          // notImplemented
          throw new SpecParserException("Unknown element <os:" + elementName + '>');
        }
      } catch (ElException ele) {
        throw new SpecParserException(new XmlException(ele));
      }
    }

    this.socialPreloads = Collections.unmodifiableMap(socialPreloads);
    this.httpPreloads = Collections.unmodifiableMap(httpPreloads);
  }

  private PipelinedData(PipelinedData socialData, Substitutions substituter) {
    Map<String, SocialData> socialPreloads = Maps.newHashMap();
    Map<String, HttpData> httpPreloads = Maps.newHashMap();

    // TODO: support hangman substitutions for social preloads?
    socialPreloads.putAll(socialData.socialPreloads);

    for (Map.Entry<String, HttpData> httpPreload : socialData.httpPreloads.entrySet()) {
      httpPreloads.put(httpPreload.getKey(), httpPreload.getValue().substitute(substituter));
    }

    this.socialPreloads = Collections.unmodifiableMap(socialPreloads);
    this.httpPreloads = Collections.unmodifiableMap(httpPreloads);

  }

  /**
   * Allows the creation of a view from an existing view so that localization
   * can be performed.
   */
  public PipelinedData substitute(Substitutions substituter) {
    return new PipelinedData(this, substituter);
  }

  public Map<String, Object> getSocialPreloads(GadgetContext context) {
    Map<String, Object> evaluatedPreloads = Maps.newHashMapWithExpectedSize(
        socialPreloads.size());
    ExpressionContext expressionContext = new GadgetExpressionContext(context);
    for (Map.Entry<String, SocialData> preload : socialPreloads.entrySet()) {
      try {
        evaluatedPreloads.put(preload.getKey(), preload.getValue().toJson(expressionContext));
      } catch (ElException e) {
        // TODO: Handle!?!
        throw new RuntimeException(e);
      }
    }

    return evaluatedPreloads;
  }

  public Map<String, RequestAuthenticationInfo> getHttpPreloads(GadgetContext context) {
    Map<String, RequestAuthenticationInfo> evaluatedPreloads = Maps.newHashMapWithExpectedSize(
        httpPreloads.size());
    ExpressionContext expressionContext = new GadgetExpressionContext(context);
    for (Map.Entry<String, HttpData> preload : httpPreloads.entrySet()) {
      try {
        evaluatedPreloads.put(preload.getKey(), preload.getValue().evaluate(expressionContext));
      } catch (ElException e) {
        // TODO: Handle!?!
        throw new RuntimeException(e);
      }
    }

    return evaluatedPreloads;
  }

  public boolean needsViewer() {
    return needsViewer;
  }

  public boolean needsOwner() {
    return needsOwner;
  }

  /** Handle the os:PeopleRequest element */
  private SocialData createPeopleRequest(Element child) throws ElException {
    SocialData expression = new SocialData(child.getAttribute("key"), "people.get");

    copyAttribute("groupId", child, expression, String.class);
    copyAttribute("userId", child, expression, JSONArray.class);
    updateUserArrayState("userId", child);
    copyAttribute("personId", child, expression, JSONArray.class);
    updateUserArrayState("personId", child);

    copyAttribute("startIndex", child, expression, Integer.class);
    copyAttribute("count", child, expression, Integer.class);
    copyAttribute("sortBy", child, expression, String.class);
    copyAttribute("sortOrder", child, expression, String.class);
    copyAttribute("filterBy", child, expression, String.class);
    copyAttribute("filterOperation", child, expression, String.class);
    copyAttribute("filterValue", child, expression, String.class);
    copyAttribute("fields", child, expression, JSONArray.class);

    return expression;
  }

  /** Handle the os:ViewerRequest element */
  private SocialData createViewerRequest(Element child) throws ElException {
    return createPersonRequest(child, "@viewer");
  }

  /** Handle the os:OwnerRequest element */
  private SocialData createOwnerRequest(Element child) throws ElException {
    return createPersonRequest(child, "@owner");
  }

  private SocialData createPersonRequest(Element child, String userId) throws ElException {
    SocialData expression = new SocialData(child.getAttribute("key"), "people.get");

    expression.addProperty("userId", userId, JSONArray.class);
    updateUserState(userId);
    copyAttribute("fields", child, expression, JSONArray.class);

    return expression;
  }

  /** Handle the os:PersonAppDataRequest element */
  private SocialData createPersonAppDataRequest(Element child) throws ElException {
    SocialData expression = new SocialData(child.getAttribute("key"), "appdata.get");

    copyAttribute("groupId", child, expression, String.class);
    copyAttribute("userId", child, expression, JSONArray.class);
    updateUserArrayState("userId", child);
    copyAttribute("appId", child, expression, String.class);
    copyAttribute("fields", child, expression, JSONArray.class);

    return expression;
  }

  /** Handle the os:ActivitiesRequest element */
  private SocialData createActivityRequest(Element child) throws ElException {
    SocialData expression = new SocialData(child.getAttribute("key"), "activities.get");

    copyAttribute("groupId", child, expression, String.class);
    copyAttribute("userId", child, expression, JSONArray.class);
    updateUserArrayState("userId", child);
    copyAttribute("appId", child, expression, String.class);
    // TODO: should be activityIds?
    copyAttribute("activityId", child, expression, JSONArray.class);
    copyAttribute("fields", child, expression, JSONArray.class);

    return expression;
  }

  /** Handle an os:MakeRequest element */
  private HttpData createMakeRequest(Element child, Uri base) throws ElException {
    HttpData data = new HttpData(child, base);

    /* TODO: check auth type, and sign-by-owner/viewer, once spec agrees
     * to remove support for EL on @authz and @sign_*.
    if (preload.getAuthType() != AuthType.NONE) {
      if (preload.isSignOwner()) {
        needsOwner = true;
      }

      if (preload.isSignViewer()) {
        needsViewer = true;
      }
    }*/

    return data;
  }

  private void copyAttribute(String name, Element element, SocialData expression, Class<?> type)
    throws ElException {
    if (element.hasAttribute(name)) {
      expression.addProperty(name, element.getAttribute(name), type);
    }
  }

  /** Look for @viewer, @owner within a userId attribute */
  private void updateUserArrayState(String name, Element element) {
    if (element.hasAttribute(name)) {
      // TODO: check after Expression evaluation?
      StringTokenizer tokens = new StringTokenizer(element.getAttribute(name), ",");
      while (tokens.hasMoreTokens()) {
        updateUserState(tokens.nextToken());
      }
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

  /**
   * A single pipelined HTTP makerequest.
   */
  private static class HttpData {
    private final Expression<String> authz;
    private final Uri base;
    private final String href;
    private final Expression<Boolean> signOwner;
    private final Expression<Boolean> signViewer;
    private final Map<String, Expression<String>> attributes;

    private static final Set<String> KNOWN_ATTRIBUTES =
          ImmutableSet.of("authz", "href", "sign_owner", "sign_viewer");

    /**
     * Create an HttpData off an <os:makeRequest> element.
     */
    public HttpData(Element element, Uri base) throws ElException {
      this.base = base;

      // TODO: Spec question;  should authz be EL enabled?
      String authz = element.hasAttribute("authz") ? element.getAttribute("authz") : "none";
      this.authz = Expressions.parse(authz, String.class);

      // TODO: Spec question;  should EL values be URL escaped?
      this.href = element.getAttribute("href");

      // TODO: Spec question;  should sign_* be EL enabled?
      this.signOwner = booleanExpression(element, "sign_owner");
      this.signViewer = booleanExpression(element, "sign_viewer");

      Map<String, Expression<String>> attributes = Maps.newHashMap();
      for (int i = 0; i < element.getAttributes().getLength(); i++) {
        Node attr = element.getAttributes().item(i);
        if (!KNOWN_ATTRIBUTES.contains(attr.getNodeName())) {
          attributes.put(attr.getNodeName(), Expressions.parse(attr.getNodeValue(), String.class));
        }
      }

      this.attributes = ImmutableMap.copyOf(attributes);
    }

    private HttpData(HttpData data, Substitutions substituter) {
      this.base = data.base;
      this.authz = data.authz;
      this.href = substituter.substituteString(data.href);
      this.signOwner = data.signOwner;
      this.signViewer = data.signViewer;
      this.attributes = data.attributes;
    }

    /** Run substitutions over an HttpData */
    public HttpData substitute(Substitutions substituter) {
      return new HttpData(this, substituter);
    }

    /**
     * Evaluate expressions and return a RequestAuthenticationInfo.
     * @throws ElException if expression evaluation fails.
     */
    public RequestAuthenticationInfo evaluate(ExpressionContext context) throws ElException {
      final AuthType authType = AuthType.parse(authz.evaluate(context));
      Expression<String> hrefExpression = Expressions.parse(href, String.class);
      final Uri evaluatedHref = base.resolve(Uri.parse(hrefExpression.evaluate(context)));

      final boolean evaluatedSignOwner = evaluateBooleanExpression(context,
          this.signOwner, true);
      final boolean evaluatedSignViewer = evaluateBooleanExpression(context,
          this.signViewer, true);
      final Map<String, String> evaluatedAttributes = Maps.newHashMap();
      for (Map.Entry<String, Expression<String>> attr : attributes.entrySet()) {
        evaluatedAttributes.put(attr.getKey(), attr.getValue().evaluate(context));
      }

      return new RequestAuthenticationInfo() {
        public Map<String, String> getAttributes() {
          return evaluatedAttributes;
        }

        public AuthType getAuthType() {
          return authType;
        }

        public Uri getHref() {
          return evaluatedHref;
        }

        public boolean isSignOwner() {
          return evaluatedSignOwner;
        }

        public boolean isSignViewer() {
          return evaluatedSignViewer;
        }
      };
    }

    /** Evaluate a boolean expression, choosing a default value if needed */
    private boolean evaluateBooleanExpression(ExpressionContext context,
        Expression<Boolean> expression, boolean defaultValue) throws ElException {
      if (expression == null) {
        return defaultValue;
      }

      Boolean value = expression.evaluate(context);
      if (value == null) {
        return defaultValue;
      }

      return value;
    }

    /** Parse a boolean expression off an XML attribute. */
    private Expression<Boolean> booleanExpression(Element element, String attrName)
        throws ElException {
      if (!element.hasAttribute(attrName)) {
        return null;
      }

      return Expressions.parse(element.getAttribute(attrName), Boolean.class);
    }
  }

  /**
   * A single social data request.
   */
  private static class SocialData {
    private final List<Property> properties = Lists.newArrayList();
    private final String id;
    private final String method;

    public SocialData(String id, String method) {
      this.id = id;
      this.method = method;
    }

    public void addProperty(String name, String value, Class<?> type) throws ElException {
      Expression<?> expression = Expressions.parse(value, type);
      properties.add(new Property(name, expression));
    }

    /** Create the JSON request form for the social data */
    public JSONObject toJson(ExpressionContext context) throws ElException {
      JSONObject object = new JSONObject();
      try {
        object.put("method", method);
        object.put("id", id);

        JSONObject params = new JSONObject();
        for (Property property : properties) {
          property.set(context, params);
        }
        object.put("params", params);
      } catch (JSONException je) {
        throw new ElException(je);
      }

      return object;
    }

    /** Single property for an expression */
    private static class Property {
      private final Expression<?> expression;
      private final String name;

      public Property(String name, Expression<?> expression) {
        this.name = name;
        this.expression = expression;
      }

      public void set(ExpressionContext context, JSONObject object) throws ElException {
        Object value = expression.evaluate(context);
        try {
          if (value != null) {
            object.put(name, value);
          }
        } catch (JSONException e) {
          throw new ElException("Error parsing property \"" + name + '\"', e);
        }
      }
    }
  }
}
