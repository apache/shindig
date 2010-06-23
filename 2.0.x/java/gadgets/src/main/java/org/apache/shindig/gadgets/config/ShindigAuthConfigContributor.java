package org.apache.shindig.gadgets.config;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.shindig.auth.AnonymousSecurityToken;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.auth.SecurityTokenDecoder;
import org.apache.shindig.auth.SecurityTokenException;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.config.ConfigContributor;

import java.util.Map;

@Singleton
public class ShindigAuthConfigContributor implements ConfigContributor {

  private SecurityTokenDecoder securityTokenCodec;

  @Inject
  public ShindigAuthConfigContributor(SecurityTokenDecoder codec) {
    this.securityTokenCodec = codec;
  }

  /** {@inheritDoc} */
  public void contribute(Map<String,Object> config, Gadget gadget) {
    final GadgetContext context = gadget.getContext();
    final SecurityToken authToken = context.getToken();
    if (authToken != null) {
      Map<String, String> authConfig = Maps.newHashMapWithExpectedSize(2);
      String updatedToken = authToken.getUpdatedToken();
      if (updatedToken != null) {
        authConfig.put("authToken", updatedToken);
      }
      String trustedJson = authToken.getTrustedJson();
      if (trustedJson != null) {
        authConfig.put("trustedJson", trustedJson);
      }
      config.put("shindig.auth", authConfig);
    }
  }

  /** {@inheritDoc} */
  public void contribute(Map<String,Object> config, String container, String host) {
    // Inject an anonymous security token TODO set TTL based on cachability of this JS?
    SecurityToken containerToken = new AnonymousSecurityToken(container, 0,"*", 1000L * 60 * 60 * 24);
    Map<String, String> authConfig = Maps.newHashMapWithExpectedSize(2);

    try {
      config.put("shindig.auth", authConfig);
      authConfig.put("authToken", securityTokenCodec.encodeToken(containerToken));

    } catch (SecurityTokenException e) {
      // ignore
    }
  }
}
