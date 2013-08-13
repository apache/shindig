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
package org.apache.shindig.gadgets.rewrite.js;

import java.io.Serializable;

import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.Result;

/**
 * Serializable holder for cacheable compiler results.
 */
public class CompileResult implements Serializable {
  private static final long serialVersionUID = 4824178999640746883L;

  private String content;
  private String externExport;

  public CompileResult(Compiler compiler, Result result) {
    content = compiler.toSource();
    externExport = result.externExport;
  }

  public CompileResult(String content) {
    this.content = content;
    externExport = null;
  }

  public String getContent() {
    return content;
  }

  public String getExternExport() {
    return externExport;
  }
}
