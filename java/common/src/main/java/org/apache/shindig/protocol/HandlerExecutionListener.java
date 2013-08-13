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

import java.util.logging.Logger;

/**
 * Called by the handler dispatcher prior to executing a handler. Used to allow
 * containers to implement cross-cutting features such as request logging.
 */
@ImplementedBy(HandlerExecutionListener.NoOpHandler.class)
public interface HandlerExecutionListener {

  /**
   * Called prior to executing a REST or RPC handler
   * @param service Name of the service being called
   * @param operation Name of operation being called
   * @param request being executed
   */
  void executing(String service, String operation, RequestItem request);
  void executed(String service, String operation, RequestItem request);

  /**
   * Default no-op implementation
   */
  public static class NoOpHandler implements HandlerExecutionListener {

    public void executing(String service, String operation, RequestItem request) {
      // No-op
    }
    public void executed(String service, String operation, RequestItem request) {
      // No-op
    }
  }

  /**
   * A simple implementation that logs the start/stop times of requests
   *
   * You can configure this for use by adding a binding in your Guice Module like this:
   *   bind(HandlerExecutionListener.class).to(HandlerExecutionListener.LoggingHandler.class);
   */

  public static class LoggingHandler implements HandlerExecutionListener {
    public static final Logger LOG = Logger.getLogger(HandlerExecutionListener.class.toString());

    public void executing(String service, String operation, RequestItem request) {
      LOG.info("start - " + service + ' ' + operation);
    }
    public void executed(String service, String operation, RequestItem request) {
      LOG.info("  end - " + service + ' ' + operation);
    }
  }
}

