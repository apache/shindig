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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates the base path for REST calls or the RPC service name a RequestHandler
 * can dispatch to. Define parameter binding for REST path variables
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Service {
  /**
   * The name of the service this handler exports. This is also the name of the
   * root path element of the REST endpoint. E.g. The "activities" service
   * consumes all paths under /activities/...
   */
  String name();

  /**
   * The structure of the REST paths used to address this service. Paths
   * can contain placeholders delimited by {...} that bind a named parameter or
   * set of parameters to the request. A plural parameter is denoted by appending '+'
   * after the named parameter. Parameters are bound in order left-to-right and
   * missing path segments are bound to null or empty sets
   *
   * E.g.
   *
   * /{userId}+/{group}/{personId}+ will parameterize the following URLs
   * /1/@self            => { userId : [1], group : @self, personId : []}
   * /1/@self            => { userId : [1], group : @self, personId : []}
   * /1,2/@friends       => { userId : [1,2], group : @friends, personId : []}
   * /1,2/@friends/2,3   => { userId : [1,2], group : @friends, personId : [2,3]}
   *
   */
  String path() default "";
}
