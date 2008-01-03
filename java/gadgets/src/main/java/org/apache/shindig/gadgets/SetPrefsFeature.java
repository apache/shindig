package org.apache.shindig.gadgets;

import java.util.Map;

public class SetPrefsFeature extends JsLibraryFeature {

  static {
    JsLibrary.register("setprefs", "../../javascript/gadgets/setprefs.js",
                       JsLibrary.Type.FILE, null, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void process(Gadget gadget, GadgetContext context,
      Map<String, String> params) throws GadgetException {
    gadget.addJsLibrary(JsLibrary.file("setprefs", "setprefs"));
  }
}
