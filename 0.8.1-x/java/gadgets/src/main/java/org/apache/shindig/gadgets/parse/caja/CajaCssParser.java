/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.parse.caja;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.CssLexer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.parser.css.CssParser;
import com.google.caja.parser.css.CssTree;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Criterion;
import com.google.inject.Singleton;

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.parse.GadgetCssParser;
import org.apache.shindig.gadgets.parse.ParsedCssDeclaration;
import org.apache.shindig.gadgets.parse.ParsedCssRule;

import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class CajaCssParser implements GadgetCssParser {

  public List<ParsedCssRule> parse(String css) throws GadgetException {
    if (css.matches("\\s*")) {
      return new ArrayList<ParsedCssRule>(0);
    }
    
    CssParser parser = getParser(css);
    CssTree.StyleSheet stylesheet = null;
    
    try {
      stylesheet = parser.parseStyleSheet();
    } catch (ParseException e) {
      throw new GadgetException(GadgetException.Code.CSS_PARSE_ERROR, e);
    }
    
    ArrayList<ParsedCssRule> rules =
        new ArrayList<ParsedCssRule>(stylesheet.children().size());
    for (CssTree node : stylesheet.children()) {
      if (node instanceof CssTree.RuleSet) {
        rules.add(new CajaParsedCssRule((CssTree.RuleSet)node));
      }
    }
    
    return rules;
  }

  public List<ParsedCssDeclaration> parseInline(String style)
      throws GadgetException {
    if (style.matches("\\s*")) {
      return new ArrayList<ParsedCssDeclaration>();
    }
    
    CssParser parser = getParser(style);
    CssTree.DeclarationGroup declGroup = null;
    
    try {
      declGroup = parser.parseDeclarationGroup();
    } catch (ParseException e) {
      throw new GadgetException(GadgetException.Code.CSS_PARSE_ERROR, e);
    }
    
    List<ParsedCssDeclaration> attributes =
        new ArrayList<ParsedCssDeclaration>(declGroup.children().size());
    for (CssTree node : declGroup.children()) {
      if (node instanceof CssTree.Declaration) {
        CssTree.Declaration decl = (CssTree.Declaration)node;
        if (decl.getProperty() != null) {
          attributes.add(new CajaParsedCssDeclaration(decl));
        }
      }
    }
    
    return attributes;
  }
  
  private CssParser getParser(String content) {
    InputSource source = null;
    try {
      source = new InputSource(new URI("http://dummy.com/"));
    } catch (URISyntaxException e) {
      // Never happens. Dummy URI needed to satisfy API.
      // We may want to pass in the gadget URI for auditing
      // purposes at some point.
    }
    CharProducer producer = CharProducer.Factory.create(new StringReader(content), source);
    CssLexer lexer = new CssLexer(producer);
    return new CssParser(new TokenQueue<CssTokenType>(
        lexer,
        source,
        new Criterion<Token<CssTokenType>>() {  
          public boolean accept(Token<CssTokenType> tok) {
            return tok.type != CssTokenType.COMMENT
                && tok.type != CssTokenType.SPACE;
          }
        }));
  }
  
  private static final String renderCssTreeElement(CssTree elem) {
    StringBuffer selBuffer = new StringBuffer();
    TokenConsumer tc = elem.makeRenderer(selBuffer, null);
    elem.render(new RenderContext(new MessageContext(), tc));
    return selBuffer.toString();
  }
  
  private static class CajaParsedCssRule implements ParsedCssRule {
    private final List<ParsedCssDeclaration> attributes;
    private final List<String> selectors;
    
    private CajaParsedCssRule(CssTree.RuleSet ruleSet) {
      attributes = new ArrayList<ParsedCssDeclaration>();
      selectors = new ArrayList<String>();
      
      for (CssTree child : ruleSet.children()) {
        if (child instanceof CssTree.Selector) {
          selectors.add(renderCssTreeElement(child));
        } else if (child instanceof CssTree.Declaration) {
          CssTree.Declaration decl = (CssTree.Declaration)child;
          if (decl.getProperty() != null) {
            attributes.add(new CajaParsedCssDeclaration(decl));
          }
        }
      }
    }

    public List<ParsedCssDeclaration> getDeclarations() {
      return attributes;
    }

    public List<String> getSelectors() {
      return selectors;
    }
  }
  
  private static class CajaParsedCssDeclaration implements ParsedCssDeclaration {
    private final String key;
    private final String value;
    
    private CajaParsedCssDeclaration(CssTree.Declaration declaration) {
      key = declaration.getProperty().getPropertyName();
      value = renderCssTreeElement(declaration.getExpr());
    }
    
    public String getName() {
      return key;
    }

    public String getValue() {
      return value;
    }
  }
}
