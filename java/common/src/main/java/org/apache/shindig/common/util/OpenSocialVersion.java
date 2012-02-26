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
package org.apache.shindig.common.util;

import com.google.common.base.Objects;

import java.util.Comparator;
import java.util.StringTokenizer;

/**
 * Convenience class for working with OpenSocial Specification and Feature versions.
 * Applies the rules specified in the OS specification
 * http://opensocial-resources.googlecode.com/svn/spec/1.0/Core-Gadget.xml#Versioning
 *
 */
public class OpenSocialVersion {

  public static Comparator<OpenSocialVersion> COMPARATOR = new VersionComparator();

  public int major = -1;
  public int minor = -1;
  public int patch = -1;

  /**
   * Create a new OpenSocialVersion based upon a versionString
   * @param versionString Version string
   */
  public OpenSocialVersion(String versionString){
    StringTokenizer tokens = new StringTokenizer(versionString,".");
    try{
      if(tokens.hasMoreTokens()){
        major = Integer.parseInt(tokens.nextToken());
      }
      if(tokens.hasMoreTokens()){
        minor = Integer.parseInt(tokens.nextToken());
      }
      if(tokens.hasMoreTokens()){
        patch = Integer.parseInt(tokens.nextToken());
      }
    } catch(NumberFormatException ex){
      //Revert if we couldn't parse
      major = -1;
      minor = -1;
      patch = -1;
    }
  }


  /**
   * Same version number matches same version number
   */
  @Override
  public boolean equals(Object o) {
    if(o instanceof OpenSocialVersion){
      OpenSocialVersion ver = (OpenSocialVersion)o;
      return (ver.major == major) && (ver.minor == minor) && (ver.patch == patch);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(major, minor, patch);
  }

  /**
   * Tests if OpenSocialVersion is equivalent to the parameter version
   * @param version Compare with this version
   * @return TRUE if is equivalent to version
   */
  public boolean isEquivalent(OpenSocialVersion version){
    int cmp = version.major - major;
    if(cmp == 0 && version.minor > -1 && minor > -1){
      cmp = version.minor - minor;
    }
    if(cmp == 0 && version.patch > -1 && patch > -1){
      cmp = version.patch - patch;
    }
    return cmp == 0;
  }

  /**
   * Tests if OpenSocialVersion is equivalent to the parameter version
   * @param version Compare with this version string
   * @return TRUE if is equivalent to version
   */
  public boolean isEquivalent(String version){
    return isEquivalent(new OpenSocialVersion(version));
  }

  /**
   * Tests if OpenSocialVersion is equal to or greater than parameter version
   * @param version Compare with this version
   * @return TRUE if is equal or greater than version
   */
  public boolean isEqualOrGreaterThan(OpenSocialVersion version){
    int cmp = version.major - major;
    if(cmp == 0){
      if(version.minor > -1 && minor > -1){
        cmp = version.minor - minor;
      } else {
        cmp = version.minor;
      }
    }
    if(cmp == 0){
      if(version.patch > -1 && patch > -1){
        cmp = version.patch - patch;
      } else {
        cmp = version.patch;
      }
    }
    return cmp <= 0;
  }

  /**
   * Tests if OpenSocialVersion is equal to or greater than parameter version
   * @param version Compare with this version string
   * @return TRUE if is equal or greater than version
   */
  public boolean isEqualOrGreaterThan(String version){
    return isEqualOrGreaterThan(new OpenSocialVersion(version));
  }

}

/**
 * Utility class for sorting OpenSocialVersion objects
 *
 */
class VersionComparator implements java.util.Comparator<OpenSocialVersion>{

  public int compare(OpenSocialVersion object1, OpenSocialVersion object2) {
    int cmp = object1.major - object2.major;
    if(cmp == 0){
      cmp = object1.minor - object2.minor;
    }
    if(cmp == 0){
      cmp = object1.patch - object2.patch;
    }
    return cmp;
  }

}

