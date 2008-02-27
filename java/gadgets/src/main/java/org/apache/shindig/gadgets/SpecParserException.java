/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets;

/**
 * Exceptions for Gadget Spec parsing.
 */
public class SpecParserException extends GadgetException {
  /**
   * @param message
   */
  public SpecParserException(String message) {
    super(GadgetException.Code.MALFORMED_XML_DOCUMENT, message);
  }

  public SpecParserException(GadgetException.Code code) {
    super(code);
  }

  public SpecParserException(GadgetException.Code code, String message) {
    super(code, message);
  }
}
