/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shindig.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * Used to consume an entire input stream. Don't use this for network
 * streams or any other stream that doesn't have a known length. This is
 * intended for reading resources from jars and the local file system only.
 */
public class InputStreamConsumer {

  /**
   * Loads content and returns it as a raw byte array.
   * @param is
   * @return The contents of the stream.
   * @throws IOException on stream reading error.
   */
  public static byte[] readToByteArray(InputStream is) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buf = new byte[8192];
    int read = 0;

    while ((read = is.read(buf)) > 0) {
      baos.write(buf, 0, read);
    }

    return baos.toByteArray();
  }

  /**
   * Loads content from the given input stream as a UTF-8-encoded string.
   *
   * @param is
   * @return The contents of the stream.
   * @throws IOException on stream reading error.
   */
  public static String readToString(InputStream is) throws IOException {
    byte[] bytes = readToByteArray(is);
    try {
      return new String(bytes, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // UTF-8 is required by the Java spec.
      throw new RuntimeException("UTF-8 not supported!", e);
    }
  }
}
