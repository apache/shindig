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

import com.google.inject.ImplementedBy;

/**
 * Called by the handler dispatcher prior to executing a handler. Used to allow
 * containers to implement cross-cutting features such as request logging.
 */
@ImplementedBy(HandlerExecutionListener.NoOpHandlerExecutionListener.class)
public interface HandlerExecutionListener {

  /**
   * Called prior to executing a REST or RPC handler
   * @param service Name of the service being called
   * @param operation Name of operation being called
   * @param request being executed
   */
  void executing(String service, String operation, RequestItem request);

  /**
   * Default no-op implementation
   */
  public static class NoOpHandlerExecutionListener implements HandlerExecutionListener {

    public void executing(String service, String operation, RequestItem request) {
      // No-op
    }
  }
}

