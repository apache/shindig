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

import org.apache.shindig.gadgets.templates.TagRegistry;

import java.util.Collection;

/**
 * Tag registry that supports multiple tags.
 */
public class CompositeTagRegistry extends AbstractTagRegistry {
  private final Collection<? extends TagRegistry> registries;

  public CompositeTagRegistry(Collection<? extends TagRegistry> registries) {
    this.registries = registries;
  }

  public TagHandler getHandlerFor(NSName name) {
    TagHandler handler;
    for (TagRegistry registry : registries) {
      handler = registry.getHandlerFor(name);
      if (handler != null) {
        return handler;
      }
    }
    return null;
  }
}
