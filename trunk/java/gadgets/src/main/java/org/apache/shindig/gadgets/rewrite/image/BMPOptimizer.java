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
import org.apache.shindig.gadgets.http.HttpResponseBuilder;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * Optimize BMP by converting to PNG
 */
public class BMPOptimizer extends PNGOptimizer {

  public static BufferedImage readBmp(InputStream is)
      throws ImageReadException, IOException {
    return Sanselan.getBufferedImage(is);
  }

  public BMPOptimizer(OptimizerConfig config, HttpResponseBuilder response) {
    super(config, response);
    ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
    outputter = new ImageIOOutputter(writer, null);
  }

  @Override
  protected String getOriginalContentType() {
    return "image/bmp";
  }

  @Override
  protected String getOriginalFormatName() {
    return "bmp";
  }
}
