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
package org.apache.shindig.common.testing;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ExecutorService used for testing. Executes all tasks immediately.
 */
public class ImmediateExecutorService extends AbstractExecutorService {
    private boolean shutdown;

    public void execute(Runnable command) {
      command.run();
    }

    public boolean isTerminated() {
      return shutdown;
    }

    public boolean isShutdown() {
      return shutdown;
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) {
      return true;
    }

    public void shutdown() {
      shutdown = true;
    }

    public List<Runnable> shutdownNow() {
      shutdown();
      return Collections.emptyList();
    }
}
