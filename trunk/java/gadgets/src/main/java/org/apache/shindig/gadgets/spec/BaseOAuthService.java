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
package org.apache.shindig.gadgets.spec;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.XmlUtil;

import org.w3c.dom.Element;

import java.util.Map;

/**
 * Information about an OAuth service that a gadget wants to use.
 * This class defines the information common to both OAuth1.0 based service and OAuth2.0 based service.
 *
 * Instances are immutable.
 */
public abstract class BaseOAuthService {

  /**
   * Constructor for testing only.
   */
  BaseOAuthService() { }

  public BaseOAuthService(Element serviceElement, Uri base) throws SpecParserException{

  }



  /**
   * Represents /OAuth/Service@name
   */
  abstract public String getName() ;

  /**
   * Method to use for requests to an OAuth request token or access token URL.
   */
  public enum Method {
    GET, POST;

    private static final Map<String, Method> METHODS =
            ImmutableMap.of(GET.toString(), GET, POST.toString(), POST, "", GET);

    public static Method parse(String value) throws SpecParserException {
      value = value.trim();
      Method result = METHODS.get(value);
      if (result == null) {
        throw new SpecParserException("Unknown OAuth method: " + value);
      }
      return result;
    }
  }

  /**
   * Location for OAuth parameters in requests to an OAuth request token,
   * access token, or resource URL.
   */
  public enum Location {
    HEADER("auth-header"),
    URL("uri-query"),
    BODY("post-body");

    private static final Map<String, Location> LOCATIONS;

    static {
      LOCATIONS = Maps.newHashMap();
      for (Location l : Location.values()) {
        LOCATIONS.put(l.locationString, l);
      }
      // Default value
      LOCATIONS.put("", Location.HEADER);
    }

    private String locationString;
    private Location(String locationString) {
      this.locationString = locationString;
    }

    @Override
    public String toString() {
      return locationString;
    }

    public static Location parse(String value) throws SpecParserException {
      value = value.trim();
      Location result = LOCATIONS.get(value);
      if (result == null) {
        throw new SpecParserException("Unknown OAuth param_location: " + value);
      }
      return result;
    }
  }

  private static final String URL_ATTR = "url";
  private static final String PARAM_LOCATION_ATTR = "param_location";
  private static final String METHOD_ATTR = "method";

  /**
   * Description of an OAuth request token or access token URL.
   */
  public static class EndPoint {
    public final Uri url;
    public Uri getUrl() {
		return url;
	}

	public Method getMethod() {
		return method;
	}

	public Location getLocation() {
		return location;
	}

	public final Method method;
    public final Location location;

    public EndPoint(Uri url, Method method, Location location) {
      this.url = url;
      this.method = method;
      this.location = location;
    }

    public String toString(String element) {
      return '<' + element + " url='" + url.toString() + "' " +
              "method='" + method + "' param_location='" + location + "'/>";
    }
  }

  Uri parseAuthorizationUrl(Element child, Uri base) throws SpecParserException {
    Uri url = XmlUtil.getHttpUriAttribute(child, URL_ATTR, base);
    if (url == null) {
      throw new SpecParserException("OAuth/Service/Authorization @url is not valid: " +
          child.getAttribute(URL_ATTR));
    }
    return base.resolve(url);
  }


  EndPoint parseEndPoint(String where, Element child, Uri base) throws SpecParserException {
    Uri url = XmlUtil.getHttpUriAttribute(child, URL_ATTR, base);
    if (url == null) {
      throw new SpecParserException("Not an HTTP url: " + child.getAttribute(URL_ATTR));
    }

    Location location = Location.parse(child.getAttribute(PARAM_LOCATION_ATTR));
    Method method = Method.parse(child.getAttribute(METHOD_ATTR));
    return new EndPoint(base.resolve(url), method, location);
  }
}
