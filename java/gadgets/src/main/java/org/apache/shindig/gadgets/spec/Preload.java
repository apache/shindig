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

import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.Substitutions;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents an addressable piece of content that can be preloaded by the server
 * to satisfy makeRequest calls
 */
public class Preload {

  public static final String AUTHZ_ATTR = "authz";

  /**
   * Preload@href
   */
  private final URI href;
  public URI getHref() {
    return href;
  }

  /**
   * Preload@auth
   */
  private final Auth auth;
  public Auth getAuth() {
    return auth;
  }

  /**
   * Preload@sign_viewer
   */
  private final boolean signViewer;
  public boolean isSignViewer() {
    return signViewer;
  }

  /**
   * Preload@sign_owner
   */
  private final boolean signOwner;
  public boolean isSignOwner() {
    return signOwner;
  }

  /**
   * Prelaod@views
   */
  private final Set<String> views;
  public Set<String> getViews() {
    return views;
  }

  public Preload substitute(Substitutions substituter) {
    return new Preload(this, substituter);
  }

  /**
   * Produces an xml representation of the Preload.
   */
  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("<Preload href='").append(href).append('\'')
       .append(" authz='").append(auth.toString().toLowerCase()).append('\'')
       .append(" sign_owner='").append(signOwner).append('\'')
       .append(" sign_viewer='").append(signViewer).append('\'')
       .append(" views='").append(StringUtils.join(views, ',')).append("'/>");
    return buf.toString();
  }
  /**
   * Creates a new Preload from an xml node.
   *
   * @param preload The Preload to create
   * @throws SpecParserException When the href is not specified
   */
  public Preload(Element preload) throws SpecParserException {
    signOwner = XmlUtil.getBoolAttribute(preload, "sign_owner", true);
    signViewer = XmlUtil.getBoolAttribute(preload, "sign_viewer", true);
    href = XmlUtil.getUriAttribute(preload, "href");
    if (href == null) {
      throw new SpecParserException("Preload/@href is missing or invalid.");
    }

    // Record all the associated views
    String viewNames = XmlUtil.getAttribute(preload, "views", "");
    Set<String> views = new HashSet<String>();
    for (String s: viewNames.split(",")) {
      s = s.trim();
      if (s.length() > 0) {
        views.add(s.trim());
      }
    }
    this.views = Collections.unmodifiableSet(views);

    auth = Auth.parse(XmlUtil.getAttribute(preload, AUTHZ_ATTR));
  }

  private Preload(Preload preload, Substitutions substituter) {
    signOwner = preload.signOwner;
    signViewer = preload.signViewer;
    views = preload.views;
    auth = preload.auth;
    href = substituter.substituteUri(null, preload.href);
  }
}