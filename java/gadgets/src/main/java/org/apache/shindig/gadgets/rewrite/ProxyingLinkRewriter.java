package org.apache.shindig.gadgets.rewrite;

import java.net.URI;
import java.net.URLEncoder;

/**
 * Simple link rewriter that expect to rewrite a link to the form
 * http://www.host.com/proxy/url=<url encoded link>
 */
public class ProxyingLinkRewriter implements LinkRewriter {

  private final String prefix;

  public ProxyingLinkRewriter(String prefix) {
    this.prefix = prefix;
  }

  public String rewrite(String link, URI context) {
    URI uri = context.resolve(link);
    return prefix + URLEncoder.encode(uri.toString());
  }

}
