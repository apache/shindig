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
package org.apache.shindig.protocol.conversion.xstream;

import org.apache.shindig.protocol.DataCollection;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

import java.util.Map;
import java.util.Map.Entry;

/**
 * This converter changes the way in which a collection is serialized
 */
public class DataCollectionConverter extends AbstractCollectionConverter {

  /**
   * @param mapper
   */
  public DataCollectionConverter(Mapper mapper) {
    super(mapper);
  }

  /**
   * {@inheritDoc}
   *
   * @see com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter#canConvert(java.lang.Class)
   */
  @Override
  // Base API is inherently unchecked
  @SuppressWarnings("unchecked")
  public boolean canConvert(Class clazz) {
    return DataCollection.class.isAssignableFrom(clazz);
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

    DataCollection collection = (DataCollection) source;
    Map<String, Map<String, Object>> internalMap = collection.getEntry();

    for (Entry<String, Map<String, Object>> eo : internalMap.entrySet()) {
      writer.startNode("entry");
      writer.startNode("key");
      writer.setValue(eo.getKey());
      writer.endNode();
      writer.startNode("value");
      for (Entry<String, Object> ei : eo.getValue().entrySet()) {
        writer.startNode("entry");
        writer.startNode("key");
        writer.setValue(ei.getKey());
        writer.endNode();
        writer.startNode("value");
        writer.setValue(ei.getValue().toString());
        writer.endNode();
        writer.endNode();
      }

      writer.endNode();
      writer.endNode();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter#unmarshal(com.thoughtworks.xstream.io.HierarchicalStreamReader,
   *      com.thoughtworks.xstream.converters.UnmarshallingContext)
   */
  @SuppressWarnings("unchecked")
  @Override
  public Object unmarshal(HierarchicalStreamReader reader,
      UnmarshallingContext context) {
    Preconditions.checkNotNull(reader);
    reader.moveDown();
    Map<String, Object> m = Maps.newHashMap();
    while (reader.hasMoreChildren()) {
      reader.moveDown(); // entry
      String ok = null;
      Object ov = null;
      while (reader.hasMoreChildren()) {
        reader.moveDown(); // key or value
        String elname = reader.getNodeName();
        if ("key".equals(elname)) {
          ok = reader.getValue();
        } else if ("value".equals(elname)) {
          ov = reader.getValue();
          if (reader.hasMoreChildren()) {
            Map<String, String> innerMap = Maps.newHashMap();
            while (reader.hasMoreChildren()) {
              reader.moveDown();// entry
              String k = null;
              String v = null;
              while (reader.hasMoreChildren()) {
                reader.moveDown(); // key or value
                if ("key".equals(elname)) {
                  k = reader.getValue();
                } else if ("value".equals(elname)) {
                  v = reader.getValue();
                }
                reader.moveUp();
              }
              innerMap.put(k, v);
              reader.moveUp();
            }
            ov = innerMap;
          } else {
          }
        }
        reader.moveUp();
      }
      reader.moveUp();
      m.put(ok, ov);
    }
    reader.moveUp();
    // scan the map, if there are any maps, then everything should be in maps.
    boolean nonmap = false;
    for (Entry<String, Object> e : m.entrySet()) {
      if (e.getValue() instanceof String) {
        nonmap = true;
      }
    }
    Map<String, Map<String, Object>> fm = Maps.newHashMap();
    if (nonmap) {
      for (Entry<String, Object> e : m.entrySet()) {
        if (e.getValue() instanceof Map) {
          fm.put(e.getKey(), (Map<String, Object>) e.getValue());
        } else {
          // not certain that this makes sense, but can't see how else.
          Map<String, Object> mv = Maps.newHashMap();
          mv.put(e.getKey(), (String) e.getValue());
          fm.put(e.getKey(), mv);
        }
      }

    } else {
      for (Entry<String, Object> e : m.entrySet()) {
        fm.put(e.getKey(), (Map<String, Object>) e.getValue());
      }
    }
    return new DataCollection(fm);
  }

}
