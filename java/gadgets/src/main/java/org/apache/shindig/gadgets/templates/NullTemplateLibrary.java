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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.templates.tags.DefaultTagRegistry;
import org.apache.shindig.gadgets.templates.tags.TagHandler;

import com.google.common.collect.ImmutableSet;

/**
 * Null object implementation of TemplateLibrary.
 */
public final class NullTemplateLibrary implements TemplateLibrary {
  public static final TemplateLibrary INSTANCE = new NullTemplateLibrary();

  private final TagRegistry registry = new DefaultTagRegistry(ImmutableSet.<TagHandler>of());

  private NullTemplateLibrary() {
  }

  public Uri getLibraryUri() {
    return null;
  }

  public TagRegistry getTagRegistry() {
    return registry;
  }

  public boolean isSafe() {
    return false;
  }

  public String serialize() {
    return null;
  }
}
