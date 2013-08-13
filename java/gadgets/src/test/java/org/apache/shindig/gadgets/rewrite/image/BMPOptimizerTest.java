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

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Test BMPOptimizer
 */
public class BMPOptimizerTest extends BaseOptimizerTest {

  static final Logger log = Logger.getLogger(BMPOptimizerTest.class.getName());

  @Test
  public void testSimpleImage() throws Exception {
    HttpResponse resp =
        createResponse("org/apache/shindig/gadgets/rewrite/image/simple.bmp", "image/bmp");
    HttpResponse rewritten = rewrite(resp);
    assertTrue(rewritten.getContentLength() < resp.getContentLength());
    assertEquals("image/png", rewritten.getHeader("Content-Type"));
  }

  @Test
  @Ignore("Kills some VMs")
  public void testEvilImages() throws Exception {
    try {
      HttpResponse resp =
          createResponse("org/apache/shindig/gadgets/rewrite/image/evil.bmp", "image/bmp");
      rewrite(resp);
      fail("Evil image should not be readable");
    } catch (RuntimeException re) {
      log.log(Level.INFO, "Good failure while reading evil image", re);
    }
  }

  @Test
  public void testEvilImage2() throws Exception {
    try {
      HttpResponse resp =
          createResponse("org/apache/shindig/gadgets/rewrite/image/evil2.bmp", "image/bmp");
      rewrite(resp);
      fail("Evil image should not be readable");
    } catch (RuntimeException re) {
      log.log(Level.INFO, "Good failure while reading evil image", re);
    }
  }

  protected HttpResponse rewrite(HttpResponse original)
       throws IOException, ImageReadException {
    HttpResponseBuilder builder = new HttpResponseBuilder(original);
    new BMPOptimizer(new OptimizerConfig(), builder).rewrite(
         Sanselan.getBufferedImage(original.getResponse()));
    return builder.create();
   }
}
