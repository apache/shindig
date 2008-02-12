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
package org.apache.shindig.gadgets;

import java.net.URL;
import java.util.Collection;
import java.util.Map;

/**
 * An abstract representation of a signing token.
 * Use in conjuction with @code GadgetSigner.
 */
public interface GadgetToken {

  /**
   * Serializes the token into a string. This can be the exact same as
   * toString; using a different name here is only to force interface
   * compliance.
   *
   * @return A string representation of the token.
   */
  public String toSerialForm();


  /**
   * Sign a URL using this token
   * @param uri The URL to sign
   * @param httpMethod The HTTP method used
   * @param parameters associated with the signing request
   * @return The signed URL
   * @throws GadgetException
   */
  public URL signUrl(URL uri, String httpMethod,
      Map<String, Collection<String>> parameters) throws GadgetException;
}
