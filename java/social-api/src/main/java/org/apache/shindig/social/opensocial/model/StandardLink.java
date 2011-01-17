/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.social.opensocial.model;

import org.apache.shindig.protocol.model.Exportablebean;
import org.apache.shindig.social.core.model.StandardLinkImpl;

import com.google.inject.ImplementedBy;

/*
 * TODO: comment this class.
 */
@ImplementedBy(StandardLinkImpl.class)
@Exportablebean
public interface StandardLink {

  /*
   * Fields that represent JSON elements for an activity entry.
   */
  public static enum Field {
    HREF("href"),
    INLINE("inline"),
    TYPE("type");

    /*
     * The name of the JSON element.
     */
    private final String jsonString;

    /*
     * Constructs the field base for the JSON element.
     * 
     * @param jsonString the name of the element
     */
    private Field(String jsonString) {
      this.jsonString = jsonString;
    }

    /*
     * Returns the name of the JSON element.
     * 
     * @return String the name of the JSON element
     */
    public String toString() {
      return jsonString;
    }
  }

  String getHref();

  void setHref(String href);
  
  String getInline();

  void setInline(String inline);

  String getType();

  void setType(String type);
}
