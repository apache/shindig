package org.apache.shindig.common;

import com.google.inject.AbstractModule;
import org.apache.shindig.gadgets.BasicGadgetTokenDecoder;
import org.apache.shindig.gadgets.BasicRemoteContentFetcher;
import org.apache.shindig.gadgets.GadgetTokenDecoder;
import org.apache.shindig.gadgets.RemoteContentFetcher;

/**
 * Provides social api component injection
 */
public class CommonGuiceModule extends AbstractModule {

  /** {@inheritDoc} */
  @Override
  protected void configure() {
    // TODO: These classes should be moved into the common package.
    // Once that happens then this common guice module can also move to
    // java/common.
    bind(RemoteContentFetcher.class).to(BasicRemoteContentFetcher.class);
    bind(GadgetTokenDecoder.class).to(BasicGadgetTokenDecoder.class);
  }
}
