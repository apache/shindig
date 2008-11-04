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
package org.apache.shindig.social.core.util.xstream;

import com.google.inject.ImplementedBy;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.mapper.Mapper;

/**
 * The configuration for the XStream converter, this class encapsulates the
 * lists and maps that define the how xstream converts the model.
 */
@ImplementedBy(XStream081Configuration.class)
public interface XStreamConfiguration {
  public static enum ConverterSet {
    MAP(), COLLECTION(), DEFAULT(); 
  };


  /**
   * @param c
   * @param dmapper 
   * @param writerStack
   * @return
   */
  InterfaceClassMapper getMapper(ConverterSet c, Mapper dmapper, WriterStack writerStack);

  /**
   * @param c
   * @param rp
   * @param mapper
   * @param driver
   * @return
   */
  XStream getXStream(ConverterSet c, ReflectionProvider rp, Mapper mapper,
      HierarchicalStreamDriver driver);

}
