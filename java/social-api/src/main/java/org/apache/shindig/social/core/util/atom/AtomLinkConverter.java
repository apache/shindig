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

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.google.common.base.Preconditions;

/**
 * Serializes links for atom, taking account of attributes.
 */
public class AtomLinkConverter implements Converter {

  /**
   * {@inheritDoc}
   *
   * @see com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object,
   *      com.thoughtworks.xstream.io.HierarchicalStreamWriter,
   *      com.thoughtworks.xstream.converters.MarshallingContext)
   */
  public void marshal(Object object, HierarchicalStreamWriter writer, MarshallingContext context) {
    AtomLink link = (AtomLink) object;
    if (link.getRel() != null) {
      writer.addAttribute("rel", link.getRel());
    }
    if (link.getHref() != null) {
      writer.setValue(link.getHref());
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see com.thoughtworks.xstream.converters.Converter#unmarshal(com.thoughtworks.xstream.io.HierarchicalStreamReader,
   *      com.thoughtworks.xstream.converters.UnmarshallingContext)
   */
  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    Preconditions.checkNotNull(reader);

    reader.moveDown();
    AtomLink al = new AtomLink(reader.getAttribute("rel"), reader.getValue());
    reader.moveUp();
    return al;
  }

  /**
   * {@inheritDoc}
   *
   * @see com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java.lang.Class)
   */
  // Base API is inherently unchecked
  @SuppressWarnings("unchecked")
  public boolean canConvert(Class clazz) {
    return AtomLink.class.equals(clazz);
  }

}
