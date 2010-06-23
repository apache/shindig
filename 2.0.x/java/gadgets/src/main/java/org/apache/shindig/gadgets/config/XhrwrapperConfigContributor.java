package org.apache.shindig.gadgets.config;

import com.google.common.collect.Maps;
import com.google.inject.Singleton;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.spec.View;

import java.util.Map;

/**
 * Provides config support for the xhrwrapper feature.
 */

@Singleton
public class XhrwrapperConfigContributor implements ConfigContributor {
  /** {@inheritDoc} */
  public void contribute(Map<String,Object> config, Gadget gadget) {
    Map<String, String> xhrWrapperConfig = Maps.newHashMapWithExpectedSize(2);
    View view = gadget.getCurrentView();
    Uri contentsUri = view.getHref();
    xhrWrapperConfig.put("contentUrl", contentsUri == null ? "" : contentsUri.toString());
    if (AuthType.OAUTH.equals(view.getAuthType())) {
      addOAuthConfig(xhrWrapperConfig, view);
    } else if (AuthType.SIGNED.equals(view.getAuthType())) {
      xhrWrapperConfig.put("authorization", "signed");
    }
    config.put("shindig.xhrwrapper", xhrWrapperConfig);
  }

  /** {@inheritDoc} */
  private void addOAuthConfig(Map<String, String> xhrWrapperConfig, View view) {
    Map<String, String> oAuthConfig = Maps.newHashMapWithExpectedSize(3);
    try {
      OAuthArguments oAuthArguments = new OAuthArguments(view);
      oAuthConfig.put("authorization", "oauth");
      oAuthConfig.put("oauthService", oAuthArguments.getServiceName());
      if (!"".equals(oAuthArguments.getTokenName())) {
        oAuthConfig.put("oauthTokenName", oAuthArguments.getTokenName());
      }
      xhrWrapperConfig.putAll(oAuthConfig);
    } catch (GadgetException e) {
      // Do not add any OAuth configuration if an exception was thrown
    }
  }
  
  public void contribute(Map<String,Object> config, String container, String host) {
    // no-op, no container specific configuration
  }
}
