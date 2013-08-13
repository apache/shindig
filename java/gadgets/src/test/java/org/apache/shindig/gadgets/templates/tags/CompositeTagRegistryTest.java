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

import org.apache.shindig.gadgets.templates.TagRegistry;
import org.apache.shindig.gadgets.templates.TemplateProcessor;
import org.apache.shindig.gadgets.templates.tags.AbstractTagHandler;
import org.apache.shindig.gadgets.templates.tags.CompositeTagRegistry;
import org.apache.shindig.gadgets.templates.tags.DefaultTagRegistry;
import org.apache.shindig.gadgets.templates.tags.TagHandler;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class CompositeTagRegistryTest {
  public static final String TEST_NAMESPACE = "#test";
  private TagHandler fooTag;
  private TagHandler fooTag2;
  private TagHandler barTag;
  private CompositeTagRegistry registry;

  @Before
  public void setUp() {
    fooTag = createTagHandler("foo");
    fooTag2 = createTagHandler("foo");
    barTag = createTagHandler("bar");

    TagRegistry first = new DefaultTagRegistry(ImmutableSet.of(fooTag, barTag));
    TagRegistry second = new DefaultTagRegistry(ImmutableSet.of(fooTag2));

    registry = new CompositeTagRegistry(ImmutableList.of(first, second));
  }

  @Test
  public void firstRegistryWins() {
    TagRegistry.NSName foo = new TagRegistry.NSName(TEST_NAMESPACE, "foo");
    assertSame(fooTag, registry.getHandlerFor(foo));
  }

  @Test
  public void secondRegistryUsed() {
    TagRegistry.NSName bar = new TagRegistry.NSName(TEST_NAMESPACE, "bar");
    assertSame(barTag, registry.getHandlerFor(bar));
  }

  @Test
  public void unknownNamesReturnNull() {
    TagRegistry.NSName baz = new TagRegistry.NSName(TEST_NAMESPACE, "baz");
    assertNull(registry.getHandlerFor(baz));
  }

  private TagHandler createTagHandler(String name) {
    return new AbstractTagHandler(TEST_NAMESPACE, name) {
      public void process(Node result, Element tag, TemplateProcessor processor) {
      }
    };

  }
}
