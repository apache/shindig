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

import org.apache.commons.io.IOUtils;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.junit.Assert;

import java.io.IOException;

/**
 * Test BasicOptimizer
 */
public abstract class BaseOptimizerTest extends Assert {

  protected HttpResponse createResponse(String resource, String mimeType) throws IOException {
    byte[] bytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream(resource));
    return new HttpResponseBuilder().addHeader("Content-Type", mimeType)
            .setResponse(bytes).create();
  }

  protected HttpResponseBuilder createResponseBuilder(String resource, String mimeType)
      throws IOException {
    byte[] bytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream(resource));
    return new HttpResponseBuilder().addHeader("Content-Type", mimeType)
        .setResponse(bytes);
  }
}
