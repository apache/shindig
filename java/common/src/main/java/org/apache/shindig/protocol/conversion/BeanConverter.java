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
package org.apache.shindig.protocol.conversion;

import java.io.IOException;

/**
 * Interface for bean conversion classes
 */
public interface BeanConverter {
  <T> T convertToObject(String string, Class<T> className);

  String convertToString(Object pojo);

  /** @return the content type of the converted data */
  String getContentType();

  /**
   * Serialize object to a buffer. Useful for high performance output.
   * @param buf Buffer to append to
   * @param pojo Object to serialize
   * @throws IOException If {@link Appendable#append(char)} throws an exception.
   */
  void append(Appendable buf, Object pojo) throws IOException;
}
