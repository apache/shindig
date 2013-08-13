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
package org.apache.shindig.gadgets.templates;

import org.apache.shindig.gadgets.templates.tags.DefaultTagRegistry;
import org.apache.shindig.gadgets.templates.tags.TagHandler;
import org.w3c.dom.Element;

import com.google.inject.ImplementedBy;

/**
 * A registry of custom tag handlers, keyed by a combination of namespace URL
 * and tag name.
 */
@ImplementedBy(DefaultTagRegistry.class)
public interface TagRegistry {

  public TagHandler getHandlerFor(Element element);

  public TagHandler getHandlerFor(NSName name);

  /**
   * A namespace-name pair used as Hash key for handler lookups.
   */
  public static class NSName {
    private final String namespaceUri;
    private final String localName;
    private final int hash;

    public NSName(String namespaceUri, String localName) {
      this.namespaceUri = namespaceUri;
      this.localName = localName;
      hash = (namespaceUri.hashCode() * 37) ^ localName.hashCode();
    }

    @Override
    public String toString() {
      return namespaceUri + ':' + localName;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) { return true; }
      if (!(obj instanceof NSName)) { return false; }
      NSName nsn = (NSName) obj;
      return namespaceUri.equals(nsn.namespaceUri) && localName.equals(nsn.localName);
    }

    @Override
    public int hashCode() {
      return hash;
    }
  }
}
