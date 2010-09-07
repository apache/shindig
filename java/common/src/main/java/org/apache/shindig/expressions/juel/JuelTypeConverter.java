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
package org.apache.shindig.expressions.juel;

import javax.el.ELException;

import org.apache.shindig.expressions.ShindigTypeConverter;

import de.odysseus.el.misc.TypeConverter;

/**
 * A converter used by Juel
 * @since 2.0.0
 */
public class JuelTypeConverter extends ShindigTypeConverter implements
    TypeConverter {

  private static final long serialVersionUID = -4382092735987940726L;

  @Override
  public <T> T convert(Object obj, Class<T> type) throws ELException {
    T retValue = super.convert(obj, type);
    if (retValue == null) {
      retValue = TypeConverter.DEFAULT.convert(obj, type);
    }

    return retValue;
  }

}
