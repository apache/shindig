/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.common.util;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Used to consume entire input streams and transform them into data buffers.
 * These are all blocking routines and should never be called from a thread
 * that will cause deadlock.
 */
@Deprecated
public class InputStreamConsumer {

  /**
   * Consumes the entire contents of the stream. Only safe to use if you are
   * sure that you're consuming a fixed-size buffer.
   * @param is
   * @return The contents of the stream.
   * @throws IOException on stream reading error.
   */
  public static byte[] readToByteArray(InputStream is) throws IOException {
    return IOUtils.toByteArray(is);
  }

  /**
   * Reads at most maxBytes bytes from the stream.
   * @param is
   * @param maxBytes
   * @return The bytes that were read
   * @throws IOException
   */
  public static byte[] readToByteArray(InputStream is, int maxBytes)
      throws IOException {
    return IOUtils.toByteArray(is);
  }

  /**
   * Loads content from the given input stream as a UTF-8-encoded string.
   * Use only when you're sure of the finite length of the input stream.
   * If you're not sure, use {@code readToString(InputStream, maxBytes)}.
   *
   * @param is
   * @return The contents of the stream.
   * @throws IOException on stream reading error.
   */
  public static String readToString(InputStream is) throws IOException {
    return IOUtils.toString(is, "UTF-8");
  }

  /**
   * Loads content from the given input stream as a UTF-8-encoded string.
   *
   * @param is
   * @return The contents of the stream.
   * @throws IOException on stream reading error.
   */
  public static String readToString(InputStream is, int maxBytes)
      throws IOException {
    return IOUtils.toString(is, "UTF-8");
  }

  /**
   * Consumes all of is and sends it to os. This is not the same as
   * Piped Input / Output streams because it reads the entire input first.
   * This means that you won't get deadlock, but it also means that this is
   * not necessarily suitable for normal piping tasks. Use a piped stream for
   * that sort of work.
   *
   * @param is
   * @param os
   * @throws IOException
   */
  public static void pipe(InputStream is, OutputStream os) throws IOException {
    IOUtils.copy(is, os);
  }
}
