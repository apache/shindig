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

package org.apache.shindig.gadgets.render;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.rewrite.GadgetRewriter;
import org.apache.shindig.gadgets.servlet.HtmlAccelServlet;

import java.util.List;

/**
 * Class to provide list of rewriters according to gadget request.
 * Provide different list of rewriters fro html accelerate request
 *
 */
public class GadgetRewritersProvider {

  private final List<GadgetRewriter> renderRewriters;
  private final List<GadgetRewriter> accelRewriters;
  
  @Inject
  public GadgetRewritersProvider(
      @Named("shindig.rewriters.gadget") List<GadgetRewriter> renderRewriters,
      @Named("shindig.rewriters.accelerate") List<GadgetRewriter> accelRewriters) {
    this.renderRewriters = renderRewriters;
    this.accelRewriters = accelRewriters;
  }

  public List<GadgetRewriter> getRewriters(GadgetContext context) {
    if (context.getParameter(HtmlAccelServlet.ACCEL_GADGET_PARAM_NAME) == 
      HtmlAccelServlet.ACCEL_GADGET_PARAM_VALUE) {
      return accelRewriters;
    }
    return renderRewriters;
  }
  
}
