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

import org.apache.shindig.gadgets.Gadget;
import org.w3c.dom.Node;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * Context for processing a single template.
 */
public class TemplateContext {
  private final Map<String, ? extends Object> top;
  private final Gadget gadget;

  private Object cur = null;
  // TODO: support unique Id
  private Map<String, ? extends Object> context = ImmutableMap.of();
  private Map<String, Object> myMap = null;
  private Node templateRoot;
  private Map<Object, TemplateResource> resources = Maps.newLinkedHashMap();

  public TemplateContext(Gadget gadget, Map<String, ? extends Object> top) {
    this.gadget = gadget;
    this.top = top;
    this.cur = top;
  }

  public Map<String, ? extends Object> getTop() {
    return top;
  }

  public Object getCur() {
    return cur;
  }

  public Object setCur(Object data) {
    Object oldCur = cur;
    cur = data;
    return oldCur;
  }

  public Map<String, ? extends Object> getContext() {
    return context;
  }

  public Map<String, ? extends Object> setContext(Map<String, ? extends Object> newContext) {
    Map<String, ? extends Object> oldContext = context;
    context = newContext;
    return oldContext;
  }

  public Map<String, Object> setMy(Map<String, Object> myMap) {
    Map<String, Object> oldMy = this.myMap;
    this.myMap = myMap;
    return oldMy;
  }

  public Map<String, Object> getMy() {
    return myMap;
  }

  public Gadget getGadget() {
    return gadget;
  }

  public Node setTemplateRoot(Node root) {
    Node oldRoot = this.templateRoot;
    this.templateRoot = root;
    return oldRoot;
  }

  public Node getTemplateRoot() {
    return this.templateRoot;
  }

  public void addResource(Object key, TemplateResource resource) {
    if (!resources.containsKey(key)) {
      resources.put(key, resource);
    }
  }

  public Collection<TemplateResource> getResources() {
    return resources.values();
  }
}
