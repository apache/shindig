package org.apache.shindig.social.core.util;

import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.Set;

/**
 * Utility class for OpenSocial enums
 */
public class EnumUtil {

  /**
   * @param vals array of enums
   * @return a set of the names for a list of Enum values defined by toString
   */
  public static Set<String> getEnumStrings(Enum... vals) {
    Set<String> result = Sets.newHashSet();
    for (Enum v : vals) {
      result.add(v.toString());
    }
    if (result.size() != vals.length) {
      throw new IllegalArgumentException("Enum names are not disjoint set");
    }
    return Collections.unmodifiableSet(result);
  }
}
