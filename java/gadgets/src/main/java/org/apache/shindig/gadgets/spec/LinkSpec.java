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

import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.Substitutions;

import org.w3c.dom.Element;

import java.net.URI;

/**
 * Represents /ModulePrefs/Link elements.
 */
public class LinkSpec {

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
  private final URI href;
  public URI getHref() {
    return href;
  }

  /**
   * Performs variable substitution on all visible elements.
   */
  public LinkSpec substitute(Substitutions substitutions) {
    return new LinkSpec(this, substitutions);
  }

  @Override
  public String toString() {
    return "<Link rel='" + rel + "' href='" + href.toString() + "'/>";
  }

  public LinkSpec(Element element) throws SpecParserException {
    rel = XmlUtil.getAttribute(element, "rel");
    if (rel == null) {
      throw new SpecParserException("Link/@rel is required!");
    }
    href = XmlUtil.getUriAttribute(element, "href");
    if (href == null) {
      throw new SpecParserException("Link/@href is required!");
    }
  }

  private LinkSpec(LinkSpec rhs, Substitutions substitutions) {
    rel = substitutions.substituteString(null, rhs.rel);
    href = substitutions.substituteUri(null, rhs.href);
  }
}
