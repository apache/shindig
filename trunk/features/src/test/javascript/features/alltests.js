/*
JsUnit - a JUnit port for JavaScript
Copyright (C) 1999,2000,2001,2002,2003,2006 Joerg Schaible

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
// util for our unit tests
// the runner runs from the features directory. These are all relative to that.
// TODO: figure out how to set the working directory in Rhino.

var testSrcDir = "src/test/javascript/features";
var testToolsDir = "src/test/javascript/lib";
var srcDir = "src/main/javascript/features";

if (!this.JsUtil) {
  if (this.WScript) {
    var fso = new ActiveXObject("Scripting.FileSystemObject");
    var file = fso.OpenTextFile(testToolsDir + "/JsUtil.js", 1);
    var all = file.ReadAll();
    file.Close();
    eval( all );
  } else {
    load(testToolsDir + "/JsUtil.js");
  }

  eval(JsUtil.prototype.include(testSrcDir + '/mocks/env.js'));
  eval(JsUtil.prototype.include(testSrcDir + '/mocks/window.js'));
  eval(JsUtil.prototype.include(testSrcDir + '/mocks/xhr.js'));
  eval(JsUtil.prototype.include(srcDir + '/globals/globals.js'));
  eval(JsUtil.prototype.include(srcDir + '/cloo/cloo.js'));
  eval(JsUtil.prototype.include(srcDir + '/domnode/constants.js'));
  eval(JsUtil.prototype.include(srcDir + '/core.config.base/config.js'));
  eval(JsUtil.prototype.include(srcDir + '/core.config/validators.js'));
  eval(JsUtil.prototype.include(srcDir + '/core.json/json-native.js'));
  eval(JsUtil.prototype.include(srcDir + '/core.json/json-jsimpl.js'));
  eval(JsUtil.prototype.include(srcDir + '/core.json/json-flatten.js'));
  eval(JsUtil.prototype.include(srcDir + '/shindig.auth/auth.js'));
  eval(JsUtil.prototype.include(srcDir + '/core.util.dom/dom.js'));
  eval(JsUtil.prototype.include(srcDir + '/core.util.string/string.js'));
  eval(JsUtil.prototype.include(srcDir + '/core.util.urlparams/urlparams.js'));
  eval(JsUtil.prototype.include(srcDir + '/core.util/util.js'));
  eval(JsUtil.prototype.include(srcDir + '/core.prefs/prefs.js'));
  eval(JsUtil.prototype.include(srcDir + '/core.log/log.js'));
  eval(JsUtil.prototype.include(srcDir + '/core.io/io.js'));
  eval(JsUtil.prototype.include(srcDir + '/container.util/constant.js'));
  eval(JsUtil.prototype.include(srcDir + '/container.util/util.js'));
  eval(JsUtil.prototype.include(srcDir + '/container/service.js'));
  eval(JsUtil.prototype.include(srcDir + '/container.site/site.js'));
  eval(JsUtil.prototype.include(srcDir + '/container.site/site_holder.js'));
  eval(JsUtil.prototype.include(srcDir + '/container.site.gadget/gadget_holder.js'));
  eval(JsUtil.prototype.include(srcDir + '/container.site.gadget/gadget_site.js'));
  eval(JsUtil.prototype.include(srcDir + '/container.site.url/url_holder.js'));
  eval(JsUtil.prototype.include(srcDir + '/container.site.url/url_site.js'));
  eval(JsUtil.prototype.include(srcDir + '/container/container.js'));
  eval(JsUtil.prototype.include(srcDir + '/i18n/currencycodemap.js'));
  eval(JsUtil.prototype.include(srcDir + '/i18n/datetimeformat.js'));
  eval(JsUtil.prototype.include(srcDir + '/i18n/datetimeparse.js'));
  eval(JsUtil.prototype.include(srcDir + '/i18n/formatting.js'));
  eval(JsUtil.prototype.include(srcDir + '/i18n/numberformat.js'));
  eval(JsUtil.prototype.include(srcDir + '/setprefs/setprefs.js'));
  eval(JsUtil.prototype.include(srcDir + '/views/views.js'));
  eval(JsUtil.prototype.include(srcDir + '/shindig.uri/uri.js'));
  eval(JsUtil.prototype.include(srcDir + '/shindig.xhrwrapper/xhrwrapper.js'));
  eval(JsUtil.prototype.include(srcDir + '/xmlutil/xmlutil.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-data-context/datacontext.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-data/data.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-reference/opensocial.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-reference/activity.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-reference/address.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-reference/album.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-reference/bodytype.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-reference/collection.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-reference/container.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-reference/datarequest.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-reference/dataresponse.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-reference/email.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-reference/enum.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-reference/environment.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-reference/idspec.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-reference/mediaitem.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-reference/message.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-reference/name.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-reference/navigationparameters.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-reference/organization.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-reference/person.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-reference/phone.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-reference/responseitem.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-reference/url.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-base/fieldtranslations.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-base/jsonactivity.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-base/jsonalbum.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-base/jsonmediaitem.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-base/jsonperson.js'));
  eval(JsUtil.prototype.include(srcDir + '/opensocial-jsonrpc/jsonrpccontainer.js'));
  eval(JsUtil.prototype.include(srcDir + '/osapi.base/osapi.js'));
  eval(JsUtil.prototype.include(srcDir + '/osapi.base/batch.js'));
  eval(JsUtil.prototype.include(srcDir + '/osapi/jsonrpctransport.js'));
  eval(JsUtil.prototype.include(srcDir + '/osapi/peoplehelpers.js'));
  eval(JsUtil.prototype.include(srcDir + '/gadgets.json.ext/json-xmltojson.js;));
  eval(JsUtil.prototype.include(testToolsDir + "/JsUnit.js"));
  eval(JsUtil.prototype.include(testToolsDir + '/testutils.js'));
  eval(JsUtil.prototype.include(testSrcDir + "/core/authtest.js"));
  eval(JsUtil.prototype.include(testSrcDir + "/core/config-test.js"));
  eval(JsUtil.prototype.include(testSrcDir + "/core/prefstest.js"));
  eval(JsUtil.prototype.include(testSrcDir + "/core.io/iotest.js"));
  eval(JsUtil.prototype.include(testSrcDir + "/opensocial-base/jsonactivitytest.js"));
  eval(JsUtil.prototype.include(testSrcDir + "/opensocial-base/jsonalbumtest.js"));
  eval(JsUtil.prototype.include(testSrcDir + "/opensocial-base/jsonmediaitemtest.js"));
  eval(JsUtil.prototype.include(testSrcDir + "/opensocial-reference/activitytest.js"));
  eval(JsUtil.prototype.include(testSrcDir + "/opensocial-templates/compiler_test.js"));
  eval(JsUtil.prototype.include(testSrcDir + "/opensocial-templates/container_test.js"));
  eval(JsUtil.prototype.include(testSrcDir + "/opensocial-templates/loader_test.js"));
  eval(JsUtil.prototype.include(testSrcDir + "/opensocial-templates/os_test.js"));
  eval(JsUtil.prototype.include(testSrcDir + "/opensocial-templates/template_test.js"));
  eval(JsUtil.prototype.include(testSrcDir + "/opensocial-templates/util_test.js"));
  eval(JsUtil.prototype.include(testSrcDir + "/osapi/osapitest.js"));
  eval(JsUtil.prototype.include(testSrcDir + "/osapi/batchtest.js"));
  eval(JsUtil.prototype.include(testSrcDir + "/osapi/jsonrpctransporttest.js"));
  eval(JsUtil.prototype.include(testSrcDir + "/views/urltemplatetest.js"));
  eval(JsUtil.prototype.include(testSrcDir + "/xhrwrapper/xhrwrappertest.js"));
  eval(JsUtil.prototype.include(testSrcDir + '/shindig.uri/uritest.js'));
  eval(JsUtil.prototype.include(testSrcDir + '/container/util_test.js'));
  eval(JsUtil.prototype.include(testSrcDir + '/container/service_test.js'));
  eval(JsUtil.prototype.include(testSrcDir + '/container/gadget_holder_test.js'));
  eval(JsUtil.prototype.include(testSrcDir + '/container/gadget_site_test.js'));
  eval(JsUtil.prototype.include(testSrcDir + '/container/container_test.js'));
  eval(JsUtil.prototype.include(testSrcDir + '/json-xmltojson/jsonxmltojson-test.js'));
}

function AllTests() {
  TestSuite.call(this, "AllTests");
}

AllTests.inherits(TestSuite);


function AllTests_suite() {
  var suite = new AllTests();
  suite.addTest(JsonRpcTransportTestSuite.prototype.suite());
  suite.addTest(BatchTestSuite.prototype.suite());
  return suite;
}


AllTests.prototype = new TestSuite();
AllTests.prototype.suite = AllTests_suite;
AllTests.glue();

var args;
if (this.WScript) {
  args = new Array();
  for (var i = 0; i < WScript.Arguments.Count(); ++i) {
    args[i] = WScript.Arguments(i);
  }
} else if (this.arguments) {
  args = arguments;
} else {
  args = new Array();
  args.push("AllTests");
}


var result = TextTestRunner.prototype.main(args);
JsUtil.prototype.quit(result);
