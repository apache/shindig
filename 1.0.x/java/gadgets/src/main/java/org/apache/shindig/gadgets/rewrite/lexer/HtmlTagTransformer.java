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
package org.apache.shindig.gadgets.rewrite.lexer;

import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.Token;

/**
 * Provide custom transformations of a stream of HTML lexical tokens
 * as provided by the Caja lexer. A tag transformer is initiated by
 * the occurence of a start tag in the token stream. The transformer is
 * fed all subsequent tokens within the tag. The transformer is given the\
 * option of continuing to consume tokens from subsequent tags even after
 * its initiating tag has ended.
 */
public interface HtmlTagTransformer {

  /**
   * Consume the token. Prior token supplied for context
   * @param token current token
   * @param lastToken 
   */
  public void accept(Token<HtmlTokenType> token,
      Token<HtmlTokenType> lastToken);

  /**
   * A new tag has begun, should this transformer continue to process
   * @param tagStart
   * @return true if continuing to process the new tag
   */
  public boolean acceptNextTag(Token<HtmlTokenType> tagStart);

  /**
   * Close the transformer, reset its state and return any content
   * for inclusion in the rewritten output
   * @return transformed content
   */
  public String close();
}
