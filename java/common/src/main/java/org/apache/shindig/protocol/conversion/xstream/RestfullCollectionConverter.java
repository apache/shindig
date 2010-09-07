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
package org.apache.shindig.protocol.conversion.xstream;

import org.apache.shindig.protocol.RestfulCollection;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

/**
 * This converter changes the way in which a collection is serialized
 */
public class RestfullCollectionConverter extends AbstractCollectionConverter {

  /**
   * @param mapper
   */
  public RestfullCollectionConverter(Mapper mapper) {
    super(mapper);
  }

  /**
   * {@inheritDoc}
   *
   * @see com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter#canConvert(java.lang.Class)
   */
  @SuppressWarnings("unchecked")
  @Override
  public boolean canConvert(Class clazz) {
    return RestfulCollection.class.isAssignableFrom(clazz);
  }

  /**
   * {@inheritDoc}
   *
   * @see com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter#marshal(java.lang.Object,
   *      com.thoughtworks.xstream.io.HierarchicalStreamWriter,
   *      com.thoughtworks.xstream.converters.MarshallingContext)
   */
  @Override
  public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {

    RestfulCollection<?> collection = (RestfulCollection<?>) source;

    // Required per spec
    writer.startNode("startIndex");
    writer.setValue(String.valueOf(collection.getStartIndex()));
    writer.endNode();
    writer.startNode("totalResults");
    writer.setValue(String.valueOf(collection.getTotalResults()));
    writer.endNode();
    writer.startNode("itemsPerPage");
    writer.setValue(String.valueOf(collection.getItemsPerPage()));
    writer.endNode();

    // Optional
    writer.startNode("isFiltered");
    writer.setValue(String.valueOf(collection.isFiltered()));
    writer.endNode();
    writer.startNode("isSorted");
    writer.setValue(String.valueOf(collection.isSorted()));
    writer.endNode();
    writer.startNode("isUpdatedSince");
    writer.setValue(String.valueOf(collection.isUpdatedSince()));
    writer.endNode();

    // TODO: resolve if entry is the container or the name of the object.
    for (Object o : collection.getEntry()) {
      writer.startNode("entry");
      writeItem(o, context, writer);
      writer.endNode();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter#unmarshal(com.thoughtworks.xstream.io.HierarchicalStreamReader,
   *      com.thoughtworks.xstream.converters.UnmarshallingContext)
   */
  @Override
  public Object unmarshal(HierarchicalStreamReader arg0, UnmarshallingContext arg1) {
    // TODO Auto-generated method stub
    return null;
  }

}
