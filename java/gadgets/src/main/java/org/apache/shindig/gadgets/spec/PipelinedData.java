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
import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.GadgetELResolver;
import org.apache.shindig.gadgets.variables.Substitutions;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.PropertyNotFoundException;
import javax.el.ValueExpression;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Parsing code for &lt;os:*&gt; elements.
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
        } else if ("DataRequest".equals(elementName)) {
          socialPreloads.put(key, createDataRequest(child));
        } else if ("HttpRequest".equals(elementName)) {
          httpPreloads.put(key, createHttpRequest(child, base));
        } else {
          // TODO: This is wrong - the spec should parse, but should preload
          // notImplemented
          throw new SpecParserException("Unknown element <os:" + elementName + '>');
        }
      } catch (ELException ele) {
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

  public interface Batch {
    Map<String, Object> getSocialPreloads();
    Map<String, RequestAuthenticationInfo> getHttpPreloads();
    Batch getNextBatch(ELResolver rootObjects);
  }

  /**
   * Gets the first batch of preload requests.  Preloads that require root
   * objects not yet available will not be executed in this batch, but may
   * become available in subsequent batches.
   *
   * @param rootObjects an ELResolver that can evaluate currently available
   *     root objects.
   * @see GadgetELResolver
   * @return a batch, or null if no batch could be created
   */
  public Batch getBatch(Expressions expressions, ELResolver rootObjects) {
    return getBatch(expressions, rootObjects, socialPreloads, httpPreloads);
  }

  /**
   * Create a Batch of preload requests
   * @param expressions expressions instance for parsing expressions
   * @param rootObjects an ELResolver that can evaluate currently available
   *     root objects.
   * @param currentSocialPreloads the remaining social preloads
   * @param currentHttpPreloads the remaining http preloads
   */
  private Batch getBatch(Expressions expressions, ELResolver rootObjects,
      Map<String, SocialData> currentSocialPreloads,
      Map<String, HttpData> currentHttpPreloads) {
    ELContext elContext = expressions.newELContext(rootObjects);

    // Evaluate all existing social preloads
    Map<String, Object> evaluatedSocialPreloads = Maps.newHashMap();
    Map<String, SocialData> pendingSocialPreloads = null;

    if (currentSocialPreloads != null) {
      for (Map.Entry<String, SocialData> preload : currentSocialPreloads.entrySet()) {
        try {
          Object value = preload.getValue().toJson(expressions, elContext);
          evaluatedSocialPreloads.put(preload.getKey(), value);
        } catch (PropertyNotFoundException pnfe) {
          // Missing top-level property: put it in the pending set
          if (pendingSocialPreloads == null) {
            pendingSocialPreloads = Maps.newHashMap();
          }
          pendingSocialPreloads.put(preload.getKey(), preload.getValue());
        } catch (ELException e) {
          // TODO: Handle!?!
          throw new RuntimeException(e);
        }
      }
    }
    // And evaluate all existing HTTP preloads
    Map<String, RequestAuthenticationInfo> evaluatedHttpPreloads = Maps.newHashMap();
    Map<String, HttpData> pendingHttpPreloads = null;

    if (currentHttpPreloads != null) {
      for (Map.Entry<String, HttpData> preload : currentHttpPreloads.entrySet()) {
        try {
          RequestAuthenticationInfo value = preload.getValue().evaluate(expressions, elContext);
          evaluatedHttpPreloads.put(preload.getKey(), value);
        } catch (PropertyNotFoundException pnfe) {
          if (pendingHttpPreloads == null) {
            pendingHttpPreloads = Maps.newHashMap();
          }
          pendingHttpPreloads.put(preload.getKey(), preload.getValue());
        } catch (ELException e) {
          // TODO: Handle!?!
          throw new RuntimeException(e);
        }
      }
    }

    // Nothing evaluated or pending;  return null for the batch.  Note that
    // there may be multiple PipelinedData objects (e.g., from multiple
    // <script type="text/os-data"> elements), so even if all evaluations
    // fail here, evaluations might succeed elsewhere and free up pending preloads
    if (evaluatedSocialPreloads.isEmpty() && evaluatedHttpPreloads.isEmpty() &&
        pendingHttpPreloads == null && pendingSocialPreloads == null) {
      return null;
    }

    return new BatchImpl(expressions, evaluatedSocialPreloads, evaluatedHttpPreloads,
        pendingSocialPreloads, pendingHttpPreloads);
  }

  /** Batch implementation */
  class BatchImpl implements Batch {

    private final Expressions expressions;
    private final Map<String, Object> evaluatedSocialPreloads;
    private final Map<String, RequestAuthenticationInfo> evaluatedHttpPreloads;
    private final Map<String, SocialData> pendingSocialPreloads;
    private final Map<String, HttpData> pendingHttpPreloads;

    public BatchImpl(Expressions expressions,
        Map<String, Object> evaluatedSocialPreloads,
        Map<String, RequestAuthenticationInfo> evaluatedHttpPreloads,
        Map<String, SocialData> pendingSocialPreloads, Map<String, HttpData> pendingHttpPreloads) {
      this.expressions = expressions;
      this.evaluatedSocialPreloads = evaluatedSocialPreloads;
      this.evaluatedHttpPreloads = evaluatedHttpPreloads;
      this.pendingSocialPreloads = pendingSocialPreloads;
      this.pendingHttpPreloads = pendingHttpPreloads;
    }

    public Map<String, Object> getSocialPreloads() {
      return evaluatedSocialPreloads;
    }

    public Map<String, RequestAuthenticationInfo> getHttpPreloads() {
      return evaluatedHttpPreloads;
    }

    public Batch getNextBatch(ELResolver rootObjects) {
      return getBatch(expressions, rootObjects, pendingSocialPreloads, pendingHttpPreloads);
    }
  }

  public boolean needsViewer() {
    return needsViewer;
  }

  public boolean needsOwner() {
    return needsOwner;
  }

  /** Handle the os:PeopleRequest element */
  private SocialData createPeopleRequest(Element child) throws ELException {
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
  private SocialData createViewerRequest(Element child) throws ELException {
    return createPersonRequest(child, "@viewer");
  }

  /** Handle the os:OwnerRequest element */
  private SocialData createOwnerRequest(Element child) throws ELException {
    return createPersonRequest(child, "@owner");
  }

  private SocialData createPersonRequest(Element child, String userId) throws ELException {
    SocialData expression = new SocialData(child.getAttribute("key"), "people.get");

    expression.addProperty("userId", userId, JSONArray.class);
    updateUserState(userId);
    copyAttribute("fields", child, expression, JSONArray.class);

    return expression;
  }

  /** Handle the os:PersonAppDataRequest element */
  private SocialData createPersonAppDataRequest(Element child) throws ELException {
    SocialData expression = new SocialData(child.getAttribute("key"), "appdata.get");

    copyAttribute("groupId", child, expression, String.class);
    copyAttribute("userId", child, expression, JSONArray.class);
    updateUserArrayState("userId", child);
    copyAttribute("appId", child, expression, String.class);
    copyAttribute("fields", child, expression, JSONArray.class);

    return expression;
  }

  /** Handle the os:ActivitiesRequest element */
  private SocialData createActivityRequest(Element child) throws ELException {
    SocialData expression = new SocialData(child.getAttribute("key"), "activities.get");

    copyAttribute("groupId", child, expression, String.class);
    copyAttribute("userId", child, expression, JSONArray.class);
    updateUserArrayState("userId", child);
    copyAttribute("appId", child, expression, String.class);
    // TODO: SHINDIG-711 should be activityIds?
    copyAttribute("activityId", child, expression, JSONArray.class);
    copyAttribute("fields", child, expression, JSONArray.class);

    // TODO: add activity paging support

    return expression;
  }

  /** Handle the os:DataRequest element */
  private SocialData createDataRequest(Element child) throws ELException, SpecParserException {
    String method = child.getAttribute("method");
    if (method == null) {
      throw new SpecParserException("Missing @method attribute on os:DataRequest");
    }

    // TODO: should we support anything that doesn't end in .get?
    // i.e, should this be a whitelist not a blacklist?
    if (method.endsWith(".update")
        || method.endsWith(".create")
        || method.endsWith(".delete")) {
      throw new SpecParserException("Unsupported @method attribute \"" + method + "\" on os:DataRequest");
    }

    SocialData expression = new SocialData(child.getAttribute("key"), method);
    NamedNodeMap nodeMap = child.getAttributes();
    for (int i = 0; i < nodeMap.getLength(); i++) {
      Node attrNode = nodeMap.item(i);
      // Skip namespaced attributes
      if (attrNode.getNamespaceURI() != null) {
        continue;
      }

      String name = attrNode.getLocalName();
      // Skip the built-in names
      if ("method".equals(name) || "key".equals(name)) {
        continue;
      }

      String value = attrNode.getNodeValue();
      expression.addProperty(name, value, Object.class);
    }

    return expression;
  }

  /** Handle an os:HttpRequest element */
  private HttpData createHttpRequest(Element child, Uri base) throws ELException {
    HttpData data = new HttpData(child, base);
    // Update needsOwner and needsViewer
    if (data.authz != AuthType.NONE) {
      if (data.signOwner) {
        needsOwner = true;
      }

      if (data.signViewer) {
        needsViewer = true;
      }
    }

    return data;
  }

  private void copyAttribute(String name, Element element, SocialData expression, Class<?> type)
    throws ELException {
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
    private final AuthType authz;
    private final Uri base;
    private final String href;
    private final boolean signOwner;
    private final boolean signViewer;
    private final Map<String, String> attributes;

    private static final Set<String> KNOWN_ATTRIBUTES =
          ImmutableSet.of("authz", "href", "sign_owner", "sign_viewer");

    /**
     * Create an HttpData off an <os:makeRequest> element.
     */
    public HttpData(Element element, Uri base) throws ELException {
      this.base = base;

      this.authz = element.hasAttribute("authz") ?
          AuthType.parse(element.getAttribute("authz")) : AuthType.NONE;

      // TODO: Spec question;  should EL values be URL escaped?
      this.href = element.getAttribute("href");

      // TODO: Spec question;  should sign_* default to true?
      this.signOwner = booleanValue(element, "sign_owner", true);
      this.signViewer = booleanValue(element, "sign_viewer", true);

      // TODO: many of these attributes should not be EL enabled
      ImmutableMap.Builder<String, String> attributes = ImmutableMap.builder();
      for (int i = 0; i < element.getAttributes().getLength(); i++) {
        Node attr = element.getAttributes().item(i);
        if (!KNOWN_ATTRIBUTES.contains(attr.getNodeName())) {
          attributes.put(attr.getNodeName(), attr.getNodeValue());
        }
      }

      this.attributes = attributes.build();
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
     * @throws ELException if expression evaluation fails.
     */
    public RequestAuthenticationInfo evaluate(Expressions expressions, ELContext context)
        throws ELException {
      String hrefString = String.valueOf(expressions.parse(href, String.class)
          .getValue(context));
      final Uri evaluatedHref = base.resolve(Uri.parse(hrefString));

      final Map<String, String> evaluatedAttributes = Maps.newHashMap();
      for (Map.Entry<String, String> attr : attributes.entrySet()) {
        ValueExpression expression = expressions.parse(attr.getValue(), String.class);
        evaluatedAttributes.put(attr.getKey(),
            String.valueOf(expression.getValue(context)));
      }

      return new RequestAuthenticationInfo() {
        public Map<String, String> getAttributes() {
          return evaluatedAttributes;
        }

        public AuthType getAuthType() {
          return authz;
        }

        public Uri getHref() {
          return evaluatedHref;
        }

        public boolean isSignOwner() {
          return signOwner;
        }

        public boolean isSignViewer() {
          return signViewer;
        }
      };
    }

    /** Parse a boolean expression off an XML attribute. */
    private boolean booleanValue(Element element, String attrName,
        boolean defaultValue) {
      if (!element.hasAttribute(attrName)) {
        return defaultValue;
      }

      return "true".equalsIgnoreCase(element.getAttribute(attrName));
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

    public void addProperty(String name, String value, Class<?> type) throws ELException {
      properties.add(new Property(name, value, type));
    }

    /** Create the JSON request form for the social data */
    public JSONObject toJson(Expressions expressions, ELContext elContext) throws ELException {
      JSONObject object = new JSONObject();
      try {
        object.put("method", method);
        object.put("id", id);

        JSONObject params = new JSONObject();
        for (Property property : properties) {
          property.set(expressions, elContext, params);
        }
        object.put("params", params);
      } catch (JSONException je) {
        throw new ELException(je);
      }

      return object;
    }

    /** Single property for an expression */
    private static class Property {
      private final String name;
      private final String value;
      private final Class<?> type;

      public Property(String name, String value, Class<?> type) {
        this.name = name;
        this.value = value;
        this.type = type;
      }

      public void set(Expressions expressions, ELContext elContext, JSONObject object)
          throws ELException {
        ValueExpression expression = expressions.parse(value, type);
        Object value = expression.getValue(elContext);
        try {
          if (value != null) {
            object.put(name, value);
          }
        } catch (JSONException e) {
          throw new ELException("Error parsing property \"" + name + '\"', e);
        }
      }
    }
  }
}
