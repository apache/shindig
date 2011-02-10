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
package org.apache.shindig.gadgets.rewrite.js;

import java.util.List;

public interface JsCompiler {
  /**
   * Compiles the provided code with the provided list of external symbols.
   * 
   * @param jsData The code to compile.
   * @param externs The list of external symbols.
   * @return A compilation result object.
   */
  public Result compile(String jsData, List<String> externs);
  
  /**
   * Generates a sequence of statements to mark the specified symbols as
   * exported.
   *
   * @param symbols The symbols to export.
   * @return A sequence of JavaScript statements to export those symbols.
   */
  public String generateExportStatements(List<String> symbols);
  
  public static class Result {
    private final String compiled;
    private final List<String> errors;
    
    public Result(String compiled) {
      this.compiled = compiled;
      this.errors = null;
    }
    
    public Result(List<String> errors) {
      this.compiled = null;
      this.errors = errors;
    }
    
    public String getCode() {
      return compiled;
    }
    
    public List<String> getErrors() {
      return errors;
    }
  }
}
