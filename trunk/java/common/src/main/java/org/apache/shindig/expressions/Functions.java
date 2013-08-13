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
package org.apache.shindig.expressions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import javax.el.FunctionMapper;

import com.google.common.collect.Maps;
import com.google.inject.ImplementedBy;
import com.google.inject.Inject;

/**
 * An implementation of FunctionMapper that uses annotated static methods
 * on classes to implement EL functions.
 * <p>
 * Each class passed to the constructor will have EL functions added
 * for any static method annotated with the @Expose annotation.
 * Each method can be exposed in one namespace prefix, with any number
 * of method names.
 * <p>
 * The default Guice instance of the Functions class has the
 * {@link OpensocialFunctions} methods registered.
 */
@ImplementedBy(Functions.DefaultFunctions.class)
public class Functions extends FunctionMapper {
  private final Map<String, Map<String, Method>> functions = Maps.newHashMap();

  /** Annotation for static methods to be exposed as functions. */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface Expose {
    /**
     * The prefix to bind functions to.
     */
    String prefix();

    /**
     * The prefix to bind functions to.
     */
    String[] names() default {};
  }

  /**
   * Creates a Functions class with the specified
   */
  public Functions(Class<?>... functionClasses) {
    for (Class<?> functionClass : functionClasses) {
      for (Method m : functionClass.getMethods()) {
        if ((m.getModifiers() & Modifier.STATIC) == 0) {
          continue;
        }

        addMethod(m);
      }
    }
  }

  @Override
  public Method resolveFunction(String prefix, String methodName) {
    Map<String, Method> prefixMap = functions.get(prefix);
    if (prefixMap == null) {
      return null;
    }

    return prefixMap.get(methodName);
  }

  /** Register functions for a single Method */
  private void addMethod(Method m) {
    Expose annotation = m.getAnnotation(Expose.class);
    if (m.isAnnotationPresent(Expose.class)) {
      String prefix = annotation.prefix();
      Map<String, Method> prefixMap = functions.get(prefix);
      if (prefixMap == null) {
        prefixMap = Maps.newHashMap();
        functions.put(prefix, prefixMap);
      }

      for (String methodName : annotation.names()) {
        Method priorMethod = prefixMap.put(methodName, m);
        if (priorMethod != null) {
          throw new IllegalStateException(
              "Method " + prefix + ':' + methodName + " re-defined.");
        }
      }
    }
  }

  /**
   * A default version for Guice;  includes the Opensocial functions.
   */
  static class DefaultFunctions extends Functions {
    @Inject
    public DefaultFunctions() {
      super(OpensocialFunctions.class);
    }
  }
}
