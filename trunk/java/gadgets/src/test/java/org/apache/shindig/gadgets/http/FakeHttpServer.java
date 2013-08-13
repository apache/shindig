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
package org.apache.shindig.gadgets.http;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;

/**
 * A simple HTTP server to test against.
 */
public abstract class FakeHttpServer {
  protected Server server = null;
  protected Context context = null;

  public void start(int port) throws Exception {
    server = new Server(port);
    context = new Context(server, "/", Context.SESSIONS);
    addServlets();
    server.start();
  }

  /** Override to add your servlets */
  protected abstract void addServlets() throws Exception;

  public void stop() throws Exception {
    server.stop();
  }
}
