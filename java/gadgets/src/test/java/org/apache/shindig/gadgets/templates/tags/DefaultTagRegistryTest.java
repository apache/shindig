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
package org.apache.shindig.gadgets.templates.tags;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.templates.TagRegistry;
import org.apache.shindig.gadgets.templates.TemplateProcessor;
import org.apache.shindig.gadgets.templates.tags.AbstractTagHandler;
import org.apache.shindig.gadgets.templates.tags.DefaultTagRegistry;
import org.apache.shindig.gadgets.templates.tags.TagHandler;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.common.collect.ImmutableSet;

public class DefaultTagRegistryTest {
  public static final String TEST_NAMESPACE = "#test";
  public static final String TEST_NAME = "Tag";
  private TagHandler tag;
  private DefaultTagRegistry registry;

  @Before
  public void setUp() {
    tag = new AbstractTagHandler(TEST_NAMESPACE, TEST_NAME) {
      public void process(Node result, Element tag, TemplateProcessor processor) {
      }
    };

    registry = new DefaultTagRegistry(ImmutableSet.of(tag));
  }

  @Test
  public void getHandlerForWithElement() {
    Element el = XmlUtil.parseSilent("<Tag xmlns='#test'/>");
    assertSame(tag, registry.getHandlerFor(el));
  }

  @Test
  public void getHandlerForUsesNamespace() {
    Element el = XmlUtil.parseSilent("<Tag xmlns='#nottest'/>");
    assertNull(registry.getHandlerFor(el));
  }

  @Test
  public void getHandlerIsCaseSensitive() {
    Element el = XmlUtil.parseSilent("<tag xmlns='#test'/>");
    assertNull(registry.getHandlerFor(el));
  }

  @Test
  public void getHandlerForWithNSName() {
    TagRegistry.NSName nsName = new TagRegistry.NSName(TEST_NAMESPACE, TEST_NAME);
    assertSame(tag, registry.getHandlerFor(nsName));
  }
}
