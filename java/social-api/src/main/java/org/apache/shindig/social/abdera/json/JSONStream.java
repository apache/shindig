/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  The ASF licenses this file to You
* under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.  For additional information regarding
* copyright in this work, please see the NOTICE file in the top level
* directory of this distribution.
*/
package org.apache.shindig.social.abdera.json;

import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.Stack;

import javax.activation.MimeType;

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.AtomDate;
import org.apache.abdera.util.EntityTag;

/*
 * TODO: This file is copied and modified from Abdera code as we needed
 * functionality different from the Abdera Json writer code base.
 * This file definitely needs cleanup and heavy refactoring
 */
public class JSONStream {

  private final Writer writer;
  private int depth = 0;
  private final Stack<Boolean> sepstack = new Stack<Boolean>();

  private void pushStack() {
    sepstack.push(true);
  }

  private boolean isStart() {
    boolean b = sepstack.peek();
    if (b) {
      sepstack.set(sepstack.size()-1, false);
    }
    return b;
  }

  private void popStack() {
    sepstack.pop();
  }

  public JSONStream(Writer writer) {
    this.writer = writer;
  }

  private void inc() {
    depth++;
  }

  private void dec() {
    depth--;
  }

  private void writeIndent() throws IOException {
    for (int n = 0; n < depth; n++) {
      writer.write(' ');
    }
    writer.flush();
  }

  private void writeNewLine() throws IOException {
    writer.write('\n');
    writer.flush();
  }

  public void startObject() throws IOException {
    writer.write('{');
    inc();
    pushStack();
    writer.flush();
  }

  public void endObject() throws IOException {
    popStack();
    dec();
    writeNewLine();
    writeIndent();
    writer.write('}');
    writer.flush();
  }

  public void startArray() throws IOException {
    writer.write('[');
    inc();
    writer.flush();
  }

  public void endArray() throws IOException {
    dec();
    //writeNewLine();
    //writeIndent();
    writer.write(']');
    writer.flush();
  }

  public void writeSeparator() throws IOException {
    writer.write(',');
    writer.flush();
  }

  private void writeColon() throws IOException {
    writer.write(':');
    writer.flush();
  }

  public void writeQuoted(String value) throws IOException {
    writer.write('"');
    writer.write(escape(value));
    writer.write('"');
    writer.flush();
  }

  public void writeValue(String value) throws IOException {
    writer.write(value);
    writer.flush();
  }

  public void writeField(String name) throws IOException {
    if (name == null) {
      return;
    }

    if (!isStart()) {
      writeSeparator();
    }
    writeNewLine();
    writeIndent();
    writeQuoted(name);
    writeColon();
  }

  public void skipWritingFieldName() throws IOException {
    if (isStart()) {
      return;
    }
    writeSeparator();
    writeNewLine();
    writeIndent();
  }


  public void writeField(String name, Date value) throws IOException {
    if (value != null) {
      writeField(name, AtomDate.format(value));
    }
  }

  public void writeField(String name, IRI value) throws IOException {
    if (value != null) {
      writeField(name, value.toASCIIString());
    }
  }

  public void writeField(String name, MimeType value) throws IOException {
    if (value != null) {
      writeField(name, value.toString());
    }
  }

  public void writeField(String name, EntityTag value) throws IOException {
    if (value != null) {
      writeField(name, value.toString());
    }
  }

  public void writeField(String name, String value) throws IOException {
    if (value != null) {
      writeField(name);
      writeQuoted(value);
    }
  }

  public void writeField(String name, Number value) throws IOException {
    if (value != null) {
      writeField(name);
      writer.write(value.toString());
    }
    writer.flush();
  }

  public void writeField(String name, Boolean value) throws IOException {
    if (value != null) {
      writeField(name);
      writer.write(value.toString());
    }
    writer.flush();
  }

  private static String escape(String value) {
    if (value == null) {
      return null;
    }
    StringBuffer buf = new StringBuffer();
    char[] chars = value.toCharArray();
    char b = 0;
    String t = null;
    for (char c : chars) {
      switch(c) {
        case '\\':
        case '"':
          buf.append('\\');
          buf.append(c);
          break;
        case '/':
          if (b == '<') {
            buf.append('\\');
          }
          buf.append(c);
          break;
        case '\b':
          buf.append("\\b");
          break;
        case '\t':
          buf.append("\\t");
          break;
        case '\n':
          buf.append("\\n");
          break;
        case '\f':
          buf.append("\\f");
          break;
        case '\r':
          buf.append("\\r");
          break;
        default:
          if (c < ' ' || c > 127) {
            t = "000" + Integer.toHexString(c);
            buf.append("\\u" + t.substring(t.length() - 4));
          } else {
            buf.append(c);
          }
        }
        b = c;
      }
    return buf.toString();
    }
}
