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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.mapper.MapperWrapper;

import java.util.List;
import java.util.Map;

/**
 * Maps Interfaces to Aliases used by {@link org.apache.shindig.protocol.conversion.BeanXStreamConverter}
 */
public class InterfaceFieldAliasingMapper extends MapperWrapper {

  private Map<String, List<InterfaceFieldAliasMapping>> serializedMap = Maps.newHashMap();
  private Map<String, List<InterfaceFieldAliasMapping>> membersMap = Maps.newHashMap();
  private WriterStack writerStack;

  /**
   * @param wrapped
   */
  public InterfaceFieldAliasingMapper(Mapper wrapped, WriterStack writerStack,
      List<InterfaceFieldAliasMapping> ifaList) {
    super(wrapped);
    this.writerStack = writerStack;
    for (InterfaceFieldAliasMapping ifa : ifaList) {

      List<InterfaceFieldAliasMapping> serializedMatches = serializedMap.get(ifa.getFieldName());
      if (serializedMatches == null) {
        serializedMatches = Lists.newArrayList();
        serializedMap.put(ifa.getFieldName(), serializedMatches);
      }
      serializedMatches.add(ifa);
      List<InterfaceFieldAliasMapping> memberMatches = membersMap.get(ifa.getAlias());
      if (memberMatches == null) {
        memberMatches = Lists.newArrayList();
        membersMap.put(ifa.getAlias(), memberMatches);
      }
      memberMatches.add(ifa);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see com.thoughtworks.xstream.mapper.MapperWrapper#realMember(java.lang.Class,
   *      java.lang.String)
   */
  @SuppressWarnings("unchecked")
  @Override
  public String realMember(Class type, String serialized) {
    // get the possible member spec, using the serialized elment as the key.
    // comes from the map of members.
    List<InterfaceFieldAliasMapping> serializedMatches = membersMap
        .get(serialized);
    if (serializedMatches != null) {
      for (InterfaceFieldAliasMapping ifa : serializedMatches) {
        if (ifa.getType().isAssignableFrom(type)) {
          return ifa.getFieldName();
        }
      }
    }
    return super.realMember(type, serialized);
  }

  /**
   * {@inheritDoc}
   *
   * @see com.thoughtworks.xstream.mapper.MapperWrapper#serializedMember(java.lang.Class,
   *      java.lang.String)
   */
  @SuppressWarnings("unchecked")
  @Override
  public String serializedMember(Class type, String memberName) {
    // get the possible serialized spec, using the memberName elment as the key.
    // comes from the map of serialized elements.
    List<InterfaceFieldAliasMapping> memberMatches = serializedMap
        .get(memberName);
    if (memberMatches != null) {
      for (InterfaceFieldAliasMapping ifa : memberMatches) {
        if (ifa.getParent() == null) {
          if (ifa.getType().isAssignableFrom(type)) {
            return ifa.getAlias();
          }
        } else {
          if (ifa.getType().isAssignableFrom(type)
              && ifa.getParent().equals(writerStack.peek())) {
            return ifa.getAlias();
          }
        }
      }
    }
    return super.serializedMember(type, memberName);
  }

}
