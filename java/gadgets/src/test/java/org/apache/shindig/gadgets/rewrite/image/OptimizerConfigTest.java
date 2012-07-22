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
package org.apache.shindig.gadgets.rewrite.image;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests for {@code OptimizerConfig}
 */
public class OptimizerConfigTest {

  @Test
  public void testForHighJpegCompression() {
    OptimizerConfig config = new OptimizerConfig(1024 * 1024, 256, true, 1.00f, 200, false, false);
    assertEquals(0.9f, config.getJpegCompression(), 0.0001);
  }

  @Test
  public void testForLowJpegCompression() {
    OptimizerConfig config = new OptimizerConfig(1024 * 1024, 256, true, 0.10f, 200, false, false);
    assertEquals(0.5f, config.getJpegCompression() , 0.0001);
  }

  @Test
  public void testForAcceptableJpegCompression() {
    OptimizerConfig config = new OptimizerConfig(1024 * 1024, 256, true, 0.85f, 200, false, false);
    assertEquals(0.85f, config.getJpegCompression(), 0.0001);
  }
}
