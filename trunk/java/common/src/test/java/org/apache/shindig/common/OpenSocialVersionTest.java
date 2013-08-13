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
package org.apache.shindig.common;

import org.apache.shindig.common.util.OpenSocialVersion;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

import junit.framework.Assert;

/**
 * Tests utility class for Version strings
 *
 */
public class OpenSocialVersionTest {

  @Test
  public void createOpenSocialVersion(){
    OpenSocialVersion version = new OpenSocialVersion("1.2.3");
    Assert.assertEquals(1, version.major);
    Assert.assertEquals(2, version.minor);
    Assert.assertEquals(3, version.patch);
    Assert.assertEquals(version, new OpenSocialVersion("1.2.3"));
  }

  @Test
  public void testEquivalence(){
    OpenSocialVersion version = new OpenSocialVersion("1.2.3");
    OpenSocialVersion version2 = new OpenSocialVersion("1.2");
    Assert.assertTrue(version.isEquivalent(version2));

    version = new OpenSocialVersion("2");
    Assert.assertTrue(version.isEquivalent("2.2"));

    version = new OpenSocialVersion("3");
    Assert.assertTrue(!version.isEquivalent("2.2"));
  }

  @Test
  public void testEqualOrGreaterThan(){
    OpenSocialVersion version = new OpenSocialVersion("1.2.3");
    OpenSocialVersion version2 = new OpenSocialVersion("1.2");
    Assert.assertTrue(version.isEqualOrGreaterThan(version2));
    Assert.assertTrue(!version2.isEqualOrGreaterThan(version));

    version = new OpenSocialVersion("2");
    version2 = new OpenSocialVersion("2.2");
    Assert.assertTrue(!version.isEqualOrGreaterThan(version2));
    Assert.assertTrue(version2.isEqualOrGreaterThan(version));

    version = new OpenSocialVersion("2.2.48");
    version2 = new OpenSocialVersion("2.2.49");
    Assert.assertTrue(!version.isEqualOrGreaterThan(version2));
    Assert.assertTrue(version2.isEqualOrGreaterThan(version));

    version = new OpenSocialVersion("3");
    Assert.assertTrue(version.isEqualOrGreaterThan("2.2"));

    version = new OpenSocialVersion("3.1.18");
    Assert.assertTrue(version.isEqualOrGreaterThan("2.2"));
  }

  @Test
  public void testVersionSorting(){
    ArrayList<OpenSocialVersion> list = new ArrayList<OpenSocialVersion>();
    list.add(new OpenSocialVersion("2.2.48"));
    list.add(new OpenSocialVersion("9.0.1"));
    list.add(new OpenSocialVersion("1.2.48"));
    list.add(new OpenSocialVersion("2.3.48"));
    list.add(new OpenSocialVersion("2.2.455"));
    list.add(new OpenSocialVersion("9.0.0"));
    Collections.sort(list, OpenSocialVersion.COMPARATOR);
    for(int i =0;i < list.size()-1;i++){
      Assert.assertTrue(list.get(i+1).isEqualOrGreaterThan(list.get(i)));
    }
  }

}
