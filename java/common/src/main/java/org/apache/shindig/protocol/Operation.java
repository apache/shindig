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
 * Annotates a method on a ServiceHandler which expose a REST/RPC operation
 * The name of the annotated method is the literal name of the method for JSON-RPC
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Operation {
  /**
   * The HTTP methods to bind this operation to.
   */
  String[] httpMethods();

  /**
   * The parameter name to bind the body content to in the RequestItem
   * passed to the REST/RPC handler.
   */
  String bodyParam() default "body";

  /**
   * The path to match for the operation to override the service
   * path matching and parameter binding. This is useful for situations
   * such as /<service>/@supportedFields where the path determines the
   * operation rather than the HTTP method in REST
   */
  String path() default "";

  /**
   * The name to match for the RPC operation to override the default behavior
   * which is to use the name of the annotated method
   */
  String name() default "";
}
