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
package org.apache.shindig.protocol;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.protocol.conversion.BeanConverter;

import java.io.Reader;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Interface exposed by a REST handler
 */
public interface RestHandler {

  /**
   * Handle the request and return a Future from which the response object
   * can be retrieved
   */
  Future<?> execute(Map<String, String[]> parameters, Reader body,
                    SecurityToken token, BeanConverter converter);
}
