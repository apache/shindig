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

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.el.ELResolver;

import com.google.common.collect.Maps;

/**
 * Fake implementation of TemplateProcessor for writing TagHandler tests.
 */
public class FakeTemplateProcessor implements TemplateProcessor {
  public Map<String, ? extends Object> expressionResults = Maps.newHashMap();
  public TemplateContext context;

  public final <T extends Object> T evaluate(String expression, Class<T> type, T defaultValue) {
    // Some quick-and-dirty mocking:  put a List in the map, and
    // you get one result per-entry
    Object result = expressionResults.get(expression);
    if (result instanceof List<?> && !type.isAssignableFrom(List.class)) {
      result = ((List<?>) result).remove(0);
    }
    return type.cast(result);
  }

  public TemplateContext getTemplateContext() {
    return context;
  }

  public DocumentFragment processTemplate(Element template,
      TemplateContext templateContext, ELResolver globals, TagRegistry registry) {
    throw new UnsupportedOperationException();
  }

  public void processChildNodes(Node result, Node source) {
    throw new UnsupportedOperationException();
  }

  public final void processRepeat(Node result, Element element, Iterable<?> dataList, Runnable onEachLoop) {
    // for (Object data : dataList) produces an unused variable warning
    Iterator<?> iterator = dataList.iterator();
    while (iterator.hasNext()) {
      iterator.next();
      onEachLoop.run();
    }
  }
}
