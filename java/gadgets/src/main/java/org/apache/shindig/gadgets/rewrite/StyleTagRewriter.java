package org.apache.shindig.gadgets.rewrite;

import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.Token;

import java.net.URI;

/**
 * Rewrite the CSS content of a style tag
 */
public class StyleTagRewriter implements HtmlTagTransformer {

  private URI source;
  private LinkRewriter linkRewriter;

  private StringBuffer sb;

  public StyleTagRewriter(URI source, LinkRewriter linkRewriter) {
    this.source = source;
    this.linkRewriter = linkRewriter;
    sb = new StringBuffer(500);
  }

  public void accept(Token<HtmlTokenType> token, Token<HtmlTokenType> lastToken) {
    if (token.type == HtmlTokenType.UNESCAPED) {
      sb.append(CssRewriter.rewrite(token.text, source, linkRewriter));
    } else {
      sb.append(HtmlRewriter.producePreTokenSeparator(token, lastToken));
      sb.append(token.text);
      sb.append(HtmlRewriter.producePostTokenSeparator(token, lastToken));
    }
  }

  public boolean acceptNextTag(Token<HtmlTokenType> tagStart) {
    return false;
  }

  public String close() {
    String result = sb.toString();
    sb.setLength(0);
    return result;
  }
}
