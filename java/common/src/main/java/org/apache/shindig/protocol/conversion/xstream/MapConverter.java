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

import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.MapMaker;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

/**
 * converts a map to and from the form &lt;container&gt;
 * &lt;key&gt;value&lt;/key&gt; &lt;key&gt;value&lt;/key&gt; <container>.
 */
public class MapConverter extends AbstractCollectionConverter {

  /**
   * Create a MapConverter that use use the supplied mapper.
   *
   * @param mapper
   *          the mapped to base the conversion on.
   */
  public MapConverter(Mapper mapper) {
    super(mapper);
  }

  /**
   * output the Map in the simplified form.
   *
   * @param source
   *          the object to be output
   * @param writer
   *          the writer to use to perform the output.
   * @param context
   *          the context in which to perform the output.
   *
   * @see com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object,
   *      com.thoughtworks.xstream.io.HierarchicalStreamWriter,
   *      com.thoughtworks.xstream.converters.MarshallingContext)
   */
  @Override
  public void marshal(Object source, HierarchicalStreamWriter writer,
      MarshallingContext context) {
    Map<?, ?> map = (Map<?, ?>) source;
    for (Entry<?, ?> e : map.entrySet()) {
      writer.startNode("entry");
      writer.startNode("key");
      writer.setValue(String.valueOf(e.getKey()));
      writer.endNode();
      writer.startNode("value");
      context.convertAnother(e.getValue());
      writer.endNode();
      writer.endNode();
    }
  }

  /**
   * Convert a suitably positioned reader stream into a Map object.
   *
   * @param reader
   *          the stream reader positioned at the start of the object.
   * @param context
   *          the unmarshalling context.
   * @return the object representing the stream.
   * @see com.thoughtworks.xstream.converters.Converter#unmarshal(com.thoughtworks.xstream.io.HierarchicalStreamReader,
   *      com.thoughtworks.xstream.converters.UnmarshallingContext)
   */
  @Override
  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    Map<String, Object> m = new MapMaker().makeMap();
    reader.moveDown();
    while (reader.hasMoreChildren()) {
      String key = reader.getNodeName();
      if ("entry".equals(key)) {
        Object value = null;
        reader.moveDown();
        String type = reader.getNodeName();
        if ("key".equals(type)) {
          key = reader.getValue();
        } else {
          if (reader.hasMoreChildren()) {
            value = readItem(reader, context, m);
          } else {
            value = reader.getValue();
          }
        }
        reader.moveUp();
        reader.moveDown();
        type = reader.getNodeName();
        if ("key".equals(type)) {
          key = reader.getValue();
        } else {
          if (reader.hasMoreChildren()) {
            value = readItem(reader, context, m);
          } else {
            value = reader.getValue();
          }
        }
        m.put(key, value);
        reader.moveUp();
      } else {
        reader.moveDown();
        if (reader.hasMoreChildren()) {
          m.put(key, readItem(reader, context, m));
        } else {
          m.put(key, reader.getValue());
        }
        reader.moveUp();
      }
    }
    reader.moveUp();
    return m;
  }

  /**
   * Can this Converter convert the type supplied.
   *
   * @param clazz
   *          the type being converted.
   * @return true if the type supplied is a form of Map.
   * @see com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java.lang.Class)
   */
  @Override
  @SuppressWarnings("unchecked")
  // API is not generic
  public boolean canConvert(Class clazz) {
    return Map.class.isAssignableFrom(clazz);
  }

}
