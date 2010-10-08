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
package org.apache.shindig.protocol;


import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

/**
 * Utility class for common API call preconditions
 */
public final class HandlerPreconditions {

  private HandlerPreconditions() {}

  public static void requireNotEmpty(Collection<?> coll, String message)
      throws ProtocolException {
    if (coll.isEmpty()) {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, message);
    }
  }

  public static void requireEmpty(Collection<?> coll, String message) throws ProtocolException {
    if (!coll.isEmpty()) {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, message);
    }
  }

  public static void requireSingular(Collection<?> coll, String message)
      throws ProtocolException {
    if (coll.size() != 1) {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, message);
    }
  }

  public static void requirePlural(Collection<?> coll, String message) throws ProtocolException {
    if (coll.size() <= 1) {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, message);
    }
  }
}
