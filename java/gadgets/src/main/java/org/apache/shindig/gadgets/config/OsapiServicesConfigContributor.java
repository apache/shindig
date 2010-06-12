package org.apache.shindig.gadgets.config;

import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.render.RpcServiceLookup;

import java.util.Map;

/**
 * Populates the osapi.services configuration, which includes
 * the osapi endpoints this container supports.
 *
 * TODO osapi.services as a configuration parameter does not
 * match a specific feature.  It would be better to store this as
 * 'osapi:{services: {...}}}
 */
@Singleton
public class OsapiServicesConfigContributor implements ConfigContributor {

  protected final RpcServiceLookup rpcServiceLookup;

  @Inject
  public OsapiServicesConfigContributor(RpcServiceLookup rpcServiceLookup) {
    this.rpcServiceLookup = rpcServiceLookup;
  }

  /** {@inheritDoc} */
  public void contribute(Map<String, Object> config, Gadget gadget) {
    GadgetContext ctx = gadget.getContext();
    addServicesConfig(config, ctx.getContainer(), ctx.getHost());
  }

  /** {@inheritDoc} */
  public void contribute(Map<String,Object> config, String container, String host) {
    addServicesConfig(config, container, host);
  }

  /**
   * Add osapi.services to the config
   * @param config config map to add it to.
   */
  private void addServicesConfig(Map<String,Object> config, String container, String host) {
    if (rpcServiceLookup != null) {
      Multimap<String, String> endpoints = rpcServiceLookup.getServicesFor(container, host);
      config.put("osapi.services", endpoints);
    }
  }
}