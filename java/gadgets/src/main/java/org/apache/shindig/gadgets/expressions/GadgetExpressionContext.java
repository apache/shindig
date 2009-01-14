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
package org.apache.shindig.gadgets.expressions;

import org.apache.shindig.gadgets.GadgetContext;

import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Implementation of ExpressionContext for gadgets.  Supports
 * two variables, "UserPrefs" and "ViewParams".
 */
public class GadgetExpressionContext implements ExpressionContext {
  private static final Logger log = Logger.getLogger(GadgetExpressionContext.class.getName());

  /** GadgetContext */
  private final GadgetContext context;
  
  /** Lazily evaluated view params */
  private JSONObject viewParams;

  public GadgetExpressionContext(GadgetContext context) {
    this.context = context;
  }

  public Object getVariable(String name) {
    if ("UserPrefs".equals(name)) {
      return context.getUserPrefs().getPrefs();
    } else if ("ViewParams".equals(name)) {
      return getViewParams();
    }
    
    return null;
  }

  /** Instantiate and return a JSONObject off of view params */
  private JSONObject getViewParams() {
    if (viewParams == null) {
      String params = context.getParameter("view-params");
      if (params == null || "".equals(params)) {
        viewParams = new JSONObject();
      } else {
        try {
          viewParams = new JSONObject(params);
        } catch (JSONException e) {
          log.warning("Could not parse " + params);
          viewParams = new JSONObject();
        }
      }
    }

    return viewParams;
  }

}
