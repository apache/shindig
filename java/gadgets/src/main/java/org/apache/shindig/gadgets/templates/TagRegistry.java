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

import org.w3c.dom.Element;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

/**
 * A registry of custom tag handlers, keyed by a combination of namespace URL
 * and tag name.
 */
public class TagRegistry {

  private final TagRegistry parentRegistry;
  private final Map<NSName, TagHandler> handlers = Maps.newHashMap();

  @Inject
  public TagRegistry(Set<TagHandler> handlers) {
    this(handlers, null);
  }
  
  /**
   * Create a new TagRegistry containing all the existing handlers, with
   * new tags added.  The handlers from the existing registry will take
   * precedence over any newly registered tags.
   */
  public TagRegistry addHandlers(Set<? extends TagHandler> handlers) {
    if (handlers.isEmpty()) {
      return this;
    }

    return new TagRegistry(handlers, this);
  }
  
  private TagRegistry(Set<? extends TagHandler> handlers, TagRegistry parentRegistry) {
    this.parentRegistry = parentRegistry;
    for (TagHandler handler : handlers) {
      this.handlers.put(new NSName(handler.getNamespaceUri(), handler.getTagName()), handler);
    }    
  }
  
  /**
   * Look up a tag handler for an element.
   */
  public TagHandler getHandlerFor(Element element) {
    if (element.getNamespaceURI() == null) {
      return null;
    }
    
    return getHandlerFor(new NSName(element.getNamespaceURI(),  element.getLocalName()));
  }
  
  private TagHandler getHandlerFor(NSName name) {
    // Check the parent registry first;  built-in tags cannot be overridden
    TagHandler handler = null;
    if (parentRegistry != null) {
      handler = parentRegistry.getHandlerFor(name);
    }
    
    if (handler == null) {
      handler =  handlers.get(name);
    }

    return handler;
  }
  
  /**
   * A namespace-name pair used as Hash key for handler lookups.
   */
  private static class NSName {
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
      return namespaceUri + ":" + localName;
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
