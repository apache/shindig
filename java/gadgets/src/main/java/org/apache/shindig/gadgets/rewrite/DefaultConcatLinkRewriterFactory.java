/*
 * To add signing
 */
package org.apache.shindig.gadgets.rewrite;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.shindig.common.uri.Uri;

@Singleton
public class DefaultConcatLinkRewriterFactory implements
    ConcatLinkRewriterFactory {

  private final ContentRewriterUris rewriterUris;

  @Inject
  public DefaultConcatLinkRewriterFactory(ContentRewriterUris rewriterUris) {
    this.rewriterUris = rewriterUris;
  }

  public ConcatLinkRewriter create(Uri gadgetUri,
      ContentRewriterFeature rewriterFeature, String container, boolean debug,
      boolean ignoreCache) {
    return new ConcatLinkRewriter(rewriterUris, gadgetUri, rewriterFeature,
        container, debug, ignoreCache);
  }
}
