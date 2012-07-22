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
package org.apache.shindig.social.core.util.atom;

import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.google.common.base.Preconditions;

/**
 * Serializes attributes correctly.
 */
public class AtomAttributeConverter implements SingleValueConverter {

  /**
   * {@inheritDoc}
   * @see com.thoughtworks.xstream.converters.SingleValueConverter#fromString(java.lang.String)
   */
  public Object fromString(String value) {
    return new AtomAttribute(Preconditions.checkNotNull(value));
  }

  /**
   * {@inheritDoc}
   * @see com.thoughtworks.xstream.converters.SingleValueConverter#toString(java.lang.Object)
   */
  public String toString(Object object) {
    return object.toString();
  }

  /**
   * {@inheritDoc}
   * @see com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java.lang.Class)
   */
  @SuppressWarnings("unchecked")
  public boolean canConvert(Class clazz) {
    return AtomAttribute.class.equals(clazz);
  }


}
