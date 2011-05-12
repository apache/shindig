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
package org.apache.shindig.common.servlet;

import javax.servlet.ServletRequest;

public class ServletRequestContext {

  public static void setRequestInfo(ServletRequest req) {
    String auth = req.getServerName() + ":" + req.getServerPort();
	String fullAuth = req.getScheme() + "://" + auth;
	authority.set(auth);
	fullAuthority.set(fullAuth);

	System.setProperty("authority",auth);
	System.setProperty("fullAuthority", fullAuth);
  }
  
  /**
   * A Thread Local holder for authority -- host + port
   */
  private static ThreadLocal<String> authority = new ThreadLocal<String>();
  
  /**
   * A Thread Local holder for full authority -- scheme + host + port
   */
  private static ThreadLocal<String> fullAuthority = new ThreadLocal<String>();
  
 
  public static String getAuthority() {
  
    String retVal = authority.get();
    if (retVal == null) {
      retVal = System.getProperty("authority");
      if (retVal == null){
    	retVal = getDefaultAuthority();
      }
    }
    return retVal;
  }
 
  private static String getDefaultAuthority() {
    
    String retVal = System.getProperty("defaultAuthority");
	if (retVal == null){
	  retVal = getServerHostname()+":"+getServerPort();
	  System.setProperty("defaultAuthority", retVal);
	}
	return retVal;
	
  }

  public static String getFullAuthority() {
 
  	String retVal = fullAuthority.get();
    if (retVal == null) {
      retVal = System.getProperty("fullAuthority");
      if (retVal == null){
        retVal = getDefaultFullAuthority();
  	  }
  	}
    return retVal;
    
  }
  
  private static String getDefaultFullAuthority() {
	  
    String retVal = System.getProperty("defaultFullAuthority");
    if ( retVal != null ){
	  retVal = "http://"+getServerHostname()+":"+getServerPort();
	  System.setProperty("defaultFullAuthority", retVal);
	}
	return retVal;
	
  }
  
  private static String getServerPort() {
	return System.getProperty("shindig.port") != null ? System.getProperty("shindig.port") :
	  System.getProperty("jetty.port") != null ? System.getProperty("jetty.port") :
	  "8080";
  }
  
  private static String getServerHostname() {
	return System.getProperty("shindig.host") != null ? System.getProperty("shindig.host") :
	  System.getProperty("jetty.host") != null ? System.getProperty("jetty.host") :
	  "localhost";
  }

}
