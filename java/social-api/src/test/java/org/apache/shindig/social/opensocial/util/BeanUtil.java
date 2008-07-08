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
package org.apache.shindig.social.opensocial.util;

import com.google.common.collect.Maps;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Date;

public class BeanUtil {

  /**
   * Utility class to compare two simple beans
   *
   * @param bean1   First bean
   * @param bean2   Second bean
   * @param compMap Map of comparators by type to override comparisons
   * @return String indicating difference or null if no difference found
   */
  public static String getExactDifferences(Object bean1, Object bean2,
      Map<Class<?>, Comparator> compMap)
      throws Exception {
    if (compMap == null) {
      compMap = Collections.emptyMap();
    }
    if (bean1 == bean2) {
      return null;
    }
    if (bean1 == null || bean2 == null) {
      return "No instance provided";
    }
    if (bean1.getClass() != bean2.getClass()) {
      return bean1.getClass().getName() + "!=" + bean2.getClass().getName();
    }
    BeanInfo info = Introspector.getBeanInfo(bean1.getClass());

    for (PropertyDescriptor p : info.getPropertyDescriptors()) {
      Object v1 = p.getReadMethod().invoke(bean1);
      Object v2 = p.getReadMethod().invoke(bean2);
      if (v1 != v2) {
        if (v1 == null || v2 == null) {
          return "[" + p.getName() + "] " + v1 + " != " + v2;
        } else if (compMap.containsKey(v1.getClass())) {
          if (compMap.get(v1.getClass()).compare(v1, v2) != 0) {
            return "[" + p.getName() + "] " + v1 + " != " + v2;
          }
        } else if (v1 instanceof Comparable) {
          if (((Comparable) v1).compareTo(v2) != 0) {
            return "[" + p.getName() + "] " + v1 + " != " + v2;
          }
        } else {
          String subdifference = getExactDifferences(v1, v2, compMap);
          if (subdifference != null) {
            return "[" + p.getName() + "] " + v1.toString() + " != " + v2.toString() +
                " [" + subdifference + "]";
          }
        }
      }
    }
    return null;
  }

  public static String getLenientDifferences(Object bean1, Object bean2) throws Exception {
    Map<Class<?>, Comparator> comparators = Maps.newHashMap();
    comparators.put(Date.class, new LenientDateComparator());
    return getExactDifferences(bean1, bean2, comparators);
  }




  public static class LenientDateComparator implements Comparator {

    private final long scale;

    public LenientDateComparator() {
      this(1000L);
    }

    public LenientDateComparator(long scale) {
      this.scale = scale;
    }

    public int compare(Date o1, Date o2) {
      Long o1Scaled = o1.getTime() / scale;
      Long o2Scaled = o2.getTime() / scale;
      return o1Scaled.compareTo(o2Scaled);
    }

    public int compare(Object o1, Object o2) {
      return compare((Date)o1, (Date)o2);
    }
  }
}
