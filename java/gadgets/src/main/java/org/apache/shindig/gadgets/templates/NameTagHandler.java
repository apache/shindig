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

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.inject.Inject;

/**
 * TagHandler for the &lt;os:Name person="..."/&gt; tag.
 */
public class NameTagHandler extends AbstractTagHandler {

  static final String TAG_NAME = "Name";
  static final String PERSON_ATTR = "person";

  @Inject
  public NameTagHandler() {
    super(TagHandler.OPENSOCIAL_NAMESPACE, TAG_NAME);
  }

  public void process(Node result, Element tag, TemplateProcessor processor) {

    JSONObject person = getValueFromTag(tag, PERSON_ATTR, processor, JSONObject.class);
    if (person == null) {
      return;
    }
    JSONObject name = person.optJSONObject("name");
    if (name == null) {
      return;
    }
    String formatted = name.optString("formatted");
    if (formatted.length() == 0) {
      formatted = name.optString("givenName") + " " + name.optString("familyName");
    }

    Document doc = result.getOwnerDocument();
    Element root = doc.createElement("b");

    appendTextNode(root, formatted);
  }
}
