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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.variables.Substitutions;

import org.w3c.dom.Element;

/**
 * Represents /ModulePrefs/Link elements.
 */
public class LinkSpec {
  private final Uri base;

  public LinkSpec(Element element, Uri base) throws SpecParserException {
    this.base = base;
    rel = XmlUtil.getAttribute(element, "rel");
    if (rel == null) {
      throw new SpecParserException("Link/@rel is required!");
    }
    href = XmlUtil.getUriAttribute(element, "href");
    if (href == null) {
      throw new SpecParserException("Link/@href is required!");
    }
    method = getMethodAttribute(element);
  }

  private LinkSpec(LinkSpec rhs, Substitutions substitutions) {
    rel = substitutions.substituteString(rhs.rel);
    base = rhs.base;
    href = base.resolve(substitutions.substituteUri(rhs.href));
    method = rhs.method;
  }

  /**
   * Link/@rel
   */
  private final String rel;
  public String getRel() {
    return rel;
  }

  /**
   * Link/@href
   */
  private final Uri href;
  public Uri getHref() {
    return href;
  }

  /**
   * Link/@method
   */
  private final String method;
  public String getMethod() {
    return method;
  }

  /**
   * Performs variable substitution on all visible elements.
   */
  public LinkSpec substitute(Substitutions substitutions) {
    return new LinkSpec(this, substitutions);
  }

  @Override
  public String toString() {
    String methodAttribute = (method != null) ? "method='" + method + "' " : "";
    return "<Link rel='" + rel + "' href='" + href.toString() + "' " + methodAttribute + "/>";
  }

  private String getMethodAttribute(Element element) {
    String method = XmlUtil.getAttribute(element, "method");
    return ("GET".equals(method) || "POST".equals(method)) ? method : "GET";
  }
}
