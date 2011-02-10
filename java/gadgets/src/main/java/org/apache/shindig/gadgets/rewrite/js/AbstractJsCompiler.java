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

import com.google.caja.util.Sets;
import com.google.common.collect.Lists;

import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Base for a JsCompiler implementation.
 */
public abstract class AbstractJsCompiler implements JsCompiler {

  public String generateExportStatements(List<String> symbols) {
    StringBuilder exportStatements = new StringBuilder();
    Set<String> allExports = Sets.newHashSet();
    List<String> exports = Lists.newArrayListWithCapacity(symbols.size());
    exports.addAll(symbols);
    Collections.sort(exports);
    String prevExport = null;
    for (String export : exports) {
      if (!export.equals(prevExport)) {
        String[] pieces = StringUtils.split(export, "\\.");
        String base = "window";
        for (int i = 0; i < pieces.length; ++i) {
          String symExported = (i == 0) ? pieces[0] : base + "." + pieces[i];
          if (!allExports.contains(symExported)) {
            String curExport = base + "['" + pieces[i] + "']=" + symExported + ";\n";
            exportStatements.append(curExport);
            allExports.add(symExported);
          }
          base = symExported;
        }
      }
      prevExport = export;
    }
    return exportStatements.toString();
  }
}
