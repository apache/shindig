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
package org.apache.shindig.gadgets.rewrite;

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;

/**
 * Utility rewriter for testing.
 */
public class CaptureRewriter implements ResponseRewriter, GadgetRewriter {
  private boolean rewroteView = false;
  private boolean rewroteResponse = false;

  public boolean responseWasRewritten() {
    return rewroteResponse;
  }

  public void rewrite(Gadget gadget, MutableContent content) {
    rewroteView = true;
  }

  public boolean viewWasRewritten() {
    return rewroteView;
  }

  public void rewrite(HttpRequest request, HttpResponseBuilder response, Gadget gadget) {
    rewroteResponse = true;
  }
}
