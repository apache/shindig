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
package org.apache.shindig.common;

import junit.framework.Test;
import junit.framework.TestSuite;

import junitx.util.DirectorySuiteBuilder;
import junitx.util.SimpleTestFilter;

/**
 * Run all gadgets tests.
 */
public class AllTests extends TestSuite {

  public static Test suite() throws Exception {
    DirectorySuiteBuilder builder = new DirectorySuiteBuilder(
      new SimpleTestFilter());
    return builder.suite("target/test-classes");
  }
}
