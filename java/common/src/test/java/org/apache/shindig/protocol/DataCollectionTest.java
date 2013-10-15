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
package org.apache.shindig.protocol;

import com.google.common.collect.Maps;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class DataCollectionTest extends Assert {

  @Test
  public void testBasicMethods() throws Exception {
    Map<String, Map<String, Object>> entry = Maps.newHashMap();
    DataCollection collection = new DataCollection(entry);
    assertEquals(entry, collection.getEntry());

    Map<String, Map<String, Object>> newEntry = Maps.newHashMap();
    Map<String, Object> value = Maps.newHashMap();
    value.put("knock knock", "who's there?");
    value.put("banana", "banana who?");
    value.put("banana!", "banana who?");
    value.put("orange!", "?");
    newEntry.put("orange you glad I didn't type banana", value);
    collection.setEntry(newEntry);
    assertEquals(newEntry, collection.getEntry());
  }

}
