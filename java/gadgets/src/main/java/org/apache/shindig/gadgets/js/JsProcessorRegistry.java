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
package org.apache.shindig.gadgets.js;

import com.google.inject.ImplementedBy;

/**
 * A class to run a series of processing steps on a JS response.
 *
 * The way the processing steps are registered is implementation-dependent.
 */
@ImplementedBy(DefaultJsProcessorRegistry.class)
public interface JsProcessorRegistry {

  /**
   * Executes the processing steps.
   *
   * @param jsRequest The JS request that originated this execution.
   * @param response A builder for the JS response.
   * @throws JsException If any of the steps resulted in an error.
   */
  void process(JsRequest jsRequest, JsResponseBuilder response)
      throws JsException;
}
