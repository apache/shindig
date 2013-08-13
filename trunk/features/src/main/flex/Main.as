/*
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

import flash.external.ExternalInterface;
import System.security;

/**
 * XPC Flash Based Transport
 * Original design by evn@google.com (Eduardo Vela)
 */
class Main {
  // Ensures that the callbacks installed by the SWF are installed only once per page context.
  private static var SINGLETON:Boolean = false;

  // Constructor: unused in this case. 
  public function Main() {
  }

  /**
   * Simple helper function to replace instances of the given from_str in the provided
   * first argument with the given to_str string.
   * @param str {String} String whose contents to replace.
   * @param from_str {String} Token to replace in str.
   * @param to_str {String} String to put in place of from_str in str.
   * @returns Modified string.
   */
  public static function replace(str:String, from_str:String, to_str:String):String {
    var out_str:String = "";
    var search_ix:Number = 0;
    while (search_ix < str.length) {
      var found_ix:Number = str.indexOf(from_str, search_ix);
      if (found_ix != -1) {
        out_str = out_str.concat(str.substring(search_ix, found_ix)).concat(to_str);
        search_ix = found_ix + from_str.length;
      } else {
        out_str = out_str.concat(str.substring(search_ix));
        search_ix = str.length;
      }
    }
    return out_str;
  }

  public static function esc(str:String):String {
    return replace(str, "\\", "\\\\");
  }

  /**
   * Removes the port piece of an assumed well-structured provided host:port pair.
   * @param {String} str String representing a URI authority.
   * @returns The host-only (minus port) piece of the inbound authority String.
   */
  public static function stripPortIfPresent(str:String):String {
    var col_ix:Number = str.indexOf(":");
    if (col_ix == -1) {
      return str;
    }
    return str.substring(0, col_ix);
  }

  /**
   * Implementation of handlers facilitating cross-domain communication through
   * Flash's ExternalInterface and LocalConnection facilities, offering sender
   * domain verification.
   *
   * This method may only be run once such that it has any effect, a fact enforced
   * by a static SINGLETON boolean. This prevents confusion if the SWF is accidentally
   * loaded more than once in a given Window or page.
   *
   * The method whitelists HTTP-to-SWF access only for the domain provided in the
   * inbound 'origin' argument. This argument in turn is passed along in each
   * call made that passes along cross-domain messages on the caller's behalf, ensuring
   * that domain verification works as intended.
   *
   * It installs a single 'setup' handler, callable from JS, which in turn sets up
   * a sendMessage one-way communication method, while starting to listen to
   * an equivalent channel set up by a party on the opposite side of an IFRAME
   * boundary. A child context should pass in "INNER" for the role argument to setup;
   * the parent passes "OUTER". rpc_key is a unique key provided on the IFRAME URL's
   * hash disambiguating it from all other IFRAMEs on-page, window, or machine, while
   * channel_id is the child's ID.
   *
   * This SWF is written specifically for the gadgets.rpc cross-domain communication
   * library, but could be rather easily adapted - with passed-in callback method
   * names, for instance - to use by other libraries as well.
   */
  public static function main(swfRoot:MovieClip):Void {
    var escFn:Function = esc;
    var replaceFn:Function = replace;
    
    if (SINGLETON) return;
    SINGLETON = true;
    
    var my_origin:String;

    if (_level0.origin == undefined){
      // No origin: accept from all HTTP callers.
      // Domain verification will not apply.
      my_origin = "http://*";
    } else {
      // Get origin from the query string.
      my_origin = _level0.origin;
    }

    var ready_method:String = "gadgets.rpctx.flash._ready";
    var recv_method:String = "gadgets.rpctx.flash._receiveMessage";
    var setup_done_method:String = "gadgets.rpctx.flash._setupDone";

    if (_level0.jsl == "1") {
      // Use 'safe-exported' methods.
      ready_method = "___jsl._fm.ready";
      recv_method = "___jsl._fm.receiveMessage";
      setup_done_method = "___jsl._fm.setupDone";
    }

    // Flash doesn't accept/honor ports, so we strip one if present
    // for canonicalization.
    var domain:String = stripPortIfPresent(
        my_origin.substr(my_origin.indexOf("//") + 2, my_origin.length));
    
    // Whitelist access to this SWF for the sending HTTP domain.
    // The my_origin field from which domain derives is passed along
    // with each sent message. Together, these ensure domain verification
    // of all sent messages is possible.
    if (my_origin.substr(0,5) === "http:") {
      security.allowInsecureDomain(domain);
    } else {
      security.allowDomain(domain);
    }

    // Install global communication channel setup method.
    ExternalInterface.addCallback("setup", { }, function(rpc_key:String, channel_id:String, role:String) {
      var other_role:String;

      if (role == "INNER") {
        other_role = "OUTER";
      } else {
        other_role = "INNER";
        role = "OUTER";
      }

      var receiving_lc:LocalConnection = new LocalConnection();
      var sending_lc:LocalConnection = new LocalConnection();
      receiving_lc.receiveMessage =
          function(to_origin:String, from_origin:String, in_rpc_key:String, message:String) {
        if ((to_origin === "*" || to_origin === my_origin) && (in_rpc_key == rpc_key)) {
          ExternalInterface.call(recv_method, escFn(message), escFn(from_origin), escFn(to_origin));
        }
      };

      ExternalInterface.addCallback("sendMessage_" + channel_id + "_" + rpc_key + "_" + role,
            { }, function(message:String, to_origin:String) {
        if (!to_origin) to_origin = "*";
        var sendId:String =
            replaceFn("channel_" + channel_id + "_" + rpc_key + "_" + other_role, ":", "");
        sending_lc.send(sendId,
            "receiveMessage", to_origin, my_origin, rpc_key, message);
      });
      var recvId:String = replaceFn("channel_" + channel_id + "_" + rpc_key + "_" + role, ":", "");
      receiving_lc.connect(recvId);
      if (role == "INNER") {
        // In child context, trigger notice that the setup method is complete.
        // This in turn initiates a child-to-parent polling procedure to complete a bidirectional
        // communication handshake, since otherwise meaningful messages could be passed and dropped
        // before the receiving end was ready.
        ExternalInterface.call(setup_done_method);
      }
    });

    // Signal completion of the setup callback to calling-context JS for proper ordering.
    ExternalInterface.call(ready_method);
  }
}
