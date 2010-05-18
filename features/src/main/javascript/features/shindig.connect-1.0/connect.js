shindig.connect = function() {
  var config = {};
  var FrameIdToGadget = {};
  var moduleIdCounter = 1;
  var Gadgets = {};

  // TODO pull this from container config, namespace variable...
  var LAYOUT = {
    loading:  "Loading...",
    chrome: {
      "default": {
        template: "<h2>%title%</h2>%iframe%",
        width: 300,
        height: 300
      },
      "default.chromeless": {
        template: "%iframe%",
        width: 200,
        height: 80
      },
      "canvas": {
        template: '<h2><a href="#" id="shindig-connect-canvas-close">X</a> %title%</h2>%iframe%',
        width: 600,
        height: 400
      }
    }
  };

  /**
   *
   */

  function init(configuration) {
    config = configuration["shindig.connect-1.0"] || {};
  }

  var requiredConfig = {
    metadataUrl: gadgets.config.NonEmptyStringValidator
  };
  gadgets.config.register("shindig.connect-1.0", requiredConfig, init);

  /**
   *
   */

  function parseTags(pageContext, opt_cb) {
    var request = {};
    var gadgetlist = [];

    // TODO eliminiate dupe code
    jQuery("li\\:login", pageContext).each(function() {
      var tag = jQuery(this);
      var mid = moduleIdCounter++;
      tag.attr("id", "gadget_id_" + mid);
      tag.html(LAYOUT['loading']);
      var g = {url: "http://localhost/~plindner/login.xml", moduleId: mid};
      gadgetlist.push(g);
      Gadgets[mid] = g;
      FrameIdToGadget['remote_iframe_' + mid] = g;
    });

    jQuery("li\\:share", pageContext).each(function() {
      var tag = jQuery(this);
      var mid = moduleIdCounter++;
      tag.attr("id", "gadget_id_" + mid);
      tag.html(LAYOUT['loading']);
      var g = {url: "http://localhost/~plindner/share.xml", moduleId: mid};
      gadgetlist.push(g);
      Gadgets[mid] = g;
      FrameIdToGadget['remote_iframe_' + mid] = g;
    });


    jQuery("os\\:gadget", pageContext).each(function() {
      var tag = jQuery(this);
      var mid = moduleIdCounter++;
      tag.attr("id", "gadget_id_" + mid);
      tag.html(LAYOUT['loading']);
      var g = {url: tag.attr('url'), moduleId: mid};
      gadgetlist.push(g);
      Gadgets[mid] = g;
      FrameIdToGadget['remote_iframe_' + mid] = g;
    });

    // defaults
    var context = {
      country: "ALL",
      language: "ALL",
      view: "default",
      container: "default"
    };

    // ignore page context, we need the global config
    jQuery("os\\:config").each(function() {
      var tag = jQuery(this);

      // Strings
      var attrs = ['country', 'language', 'view'];
      for (var apos = 0; apos < attrs.length; apos++) {
        var attr = attrs[apos];
        context[attr] = tag.attr(attr) || context[attr];
      }

      // Bools
      context.debug = (tag.attr('debug') === 'true');
      context.ignoreCache = (tag.attr('nocache') === 'true');
    });

    var req = gadgets.json.stringify({ context: context, gadgets: gadgetlist});

    // TODO should get URL from container injected value instead of from os:config / default
    var metadataUrl = config.metadataUrl || "/gadgets/metadata";

    jQuery.getJSON(metadataUrl + "?req=" + req + "&callback=?",
        function(data) {
          renderGadgets(data, pageContext, context, opt_cb);
        });
  }

  /**
   *
   */
  function renderGadgets(obj, pageContext, context, opt_cb) {
    if (obj.viewer) {
      // Substitute vars..
      jQuery("os\\:Name", pageContext).each(function() {
        var tag = jQuery(this);
        var person = tag.attr('person');
        tag.html(obj.viewer.name.unstructured);
      });
    }

    var gadgetList = obj.gadgets;

    // calculate the libs parameter
    var features = {};
    for (var i = 0, gadget; gadget = gadgetList[i]; ++i) {
      var feats = gadget.features || [];
      for (var j = 0, feature; feature = feats[j]; ++j) {
        features[feature] = true;
      }
    }
    var libs = [];
    for (var lib in features) {
      libs.push(lib);
    }
    libs.sort();
    libs = libs.join(":");

    // TODO override all that above...
    libs = "core:rpc";

    for (var i = 0, gadget; gadget = gadgetList[i]; ++i) {
      var rpcToken = (0x7FFFFFFF * Math.random()) | 0;

      var newGadget = document.createElement("div");

      if (gadget.errors && gadget.errors.length > 0) {
         // TODO change to error class, use error template
        newGadget.className = "shindig-connect-gadget";
        newGadget.innerHTML = ["Unable to process gadget: ", gadget.url, ". Errors: <pre>", gadget.errors.join("\n"), "</pre>"].join("");
        jQuery("#gadget_id_" + gadget.moduleId).html(newGadget);
      } else {
        var viewname = context.view || 'default';
        var view = (gadget.views) ? gadget.views[viewname] : undefined;
        var style = (view && view.attributes && view.attributes.style) || "gadget";

        // TODO chop views like canvas.XXX to just canvas
        var layout = LAYOUT.chrome[viewname + '.' + style] || LAYOUT.chrome[viewname] || {};
        console.log(layout);

        newGadget.className = "shindig-connect-" + style;
        var remoteIframe = "remote_iframe_" + gadget.moduleId;
        var iframeHeight = view.preferredHeight || gadget.height || layout.height;
        var iframeWidth  = view.preferredWidth  || gadget.width  || layout.width;
        var html = layout.template || '%iframe%';

        html = html.replace('%title%', '<span id="gadget_id_' + gadget.moduleId + '_title">' + gadget.title + '</span>');
        html = html.replace('%iframe%',
         ['<iframe src="', gadget.iframeUrl,
          '&libs=', libs ,
          '&parent=', encodeURIComponent(document.location.protocol  + '//' + document.location.host),
          '#rpcToken=', rpcToken, '"',
          ' id="', remoteIframe, '"',
          ' name="', remoteIframe, '"',
          ' style="height: ', iframeHeight,  'px"',
          ' scrolling="', (gadget.scrolling ? 'yes' : 'no'), '"',
          ' width="', iframeWidth, '"',
          '></iframe>'
        ].join(""));
        newGadget.innerHTML = html;
        jQuery("#gadget_id_" + gadget.moduleId).html(newGadget);
        gadgets.rpc.setupReceiver(remoteIframe);
      }
    }
    if (opt_cb) opt_cb();
  }

  jQuery(document).ready(function() {
    parseTags(this);
    // Add boilerplate things
    jQuery('body').append('<div id="shindig-connect-canvas"></div><div id="shindig-connect-canvas-background"></div>');
    // Add zuul gadget if not there already..
  });


  // TODO add refetch param
  gadgets.rpc.register('reload_page', function(refetch) {
    window.location.reload( false );
  });
  
  gadgets.rpc.register('resize_iframe', function(height) {
    // TODO deal with canvas page.
    var element = document.getElementById(this.f);
    if (element) {
      element.style.height = height + 'px';
    }
  });

  gadgets.rpc.register('requestNavigateTo', function(view, opt_params) {
    var id = this.f;
    var g = FrameIdToGadget[id];

    var canvas = jQuery('#shindig-connect-canvas');

    var moduleId = moduleIdCounter++;
    canvas.html('<os:gadget url="' + g.url + '"></os:gadget><os:config view="canvas"></os:config>');

    var windowWidth = document.documentElement.clientWidth;
    var windowHeight = document.documentElement.clientHeight;

    // Canvas at 80% of current size
    var height = Math.floor(windowHeight * .8);
    var width = Math.floor(windowWidth * .8);

    // allow local override
    if (opt_params) {
      height = opt_params.height || height;
      width = opt_params.width || width;
    }

    // Set div size, and make modal, add close box?
    parseTags(canvas, function() {
      var background = jQuery("#shindig-connect-canvas-background");

      var popupHeight = canvas.height();
      var popupWidth = canvas.width();

      canvas.css({
        "position": "absolute",
        "top": windowHeight / 2 - popupHeight / 2,
        "left": windowWidth / 2 - popupWidth / 2
      });
      background.css({"opacity": "0.7", height: windowHeight});
      background.fadeIn("slow");
      canvas.fadeIn("slow");
      jQuery('#shindig-connect-canvas-close').click(function() {
        $(background).fadeOut("slow");
        $(canvas).fadeOut("slow");
      });
    });
  });
}();
