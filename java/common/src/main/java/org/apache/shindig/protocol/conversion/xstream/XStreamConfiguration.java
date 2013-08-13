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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.mapper.Mapper;

import java.util.Map;

/**
 * The configuration for the XStream converter, this class encapsulates the
 * lists and maps that define the how xstream converts the model.
 */
public interface XStreamConfiguration {
  public static enum ConverterSet {
    MAP(), COLLECTION(), DEFAULT()
  }

  public class ConverterConfig {
    public InterfaceClassMapper mapper;
    public XStream xstream;

    public ConverterConfig(InterfaceClassMapper mapper, XStream xstream) {
      this.mapper = mapper;
      this.xstream = xstream;
    }
  }

  /**
   * Generate the converter config.
   *
   * @param c
   *          which converter set.
   * @param rp
   *          an XStream reflection provider.
   * @param dmapper
   *          the XStream mapper.
   * @param driver
   *          the XStream driver
   * @param writerStack
   *          a hirachical stack recorder.
   * @return the converter config, used for serialization.
   */
  ConverterConfig getConverterConfig(ConverterSet c, ReflectionProvider rp,
      Mapper dmapper, HierarchicalStreamDriver driver, WriterStack writerStack);

  /**
   * @return get the namespace mappings used by the driver.
   */
  Map<String, NamespaceSet> getNameSpaces();

}
