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
package org.apache.shindig.social.core.util.atom;

/**
 * represents an atom:link element.
 */
public class AtomLink {

  private String href;
  private String rel;
  private String type;
  private String title;

  /**
   * Construct a new AtomLink
   * @param rel a value for the rel attribute
   * @param href a value for the href attribute
   */
  public AtomLink(String rel, String href) {
    this.rel = rel;
    this.href = href;
  }

  /**
   * @return the link href
   */
  public String getHref() {
    return href;
  }

  /**
   * @return the rel
   */
  public String getRel() {
    return rel;
  }

  public String getType() {
    return type;
  }

  public String getTitle() {
    return title;
  }
}
