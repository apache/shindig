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

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.inject.Inject;

public class IfTagHandler extends AbstractTagHandler {

  static final String TAG_IF = "If";
  static final String CONDITION_ATTR = "condition";

  @Inject
  public IfTagHandler() {
    super(TagHandler.OPENSOCIAL_NAMESPACE, TAG_IF);
  }

  public void process(Node result, Element tag, TemplateProcessor processor) {
    Boolean condition = getValueFromTag(tag, CONDITION_ATTR, processor, Boolean.class);
    if (condition == null || !condition.booleanValue()) {
      return;
    }

    // Condition succeeded, process all child nodes 
    processor.processChildNodes(result, tag);
  }
}
