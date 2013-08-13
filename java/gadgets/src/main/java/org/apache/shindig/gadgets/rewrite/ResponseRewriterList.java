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
package org.apache.shindig.gadgets.rewrite;

import com.google.inject.BindingAnnotation;

import org.apache.shindig.config.ContainerConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that specifies a list of rewriters with the rewriteFlow and
 * container they are meant to be applied to.
 */
@BindingAnnotation
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ResponseRewriterList {

  // Enum of rewrite flows being used.
  public enum RewriteFlow {
    DEFAULT,
    REQUEST_PIPELINE,
    ACCELERATE,
    DUMMY_FLOW
  }

  // The flow id signifying what type of rewriting is done.
  RewriteFlow rewriteFlow();

  // The container context.
  String container() default ContainerConfig.DEFAULT_CONTAINER;
}
