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
package org.apache.shindig.social.core.util;

import com.google.inject.Inject;

import org.apache.shindig.protocol.ContentTypes;
import org.apache.shindig.protocol.conversion.BeanXStreamConverter;
import org.apache.shindig.protocol.conversion.xstream.XStreamConfiguration;
import org.apache.shindig.social.core.util.atom.AtomFeed;

/**
 * Converts output to atom.
 * TODO: Move to common once atom binding can be decoupled form social code
 */
public class BeanXStreamAtomConverter extends BeanXStreamConverter {

  /**
   * @param configuration
   */
  @Inject
  public BeanXStreamAtomConverter(XStreamConfiguration configuration) {
    super(configuration);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.shindig.protocol.conversion.BeanXStreamConverter#getContentType()
   */
  @Override
  public String getContentType() {
    return ContentTypes.OUTPUT_ATOM_CONTENT_TYPE;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.shindig.protocol.conversion.BeanXStreamConverter#convertToString(java.lang.Object)
   */
  @Override
  public String convertToString(Object obj) {
    writerStack.reset();
    AtomFeed af = new AtomFeed(obj);
    XStreamConfiguration.ConverterConfig cc = converterMap.get(XStreamConfiguration.ConverterSet.DEFAULT);
    cc.mapper.setBaseObject(af); // thread safe method

    return cc.xstream.toXML(af);
  }
}
