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

/**
 * Encapsulation of a single resource imported by a library for template
 * execution.
 */
public final class TemplateResource {
  private final String content;
  private final Type type;
  private final boolean isSafe;

  public enum Type { JAVASCRIPT, STYLE }

    /**
   * Create a Javascript resource.
   * @param javascript the script content
   * @param library the library that is the source of the script
   */
  public static TemplateResource newJavascriptResource(String javascript, TemplateLibrary library) {
    return new TemplateResource(javascript, Type.JAVASCRIPT, library.isSafe());
  }

  /**
   * Create a CSS resource.
   * @param style the CSS content
   * @param library the library that is the source of the content
   */
  public static TemplateResource newStyleResource(String style, TemplateLibrary library) {
    return new TemplateResource(style, Type.STYLE, library.isSafe());
  }

  private TemplateResource(String content, Type type, boolean isSafe) {
    this.content = content;
    this.type = type;
    this.isSafe = isSafe;
  }

  public String getContent() {
    return content;
  }

  public Type getType() {
    return type;
  }

  public boolean isSafe() {
    return isSafe;
  }

  @Override
  public String toString() {
    return "<" + type + '>' + content + "</" + type + '>';
  }
}
