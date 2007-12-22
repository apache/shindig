var goog = goog || {};
goog.global = this;
goog.evalWorksForGlobals_ = null;
goog.provide = function(name) {
  goog.exportPath_(name)
};
goog.exportPath_ = function(name, opt_object) {
  var parts = name.split("."), cur = goog.global, part;
  while(part = parts.shift()) {
    if(!parts.length && goog.isDef(opt_object)) {
      cur[part] = opt_object
    }else if(cur[part]) {
      cur = cur[part]
    }else {
      cur = (cur[part] = {})
    }
  }
};
goog.getObjectByName = function(name) {
  var parts = name.split("."), cur = goog.global;
  for(var part;part = parts.shift();) {
    if(cur[part]) {
      cur = cur[part]
    }else {
      return null
    }
  }return cur
};
goog.globalize = function(obj, opt_global) {
  var global = opt_global || goog.global;
  for(var x in obj) {
    global[x] = obj[x]
  }
};
goog.addDependency = function(relPath, provides, requires) {
};
goog.require = function(rule) {
};
goog.basePath = "";
goog.nullFunction = function() {
};
goog.JsType_ = {UNDEFINED:"undefined", NUMBER:"number", STRING:"string", BOOLEAN:"boolean", FUNCTION:"function", OBJECT:"object"};
goog.isDef = function(val) {
  return typeof val != goog.JsType_.UNDEFINED
};
goog.isNull = function(val) {
  return val === null
};
goog.isDefAndNotNull = function(val) {
  return goog.isDef(val) && !goog.isNull(val)
};
goog.isArray = function(val) {
  return val instanceof Array || goog.isObject(val) && typeof val.join == goog.JsType_.FUNCTION && typeof val.reverse == goog.JsType_.FUNCTION
};
goog.isArrayLike = function(val) {
  return goog.isObject(val) && typeof val.length == goog.JsType_.NUMBER
};
goog.isDateLike = function(val) {
  return goog.isObject(val) && goog.isFunction(val.getFullYear)
};
goog.isString = function(val) {
  return typeof val == goog.JsType_.STRING
};
goog.isBoolean = function(val) {
  return typeof val == goog.JsType_.BOOLEAN
};
goog.isNumber = function(val) {
  return typeof val == goog.JsType_.NUMBER
};
goog.isFunction = function(val) {
  return typeof val == goog.JsType_.FUNCTION || !(!(val && val.call))
};
goog.isObject = function(val) {
  return val != null && typeof val == goog.JsType_.OBJECT
};
goog.getHashCode = function(obj) {
  if(obj.hasOwnProperty && obj.hasOwnProperty(goog.HASH_CODE_PROPERTY_)) {
    return obj[goog.HASH_CODE_PROPERTY_]
  }if(!obj[goog.HASH_CODE_PROPERTY_]) {
    obj[goog.HASH_CODE_PROPERTY_] = ++goog.hashCodeCounter_
  }return obj[goog.HASH_CODE_PROPERTY_]
};
goog.removeHashCode = function(obj) {
  if("removeAttribute" in obj) {
    obj.removeAttribute(goog.HASH_CODE_PROPERTY_)
  }try {
    delete obj[goog.HASH_CODE_PROPERTY_]
  }catch(ex) {
  }
};
goog.HASH_CODE_PROPERTY_ = "closure_hashCode_";
goog.hashCodeCounter_ = 0;
goog.cloneObject = function(proto) {
  if(goog.isObject(proto)) {
    if(proto.clone) {
      return proto.clone()
    }var clone = goog.isArray(proto) ? [] : {};
    for(var key in proto) {
      clone[key] = goog.cloneObject(proto[key])
    }return clone
  }return proto
};
goog.bind = function(fn, self) {
  var boundArgs = fn.boundArgs_;
  if(arguments.length > 2) {
    var args = Array.prototype.slice.call(arguments, 2);
    if(boundArgs) {
      args.unshift.apply(args, boundArgs)
    }boundArgs = args
  }self = fn.boundSelf_ || self;
  fn = fn.boundFn_ || fn;
  var newfn, context = self || goog.global;
  if(boundArgs) {
    newfn = function() {
      var args = Array.prototype.slice.call(arguments);
      args.unshift.apply(args, boundArgs);
      return fn.apply(context, args)
    }
  }else {
    newfn = function() {
      return fn.apply(context, arguments)
    }
  }newfn.boundArgs_ = boundArgs;
  newfn.boundSelf_ = self;
  newfn.boundFn_ = fn;
  return newfn
};
goog.partial = function(fn) {
  var args = Array.prototype.slice.call(arguments, 1);
  args.unshift(fn, null);
  return goog.bind.apply(null, args)
};
goog.mixin = function(target, source) {
  for(var x in source) {
    target[x] = source[x]
  }
};
goog.now = function() {
  return(new Date).getTime()
};
goog.globalEval = function(script) {
  if(goog.global.execScript) {
    goog.global.execScript(script, "JavaScript")
  }else if(goog.global.eval) {
    if(goog.evalWorksForGlobals_ == null) {
      goog.global.eval("var _et_ = 1;");
      if(typeof goog.global._et_ != "undefined") {
        delete goog.global._et_;
        goog.evalWorksForGlobals_ = true
      }else {
        goog.evalWorksForGlobals_ = false
      }
    }if(goog.evalWorksForGlobals_) {
      goog.global.eval(script)
    }else {
      var doc = goog.global.document, scriptElt = doc.createElement("script");
      scriptElt.type = "text/javascript";
      scriptElt.defer = false;
      scriptElt.text = script;
      doc.body.appendChild(scriptElt);
      doc.body.removeChild(scriptElt)
    }
  }else {
    throw Error("goog.globalEval not available");
  }
};
goog.getMsg = function(str, opt_values) {
  var values = opt_values || {};
  for(var key in values) {
    str = str.replace(new RegExp("\\{\\$" + key + "\\}", "gi"), values[key])
  }return str
};
goog.exportSymbol = function(publicPath, object) {
  goog.exportPath_(publicPath, object)
};
goog.exportProperty = function(object, publicName, symbol) {
  object[publicName] = symbol
};
if(!Function.prototype.apply) {
  Function.prototype.apply = function(oScope, args) {
    var sarg = [], rtrn, call;
    if(!oScope)oScope = goog.global;
    if(!args)args = [];
    for(var i = 0;i < args.length;i++) {
      sarg[i] = "args[" + i + "]"
    }call = "oScope.__applyTemp__.peek().(" + sarg.join(",") + ");";
    if(!oScope.__applyTemp__) {
      oScope.__applyTemp__ = []
    }oScope.__applyTemp__.push(this);
    rtrn = eval(call);
    oScope.__applyTemp__.pop();
    return rtrn
  }
}Function.prototype.bind = function(self) {
  if(arguments.length > 1) {
    var args = Array.prototype.slice.call(arguments, 1);
    args.unshift(this, self);
    return goog.bind.apply(null, args)
  }else {
    return goog.bind(this, self)
  }
};
Function.prototype.partial = function() {
  var args = Array.prototype.slice.call(arguments);
  args.unshift(this, null);
  return goog.bind.apply(null, args)
};
Function.prototype.inherits = function(parentCtor) {
  goog.inherits(this, parentCtor)
};
goog.inherits = function(childCtor, parentCtor) {
  function tempCtor() {
  }
  tempCtor.prototype = parentCtor.prototype;
  childCtor.superClass_ = parentCtor.prototype;
  childCtor.prototype = new tempCtor;
  childCtor.prototype.constructor = childCtor
};
Function.prototype.mixin = function(source) {
  goog.mixin(this.prototype, source)
};var s2 = {};
s2.data = {};
s2.data.Data = function() {
};
s2.data.Data.prototype.each = function(fn) {
};
s2.data.Data.prototype.eachProp = function(fn) {
};
s2.data.Data.prototype.getProp = function(name) {
  this.eachProp(function(curName, value) {
    if(curName == name) {
      return value
    }
  });
  return null
};
s2.data.Data.prototype.putProp = function(name, value) {
};
s2.data.Data.prototype.removeProp = function(name) {
};
s2.data.Data.prototype.propSize = function() {
  var count = 0;
  this.eachProp(function() {
    count++
  });
  return count
};
s2.data.Data.prototype.removeAllProps = function() {
  var props = [];
  this.eachProp(function(name, value) {
    props.push(name)
  });
  for(var i = 0;i < props.length;i++) {
    this.removeProp(props[i])
  }
};
s2.data.Data.prototype.getAt = function(key) {
  this.each(function(value, curKey) {
    if(curKey == key) {
      return value
    }
  });
  return null
};
s2.data.Data.prototype.putAt = function(key, value) {
};
s2.data.Data.prototype.removeAt = function(key) {
};
s2.data.Data.prototype.removeAll = function() {
  var keys = [];
  this.each(function(value, key) {
    keys.push(key)
  });
  for(var i = keys.length - 1;i >= 0;i--) {
    this.removeAt(keys[i])
  }
};
s2.data.Data.prototype.size = function() {
  var count = 0;
  this.each(function() {
    count++
  });
  return count
};
s2.data.Data.prototype.equals = function(other) {
  return s2.data.equals(this, other, true)
};
s2.data.Data.prototype.isCollection = function() {
  return false
};
s2.data.Data.prototype.find = function(matcher) {
  if(goog.isFunction(matcher)) {
    var result = [];
    this.each(function(value) {
      if(matcher(value)) {
        result.push(value)
      }
    });
    return new s2.data.ArrayWrapper(result)
  }else {
    var expr = new s2.data.Expr(matcher);
    return expr.find(this)
  }
};
s2.data.Data.prototype.findOne = function(matcher) {
  if(goog.isFunction(matcher)) {
    var found = null;
    this.each(function(value) {
      if(matcher(value) && !found) {
        found = value
      }
    });
    return found
  }else {
    var expr = new s2.data.Expr(matcher);
    return expr.evaluate(this)
  }
};
s2.data.Data.prototype.transform = function(transform) {
  var results = [];
  this.each(function(value, key) {
    results.push(transform(value, key))
  });
  return s2.data.from(results)
};
s2.data.Data.prototype.sort = function(sort, opt_descending) {
  var descending = !(!opt_descending), results = [];
  this.each(function(value, key) {
    results.push(value)
  });
  if(goog.isFunction(sort)) {
    results.sort(sort, descending)
  }else {
    var expr = new s2.data.Expr(sort);
    results.sort(function(lhs, rhs) {
      var lhfield = expr.evaluate(lhs).getValue(), rhfield = expr.evaluate(rhs).getValue(), compare = s2.data.compare(lhfield, rhfield);
      if(descending) {
        return-1 * compare
      }else {
        return compare
      }
    })
  }return s2.data.from(results)
};
s2.data.Data.prototype.evaluate = function(expr) {
  var expr = new s2.data.Expr(expr);
  return expr.evaluate(this)
};
s2.data.Data.prototype.getValue = function() {
  return this
};
s2.data.Data.prototype.newInstance = function() {
};
s2.data.Data.prototype.clone = function() {
  var clone = this.newInstance();
  s2.data.cloneContents(this, clone);
  return clone
};
s2.data.Data.prototype.getRoot = function() {
  var parent = this.getParent();
  if(parent) {
    return parent.getRoot()
  }else {
    return s2.data.Root.getGlobal()
  }
};
s2.data.Data.prototype.getParent = function() {
  return null
};
s2.data.Data.prototype.setParent = function() {
};
s2.data.Data.prototype.isRef = function() {
  return false
};
s2.data.from = function(obj, opt_parent, opt_wrapPrimitives) {
  if(goog.isObject(obj)) {
    if(obj.each || obj.eachProp) {
      return obj
    }else if(s2.data.Ref.canCreateReference(obj)) {
      return s2.data.Ref.createRef(obj)
    }else if(goog.isArray(obj)) {
      return new s2.data.ArrayWrapper(obj)
    }else {
      return new s2.data.ObjectWrapper(obj)
    }
  }else {
    if(opt_wrapPrimitives) {
      if(obj == null) {
        return s2.data.Null.getNull()
      }else {
        return new s2.data.Primitive(obj)
      }
    }else {
      return obj
    }
  }
};
s2.data.fromParent = function(obj, parent, opt_wrapPrimitives) {
  var obj = s2.data.from(obj, opt_wrapPrimitives);
  if(obj && obj.setParent) {
    obj.setParent(parent)
  }return obj
};
s2.data.unwrapIfNeeded = function(obj) {
  if(obj == null) {
    return obj
  }else if(obj[s2.data.DATA_PROP]) {
    return obj[s2.data.DATA_PROP]
  }else {
    return obj
  }
};
s2.data.Data.prototype.toDebugString = function() {
  var out = "";
  this.eachProp(function(name, value) {
    out += name + ": " + value.toString() + "\n"
  });
  this.each(function(value, key) {
    out += "[" + key + "]: " + value.toString() + "\n"
  });
  if(out == "") {
    out = "<No Data>"
  }return out
};
s2.data.equals = function(lhs, rhs, opt_noTestObjEquals) {
  if(lhs === rhs) {
    return true
  }if(lhs == null && rhs != null) {
    return false
  }if(rhs == null && lhs != null) {
    return false
  }if(!opt_noTestObjEquals && lhs.equals) {
    return lhs.equals(rhs)
  }if(!opt_noTestObjEquals && rhs.equals) {
    return rhs.equals(lhs)
  }if(goog.isString(lhs) || goog.isNumber(lhs)) {
    return lhs == rhs
  }if(lhs.propSize() != rhs.propSize()) {
    return false
  }var mismatches = 0;
  lhs.eachProp(function(name, value) {
    if(!s2.data.equals(rhs.getProp(name), value)) {
      mismatches++
    }
  });
  if(mismatches > 0) {
    return false
  }if(lhs.size() != rhs.size()) {
    return false
  }mismatches = 0;
  lhs.each(function(value, key) {
    if(!s2.data.equals(rhs.getAt(key), value)) {
      mismatches++
    }
  });
  if(mismatches > 0) {
    return false
  }return true
};
s2.data.compare = function(lhs, rhs) {
  if(lhs == null && rhs != null) {
    return 1
  }if(rhs == null && lhs != null) {
    return-1
  }if(rhs == lhs) {
    return 0
  }if(goog.isString(lhs)) {
    lhs = lhs.toLowerCase()
  }if(goog.isString(rhs)) {
    rhs = rhs.toLowerCase()
  }return rhs > lhs ? -1 : 1
};
s2.data.cloneContents = function(src, target) {
  src.eachProp(function(name, value) {
    if(value.clone) {
      value = value.clone()
    }target.putProp(name, value)
  });
  src.each(function(value, key) {
    if(value.clone) {
      value = value.clone()
    }target.putAt(key, value)
  })
};
s2.data.DATA_PROP = "__data__";
s2.data.PARENT_PROP = "__parentdata__";s2.data.Expr = function(src) {
  if(src.evaluate) {
    return src
  }if(goog.isFunction(src)) {
    this.evaluator_ = src
  }else if(goog.isString(src)) {
    this.src_ = src;
    this.getterStack_ = [];
    if(src.indexOf("$") == 0) {
      this.getterStack_.push({type:"getGlobal"});
      src = src.substring(1)
    }var parts = src.split(".");
    for(var i = 0;i < parts.length;i++) {
      var part = parts[i], indexes = part.split("[");
      if(!goog.string.startsWith(part, "[")) {
        this.getterStack_.push({type:"getProp", value:indexes[0]})
      }for(var j = 1;j < indexes.length;j++) {
        var index = indexes[j], indexEnd = index.indexOf("]"), indexValue = index.substring(0, indexEnd), firstChar = index.substring(0, 1);
        if(firstChar == "*") {
          this.getterStack_.push({type:"getAllChildren"})
        }else if(firstChar == "'" || firstChar == '"') {
          this.getterStack_.push({type:"getAt", value:indexValue.substring(1, indexValue.length - 1)})
        }else {
          this.getterStack_.push({type:"getAt", value:Number(indexValue)})
        }
      }
    }var getterStack = this.getterStack_;
    this.evaluator_ = function(context) {
      var cur = context;
      for(var i = 0;i < getterStack.length;i++) {
        var type = getterStack[i].type, value = getterStack[i].value;
        if(type == "getGlobal") {
          if(!cur) {
            cur = s2.data.Root.getGlobal()
          }else {
            cur = cur.getRoot()
          }
        }if(type == "getProp") {
          cur = cur.getProp(value)
        }else if(type == "getAt") {
          cur = cur.getAt(value)
        }else if(type == "getAllChildren") {
          cur = cur.find(function() {
            return true
          })
        }
      }return cur
    }
  }
};
s2.data.Expr.prototype.evaluate = function(opt_context) {
  var context = opt_context || s2.data.Root.getGlobal(), result = this.evaluator_(new s2.data.ChainingProxy(context));
  if(result.getProxiedObject) {
    result = result.getProxiedObject()
  }return result
};
s2.data.Expr.prototype.find = function(opt_context) {
  var context = opt_context || s2.data.Root.getGlobal(), result = this.evaluator_(new s2.data.ChainingProxy(context));
  if(!result.isCollection()) {
    if(result.getValue && result.getValue() == null) {
      result = s2.data.from([])
    }else {
      result = s2.data.from([result])
    }
  }return result
};s2.data.Null = function() {
  s2.data.Data.call(this)
};
s2.data.Null.inherits(s2.data.Data);
s2.data.Null.prototype.getAt = function(key) {
  return s2.data.Null.getNull()
};
s2.data.Null.prototype.getProp = function(name) {
  return s2.data.Null.getNull()
};
s2.data.Null.prototype.getValue = function() {
  return null
};
s2.data.Null.prototype.equals = function(other) {
  return other == null || other.getValue && other.getValue() == null
};
s2.data.Null.theOnlyNullData_ = new s2.data.Null;
s2.data.Null.getNull = function() {
  return s2.data.Null.theOnlyNullData_
};s2.data.Primitive = function(value) {
  s2.data.Data.call(this);
  this.value_ = value
};
s2.data.Primitive.inherits(s2.data.Data);
s2.data.Primitive.prototype.getAt = function(key) {
  return s2.data.Null.getNull()
};
s2.data.Primitive.prototype.getProp = function(name) {
  return s2.data.Null.getNull()
};
s2.data.Primitive.prototype.equals = function(other) {
  if(other.getValue) {
    return this.value_ == other.getValue()
  }else {
    return this.value_ == other
  }
};
s2.data.Primitive.prototype.getValue = function() {
  return this.value_
};s2.data.Proxy = function(original) {
  this.original_ = original
};
s2.data.Proxy.prototype.getProxiedObject = function() {
  return this.original_
};
s2.data.Proxy.createProxyFunction = function(name) {
  return function() {
    var proxied = this.getProxiedObject();
    return proxied[name].apply(proxied, arguments)
  }
};
s2.data.Proxy.createPrototype = function() {
  for(var name in s2.data.Data.prototype) {
    s2.data.Proxy.prototype[name] = s2.data.Proxy.createProxyFunction(name)
  }
};
s2.data.Proxy.createPrototype();s2.data.ChainingProxy = function(toProxy) {
  s2.data.Proxy.call(this, toProxy)
};
s2.data.ChainingProxy.inherits(s2.data.Proxy);
s2.data.ChainingProxy.prototype.getAt = function(key) {
  var result = s2.data.Proxy.prototype.getAt.call(this, key);
  return this.decorate_(result)
};
s2.data.ChainingProxy.prototype.getRoot = function() {
  var result = s2.data.Proxy.prototype.getRoot.call(this);
  return this.decorate_(result)
};
s2.data.ChainingProxy.prototype.getProp = function(name) {
  var result = s2.data.Proxy.prototype.getProp.call(this, name);
  return this.decorate_(result)
};
s2.data.ChainingProxy.prototype.decorate_ = function(result) {
  if(result == null) {
    return s2.data.Null.getNull()
  }else if(!goog.isObject(result)) {
    return new s2.data.Primitive(result)
  }else {
    return new s2.data.ChainingProxy(result)
  }
};s2.data.Ref = function(type, id) {
  s2.data.Proxy.call(this, null);
  this.type_ = type;
  this.id_ = id
};
s2.data.Ref.inherits(s2.data.Proxy);
s2.data.Ref.prototype.getProxiedObject = function() {
  return s2.data.Repository.getRepositoryByType(this.type_, this.getRoot()).getAt(this.id_)
};
s2.data.Ref.prototype.getRoot = function() {
  if(this.parent_) {
    return this.parent_.getRoot()
  }else {
    return s2.data.Root.getGlobal()
  }
};
s2.data.Ref.prototype.getParent = function() {
  return this.parent_
};
s2.data.Ref.prototype.setParent = function(parent) {
  this.parent_ = parent
};
s2.data.Ref.prototype.isRef = function() {
  return true
};
s2.data.Ref.prototype.toString = function() {
  return this.type_ + ": " + this.id_
};
s2.data.Ref.Field = {Type:"Type", Id:"Id"};
s2.data.Ref.canCreateReference = function(obj) {
  return obj && obj[s2.data.Ref.Field.Type] && obj[s2.data.Ref.Field.Id]
};
s2.data.Ref.createRef = function(obj) {
  var dataRef = new s2.data.Ref(obj[s2.data.Ref.Field.Type], obj[s2.data.Ref.Field.Id]);
  obj[s2.data.DATA_PROP] = dataRef;
  return dataRef
};s2.data.ArrayWrapper = function(array) {
  this.array_ = array;
  this.array_[s2.data.DATA_PROP] = this
};
s2.data.ArrayWrapper.inherits(s2.data.Data);
s2.data.ArrayWrapper.prototype.isCollection = function() {
  return true
};
s2.data.ArrayWrapper.prototype.each = function(fn) {
  var len = this.array_.length;
  for(var i = 0;i < len;i++) {
    var value = this.array_[i];
    fn(s2.data.fromParent(value, this), i)
  }
};
s2.data.ArrayWrapper.prototype.getAt = function(key) {
  return s2.data.fromParent(this.array_[key], this)
};
s2.data.ArrayWrapper.prototype.putAt = function(key, value) {
  if(value.setParent) {
    value.setParent(this)
  }this.array_[key] = s2.data.from(value)
};
s2.data.ArrayWrapper.prototype.removeAt = function(key) {
  this.array_.splice(key, 1)
};
s2.data.ArrayWrapper.prototype.getProp = function(name) {
  var results = [];
  this.each(function(value, key) {
    if(value.getProp) {
      var prop = value.getProp(name);
      if(prop) {
        if(prop.isCollection && prop.isCollection()) {
          prop.each(function(value) {
            results.push(value)
          })
        }else {
          results.push(prop)
        }
      }
    }
  });
  return new s2.data.ArrayWrapper(results)
};
s2.data.ArrayWrapper.prototype.newInstance = function() {
  return new s2.data.ArrayWrapper([])
};
s2.data.ArrayWrapper.prototype.push = function(value) {
  this.array_.push(s2.data.unwrapIfNeeded(value))
};
s2.data.ArrayWrapper.prototype.getParent = function() {
  return s2.data.from(this.array_[s2.data.PARENT_PROP])
};
s2.data.ArrayWrapper.prototype.setParent = function(parent) {
  this.array_[s2.data.PARENT_PROP] = s2.data.unwrapIfNeeded(parent)
};s2.data.ObjectWrapper = function(obj) {
  this.obj_ = obj;
  this.obj_[s2.data.DATA_PROP] = this
};
s2.data.ObjectWrapper.inherits(s2.data.Data);
s2.data.ObjectWrapper.prototype.newInstance = function() {
  return new s2.data.ObjectWrapper({})
};
s2.data.ObjectWrapper.prototype.eachProp = function(fn) {
  for(var name in this.obj_) {
    var value = this.obj_[name];
    if(name != s2.data.DATA_PROP && name != s2.data.PARENT_PROP && !goog.isFunction(value)) {
      fn(name, s2.data.fromParent(value, this))
    }
  }
};
s2.data.ObjectWrapper.prototype.getProp = function(name) {
  var value = this.obj_[name];
  if(name != s2.data.DATA_PROP && name != s2.data.PARENT_PROP && !goog.isFunction(value)) {
    return s2.data.fromParent(value, this)
  }return null
};
s2.data.ObjectWrapper.prototype.removeProp = function(name) {
  delete this.obj_[name]
};
s2.data.ObjectWrapper.prototype.putProp = function(name, value) {
  if(value.setParent) {
    value.setParent(this)
  }this.obj_[name] = s2.data.unwrapIfNeeded(value)
};
s2.data.ObjectWrapper.prototype.getParent = function() {
  return s2.data.from(this.obj_[s2.data.PARENT_PROP])
};
s2.data.ObjectWrapper.prototype.setParent = function(parent) {
  this.obj_[s2.data.PARENT_PROP] = s2.data.unwrapIfNeeded(parent)
};s2.data.Repository = function(name, type, opt_parent) {
  var parent = opt_parent || s2.data.Root.getGlobal();
  this.name_ = name;
  this.parent_ = parent;
  this.type_ = type;
  this.instances_ = {};
  if(this.parent_.getProp(name)) {
    return this.parent_.getProp(name)
  }this.parent_.putProp(name, this);
  s2.data.Repository.getTypeMapRoot_(this.parent_).putProp(type, this)
};
s2.data.Repository.inherits(s2.data.Data);
s2.data.Repository.getRepositoryByType = function(type, opt_root) {
  var root = opt_root || s2.data.getGlobalRoot();
  return s2.data.Repository.getTypeMapRoot_(root).getProp(type)
};
s2.data.Repository.getTypeMapRoot_ = function(root) {
  var map = root.getProp("DataStores");
  if(!map) {
    root.putProp("DataStores", s2.data.from({}));
    map = root.getProp("DataStores")
  }return map
};
s2.data.Repository.prototype.getAt = function(key) {
  return s2.data.fromParent(this.instances_[key], this)
};
s2.data.Repository.prototype.putAt = function(key, value) {
  if(value.setParent) {
    value.setParent(this)
  }this.instances_[key] = value
};
s2.data.Repository.prototype.removeAt = function(key) {
  var value = this.instances_[key];
  if(value && value.setParent) {
    value.setParent(null)
  }delete this.instances_[key]
};
s2.data.Repository.prototype.getParent = function() {
  return this.parent_
};
s2.data.Repository.prototype.setParent = function(parent) {
  this.parent_ = parent
};
s2.data.Repository.prototype.each = function(fn) {
  for(var type in this.instances_) {
    fn(this.instances_[type], type)
  }
};s2.data.Root = function() {
  s2.data.ObjectWrapper.call(this, {})
};
s2.data.Root.inherits(s2.data.ObjectWrapper);
s2.data.Root.prototype.getRoot = function() {
  return this
};
s2.data.Root.globalInstance_ = null;
s2.data.Root.getGlobal = function() {
  if(!s2.data.Root.globalInstance_) {
    s2.data.Root.globalInstance_ = new s2.data.Root
  }return s2.data.Root.globalInstance_
};
s2.data.Root.clear = function() {
  s2.data.Root.globalInstance_ = new s2.data.Root
};goog.array = {};
goog.array.peek = function(array) {
  return array[array.length - 1]
};
goog.array.indexOf = function(arr, obj, opt_fromIndex) {
  if(arr.indexOf) {
    return arr.indexOf(obj, opt_fromIndex)
  }if(Array.indexOf) {
    return Array.indexOf(arr, obj, opt_fromIndex)
  }if(opt_fromIndex == null) {
    opt_fromIndex = 0
  }else if(opt_fromIndex < 0) {
    opt_fromIndex = Math.max(0, arr.length + opt_fromIndex)
  }for(var i = opt_fromIndex;i < arr.length;i++) {
    if(i in arr && arr[i] === obj)return i
  }return-1
};
goog.array.lastIndexOf = function(arr, obj, opt_fromIndex) {
  if(opt_fromIndex == null) {
    opt_fromIndex = arr.length - 1
  }if(arr.lastIndexOf) {
    return arr.lastIndexOf(obj, opt_fromIndex)
  }if(Array.lastIndexOf) {
    return Array.lastIndexOf(arr, obj, opt_fromIndex)
  }if(opt_fromIndex < 0) {
    opt_fromIndex = Math.max(0, arr.length + opt_fromIndex)
  }for(var i = opt_fromIndex;i >= 0;i--) {
    if(i in arr && arr[i] === obj)return i
  }return-1
};
goog.array.forEach = function(arr, f, opt_obj) {
  if(arr.forEach) {
    arr.forEach(f, opt_obj)
  }else if(Array.forEach) {
    Array.forEach(arr, f, opt_obj)
  }else {
    var l = arr.length, arr2 = goog.isString(arr) ? arr.split("") : arr;
    for(var i = 0;i < l;i++) {
      if(i in arr2) {
        f.call(opt_obj, arr2[i], i, arr)
      }
    }
  }
};
goog.array.forEachRight = function(arr, f, opt_obj) {
  var l = arr.length, arr2 = goog.isString(arr) ? arr.split("") : arr;
  for(var i = l - 1;i >= 0;--i) {
    if(i in arr2) {
      f.call(opt_obj, arr2[i], i, arr)
    }
  }
};
goog.array.filter = function(arr, f, opt_obj) {
  if(arr.filter) {
    return arr.filter(f, opt_obj)
  }if(Array.filter) {
    return Array.filter(arr, f, opt_obj)
  }var l = arr.length, res = [], arr2 = goog.isString(arr) ? arr.split("") : arr;
  for(var i = 0;i < l;i++) {
    if(i in arr2) {
      var val = arr2[i];
      if(f.call(opt_obj, val, i, arr)) {
        res.push(val)
      }
    }
  }return res
};
goog.array.map = function(arr, f, opt_obj) {
  if(arr.map) {
    return arr.map(f, opt_obj)
  }if(Array.map) {
    return Array.map(arr, f, opt_obj)
  }var l = arr.length, res = [], arr2 = goog.isString(arr) ? arr.split("") : arr;
  for(var i = 0;i < l;i++) {
    if(i in arr2) {
      res.push(f.call(opt_obj, arr2[i], i, arr))
    }
  }return res
};
goog.array.reduce = function(arr, f, val, opt_obj) {
  if(arr.reduce) {
    if(opt_obj) {
      return arr.reduce(goog.bind(f, opt_obj), val)
    }else {
      return arr.reduce(f, val)
    }
  }var rval = val;
  goog.array.forEach(arr, function(val, index) {
    rval = f.call(opt_obj, rval, val, index, arr)
  });
  return rval
};
goog.array.reduceRight = function(arr, f, val, opt_obj) {
  if(arr.reduceRight) {
    if(opt_obj) {
      return arr.reduceRight(goog.bind(f, opt_obj), val)
    }else {
      return arr.reduceRight(f, val)
    }
  }var rval = val;
  goog.array.forEachRight(arr, function(val, index) {
    rval = f.call(opt_obj, rval, val, index, arr)
  });
  return rval
};
goog.array.some = function(arr, f, opt_obj) {
  if(arr.some) {
    return arr.some(f, opt_obj)
  }if(Array.some) {
    return Array.some(arr, f, opt_obj)
  }var l = arr.length, arr2 = goog.isString(arr) ? arr.split("") : arr;
  for(var i = 0;i < l;i++) {
    if(i in arr2 && f.call(opt_obj, arr2[i], i, arr)) {
      return true
    }
  }return false
};
goog.array.every = function(arr, f, opt_obj) {
  if(arr.every) {
    return arr.every(f, opt_obj)
  }if(Array.every) {
    return Array.every(arr, f, opt_obj)
  }var l = arr.length, arr2 = goog.isString(arr) ? arr.split("") : arr;
  for(var i = 0;i < l;i++) {
    if(i in arr2 && !f.call(opt_obj, arr2[i], i, arr)) {
      return false
    }
  }return true
};
goog.array.contains = function(arr, obj) {
  if(arr.contains) {
    return arr.contains(obj)
  }return goog.array.indexOf(arr, obj) > -1
};
goog.array.isEmpty = function(arr) {
  return arr.length == 0
};
goog.array.clear = function(arr) {
  if(!goog.isArray(arr)) {
    for(var i = arr.length - 1;i >= 0;i--) {
      delete arr[i]
    }
  }arr.length = 0
};
goog.array.insert = function(arr, obj) {
  if(!goog.array.contains(arr, obj)) {
    arr.push(obj)
  }
};
goog.array.insertAt = function(arr, obj, opt_i) {
  goog.array.splice(arr, opt_i, 0, obj)
};
goog.array.insertBefore = function(arr, obj, opt_obj2) {
  var i;
  if(arguments.length == 2 || (i = goog.array.indexOf(arr, opt_obj2)) == -1) {
    arr.push(obj)
  }else {
    goog.array.insertAt(arr, obj, i)
  }
};
goog.array.remove = function(arr, obj) {
  var i = goog.array.indexOf(arr, obj), rv;
  if(rv = i != -1) {
    goog.array.removeAt(arr, i)
  }return rv
};
goog.array.removeAt = function(arr, i) {
  return Array.prototype.splice.call(arr, i, 1).length == 1
};
goog.array.clone = function(arr) {
  if(goog.isArray(arr)) {
    return arr.concat()
  }else {
    var rv = [];
    for(var i = 0, len = arr.length;i < len;i++) {
      rv[i] = arr[i]
    }return rv
  }
};
goog.array.toArray = function(object) {
  if(goog.isArray(object)) {
    return object.concat()
  }return goog.array.clone(object)
};
goog.array.extend = function(arr1, var_args) {
  for(var i = 1;i < arguments.length;i++) {
    var arr2 = arguments[i];
    if(!goog.isArray(arr2)) {
      arr1.push(arr2)
    }else {
      arr1.push.apply(arr1, arr2)
    }
  }
};
goog.array.splice = function(arr, index, howMany, opt_el) {
  return Array.prototype.splice.apply(arr, goog.array.slice(arguments, 1))
};
goog.array.slice = function(arr, start, opt_end) {
  if(arguments.length <= 2) {
    return Array.prototype.slice.call(arr, start)
  }else {
    return Array.prototype.slice.call(arr, start, opt_end)
  }
};
goog.array.find = goog.array.indexOf;
goog.array.insertValue = goog.array.insert;
goog.array.deleteValue = goog.array.remove;
goog.array.removeDuplicates = function(arr, opt_rv) {
  var rv = opt_rv || arr, seen = {}, cursorInsert = 0, cursorRead = 0;
  while(cursorRead < arr.length) {
    var current = arr[cursorRead++], hc = goog.isObject(current) ? goog.getHashCode(current) : current;
    if(!(hc in seen)) {
      seen[hc] = true;
      rv[cursorInsert++] = current
    }
  }rv.length = cursorInsert
};
goog.array.binarySearch = function(arr, target, opt_compareFn) {
  var left = 0, right = arr.length - 1, compareFn = opt_compareFn || goog.array.defaultCompare;
  while(left <= right) {
    var mid = left + right >> 1, compareResult = compareFn(target, arr[mid]);
    if(compareResult > 0) {
      left = mid + 1
    }else if(compareResult < 0) {
      right = mid - 1
    }else {
      return mid
    }
  }return-(left + 1)
};
goog.array.sort = function(arr, opt_compareFn) {
  Array.prototype.sort.call(arr, opt_compareFn || goog.array.defaultCompare)
};
goog.array.defaultCompare = function(a, b) {
  return a > b ? 1 : (a < b ? -1 : 0)
};
goog.array.binaryInsert = function(array, value, opt_compareFn) {
  var index = goog.array.binarySearch(array, value, opt_compareFn);
  if(index < 0) {
    goog.array.insertAt(array, value, -(index + 1));
    return true
  }return false
};
goog.array.binaryRemove = function(array, value, opt_compareFn) {
  var index = goog.array.binarySearch(array, value, opt_compareFn);
  return index >= 0 ? goog.array.removeAt(array, index) : false
};goog.object = {};
goog.object.forEach = function(obj, f, opt_obj) {
  for(var key in obj) {
    f.call(opt_obj, obj[key], key, obj)
  }
};
goog.object.filter = function(obj, f, opt_obj) {
  var res = {};
  for(var key in obj) {
    if(f.call(opt_obj, obj[key], key, obj)) {
      res[key] = obj[key]
    }
  }return res
};
goog.object.map = function(obj, f, opt_obj) {
  var res = {};
  for(var key in obj) {
    res[key] = f.call(opt_obj, obj[key], key, obj)
  }return res
};
goog.object.some = function(obj, f, opt_obj) {
  for(var key in obj) {
    if(f.call(opt_obj, obj[key], key, obj)) {
      return true
    }
  }return false
};
goog.object.every = function(obj, f, opt_obj) {
  for(var key in obj) {
    if(!f.call(opt_obj, obj[key], key, obj)) {
      return false
    }
  }return true
};
goog.object.getCount = function(obj) {
  var rv = 0;
  for(var key in obj) {
    rv++
  }return rv
};
goog.object.contains = function(obj, val) {
  return goog.object.containsValue(obj, val)
};
goog.object.getValues = function(obj) {
  var res = [];
  for(var key in obj) {
    res.push(obj[key])
  }return res
};
goog.object.getKeys = function(obj) {
  var res = [];
  for(var key in obj) {
    res.push(key)
  }return res
};
goog.object.containsKey = function(obj, key) {
  return key in obj
};
goog.object.containsValue = function(obj, val) {
  for(var key in obj) {
    if(obj[key] == val) {
      return true
    }
  }return false
};
goog.object.isEmpty = function(obj) {
  for(var key in obj) {
    return false
  }return true
};
goog.object.clear = function(obj) {
  var keys = goog.object.getKeys(obj);
  for(var i = keys.length - 1;i >= 0;i--) {
    goog.object.remove(obj, keys[i])
  }
};
goog.object.remove = function(obj, key) {
  var rv;
  if(rv = key in obj) {
    delete obj[key]
  }return rv
};
goog.object.add = function(obj, key, val) {
  if(key in obj) {
    throw Error('The object already contains the key "' + key + '"');
  }goog.object.set(obj, key, val)
};
goog.object.get = function(obj, key, opt_val) {
  if(key in obj) {
    return obj[key]
  }return opt_val
};
goog.object.set = function(obj, key, value) {
  obj[key] = value
};
goog.object.clone = function(obj) {
  var res = {};
  for(var key in obj) {
    res[key] = obj[key]
  }return res
};
goog.object.transpose = function(obj) {
  var transposed = {}, keys = goog.object.getKeys(obj);
  for(var i = 0, len = keys.length;i < len;i++) {
    var key = keys[i];
    transposed[obj[key]] = key
  }return transposed
};
goog.object.PROTOTYPE_FIELDS_ = ["constructor", "hasOwnProperty", "isPrototypeOf", "propertyIsEnumerable", "toLocaleString", "toString", "valueOf"];
goog.object.extend = function(target, var_args) {
  var key, source;
  for(var i = 1;i < arguments.length;i++) {
    source = arguments[i];
    for(key in source) {
      target[key] = source[key]
    }for(var j = 0;j < goog.object.PROTOTYPE_FIELDS_.length;j++) {
      key = goog.object.PROTOTYPE_FIELDS_[j];
      if(Object.prototype.hasOwnProperty.call(source, key)) {
        target[key] = source[key]
      }
    }
  }
};goog.string = {};
goog.string.startsWith = function(str, prefix) {
  return str.indexOf(prefix) == 0
};
goog.string.endsWith = function(str, suffix) {
  var l = str.length - suffix.length;
  return l >= 0 && str.lastIndexOf(suffix, l) == l
};
goog.string.subs = function(str) {
  for(var i = 1;i < arguments.length;i++) {
    var replacement = String(arguments[i]).replace(/\$/g, "$$$$");
    str = str.replace(/\%s/, replacement)
  }return str
};
goog.string.collapseWhitespace = function(str) {
  return str.replace(/\s+/g, " ").replace(/^\s+|\s+$/g, "")
};
goog.string.isEmpty = function(str) {
  return/^\s*$/.test(str)
};
goog.string.isEmptySafe = function(str) {
  return goog.string.isEmpty(goog.string.makeSafe(str))
};
goog.string.isAlpha = function(str) {
  return!/[^a-zA-Z]/.test(str)
};
goog.string.isNumeric = function(str) {
  return!/[^0-9]/.test(str)
};
goog.string.isAlphaNumeric = function(str) {
  return!/[^a-zA-Z0-9]/.test(str)
};
goog.string.isSpace = function(ch) {
  return ch == " "
};
goog.string.isUnicodeChar = function(ch) {
  return ch.length == 1 && ch >= " " && ch <= "~" || ch >= "\u0080" && ch <= "\ufffd"
};
goog.string.stripNewlines = function(str) {
  return str.replace(/(\r\n|\r|\n)+/g, " ")
};
goog.string.canonicalizeNewlines = function(str) {
  return str.replace(/(\r\n|\r|\n)/g, "\n")
};
goog.string.normalizeWhitespace = function(str) {
  return str.replace(/\xa0|\s/g, " ")
};
goog.string.normalizeSpaces = function(str) {
  return str.replace(/\xa0|[ \t]+/g, " ")
};
goog.string.trim = function(str) {
  return str.replace(/^\s+|\s+$/g, "")
};
goog.string.trimLeft = function(str) {
  return str.replace(/^\s+/, "")
};
goog.string.trimRight = function(str) {
  return str.replace(/\s+$/, "")
};
goog.string.caseInsensitiveCompare = function(str1, str2) {
  var test1 = String(str1).toLowerCase(), test2 = String(str2).toLowerCase();
  if(test1 < test2) {
    return-1
  }else if(test1 == test2) {
    return 0
  }else {
    return 1
  }
};
goog.string.numerateCompareRegExp_ = /(\.\d+)|(\d+)|(\D+)/g;
goog.string.numerateCompare = function(str1, str2) {
  if(str1 == str2) {
    return 0
  }if(!str1) {
    return-1
  }if(!str2) {
    return 1
  }var tokens1 = str1.toLowerCase().match(goog.string.numerateCompareRegExp_), tokens2 = str2.toLowerCase().match(goog.string.numerateCompareRegExp_), count = Math.min(tokens1.length, tokens2.length);
  for(var i = 0;i < count;i++) {
    var a = tokens1[i], b = tokens2[i];
    if(a != b) {
      var num1 = parseInt(a, 10);
      if(!isNaN(num1)) {
        var num2 = parseInt(b, 10);
        if(!isNaN(num2) && num1 - num2) {
          return num1 - num2
        }
      }return a < b ? -1 : 1
    }
  }if(tokens1.length != tokens2.length) {
    return tokens1.length - tokens2.length
  }return str1 < str2 ? -1 : 1
};
goog.string.encodeUriRegExp_ = /^[a-zA-Z0-9\-_.!~*'()]*$/;
goog.string.urlEncode = function(str) {
  str = String(str);
  if(!goog.string.encodeUriRegExp_.test(str)) {
    return encodeURIComponent(str)
  }return str
};
goog.string.urlDecode = function(str) {
  return decodeURIComponent(str.replace(/\+/g, " "))
};
goog.string.newLineToBr = function(str, opt_xml) {
  return str.replace(/(\r\n|\r|\n)/g, opt_xml ? "<br />" : "<br>")
};
goog.string.htmlEscape = function(str, opt_isLikelyToContainHtmlChars) {
  if(opt_isLikelyToContainHtmlChars) {
    return str.replace(goog.string.amperRe_, goog.string.amperStrRepl_).replace(goog.string.ltRe_, goog.string.ltStrRepl_).replace(goog.string.gtRe_, goog.string.gtStrRepl_).replace(goog.string.quotRe_, goog.string.quotStrRepl_)
  }else {
    if(!goog.string.allRe_.test(str))return str;
    if(str.indexOf(goog.string.amperStrOrig_) != -1) {
      str = str.replace(goog.string.amperRe_, goog.string.amperStrRepl_)
    }if(str.indexOf(goog.string.ltStrOrig_) != -1) {
      str = str.replace(goog.string.ltRe_, goog.string.ltStrRepl_)
    }if(str.indexOf(goog.string.gtStrOrig_) != -1) {
      str = str.replace(goog.string.gtRe_, goog.string.gtStrRepl_)
    }if(str.indexOf(goog.string.quotStrOrig_) != -1) {
      str = str.replace(goog.string.quotRe_, goog.string.quotStrRepl_)
    }return str
  }
};
goog.string.amperStrOrig_ = "&";
goog.string.ltStrOrig_ = "<";
goog.string.gtStrOrig_ = ">";
goog.string.quotStrOrig_ = '"';
goog.string.amperStrRepl_ = "&amp;";
goog.string.ltStrRepl_ = "&lt;";
goog.string.gtStrRepl_ = "&gt;";
goog.string.quotStrRepl_ = "&quot;";
goog.string.amperRe_ = /&/g;
goog.string.ltRe_ = /</g;
goog.string.gtRe_ = />/g;
goog.string.quotRe_ = /\"/g;
goog.string.allRe_ = /[&<>\"]/;
goog.string.unescapeEntities = function(str) {
  if(goog.string.contains(str, "&")) {
    if("document" in goog.global && !goog.string.contains(str, "<")) {
      var el = goog.global.document.createElement("a");
      el.innerHTML = str;
      if(el[goog.string.NORMALIZE_FN_]) {
        el[goog.string.NORMALIZE_FN_]()
      }str = el.firstChild.nodeValue;
      el.innerHTML = ""
    }else {
      return str.replace(/&([^;]+);/g, function(s, entity) {
        switch(entity) {
          case "amp":
            return"&";
          case "lt":
            return"<";
          case "gt":
            return">";
          case "quot":
            return'"';
          default:
            if(entity.charAt(0) == "#") {
              var n = Number("0" + entity.substr(1));
              if(!isNaN(n)) {
                return String.fromCharCode(n)
              }
            }return s
        }
      })
    }
  }return str
};
goog.string.NORMALIZE_FN_ = "normalize";
goog.string.whitespaceEscape = function(str, opt_xml) {
  return goog.string.newLineToBr(str.replace(/  /g, " &#160;"), opt_xml)
};
goog.string.stripQuotes = function(str, quotechar) {
  if(str.charAt(0) == quotechar && str.charAt(str.length - 1) == quotechar) {
    return str.substring(1, str.length - 1)
  }return str
};
goog.string.truncate = function(str, chars, opt_protectEscapedCharacters) {
  if(opt_protectEscapedCharacters) {
    str = goog.string.unescapeEntities(str)
  }if(str.length > chars) {
    str = str.substring(0, chars - 3) + "..."
  }if(opt_protectEscapedCharacters) {
    str = goog.string.htmlEscape(str)
  }return str
};
goog.string.truncateMiddle = function(str, chars, opt_protectEscapedCharacters) {
  if(opt_protectEscapedCharacters) {
    str = goog.string.unescapeEntities(str)
  }if(str.length > chars) {
    var half = Math.floor(chars / 2), endPos = str.length - half;
    half += chars % 2;
    str = str.substring(0, half) + "..." + str.substring(endPos)
  }if(opt_protectEscapedCharacters) {
    str = goog.string.htmlEscape(str)
  }return str
};
goog.string.jsEscapeCache_ = {"\u0008":"\\b", "\u000c":"\\f", "\n":"\\n", "\r":"\\r", "\t":"\\t", "\u000b":"\\x0B", '"':'\\"', "'":"\\'", "\\":"\\\\"};
goog.string.quote = function(s) {
  s = String(s);
  if(s.quote) {
    return s.quote()
  }else {
    var rv = '"';
    for(var i = 0;i < s.length;i++) {
      rv += goog.string.escapeChar(s.charAt(i))
    }return rv + '"'
  }
};
goog.string.escapeChar = function(c) {
  if(c in goog.string.jsEscapeCache_) {
    return goog.string.jsEscapeCache_[c]
  }var rv = c, cc = c.charCodeAt(0);
  if(cc > 31 && cc < 127) {
    rv = c
  }else {
    if(cc < 256) {
      rv = "\\x";
      if(cc < 16 || cc > 256) {
        rv += "0"
      }
    }else {
      rv = "\\u";
      if(cc < 4096) {
        rv += "0"
      }
    }rv += cc.toString(16).toUpperCase()
  }return goog.string.jsEscapeCache_[c] = rv
};
goog.string.toMap = function(s) {
  var rv = {};
  for(var i = 0;i < s.length;i++) {
    rv[s.charAt(i)] = true
  }return rv
};
goog.string.JS_REG_EXP_ESCAPE_CHAR_MAP_ = goog.string.toMap("()[]{}+-?*.$^|,:#<!\\");
goog.string.contains = function(s, ss) {
  return s.indexOf(ss) != -1
};
goog.string.regExpEscape = function(s) {
  s = String(s);
  var rv = "", c;
  for(var i = 0;i < s.length;i++) {
    c = s.charAt(i);
    if(c == "\u0008") {
      c = "\\x08"
    }else if(c in goog.string.JS_REG_EXP_ESCAPE_CHAR_MAP_) {
      c = "\\" + c
    }rv += c
  }return rv
};
goog.string.repeat = function(string, length) {
  return(new Array(length + 1)).join(string)
};
goog.string.padNumber = function(num, length, opt_precision) {
  var i = Math.floor(num), s = String(i);
  return goog.string.repeat("0", Math.max(0, length - s.length)) + (goog.isDef(opt_precision) ? num.toFixed(opt_precision) : num)
};
goog.string.makeSafe = function(obj) {
  return obj == null ? "" : String(obj)
};
goog.string.buildString = function() {
  return Array.prototype.join.call(arguments, "")
};
goog.string.getRandomString = function() {
  return Math.floor(Math.random() * 2147483648).toString(36) + (Math.floor(Math.random() * 2147483648) ^ (new Date).getTime()).toString(36)
};
goog.string.compareVersions = function(version1, version2) {
  var order = 0, v1Subs = String(version1).split("."), v2Subs = String(version2).split("."), subCount = Math.max(v1Subs.length, v2Subs.length);
  for(var subIdx = 0;order == 0 && subIdx < subCount;subIdx++) {
    var v1Sub = v1Subs[subIdx] || "", v2Sub = v2Subs[subIdx] || "", v1CompParser = new RegExp("(\\d*)(\\D*)", "g"), v2CompParser = new RegExp("(\\d*)(\\D*)", "g");
    do {
      var v1Comp = v1CompParser.exec(v1Sub) || ["", "", ""], v2Comp = v2CompParser.exec(v2Sub) || ["", "", ""];
      if(v1Comp[0].length == 0 && v2Comp[0].length == 0) {
        break
      }var v1CompNum = v1Comp[1].length == 0 ? 0 : parseInt(v1Comp[1], 10), v2CompNum = v2Comp[1].length == 0 ? 0 : parseInt(v2Comp[1], 10);
      order = goog.string.compareElements_(v1CompNum, v2CompNum) || goog.string.compareElements_(v1Comp[2].length == 0, v2Comp[2].length == 0) || goog.string.compareElements_(v1Comp[2], v2Comp[2])
    }while(order == 0)
  }return order
};
goog.string.compareElements_ = function(left, right) {
  if(left < right) {
    return-1
  }else if(left > right) {
    return 1
  }return 0
};goog.math = {};
goog.math.randomInt = function(a) {
  return Math.floor(Math.random() * a)
};
goog.math.uniformRandom = function(a, b) {
  return a + Math.random() * (b - a)
};
goog.math.clamp = function(value, min, max) {
  return Math.min(Math.max(value, min), max)
};
goog.math.modulo = function(a, b) {
  var r = a % b;
  return r * b < 0 ? r + b : r
};
goog.math.lerp = function(a, b, x) {
  return a + x * (b - a)
};
goog.math.nearlyEquals = function(a, b, opt_tolerance) {
  return Math.abs(a - b) <= (opt_tolerance || 1.0E-6)
};
goog.math.Size = function(opt_w, opt_h) {
  this.width = goog.isDef(opt_w) ? Number(opt_w) : undefined;
  this.height = goog.isDef(opt_h) ? Number(opt_h) : undefined
};
goog.math.Size.prototype.clone = function() {
  return new goog.math.Size(this.width, this.height)
};
goog.math.Size.prototype.toString = function() {
  return"(" + this.width + " x " + this.height + ")"
};
goog.math.Size.equals = function(a, b) {
  if(a == b) {
    return true
  }if(!a || !b) {
    return false
  }return a.width == b.width && a.height == b.height
};
goog.math.Coordinate = function(opt_x, opt_y) {
  this.x = goog.isDef(opt_x) ? Number(opt_x) : undefined;
  this.y = goog.isDef(opt_y) ? Number(opt_y) : undefined
};
goog.math.Coordinate.prototype.clone = function() {
  return new goog.math.Coordinate(this.x, this.y)
};
goog.math.Coordinate.prototype.toString = function() {
  return"(" + this.x + ", " + this.y + ")"
};
goog.math.Coordinate.equals = function(a, b) {
  if(a == b) {
    return true
  }if(!a || !b) {
    return false
  }return a.x == b.x && a.y == b.y
};
goog.math.Coordinate.distance = function(a, b) {
  var dx = a.x - b.x, dy = a.y - b.y;
  return Math.sqrt(dx * dx + dy * dy)
};
goog.math.Coordinate.squaredDistance = function(a, b) {
  var dx = a.x - b.x, dy = a.y - b.y;
  return dx * dx + dy * dy
};
goog.math.Coordinate.difference = function(a, b) {
  return new goog.math.Coordinate(a.x - b.x, a.y - b.y)
};
goog.math.Range = function(a, b) {
  a = Number(a);
  b = Number(b);
  this.start = a < b ? a : b;
  this.end = a < b ? b : a
};
goog.math.Range.prototype.clone = function() {
  return new goog.math.Range(this.start, this.end)
};
goog.math.Range.prototype.toString = function() {
  return"[" + this.start + ", " + this.end + "]"
};
goog.math.Range.equals = function(a, b) {
  if(a == b) {
    return true
  }if(!a || !b) {
    return false
  }return a.start == b.start && a.end == b.end
};
goog.math.Range.intersection = function(a, b) {
  var c0 = Math.max(a.start, b.start), c1 = Math.min(a.end, b.end);
  return c0 <= c1 ? new goog.math.Range(c0, c1) : null
};
goog.math.Range.boundingRange = function(a, b) {
  return new goog.math.Range(Math.min(a.start, b.start), Math.max(a.end, b.end))
};
goog.math.Range.contains = function(a, b) {
  return a.start <= b.start && a.end >= b.end
};
goog.math.Rect = function(opt_x, opt_y, opt_w, opt_h) {
  this.left = goog.isDef(opt_x) ? Number(opt_x) : undefined;
  this.top = goog.isDef(opt_y) ? Number(opt_y) : undefined;
  this.width = goog.isDef(opt_w) ? Number(opt_w) : undefined;
  this.height = goog.isDef(opt_h) ? Number(opt_h) : undefined
};
goog.math.Rect.prototype.clone = function() {
  return new goog.math.Rect(this.left, this.top, this.width, this.height)
};
goog.math.Rect.prototype.toBox = function() {
  return new goog.math.Box(this.top, this.left + this.width || undefined, this.top + this.height || undefined, this.left)
};
goog.math.Rect.prototype.toString = function() {
  return"(" + this.left + ", " + this.top + " - " + this.width + "w x " + this.height + "h)"
};
goog.math.Rect.equals = function(a, b) {
  if(a == b) {
    return true
  }if(!a || !b) {
    return false
  }return a.left == b.left && a.width == b.width && a.top == b.top && a.height == b.height
};
goog.math.Rect.prototype.intersection = function(rect) {
  var x0 = Math.max(this.left, rect.left), x1 = Math.min(this.left + this.width, rect.left + rect.width);
  if(x0 <= x1) {
    var y0 = Math.max(this.top, rect.top), y1 = Math.min(this.top + this.height, rect.top + rect.height);
    if(y0 <= y1) {
      this.left = x0;
      this.top = y0;
      this.width = x1 - x0;
      this.height = y1 - y0;
      return true
    }
  }return false
};
goog.math.Rect.intersection = function(a, b) {
  var x0 = Math.max(a.left, b.left), x1 = Math.min(a.left + a.width, b.left + b.width);
  if(x0 <= x1) {
    var y0 = Math.max(a.top, b.top), y1 = Math.min(a.top + a.height, b.top + b.height);
    if(y0 <= y1) {
      return new goog.math.Rect(x0, y0, x1 - x0, y1 - y0)
    }
  }return null
};
goog.math.Rect.difference = function(a, b) {
  if(!goog.math.Rect.intersection(a, b)) {
    return[a.clone()]
  }var result = [], top = a.top, height = a.height, ar = a.left + a.width, ab = a.top + a.height, br = b.left + b.width, bb = b.top + b.height;
  if(b.top > a.top) {
    result.push(new goog.math.Rect(a.left, a.top, a.width, b.top - a.top));
    top = b.top
  }if(bb < ab) {
    result.push(new goog.math.Rect(a.left, bb, a.width, ab - bb));
    height = bb - top
  }if(b.left > a.left) {
    result.push(new goog.math.Rect(a.left, top, b.left - a.left, height))
  }if(br < ar) {
    result.push(new goog.math.Rect(br, top, ar - br, height))
  }return result
};
goog.math.Rect.prototype.difference = function(rect) {
  return goog.math.Rect.difference(this, rect)
};
goog.math.Rect.prototype.boundingRect = function(rect) {
  var right = Math.max(this.left + this.width, rect.left + rect.width), bottom = Math.max(this.top + this.height, rect.top + rect.height);
  this.left = Math.min(this.left, rect.left);
  this.top = Math.min(this.top, rect.top);
  this.width = right - this.left;
  this.height = bottom - this.top
};
goog.math.Rect.boundingRect = function(a, b) {
  if(!a || !b) {
    return null
  }var clone = a.clone();
  clone.boundingRect(b);
  return clone
};
goog.math.Box = function(opt_top, opt_right, opt_bottom, opt_left) {
  this.top = goog.isDef(opt_top) ? Number(opt_top) : undefined;
  this.right = goog.isDef(opt_right) ? Number(opt_right) : undefined;
  this.bottom = goog.isDef(opt_bottom) ? Number(opt_bottom) : undefined;
  this.left = goog.isDef(opt_left) ? Number(opt_left) : undefined
};
goog.math.Box.boundingBox = function() {
  var box = new goog.math.Box(arguments[0].y, arguments[0].x, arguments[0].y, arguments[0].x);
  for(var i = 1;i < arguments.length;i++) {
    var coord = arguments[i];
    box.top = Math.min(box.top, coord.y);
    box.right = Math.max(box.right, coord.x);
    box.bottom = Math.max(box.bottom, coord.y);
    box.left = Math.min(box.left, coord.x)
  }return box
};
goog.math.Box.prototype.clone = function() {
  return new goog.math.Box(this.top, this.right, this.bottom, this.left)
};
goog.math.Box.prototype.toRect = function() {
  return new goog.math.Rect(this.left, this.top, this.right - this.left || undefined, this.bottom - this.top || undefined)
};
goog.math.Box.prototype.toString = function() {
  return"(" + this.top + "t, " + this.right + "r, " + this.bottom + "b, " + this.left + "l)"
};
goog.math.Box.prototype.contains = function(coord) {
  return goog.math.Box.contains(this, coord)
};
goog.math.Box.prototype.expand = function(top, opt_right, opt_bottom, opt_left) {
  if(goog.isObject(top)) {
    this.top -= top.top;
    this.right += top.right;
    this.bottom += top.bottom;
    this.left -= top.left
  }else {
    this.top -= top;
    this.right += opt_right;
    this.bottom += opt_bottom;
    this.left -= opt_left
  }return this
};
goog.math.Box.equals = function(a, b) {
  if(a == b) {
    return true
  }if(!a || !b) {
    return false
  }return a.top == b.top && a.right == b.right && a.bottom == b.bottom && a.left == b.left
};
goog.math.Box.contains = function(box, coord) {
  if(!box || !coord) {
    return false
  }return coord.x >= box.left && coord.x <= box.right && coord.y >= box.top && coord.y <= box.bottom
};
goog.math.Box.distance = function(box, coord) {
  if(coord.x >= box.left && coord.x <= box.right) {
    if(coord.y >= box.top && coord.y <= box.bottom) {
      return 0
    }return coord.y < box.top ? box.top - coord.y : coord.y - box.bottom
  }if(coord.y >= box.top && coord.y <= box.bottom) {
    return coord.x < box.left ? box.left - coord.x : coord.x - box.right
  }return goog.math.Coordinate.distance(coord, new goog.math.Coordinate(coord.x < box.left ? box.left : box.right, coord.y < box.top ? box.top : box.bottom))
};
goog.math.standardAngle = function(angle) {
  return goog.math.modulo(angle, 360)
};
goog.math.toRadians = function(angleDegrees) {
  return angleDegrees * Math.PI / 180
};
goog.math.toDegrees = function(angleRadians) {
  return angleRadians * 180 / Math.PI
};
goog.math.angleDx = function(degrees, radius) {
  return radius * Math.cos(goog.math.toRadians(degrees))
};
goog.math.angleDy = function(degrees, radius) {
  return radius * Math.sin(goog.math.toRadians(degrees))
};
goog.math.angle = function(x1, y1, x2, y2) {
  return goog.math.standardAngle(goog.math.toDegrees(Math.atan2(y2 - y1, x2 - x1)))
};
goog.math.angleDifference = function(startAngle, endAngle) {
  var d = goog.math.standardAngle(endAngle) - goog.math.standardAngle(startAngle);
  if(d > 180) {
    d = d - 360
  }else if(d <= -180) {
    d = 360 + d
  }return d
};goog.userAgent = {};
(function() {
  var isOpera = false, isIe = false, isSafari = false, isGecko = false, isCamino = false, isKonqueror = false, isKhtml = false, isMac = false, isWindows = false, isLinux = false, isMobile = false, platform = "";
  if(goog.global.navigator) {
    var ua = navigator.userAgent;
    isOpera = typeof opera != "undefined";
    isIe = !isOpera && ua.indexOf("MSIE") != -1;
    isSafari = !isOpera && ua.indexOf("WebKit") != -1;
    isMobile = isSafari && ua.indexOf("Mobile") != -1;
    isGecko = !isOpera && navigator.product == "Gecko" && !isSafari;
    isCamino = isGecko && navigator.vendor == "Camino";
    isKonqueror = !isOpera && ua.indexOf("Konqueror") != -1;
    isKhtml = isKonqueror || isSafari;
    var version, re;
    if(isOpera) {
      version = opera.version()
    }else {
      if(isGecko) {
        re = /rv\:([^\);]+)(\)|;)/
      }else if(isIe) {
        re = /MSIE\s+([^\);]+)(\)|;)/
      }else if(isSafari) {
        re = /WebKit\/(\S+)/
      }else if(isKonqueror) {
        re = /Konqueror\/([^\);]+)(\)|;)/
      }if(re) {
        re.test(ua);
        version = RegExp.$1
      }
    }platform = navigator.platform;
    isMac = platform.indexOf("Mac") != -1;
    isWindows = platform.indexOf("Win") != -1;
    isLinux = platform.indexOf("Linux") != -1
  }goog.userAgent.OPERA = isOpera;
  goog.userAgent.IE = isIe;
  goog.userAgent.GECKO = isGecko;
  goog.userAgent.CAMINO = isCamino;
  goog.userAgent.KONQUEROR = isKonqueror;
  goog.userAgent.SAFARI = isSafari;
  goog.userAgent.KHTML = isKhtml;
  goog.userAgent.VERSION = version;
  goog.userAgent.PLATFORM = platform;
  goog.userAgent.MAC = isMac;
  goog.userAgent.WINDOWS = isWindows;
  goog.userAgent.LINUX = isLinux;
  goog.userAgent.MOBILE = isMobile
})();
goog.userAgent.compare = function(v1, v2) {
  return goog.string.compareVersions(v1, v2)
};
goog.userAgent.isVersion = function(version) {
  return goog.userAgent.compare(goog.userAgent.VERSION, version) >= 0
};goog.dom = {};
goog.dom.NodeType = {ELEMENT:1, ATTRIBUTE:2, TEXT:3, CDATA_SECTION:4, ENTITY_REFERENCE:5, ENTITY:6, PROCESSING_INSTRUCTION:7, COMMENT:8, DOCUMENT:9, DOCUMENT_TYPE:10, DOCUMENT_FRAGMENT:11, NOTATION:12};
goog.dom.getDefaultDomHelper_ = function() {
  if(!goog.dom.defaultDomHelper_) {
    goog.dom.defaultDomHelper_ = new goog.dom.DomHelper
  }return goog.dom.defaultDomHelper_
};
goog.dom.getDomHelper = function(opt_element) {
  return opt_element ? new goog.dom.DomHelper(goog.dom.getOwnerDocument(opt_element)) : goog.dom.getDefaultDomHelper_()
};
goog.dom.getDocument = function() {
  return goog.dom.getDefaultDomHelper_().getDocument()
};
goog.dom.getElement = function(element) {
  return goog.dom.getDefaultDomHelper_().getElement(element)
};
goog.dom.$ = goog.dom.getElement;
goog.dom.getElementsByTagNameAndClass = function(opt_tag, opt_class, opt_el) {
  return goog.dom.getDefaultDomHelper_().getElementsByTagNameAndClass(opt_tag, opt_class, opt_el)
};
goog.dom.$$ = goog.dom.getElementsByTagNameAndClass;
goog.dom.setProperties = function(element, properties) {
  goog.object.forEach(properties, function(val, key) {
    if(key == "style") {
      element.style.cssText = val
    }else if(key == "class") {
      element.className = val
    }else if(key == "for") {
      element.htmlFor = val
    }else if(key in goog.dom.DIRECT_ATTRIBUTE_MAP_) {
      element.setAttribute(goog.dom.DIRECT_ATTRIBUTE_MAP_[key], val)
    }else {
      element[key] = val
    }
  })
};
goog.dom.DIRECT_ATTRIBUTE_MAP_ = {cellpadding:"cellPadding", cellspacing:"cellSpacing", colspan:"colSpan", rowspan:"rowSpan", valign:"vAlign", height:"height", width:"width", usemap:"useMap", frameborder:"frameBorder"};
goog.dom.getViewportSize = function(opt_window) {
  var win = opt_window || goog.global || window, doc = win.document;
  if(goog.userAgent.SAFARI && !goog.userAgent.isVersion("500") && !goog.userAgent.MOBILE) {
    if(typeof win.innerHeight == "undefined") {
      win = window
    }var innerHeight = win.innerHeight, scrollHeight = win.document.documentElement.scrollHeight;
    if(win == win.top) {
      if(scrollHeight < innerHeight) {
        innerHeight -= 15
      }
    }return new goog.math.Size(win.innerWidth, innerHeight)
  }var dh = goog.dom.getDomHelper(doc), el = dh.getCompatMode() == "CSS1Compat" && (!goog.userAgent.OPERA || goog.userAgent.OPERA && goog.userAgent.isVersion("9.50")) ? doc.documentElement : doc.body;
  return new goog.math.Size(el.clientWidth, el.clientHeight)
};
goog.dom.getPageScroll = function(opt_window) {
  var win = opt_window || goog.global || window, doc = win.document, x, y;
  if(!goog.userAgent.SAFARI && doc.compatMode == "CSS1Compat") {
    x = doc.documentElement.scrollLeft;
    y = doc.documentElement.scrollTop
  }else {
    x = doc.body.scrollLeft;
    y = doc.body.scrollTop
  }return new goog.math.Coordinate(x, y)
};
goog.dom.getWindow = function(doc) {
  if(goog.userAgent.SAFARI && !goog.userAgent.isVersion("500") && !goog.userAgent.MOBILE) {
    return null
  }else {
    return doc.parentWindow || doc.defaultView
  }
};
goog.dom.createDom = function(tagName, opt_attributes) {
  var dh = goog.dom.getDefaultDomHelper_();
  return dh.createDom.apply(dh, arguments)
};
goog.dom.$dom = goog.dom.createDom;
goog.dom.createElement = function(name) {
  return goog.dom.getDefaultDomHelper_().createElement(name)
};
goog.dom.createTextNode = function(content) {
  return goog.dom.getDefaultDomHelper_().createTextNode(content)
};
goog.dom.htmlToDocumentFragment = function(htmlString) {
  return goog.dom.getDefaultDomHelper_().htmlToDocumentFragment(htmlString)
};
goog.dom.getCompatMode = function() {
  return goog.dom.getDefaultDomHelper_().getCompatMode()
};
goog.dom.appendChild = function(parent, child) {
  parent.appendChild(child)
};
goog.dom.removeChildren = function(node) {
  var child;
  while(child = node.firstChild) {
    node.removeChild(child)
  }
};
goog.dom.insertSiblingBefore = function(newNode, refNode) {
  if(refNode.parentNode) {
    refNode.parentNode.insertBefore(newNode, refNode)
  }
};
goog.dom.insertSiblingAfter = function(newNode, refNode) {
  if(refNode.parentNode) {
    refNode.parentNode.insertBefore(newNode, refNode.nextSibling)
  }
};
goog.dom.removeNode = function(node) {
  return node && node.parentNode ? node.parentNode.removeChild(node) : null
};
goog.dom.getFirstElementChild = function(node) {
  return goog.dom.getNextElementNode_(node.firstChild, true)
};
goog.dom.getLastElementChild = function(node) {
  return goog.dom.getNextElementNode_(node.lastChild, false)
};
goog.dom.getNextElementSibling = function(node) {
  return goog.dom.getNextElementNode_(node.nextSibling, true)
};
goog.dom.getPreviousElementSibling = function(node) {
  return goog.dom.getNextElementNode_(node.previousSibling, false)
};
goog.dom.getNextElementNode_ = function(node, forward) {
  while(node && node.nodeType != goog.dom.NodeType.ELEMENT) {
    node = forward ? node.nextSibling : node.previousSibling
  }return node
};
goog.dom.isNodeLike = function(obj) {
  return goog.isObject(obj) && obj.nodeType > 0
};
goog.dom.BAD_CONTAINS_SAFARI_ = goog.userAgent.SAFARI && goog.userAgent.compare(goog.userAgent.VERSION, "521") <= 0;
goog.dom.contains = function(parent, descendant) {
  if(typeof parent.contains != "undefined" && !goog.dom.BAD_CONTAINS_SAFARI_ && descendant.nodeType == goog.dom.NodeType.ELEMENT) {
    return parent == descendant || parent.contains(descendant)
  }if(typeof parent.compareDocumentPosition != "undefined") {
    return parent == descendant || Boolean(parent.compareDocumentPosition(descendant) & 16)
  }while(descendant && parent != descendant) {
    descendant = descendant.parentNode
  }return descendant == parent
};
goog.dom.compareNodeOrder = function(node1, node2) {
  if(node1 == node2) {
    return 0
  }if(node1.compareDocumentPosition) {
    return node1.compareDocumentPosition(node2) & 2 ? 1 : -1
  }if("sourceIndex" in node1 || node1.parentNode && "sourceIndex" in node1.parentNode) {
    var isElement1 = node1.nodeType == goog.dom.NodeType.ELEMENT, isElement2 = node2.nodeType == goog.dom.NodeType.ELEMENT, index1 = isElement1 ? node1.sourceIndex : node1.parentNode.sourceIndex, index2 = isElement2 ? node2.sourceIndex : node2.parentNode.sourceIndex;
    if(index1 != index2) {
      return index1 - index2
    }else {
      if(isElement1) {
        return-1
      }if(isElement2) {
        return 1
      }var s = node2;
      while(s = s.previousSibling) {
        if(s == node1) {
          return-1
        }
      }return 1
    }
  }var doc = goog.dom.getOwnerDocument(node1), range1, range2;
  range1 = doc.createRange();
  range1.selectNode(node1);
  range1.collapse(true);
  range2 = doc.createRange();
  range2.selectNode(node2);
  range2.collapse(true);
  return range1.compareBoundaryPoints(Range.START_TO_END, range2)
};
goog.dom.getOwnerDocument = function(node) {
  return node.nodeType == goog.dom.NodeType.DOCUMENT ? node : node.ownerDocument || node.document
};
goog.dom.getFrameContentDocument = function(frame) {
  return goog.userAgent.SAFARI ? frame.document || frame.contentWindow.document : frame.contentDocument || frame.contentWindow.document
};
goog.dom.setTextContent = function(element, text) {
  if("textContent" in element) {
    element.textContent = text
  }else if(element.firstChild && element.firstChild.nodeType == goog.dom.NodeType.TEXT) {
    while(element.lastChild != element.firstChild) {
      element.removeChild(element.lastChild)
    }element.firstChild.data = text
  }else {
    while(element.hasChildNodes()) {
      element.removeChild(element.lastChild)
    }var doc = goog.dom.getOwnerDocument(element);
    element.appendChild(doc.createTextNode(text))
  }
};
goog.dom.findNode = function(root, p) {
  var rv = [];
  goog.dom.findNodes_(root, p, rv, true);
  return rv.length ? rv[0] : undefined
};
goog.dom.findNodes = function(root, p) {
  var rv = [];
  goog.dom.findNodes_(root, p, rv, false);
  return rv
};
goog.dom.findNodes_ = function(root, p, rv, findOne) {
  if(root != null) {
    for(var i = 0, child;child = root.childNodes[i];i++) {
      if(p(child)) {
        rv.push(child);
        if(findOne) {
          return
        }
      }goog.dom.findNodes_(child, p, rv, findOne)
    }
  }
};
goog.dom.TAGS_TO_IGNORE = {SCRIPT:1, STYLE:1, HEAD:1, IFRAME:1, OBJECT:1};
goog.dom.PREDEFINED_TAG_VALUES = {IMG:" ", BR:"\n"};
goog.dom.getTextContent = function(node) {
  if(goog.userAgent.IE && "innerText" in node) {
    return goog.string.canonicalizeNewlines(node.innerText)
  }var buf = [];
  goog.dom.getTextContent_(node, buf, true);
  var rv = buf.join("").replace(/ +/g, " ");
  if(rv != " ") {
    rv = rv.replace(/^\s*/, "")
  }return rv
};
goog.dom.getRawTextContent = function(node) {
  var buf = [];
  goog.dom.getTextContent_(node, buf, false);
  return buf.join("")
};
goog.dom.getTextContent_ = function(node, buf, normalizeWhitespace) {
  if(node.nodeName in goog.dom.TAGS_TO_IGNORE) {
  }else if(node.nodeType == goog.dom.NodeType.TEXT) {
    if(normalizeWhitespace) {
      buf.push(String(node.nodeValue).replace(/(\r\n|\r|\n)/g, ""))
    }else {
      buf.push(node.nodeValue)
    }
  }else if(node.nodeName in goog.dom.PREDEFINED_TAG_VALUES) {
    buf.push(goog.dom.PREDEFINED_TAG_VALUES[node.nodeName])
  }else {
    var child = node.firstChild;
    while(child) {
      goog.dom.getTextContent_(child, buf, normalizeWhitespace);
      child = child.nextSibling
    }
  }
};
goog.dom.getNodeTextLength = function(node) {
  return goog.dom.getTextContent(node).length
};
goog.dom.getNodeTextOffset = function(node, opt_offsetParent) {
  var root = opt_offsetParent || goog.dom.getOwnerDocument(node).body, buf = [];
  while(node && node != root) {
    var cur = node;
    while(cur = cur.previousSibling) {
      buf.unshift(goog.dom.getTextContent(cur))
    }node = node.parentNode
  }return goog.string.trimLeft(buf.join("")).replace(/ +/g, " ").length
};
goog.dom.getNodeAtOffset = function(parent, offset, opt_result) {
  var stack = [parent], pos = 0, cur;
  while(stack.length > 0 && pos < offset) {
    cur = stack.pop();
    if(cur.nodeName in goog.dom.TAGS_TO_IGNORE) {
    }else if(cur.nodeType == goog.dom.NodeType.TEXT) {
      var text = cur.nodeValue.replace(/(\r\n|\r|\n)/g, "").replace(/ +/g, " ");
      pos += text.length
    }else if(cur.nodeName in goog.dom.PREDEFINED_TAG_VALUES) {
      pos += goog.dom.PREDEFINED_TAG_VALUES(cur.nodeName).length
    }else {
      for(var i = cur.childNodes.length - 1;i >= 0;i--) {
        stack.push(cur.childNodes[i])
      }
    }
  }if(goog.isObject(opt_result)) {
    opt_result.remainder = cur ? cur.nodeValue.length + offset - pos - 1 : 0;
    opt_result.node = cur
  }return cur
};
goog.dom.DomHelper = function(opt_document) {
  this.document_ = opt_document || goog.global.document || document
};
goog.dom.DomHelper.prototype.getDomHelper = goog.dom.getDomHelper;
goog.dom.DomHelper.prototype.setDocument = function(document) {
  this.document_ = document
};
goog.dom.DomHelper.prototype.getDocument = function() {
  return this.document_
};
goog.dom.DomHelper.prototype.getElement = function(element) {
  if(goog.isString(element)) {
    return this.document_.getElementById(element)
  }else {
    return element
  }
};
goog.dom.DomHelper.prototype.$ = goog.dom.DomHelper.prototype.getElement;
goog.dom.DomHelper.prototype.getElementsByTagNameAndClass = function(opt_tag, opt_class, opt_el) {
  var tag = opt_tag || "*", parent = opt_el || this.document_, els = parent.getElementsByTagName(tag);
  if(opt_class) {
    var rv = [];
    for(var i = 0, el;el = els[i];i++) {
      var className = el.className;
      if(typeof className.split == "function" && goog.array.contains(className.split(" "), opt_class)) {
        rv.push(el)
      }
    }return rv
  }else {
    return els
  }
};
goog.dom.DomHelper.prototype.$$ = goog.dom.DomHelper.prototype.getElementsByTagNameAndClass;
goog.dom.DomHelper.prototype.setProperties = goog.dom.setProperties;
goog.dom.DomHelper.prototype.getViewportSize = goog.dom.getViewportSize;
goog.dom.DomHelper.prototype.createDom = function(tagName, opt_attributes) {
  if(goog.userAgent.IE && opt_attributes && opt_attributes.name) {
    tagName = "<" + tagName + ' name="' + goog.string.htmlEscape(opt_attributes.name) + '">'
  }var element = this.createElement(tagName);
  if(opt_attributes) {
    goog.dom.setProperties(element, opt_attributes)
  }if(arguments.length > 2) {
    function childHandler(child) {
      if(child) {
        this.appendChild(element, goog.isString(child) ? this.createTextNode(child) : child)
      }
    }
    for(var i = 2;i < arguments.length;i++) {
      var arg = arguments[i];
      if((goog.isArrayLike(arg) || goog.userAgent.SAFARI && typeof arg == "function" && typeof arg.length == "number") && !goog.dom.isNodeLike(arg)) {
        goog.array.forEach(goog.isArray(arg) ? arg : goog.array.clone(arg), childHandler, this)
      }else {
        childHandler.call(this, arg)
      }
    }
  }return element
};
goog.dom.DomHelper.prototype.$dom = goog.dom.DomHelper.prototype.createDom;
goog.dom.DomHelper.prototype.createElement = function(name) {
  return this.document_.createElement(name)
};
goog.dom.DomHelper.prototype.createTextNode = function(content) {
  return this.document_.createTextNode(content)
};
goog.dom.DomHelper.prototype.htmlToDocumentFragment = function(htmlString) {
  var tempDiv = this.document_.createElement("div");
  tempDiv.innerHTML = htmlString;
  if(tempDiv.childNodes.length == 1) {
    return tempDiv.firstChild
  }else {
    var fragment = this.document_.createDocumentFragment();
    while(tempDiv.firstChild) {
      fragment.appendChild(tempDiv.firstChild)
    }return fragment
  }
};
goog.dom.DomHelper.prototype.getCompatMode = function() {
  if(this.document_.compatMode) {
    return this.document_.compatMode
  }if(goog.userAgent.SAFARI) {
    var el = this.createDom("div", {style:"position:absolute;width:0;height:0;width:1"}), val = el.style.width == "1px" ? "BackCompat" : "CSS1Compat";
    return this.document_.compatMode = val
  }return"BackCompat"
};
goog.dom.DomHelper.prototype.appendChild = goog.dom.appendChild;
goog.dom.DomHelper.prototype.removeChildren = goog.dom.removeChildren;
goog.dom.DomHelper.prototype.insertSiblingBefore = goog.dom.insertSiblingBefore;
goog.dom.DomHelper.prototype.insertSiblingAfter = goog.dom.insertSiblingAfter;
goog.dom.DomHelper.prototype.removeNode = goog.dom.removeNode;
goog.dom.DomHelper.prototype.getFirstElementChild = goog.dom.getFirstElementChild;
goog.dom.DomHelper.prototype.getLastElementChild = goog.dom.getLastElementChild;
goog.dom.DomHelper.prototype.getNextElementSibling = goog.dom.getNextElementSibling;
goog.dom.DomHelper.prototype.getPreviousElementSibling = goog.dom.getPreviousElementSibling;
goog.dom.DomHelper.prototype.isNodeLike = goog.dom.isNodeLike;
goog.dom.DomHelper.prototype.contains = goog.dom.contains;
goog.dom.DomHelper.prototype.getOwnerDocument = goog.dom.getOwnerDocument;
goog.dom.DomHelper.prototype.getFrameContentDocument = goog.dom.getFrameContentDocument;
goog.dom.DomHelper.prototype.setTextContent = goog.dom.setTextContent;
goog.dom.DomHelper.prototype.findNode = goog.dom.findNode;
goog.dom.DomHelper.prototype.findNodes = goog.dom.findNodes;
goog.dom.DomHelper.prototype.getTextContent = goog.dom.getTextContent;
goog.dom.DomHelper.prototype.getNodeTextLength = goog.dom.getNodeTextLength;
goog.dom.DomHelper.prototype.getNodeTextOffset = goog.dom.getNodeTextOffset;goog.net = {};
goog.net.EventType = {COMPLETE:"complete", SUCCESS:"success", ERROR:"error", ABORT:"abort", READY:"ready", READY_STATE_CHANGE:"readystatechange", TIMEOUT:"timeout", INCREMENTAL_DATA:"incrementaldata"};goog.net.XmlHttp = function() {
  return goog.net.XmlHttp.factory_()
};
goog.net.XmlHttp.getOptions = function() {
  return goog.net.XmlHttp.cachedOptions_ || (goog.net.XmlHttp.cachedOptions_ = goog.net.XmlHttp.optionsFactory_())
};
goog.net.XmlHttp.factory_ = null;
goog.net.XmlHttp.optionsFactory_ = null;
goog.net.XmlHttp.cachedOptions_ = null;
goog.net.XmlHttp.setFactory = function(factory, optionsFactory) {
  goog.net.XmlHttp.factory_ = factory;
  goog.net.XmlHttp.optionsFactory_ = optionsFactory;
  goog.net.XmlHttp.cachedOptions_ = null
};
goog.net.XmlHttp.defaultFactory_ = function() {
  var progId = goog.net.XmlHttp.getProgId_();
  if(progId) {
    return new ActiveXObject(progId)
  }else {
    return new XMLHttpRequest
  }
};
goog.net.XmlHttp.defaultOptionsFactory_ = function() {
  var progId = goog.net.XmlHttp.getProgId_(), options = {};
  if(progId) {
    options[goog.net.XmlHttp.OptionType.USE_NULL_FUNCTION] = true;
    options[goog.net.XmlHttp.OptionType.LOCAL_REQUEST_ERROR] = true
  }return options
};
goog.net.XmlHttp.setFactory(goog.net.XmlHttp.defaultFactory_, goog.net.XmlHttp.defaultOptionsFactory_);
goog.net.XmlHttp.OptionType = {USE_NULL_FUNCTION:0, LOCAL_REQUEST_ERROR:1};
goog.net.XmlHttp.ReadyState = {};
goog.net.XmlHttp.ReadyState.UNINITIALIZED = 0;
goog.net.XmlHttp.ReadyState.LOADING = 1;
goog.net.XmlHttp.ReadyState.LOADED = 2;
goog.net.XmlHttp.ReadyState.INTERACTIVE = 3;
goog.net.XmlHttp.ReadyState.COMPLETE = 4;
goog.net.XmlHttp.ieProgId_ = null;
goog.net.XmlHttp.getProgId_ = function() {
  if(!goog.net.XmlHttp.ieProgId_ && typeof XMLHttpRequest == "undefined" && typeof ActiveXObject != "undefined") {
    var ACTIVE_X_IDENTS = ["MSXML2.XMLHTTP.6.0", "MSXML2.XMLHTTP.3.0", "MSXML2.XMLHTTP", "Microsoft.XMLHTTP"];
    for(var i = 0;i < ACTIVE_X_IDENTS.length;i++) {
      var candidate = ACTIVE_X_IDENTS[i];
      try {
        new ActiveXObject(candidate);
        goog.net.XmlHttp.ieProgId_ = candidate;
        return candidate
      }catch(e) {
      }
    }throw Error("Could not create ActiveXObject. ActiveX might be disabled, or MSXML might not be installed");
  }return goog.net.XmlHttp.ieProgId_
};goog.net.ErrorCode = {NO_ERROR:0, ACCESS_DENIED:1, FILE_NOT_FOUND:2, FF_SILENT_ERROR:3, CUSTOM_ERROR:4, EXCEPTION:5, HTTP_ERROR:6, ABORT:7, TIMEOUT:8};
goog.net.ErrorCode.getDebugMessage = function(errorCode) {
  switch(errorCode) {
    case goog.net.ErrorCode.NO_ERROR:
      return"No Error";
    case goog.net.ErrorCode.ACCESS_DENIED:
      return"Access denied to content document";
    case goog.net.ErrorCode.FILE_NOT_FOUND:
      return"File not found";
    case goog.net.ErrorCode.FF_SILENT_ERROR:
      return"Firefox silently errored";
    case goog.net.ErrorCode.CUSTOM_ERROR:
      return"Application custom error";
    case goog.net.ErrorCode.EXCEPTION:
      return"An exception occurred";
    case goog.net.ErrorCode.HTTP_ERROR:
      return"Http response at 400 or 500 level";
    case goog.net.ErrorCode.ABORT:
      return"Request was aborted";
    case goog.net.ErrorCode.TIMEOUT:
      return"Request timed out";
    default:
      return"Unrecognized error code"
  }
};goog.structs = {};
goog.structs.getCount = function(col) {
  if(typeof col.getCount == "function") {
    return col.getCount()
  }if(goog.isArrayLike(col) || goog.isString(col)) {
    return col.length
  }return goog.object.getCount(col)
};
goog.structs.getValues = function(col) {
  if(typeof col.getValues == "function") {
    return col.getValues()
  }if(goog.isString(col)) {
    return col.split("")
  }if(goog.isArrayLike(col)) {
    var rv = [], l = col.length;
    for(var i = 0;i < l;i++) {
      rv.push(col[i])
    }return rv
  }return goog.object.getValues(col)
};
goog.structs.getKeys = function(col) {
  if(typeof col.getKeys == "function") {
    return col.getKeys()
  }if(typeof col.getValues == "function") {
    return undefined
  }if(goog.isArrayLike(col) || goog.isString(col)) {
    var rv = [], l = col.length;
    for(var i = 0;i < l;i++) {
      rv.push(i)
    }return rv
  }return goog.object.getKeys(col)
};
goog.structs.contains = function(col, val) {
  if(typeof col.contains == "function") {
    return col.contains(val)
  }if(typeof col.containsValue == "function") {
    return col.containsValue(val)
  }if(goog.isArrayLike(col) || goog.isString(col)) {
    return goog.array.contains(col, val)
  }return goog.object.containsValue(col, val)
};
goog.structs.isEmpty = function(col) {
  if(typeof col.isEmpty == "function") {
    return col.isEmpty()
  }if(goog.isArrayLike(col) || goog.isString(col)) {
    return goog.array.isEmpty(col)
  }return goog.object.isEmpty(col)
};
goog.structs.clear = function(col) {
  if(typeof col.clear == "function") {
    col.clear()
  }else if(goog.isArrayLike(col)) {
    goog.array.clear(col)
  }else {
    goog.object.clear(col)
  }
};
goog.structs.forEach = function(col, f, opt_obj) {
  if(typeof col.forEach == "function") {
    col.forEach(f, opt_obj)
  }else if(goog.isArrayLike(col) || goog.isString(col)) {
    goog.array.forEach(col, f, opt_obj)
  }else {
    var keys = goog.structs.getKeys(col), values = goog.structs.getValues(col), l = values.length;
    for(var i = 0;i < l;i++) {
      f.call(opt_obj, values[i], keys && keys[i], col)
    }
  }
};
goog.structs.filter = function(col, f, opt_obj, opt_constr) {
  if(typeof col.filter == "function") {
    return col.filter(f, opt_obj)
  }if(goog.isArrayLike(col) || goog.isString(col)) {
    return goog.array.filter(col, f, opt_obj)
  }var rv, keys = goog.structs.getKeys(col), values = goog.structs.getValues(col), l = values.length;
  if(keys && goog.structs.Map) {
    rv = new (opt_constr || Object);
    for(var i = 0;i < l;i++) {
      if(f.call(opt_obj, values[i], keys[i], col)) {
        goog.structs.Map.set(rv, keys[i], values[i])
      }
    }
  }else if(goog.structs.Set) {
    rv = new (opt_constr || Array);
    for(var i = 0;i < l;i++) {
      if(f.call(opt_obj, values[i], undefined, col)) {
        goog.structs.Set.add(rv, values[i])
      }
    }
  }return rv
};
goog.structs.map = function(col, f, opt_obj, opt_constr) {
  if(typeof col.map == "function") {
    return col.map(f, opt_obj)
  }if(goog.isArrayLike(col) || goog.isString(col)) {
    return goog.array.map(col, f, opt_obj)
  }var rv, keys = goog.structs.getKeys(col), values = goog.structs.getValues(col), l = values.length;
  if(keys && goog.structs.Map) {
    rv = new (opt_constr || Object);
    for(var i = 0;i < l;i++) {
      goog.structs.Map.set(rv, keys[i], f.call(opt_obj, values[i], keys[i], col))
    }
  }else if(goog.structs.Set) {
    rv = new (opt_constr || Array);
    for(var i = 0;i < l;i++) {
      goog.structs.Set.add(rv, f.call(opt_obj, values[i], undefined, col))
    }
  }return rv
};
goog.structs.some = function(col, f, opt_obj) {
  if(typeof col.some == "function") {
    return col.some(f, opt_obj)
  }if(goog.isArrayLike(col) || goog.isString(col)) {
    return goog.array.some(col, f, opt_obj)
  }var keys = goog.structs.getKeys(col), values = goog.structs.getValues(col), l = values.length;
  for(var i = 0;i < l;i++) {
    if(f.call(opt_obj, values[i], keys && keys[i], col)) {
      return true
    }
  }return false
};
goog.structs.every = function(col, f, opt_obj) {
  if(typeof col.every == "function") {
    return col.every(f, opt_obj)
  }if(goog.isArrayLike(col) || goog.isString(col)) {
    return goog.array.every(col, f, opt_obj)
  }var keys = goog.structs.getKeys(col), values = goog.structs.getValues(col), l = values.length;
  for(var i = 0;i < l;i++) {
    if(!f.call(opt_obj, values[i], keys && keys[i], col)) {
      return false
    }
  }return true
};goog.structs.Map = function(opt_map) {
  this.map_ = {};
  this.keys_ = [];
  if(opt_map) {
    this.addAll(opt_map)
  }
};
goog.structs.Map.keyPrefix_ = ":";
goog.structs.Map.keyPrefixCharCode_ = goog.structs.Map.keyPrefix_.charCodeAt(0);
goog.structs.Map.prototype.count_ = 0;
goog.structs.Map.toInternalKey_ = function(key) {
  key = String(key);
  if(key in Object.prototype) {
    return goog.structs.Map.keyPrefix_ + key
  }else if(key.charCodeAt(0) == goog.structs.Map.keyPrefixCharCode_) {
    return goog.structs.Map.keyPrefix_ + key
  }else {
    return key
  }
};
goog.structs.Map.fromInternalKey_ = function(internalKey) {
  if(internalKey.charCodeAt(0) == goog.structs.Map.keyPrefixCharCode_) {
    return internalKey.substring(1)
  }else {
    return internalKey
  }
};
goog.structs.Map.prototype.getCount = function() {
  return this.count_
};
goog.structs.Map.prototype.getValues = function() {
  this.cleanupKeysArray_();
  var rv = [];
  for(var i = 0;i < this.keys_.length;i++) {
    var key = this.keys_[i];
    rv.push(this.map_[key])
  }return rv
};
goog.structs.Map.prototype.getKeys = function() {
  this.cleanupKeysArray_();
  var rv = [];
  for(var i = 0;i < this.keys_.length;i++) {
    var key = this.keys_[i];
    rv.push(goog.structs.Map.fromInternalKey_(key))
  }return rv
};
goog.structs.Map.prototype.containsKey = function(key) {
  return goog.structs.Map.toInternalKey_(key) in this.map_
};
goog.structs.Map.prototype.containsValue = function(val) {
  for(var i = 0;i < this.keys_.length;i++) {
    var key = this.keys_[i];
    if(key in this.map_) {
      if(this.map_[key] == val) {
        return true
      }
    }
  }return false
};
goog.structs.Map.prototype.isEmpty = function() {
  return this.count_ == 0
};
goog.structs.Map.prototype.clear = function() {
  this.map_ = {};
  this.keys_.length = 0;
  this.count_ = 0
};
goog.structs.Map.prototype.remove = function(key) {
  var internalKey = goog.structs.Map.toInternalKey_(key);
  if(goog.object.remove(this.map_, internalKey)) {
    this.count_--;
    if(this.keys_.length > 2 * this.count_) {
      this.cleanupKeysArray_()
    }return true
  }return false
};
goog.structs.Map.prototype.cleanupKeysArray_ = function() {
  if(this.count_ != this.keys_.length) {
    var srcIndex = 0, destIndex = 0;
    while(srcIndex < this.keys_.length) {
      var key = this.keys_[srcIndex];
      if(key in this.map_) {
        this.keys_[destIndex++] = key
      }srcIndex++
    }this.keys_.length = destIndex
  }if(this.count_ != this.keys_.length) {
    var seen = {}, srcIndex = 0, destIndex = 0;
    while(srcIndex < this.keys_.length) {
      var key = this.keys_[srcIndex];
      if(!(key in seen)) {
        this.keys_[destIndex++] = key;
        seen[key] = 1
      }srcIndex++
    }this.keys_.length = destIndex
  }
};
goog.structs.Map.prototype.get = function(key, opt_val) {
  var internalKey = goog.structs.Map.toInternalKey_(key);
  if(internalKey in this.map_) {
    return this.map_[internalKey]
  }return opt_val
};
goog.structs.Map.prototype.set = function(key, value) {
  var internalKey = goog.structs.Map.toInternalKey_(key);
  if(!(internalKey in this.map_)) {
    this.count_++;
    this.keys_.push(internalKey)
  }this.map_[internalKey] = value
};
goog.structs.Map.prototype.addAll = function(map) {
  var keys, values;
  if(map instanceof goog.structs.Map) {
    keys = map.getKeys();
    values = map.getValues()
  }else {
    keys = goog.object.getKeys(map);
    values = goog.object.getValues(map)
  }for(var i = 0;i < keys.length;i++) {
    this.set(keys[i], values[i])
  }
};
goog.structs.Map.prototype.clone = function() {
  return new goog.structs.Map(this)
};
goog.structs.Map.getCount = function(map) {
  return goog.structs.getCount(map)
};
goog.structs.Map.getValues = function(map) {
  return goog.structs.getValues(map)
};
goog.structs.Map.getKeys = function(map) {
  if(typeof map.getKeys == goog.JsType_.FUNCTION) {
    return map.getKeys()
  }var rv = [];
  if(goog.isArrayLike(map)) {
    for(var i = 0;i < map.length;i++) {
      rv.push(i)
    }
  }else {
    return goog.object.getKeys(map)
  }return rv
};
goog.structs.Map.containsKey = function(map, key) {
  if(typeof map.containsKey == goog.JsType_.FUNCTION) {
    return map.containsKey(key)
  }if(goog.isArrayLike(map)) {
    return key < map.length
  }return goog.object.containsKey(map, key)
};
goog.structs.Map.containsValue = function(map, val) {
  return goog.structs.contains(map, val)
};
goog.structs.Map.isEmpty = function(map) {
  return goog.structs.isEmpty(map)
};
goog.structs.Map.clear = function(map) {
  goog.structs.clear(map)
};
goog.structs.Map.remove = function(map, key) {
  if(typeof map.remove == goog.JsType_.FUNCTION) {
    return map.remove(key)
  }if(goog.isArrayLike(map)) {
    return goog.array.removeAt(map, key)
  }return goog.object.remove(map, key)
};
goog.structs.Map.add = function(map, key, val) {
  if(typeof map.add == goog.JsType_.FUNCTION) {
    map.add(key, val)
  }else if(goog.structs.Map.containsKey(map, key)) {
    throw Error('The collection already contains the key "' + key + '"');
  }else {
    goog.object.set(map, key, val)
  }
};
goog.structs.Map.get = function(map, key, opt_val) {
  if(typeof map.get == goog.JsType_.FUNCTION) {
    return map.get(key, opt_val)
  }if(goog.structs.Map.containsKey(map, key)) {
    return map[key]
  }return opt_val
};
goog.structs.Map.set = function(map, key, val) {
  if(typeof map.set == goog.JsType_.FUNCTION) {
    map.set(key, val)
  }else {
    map[key] = val
  }
};goog.structs.Set = function(opt_set) {
  this.map_ = new goog.structs.Map;
  if(opt_set) {
    this.addAll(opt_set)
  }
};
goog.structs.Set.getKey_ = function(val) {
  var type = typeof val;
  if(type == "object") {
    return"o" + goog.getHashCode(val)
  }else {
    return type.substr(0, 1) + val
  }
};
goog.structs.Set.prototype.getCount = function() {
  return this.map_.getCount()
};
goog.structs.Set.prototype.add = function(obj) {
  this.map_.set(goog.structs.Set.getKey_(obj), obj)
};
goog.structs.Set.prototype.addAll = function(set) {
  var values = goog.structs.Set.getValues(set), l = values.length;
  for(var i = 0;i < l;i++) {
    this.add(values[i])
  }
};
goog.structs.Set.prototype.remove = function(obj) {
  return this.map_.remove(goog.structs.Set.getKey_(obj))
};
goog.structs.Set.prototype.clear = function() {
  this.map_.clear()
};
goog.structs.Set.prototype.isEmpty = function() {
  return this.map_.isEmpty()
};
goog.structs.Set.prototype.contains = function(obj) {
  return this.map_.containsKey(goog.structs.Set.getKey_(obj))
};
goog.structs.Set.prototype.getValues = function() {
  return this.map_.getValues()
};
goog.structs.Set.prototype.clone = function() {
  return new goog.structs.Set(this)
};
goog.structs.Set.getCount = function(col) {
  return goog.structs.getCount(col)
};
goog.structs.Set.getValues = function(col) {
  return goog.structs.getValues(col)
};
goog.structs.Set.contains = function(col, val) {
  return goog.structs.contains(col, val)
};
goog.structs.Set.isEmpty = function(col) {
  return goog.structs.isEmpty(col)
};
goog.structs.Set.clear = function(col) {
  goog.structs.clear(col)
};
goog.structs.Set.remove = function(col, val) {
  if(typeof col.remove == "function") {
    return col.remove(val)
  }else if(goog.isArrayLike(col)) {
    return goog.array.remove(col, val)
  }else {
    for(var key in col) {
      if(col[key] == val) {
        delete col[key];
        return true
      }
    }return false
  }
};
goog.structs.Set.add = function(col, val) {
  if(typeof col.add == "function") {
    col.add(val)
  }else if(goog.isArrayLike(col)) {
    col[col.length] = val
  }else {
    throw Error('The collection does not know how to add "' + val + '"');
  }
};goog.debug = {};
goog.debug.catchErrors = function(opt_logger, opt_cancel, opt_target) {
  var logger = opt_logger || goog.debug.LogManager.getRoot(), target = opt_target || goog.global, oldErrorHandler = target.onerror;
  target.onerror = function(message, url, line) {
    if(oldErrorHandler) {
      oldErrorHandler(message, url, line)
    }var file = String(url).split(/[\/\\]/).pop();
    logger.severe("Error: " + message + " (" + file + " @ Line: " + line + ")");
    return Boolean(opt_cancel)
  }
};
goog.debug.expose = function(obj, opt_showFn) {
  if(typeof obj == "undefined") {
    return"undefined"
  }if(obj == null) {
    return"NULL"
  }var str = [];
  for(var x in obj) {
    if(!opt_showFn && goog.isFunction(obj[x])) {
      continue
    }var s = x + " = ";
    try {
      s += obj[x]
    }catch(e) {
      s += "*** " + e + " ***"
    }str.push(s)
  }return str.join("\n")
};
goog.debug.deepExpose = function(obj, opt_showFn) {
  var previous = new goog.structs.Set, str = [], helper = function(obj, space) {
    var nestspace = space + "  ", indentMultiline = function(str) {
      return str.replace(/\n/g, "\n" + space)
    };
    try {
      if(!goog.isDef(obj)) {
        str.push("undefined")
      }else if(goog.isNull(obj)) {
        str.push("NULL")
      }else if(goog.isString(obj)) {
        str.push('"' + indentMultiline(obj) + '"')
      }else if(goog.isFunction(obj)) {
        str.push(indentMultiline(String(obj)))
      }else if(goog.isObject(obj)) {
        if(previous.contains(obj)) {
          str.push("*** reference loop detected ***")
        }else {
          previous.add(obj);
          str.push("{");
          for(var x in obj) {
            if(!opt_showFn && goog.isFunction(obj[x])) {
              continue
            }str.push("\n");
            str.push(nestspace);
            str.push(x + " = ");
            helper(obj[x], nestspace)
          }str.push("\n" + space + "}")
        }
      }else {
        str.push(obj)
      }
    }catch(e) {
      str.push("*** " + e + " ***")
    }
  };
  helper(obj, "");
  return str.join("")
};
goog.debug.exposeArray = function(arr) {
  var str = [];
  for(var i = 0;i < arr.length;i++) {
    if(goog.isArray(arr[i])) {
      str.push(goog.debug.exposeArray(arr[i]))
    }else {
      str.push(arr[i])
    }
  }return"[ " + str.join(", ") + " ]"
};
goog.debug.exposeException = function(err, opt_fn) {
  try {
    var e = goog.debug.normalizeErrorObject(err), error = "Message: " + goog.string.htmlEscape(e.message) + '\nUrl: <a href="view-source:' + e.fileName + '" target="_new">' + e.fileName + "</a>\nLine: " + e.lineNumber + "\n\nBrowser stack:\n" + goog.string.htmlEscape(e.stack + "-> ") + "[end]\n\nJS stack traversal:\n" + goog.string.htmlEscape(goog.debug.getStacktrace(opt_fn) + "-> ");
    return error
  }catch(e2) {
    return"Exception trying to expose exception! You win, we lose. " + e2
  }
};
goog.debug.normalizeErrorObject = function(err) {
  var href = goog.getObjectByName("document.location.href");
  return typeof err == "string" ? {message:err, name:"Unknown error", lineNumber:"Not available", fileName:href, stack:"Not available"} : (!err.lineNumber || !err.fileName || !err.stack ? {message:err.message, name:err.name, lineNumber:"Not available", fileName:href, stack:"Not available"} : err)
};
goog.debug.enhanceError = function(err, opt_message) {
  if(typeof err == "string") {
    err = Error(err)
  }if(!err.stack) {
    err.stack = goog.debug.getStacktrace(arguments.callee.caller)
  }if(opt_message) {
    var x = 0;
    while(err["message" + x]) {
      ++x
    }err["message" + x] = String(opt_message)
  }return err
};
goog.debug.getStacktraceSimple = function(opt_depth) {
  var sb = [], fn = arguments.callee.caller, depth = 0;
  while(fn && (!opt_depth || depth < opt_depth)) {
    sb.push(goog.debug.getFunctionName(fn));
    sb.push("()\n");
    try {
      fn = fn.caller
    }catch(e) {
      sb.push("[exception trying to get caller]\n");
      break
    }depth++;
    if(depth >= goog.debug.MAX_STACK_DEPTH) {
      sb.push("[...long stack...]");
      break
    }
  }if(opt_depth && depth >= opt_depth) {
    sb.push("[...reached max depth limit...]")
  }else {
    sb.push("[end]")
  }return sb.join("")
};
goog.debug.MAX_STACK_DEPTH = 50;
goog.debug.getStacktrace = function(opt_fn) {
  return goog.debug.getStacktraceHelper_(opt_fn || arguments.callee.caller, [])
};
goog.debug.getStacktraceHelper_ = function(fn, visited) {
  var sb = [];
  if(goog.array.contains(visited, fn)) {
    sb.push("[...circular reference...]")
  }else if(fn && visited.length < goog.debug.MAX_STACK_DEPTH) {
    sb.push(goog.debug.getFunctionName(fn) + "(");
    var args = fn.arguments;
    for(var i = 0;i < args.length;i++) {
      if(i > 0) {
        sb.push(", ")
      }var argDesc, arg = args[i];
      switch(typeof arg) {
        case "object":
          argDesc = arg ? "object" : "null";
          break;
        case "string":
          argDesc = arg;
          break;
        case "number":
          argDesc = String(arg);
          break;
        case "boolean":
          argDesc = arg ? "true" : "false";
          break;
        case "function":
          argDesc = goog.debug.getFunctionName(arg);
          argDesc = argDesc ? argDesc : "[fn]";
          break;
        case "undefined":
        ;
        default:
          argDesc = typeof arg;
          break
      }
      if(argDesc.length > 40) {
        argDesc = argDesc.substr(0, 40) + "..."
      }sb.push(argDesc)
    }visited.push(fn);
    sb.push(")\n");
    try {
      sb.push(goog.debug.getStacktraceHelper_(fn.caller, visited))
    }catch(e) {
      sb.push("[exception trying to get caller]\n")
    }
  }else if(fn) {
    sb.push("[...long stack...]")
  }else {
    sb.push("[end]")
  }return sb.join("")
};
goog.debug.getFunctionName = function(fn) {
  if(!goog.debug.fnNameCache_[fn]) {
    var matches = /function ([^\(]+)/.exec(String(fn));
    if(matches) {
      var method = matches[1], hasDollarSigns = /^\$(.+)\$$/.exec(method);
      if(hasDollarSigns) {
        method = hasDollarSigns[1].replace(/\${1,2}/g, ".")
      }goog.debug.fnNameCache_[fn] = method
    }else {
      goog.debug.fnNameCache_[fn] = "[Anonymous]"
    }
  }return goog.debug.fnNameCache_[fn]
};
goog.debug.getAnonFunctionName_ = function(fn, opt_obj, opt_prefix, opt_depth) {
  if(goog.getObjectByName("document.all")) {
    return""
  }var obj = opt_obj || goog.global, prefix = opt_prefix || "", depth = opt_depth || 0;
  if(obj == fn) {
    return prefix
  }for(var i in obj) {
    if(i == "Packages" || i == "sun" || i == "netscape" || i == "java") {
      continue
    }if(obj[i] == fn) {
      return prefix + i
    }if((typeof obj[i] == "function" || typeof obj[i] == "object") && obj[i] != goog.global && obj[i] != goog.getObjectByName("document") && obj.hasOwnProperty(i) && depth < 6) {
      var rv = goog.debug.getAnonFunctionName_(fn, obj[i], prefix + i + ".", depth + 1);
      if(rv)return rv
    }
  }return""
};
goog.debug.fnNameCache_ = {};goog.debug.LogRecord = function(level, msg, loggerName) {
  this.sequenceNumber_ = goog.debug.LogRecord.nextSequenceNumber_++;
  this.time_ = goog.now();
  this.level_ = level;
  this.msg_ = msg;
  this.loggerName_ = loggerName
};
goog.debug.LogRecord.prototype.exception_ = null;
goog.debug.LogRecord.prototype.exceptionText_ = null;
goog.debug.LogRecord.nextSequenceNumber_ = 0;
goog.debug.LogRecord.prototype.getLoggerName = function() {
  return this.loggerName_
};
goog.debug.LogRecord.prototype.getException = function() {
  return this.exception_
};
goog.debug.LogRecord.prototype.setException = function(exception) {
  this.exception_ = exception
};
goog.debug.LogRecord.prototype.getExceptionText = function() {
  return this.exceptionText_
};
goog.debug.LogRecord.prototype.setExceptionText = function(text) {
  this.exceptionText_ = text
};
goog.debug.LogRecord.prototype.setLoggerName = function(loggerName) {
  this.loggerName_ = loggerName
};
goog.debug.LogRecord.prototype.getLevel = function() {
  return this.level_
};
goog.debug.LogRecord.prototype.setLevel = function(level) {
  this.level_ = level
};
goog.debug.LogRecord.prototype.getMessage = function() {
  return this.msg_
};
goog.debug.LogRecord.prototype.setMessage = function(msg) {
  this.msg_ = msg
};
goog.debug.LogRecord.prototype.getMillis = function() {
  return this.time_
};
goog.debug.LogRecord.prototype.setMillis = function(time) {
  this.time_ = time
};
goog.debug.LogRecord.prototype.getSequenceNumber = function() {
  return this.sequenceNumber_
};goog.debug.Logger = function(name) {
  this.name_ = name;
  this.parent_ = null;
  this.children_ = {};
  this.handlers_ = []
};
goog.debug.Logger.prototype.level_ = null;
goog.debug.Logger.Level = function(name, value) {
  this.name = name;
  this.value = value
};
goog.debug.Logger.Level.prototype.toString = function() {
  return this.name
};
goog.debug.Logger.Level.OFF = new goog.debug.Logger.Level("OFF", Infinity);
goog.debug.Logger.Level.SHOUT = new goog.debug.Logger.Level("SHOUT", 1200);
goog.debug.Logger.Level.SEVERE = new goog.debug.Logger.Level("SEVERE", 1000);
goog.debug.Logger.Level.WARNING = new goog.debug.Logger.Level("WARNING", 900);
goog.debug.Logger.Level.INFO = new goog.debug.Logger.Level("INFO", 800);
goog.debug.Logger.Level.CONFIG = new goog.debug.Logger.Level("CONFIG", 700);
goog.debug.Logger.Level.FINE = new goog.debug.Logger.Level("FINE", 500);
goog.debug.Logger.Level.FINER = new goog.debug.Logger.Level("FINER", 400);
goog.debug.Logger.Level.FINEST = new goog.debug.Logger.Level("FINEST", 300);
goog.debug.Logger.Level.ALL = new goog.debug.Logger.Level("ALL", 0);
goog.debug.Logger.Level.PREDEFINED_LEVELS = [goog.debug.Logger.Level.OFF, goog.debug.Logger.Level.SHOUT, goog.debug.Logger.Level.SEVERE, goog.debug.Logger.Level.WARNING, goog.debug.Logger.Level.INFO, goog.debug.Logger.Level.CONFIG, goog.debug.Logger.Level.FINE, goog.debug.Logger.Level.FINER, goog.debug.Logger.Level.FINEST, goog.debug.Logger.Level.ALL];
goog.debug.Logger.Level.predefinedLevelMap_ = null;
goog.debug.Logger.Level.getPredefinedLevel = function(name) {
  if(!goog.debug.Logger.Level.predefinedLevelMap_) {
    var levelMap = goog.debug.Logger.Level.predefinedLevelMap_ = {}, levels = goog.debug.Logger.Level.PREDEFINED_LEVELS;
    for(var i = 0;i < levels.length;i++) {
      var level = levels[i];
      levelMap[level.name] = level
    }
  }return goog.debug.Logger.Level.predefinedLevelMap_[name]
};
goog.debug.Logger.getLogger = function(name) {
  return goog.debug.LogManager.getLogger(name)
};
goog.debug.Logger.prototype.getName = function() {
  return this.name_
};
goog.debug.Logger.prototype.addHandler = function(handler) {
  this.handlers_.push(handler)
};
goog.debug.Logger.prototype.removeHandler = function(handler) {
  return goog.array.remove(this.handlers_, handler)
};
goog.debug.Logger.prototype.getParent = function() {
  return this.parent_
};
goog.debug.Logger.prototype.getChildren = function() {
  return this.children_
};
goog.debug.Logger.prototype.setLevel = function(level) {
  this.level_ = level
};
goog.debug.Logger.prototype.getLevel = function() {
  return this.level_
};
goog.debug.Logger.prototype.getEffectiveLevel = function() {
  if(this.level_) {
    return this.level_
  }if(this.parent_) {
    return this.parent_.getEffectiveLevel()
  }return null
};
goog.debug.Logger.prototype.isLoggable = function(level) {
  if(this.level_) {
    return level.value >= this.level_.value
  }if(this.parent_) {
    return this.parent_.isLoggable(level)
  }return false
};
goog.debug.Logger.prototype.log = function(level, msg, opt_exception) {
  if(!this.isLoggable(level)) {
    return
  }var logRecord = new goog.debug.LogRecord(level, String(msg), this.name_);
  if(opt_exception) {
    logRecord.setException(opt_exception);
    logRecord.setExceptionText(goog.debug.exposeException(opt_exception, arguments.callee.caller))
  }this.logRecord(logRecord)
};
goog.debug.Logger.prototype.shout = function(msg, opt_exception) {
  this.log(goog.debug.Logger.Level.SHOUT, msg, opt_exception)
};
goog.debug.Logger.prototype.severe = function(msg, opt_exception) {
  this.log(goog.debug.Logger.Level.SEVERE, msg, opt_exception)
};
goog.debug.Logger.prototype.warning = function(msg, opt_exception) {
  this.log(goog.debug.Logger.Level.WARNING, msg, opt_exception)
};
goog.debug.Logger.prototype.info = function(msg, opt_exception) {
  this.log(goog.debug.Logger.Level.INFO, msg, opt_exception)
};
goog.debug.Logger.prototype.config = function(msg, opt_exception) {
  this.log(goog.debug.Logger.Level.CONFIG, msg, opt_exception)
};
goog.debug.Logger.prototype.fine = function(msg, opt_exception) {
  this.log(goog.debug.Logger.Level.FINE, msg, opt_exception)
};
goog.debug.Logger.prototype.finer = function(msg, opt_exception) {
  this.log(goog.debug.Logger.Level.FINER, msg, opt_exception)
};
goog.debug.Logger.prototype.finest = function(msg, opt_exception) {
  this.log(goog.debug.Logger.Level.FINEST, msg, opt_exception)
};
goog.debug.Logger.prototype.logRecord = function(logRecord) {
  if(!this.isLoggable(logRecord.getLevel())) {
    return
  }var target = this;
  while(target) {
    target.callPublish_(logRecord);
    target = target.getParent()
  }
};
goog.debug.Logger.prototype.callPublish_ = function(logRecord) {
  for(var i = 0;i < this.handlers_.length;i++) {
    this.handlers_[i](logRecord)
  }
};
goog.debug.Logger.prototype.setParent_ = function(parent) {
  this.parent_ = parent
};
goog.debug.Logger.prototype.addChild_ = function(name, logger) {
  this.children_[name] = logger
};
goog.debug.LogManager = {};
goog.debug.LogManager.loggers_ = {};
goog.debug.LogManager.rootLogger_ = null;
goog.debug.LogManager.initialize = function() {
  if(!goog.debug.LogManager.rootLogger_) {
    goog.debug.LogManager.rootLogger_ = new goog.debug.Logger("");
    goog.debug.LogManager.loggers_[""] = goog.debug.LogManager.rootLogger_;
    goog.debug.LogManager.rootLogger_.setLevel(goog.debug.Logger.Level.CONFIG)
  }
};
goog.debug.LogManager.getLoggers = function() {
  return goog.debug.LogManager.loggers_
};
goog.debug.LogManager.getRoot = function() {
  goog.debug.LogManager.initialize();
  return goog.debug.LogManager.rootLogger_
};
goog.debug.LogManager.getLogger = function(name) {
  goog.debug.LogManager.initialize();
  if(name in goog.debug.LogManager.loggers_) {
    return goog.debug.LogManager.loggers_[name]
  }else {
    return goog.debug.LogManager.createLogger_(name)
  }
};
goog.debug.LogManager.createLogger_ = function(name) {
  var logger = new goog.debug.Logger(name), parts = name.split("."), leafName = parts[parts.length - 1];
  parts.length = parts.length - 1;
  var parentName = parts.join("."), parentLogger = goog.debug.LogManager.getLogger(parentName);
  parentLogger.addChild_(leafName, logger);
  logger.setParent_(parentLogger);
  goog.debug.LogManager.loggers_[name] = logger;
  return logger
};goog.debug.Formatter = function(opt_prefix) {
  this.prefix_ = opt_prefix || "";
  this.relativeTimeStart_ = goog.now()
};
goog.debug.Formatter.prototype.showAbsoluteTime = true;
goog.debug.Formatter.prototype.showRelativeTime = true;
goog.debug.Formatter.prototype.showLoggerName = true;
goog.debug.Formatter.prototype.formatRecord = function(logRecord) {
  throw Error("Must override formatRecord");
};
goog.debug.Formatter.prototype.resetRelativeTimeStart = function() {
  this.relativeTimeStart_ = goog.now()
};
goog.debug.Formatter.getDateTimeStamp_ = function(logRecord) {
  var time = new Date(logRecord.getMillis());
  return goog.debug.Formatter.getTwoDigitString_(time.getFullYear() - 2000) + goog.debug.Formatter.getTwoDigitString_(time.getMonth() + 1) + goog.debug.Formatter.getTwoDigitString_(time.getDate()) + " " + goog.debug.Formatter.getTwoDigitString_(time.getHours()) + ":" + goog.debug.Formatter.getTwoDigitString_(time.getMinutes()) + ":" + goog.debug.Formatter.getTwoDigitString_(time.getSeconds()) + "." + goog.debug.Formatter.getTwoDigitString_(Math.floor(time.getMilliseconds() / 10))
};
goog.debug.Formatter.getTwoDigitString_ = function(n) {
  if(n < 10) {
    return"0" + n
  }return String(n)
};
goog.debug.Formatter.getRelativeTime_ = function(logRecord, relativeTimeStart) {
  var ms = logRecord.getMillis() - relativeTimeStart, sec = ms / 1000, str = sec.toFixed(3), spacesToPrepend = 0;
  if(sec < 1) {
    spacesToPrepend = 2
  }else {
    while(sec < 100) {
      spacesToPrepend++;
      sec *= 10
    }
  }while(spacesToPrepend-- > 0) {
    str = " " + str
  }return str
};
goog.debug.HtmlFormatter = function(opt_prefix) {
  goog.debug.Formatter.call(this, opt_prefix)
};
goog.debug.HtmlFormatter.inherits(goog.debug.Formatter);
goog.debug.HtmlFormatter.prototype.formatRecord = function(logRecord) {
  var className;
  switch(logRecord.getLevel().value) {
    case goog.debug.Logger.Level.SHOUT.value:
      className = "dbg-sh";
      break;
    case goog.debug.Logger.Level.SEVERE.value:
      className = "dbg-sev";
      break;
    case goog.debug.Logger.Level.WARNING.value:
      className = "dbg-w";
      break;
    case goog.debug.Logger.Level.INFO.value:
      className = "dbg-i";
      break;
    case goog.debug.Logger.Level.FINE.value:
    ;
    default:
      className = "dbg-f";
      break
  }
  var sb = [];
  sb.push(this.prefix_, " ");
  if(this.showAbsoluteTime) {
    sb.push("[", goog.debug.Formatter.getDateTimeStamp_(logRecord), "] ")
  }if(this.showRelativeTime) {
    sb.push("[", goog.string.whitespaceEscape(goog.debug.Formatter.getRelativeTime_(logRecord, this.relativeTimeStart_)), "s] ")
  }if(this.showLoggerName) {
    sb.push("[", goog.string.htmlEscape(logRecord.getLoggerName()), "] ")
  }sb.push('<span class="', className, '">', goog.string.newLineToBr(goog.string.whitespaceEscape(goog.string.htmlEscape(logRecord.getMessage()))));
  if(logRecord.getException()) {
    sb.push("<br>", goog.string.newLineToBr(goog.string.whitespaceEscape(logRecord.getExceptionText())))
  }sb.push("</span><br>");
  return sb.join("")
};
goog.debug.TextFormatter = function(opt_prefix) {
  goog.debug.Formatter.call(this, opt_prefix)
};
goog.debug.TextFormatter.inherits(goog.debug.Formatter);
goog.debug.TextFormatter.prototype.formatRecord = function(logRecord) {
  var sb = [];
  sb.push(this.prefix_, " ");
  if(this.showAbsoluteTime) {
    sb.push("[", goog.debug.Formatter.getDateTimeStamp_(logRecord), "] ")
  }if(this.showRelativeTime) {
    sb.push("[", goog.debug.Formatter.getRelativeTime_(logRecord, this.relativeTimeStart_), "s] ")
  }if(this.showLoggerName) {
    sb.push("[", logRecord.getLoggerName(), "] ")
  }sb.push(logRecord.getMessage(), "\n");
  return sb.join("")
};goog.net.XhrMonitor_ = function() {
  if(!goog.userAgent.GECKO)return;
  this.contextsToXhr_ = {};
  this.xhrToContexts_ = {};
  this.stack_ = []
};
goog.net.XhrMonitor_.prototype.logger_ = goog.debug.Logger.getLogger("goog.net.xhrMonitor");
goog.net.XhrMonitor_.prototype.pushContext = function(context) {
  if(!goog.userAgent.GECKO)return;
  var key = this.getKey_(context);
  this.logger_.finest("Pushing context: " + context + " (" + key + ")");
  this.stack_.push(key)
};
goog.net.XhrMonitor_.prototype.popContext = function() {
  if(!goog.userAgent.GECKO)return;
  var context = this.stack_.pop();
  this.logger_.finest("Popping context: " + context);
  this.updateDependentContexts_(context)
};
goog.net.XhrMonitor_.prototype.isContextSafe = function(context) {
  if(!goog.userAgent.GECKO)return true;
  var deps = this.contextsToXhr_[this.getKey_(context)];
  this.logger_.fine("Context is safe : " + context + " - " + deps);
  return!deps
};
goog.net.XhrMonitor_.prototype.markXhrOpen = function(xhr) {
  if(!goog.userAgent.GECKO)return;
  var hc = goog.getHashCode(xhr);
  this.logger_.fine("Opening XHR : " + hc);
  for(var i = 0;i < this.stack_.length;i++) {
    var context = this.stack_[i];
    this.addToMap_(this.contextsToXhr_, context, hc);
    this.addToMap_(this.xhrToContexts_, hc, context)
  }
};
goog.net.XhrMonitor_.prototype.markXhrClosed = function(xhr) {
  if(!goog.userAgent.GECKO)return;
  var hc = goog.getHashCode(xhr);
  this.logger_.fine("Closing XHR : " + hc);
  delete this.xhrToContexts_[hc];
  for(var context in this.contextsToXhr_) {
    goog.array.remove(this.contextsToXhr_[context], hc);
    if(this.contextsToXhr_[context].length == 0) {
      delete this.contextsToXhr_[context]
    }
  }
};
goog.net.XhrMonitor_.prototype.updateDependentContexts_ = function(xhrHc) {
  var contexts = this.xhrToContexts_[xhrHc], xhrs = this.contextsToXhr_[xhrHc];
  if(contexts && xhrs) {
    this.logger_.finest("Updating dependent contexts");
    goog.array.forEach(contexts, function(context) {
      goog.array.forEach(xhrs, function(xhr) {
        this.addToMap_(this.contextsToXhr_, context, xhr);
        this.addToMap_(this.xhrToContexts_, xhr, context)
      }, this)
    }, this)
  }
};
goog.net.XhrMonitor_.prototype.getKey_ = function(obj) {
  return goog.isString(obj) ? obj : goog.getHashCode(obj)
};
goog.net.XhrMonitor_.prototype.addToMap_ = function(map, key, value) {
  if(!map[key]) {
    map[key] = []
  }if(!goog.array.contains(map[key], value)) {
    map[key].push(value)
  }
};
goog.net.xhrMonitor = new goog.net.XhrMonitor_;goog.Disposable = function() {
};
goog.Disposable.prototype.disposed_ = false;
goog.Disposable.prototype.getDisposed = function() {
  return this.disposed_
};
goog.Disposable.prototype.dispose = function() {
  if(!this.disposed_) {
    this.disposed_ = true
  }
};
goog.dispose = function(obj) {
  if(typeof obj.dispose == "function") {
    obj.dispose()
  }
};goog.events = {};
goog.events.Event = function(type, opt_target) {
  this.type = type;
  this.target = opt_target;
  this.currentTarget = this.target
};
goog.events.Event.inherits(goog.Disposable);
goog.events.Event.prototype.propagationStopped_ = false;
goog.events.Event.prototype.returnValue_ = true;
goog.events.Event.prototype.stopPropagation = function() {
  this.propagationStopped_ = true
};
goog.events.Event.prototype.preventDefault = function() {
  this.returnValue_ = false
};goog.events.BrowserEvent = function(opt_e, opt_currentTarget) {
  if(opt_e) {
    this.init(opt_e, opt_currentTarget)
  }
};
goog.events.BrowserEvent.inherits(goog.events.Event);
goog.events.BrowserEvent.MouseButton = {LEFT:0, MIDDLE:1, RIGHT:2};
goog.events.BrowserEvent.IEButtonMap_ = [1, 4, 2];
goog.events.BrowserEvent.prototype.type = null;
goog.events.BrowserEvent.prototype.target = null;
goog.events.BrowserEvent.prototype.currentTarget = null;
goog.events.BrowserEvent.prototype.relatedTarget = null;
goog.events.BrowserEvent.prototype.offsetX = 0;
goog.events.BrowserEvent.prototype.offsetY = 0;
goog.events.BrowserEvent.prototype.clientX = 0;
goog.events.BrowserEvent.prototype.clientY = 0;
goog.events.BrowserEvent.prototype.screenX = 0;
goog.events.BrowserEvent.prototype.screenY = 0;
goog.events.BrowserEvent.prototype.button = 0;
goog.events.BrowserEvent.prototype.keyCode = 0;
goog.events.BrowserEvent.prototype.charCode = 0;
goog.events.BrowserEvent.prototype.ctrlKey = false;
goog.events.BrowserEvent.prototype.altKey = false;
goog.events.BrowserEvent.prototype.shiftKey = false;
goog.events.BrowserEvent.prototype.metaKey = false;
goog.events.BrowserEvent.prototype.event_ = null;
goog.events.BrowserEvent.prototype.init = function(e, opt_currentTarget) {
  this.type = e.type;
  this.target = e.target || e.srcElement;
  this.currentTarget = opt_currentTarget;
  if(e.relatedTarget) {
    this.relatedTarget = e.relatedTarget
  }else if(this.type == goog.events.EventType.MOUSEOVER) {
    this.relatedTarget = e.fromElement
  }else if(this.type == goog.events.EventType.MOUSEOUT) {
    this.relatedTarget = e.toElement
  }else {
    this.relatedTarget = null
  }this.offsetX = typeof e.layerX == "number" ? e.layerX : e.offsetX;
  this.offsetY = typeof e.layerY == "number" ? e.layerY : e.offsetY;
  this.clientX = typeof e.clientX == "number" ? e.clientX : e.pageX;
  this.clientY = typeof e.clientY == "number" ? e.clientY : e.pageY;
  this.screenX = e.screenX || 0;
  this.screenY = e.screenY || 0;
  this.button = e.button;
  this.keyCode = e.keyCode || 0;
  this.charCode = e.charCode || (this.type == goog.events.EventType.KEYPRESS ? e.keyCode : 0);
  this.ctrlKey = e.ctrlKey;
  this.altKey = e.altKey;
  this.shiftKey = e.shiftKey;
  this.metaKey = e.metaKey;
  this.event_ = e;
  this.returnValue_ = null;
  this.propagationStopped_ = null
};
goog.events.BrowserEvent.prototype.isButton = function(button) {
  if(goog.userAgent.IE) {
    return!(!(this.event_.button & goog.events.BrowserEvent.IEButtonMap_[button]))
  }else {
    return this.event_.button == button
  }return false
};
goog.events.BrowserEvent.prototype.stopPropagation = function() {
  this.propagationStopped_ = true;
  if(this.event_.stopPropagation) {
    this.event_.stopPropagation()
  }else {
    this.event_.cancelBubble = true
  }
};
goog.events.BrowserEvent.prototype.preventDefault = function() {
  this.returnValue_ = false;
  if(!this.event_.preventDefault) {
    this.event_.returnValue = false;
    try {
      this.event_.keyCode = -1
    }catch(ex) {
    }
  }else {
    this.event_.preventDefault()
  }
};
goog.events.BrowserEvent.prototype.getBrowserEvent = function() {
  return this.event_
};
goog.events.BrowserEvent.prototype.dispose = function() {
  if(!this.getDisposed()) {
    goog.events.Event.prototype.dispose.call(this);
    this.event_ = null
  }
};goog.events.Listener = function() {
};
goog.events.Listener.counter_ = 0;
goog.events.Listener.prototype.isFunctionListener_ = null;
goog.events.Listener.prototype.listener = null;
goog.events.Listener.prototype.proxy = null;
goog.events.Listener.prototype.src = null;
goog.events.Listener.prototype.type = null;
goog.events.Listener.prototype.capture = null;
goog.events.Listener.prototype.handler = null;
goog.events.Listener.prototype.key = 0;
goog.events.Listener.prototype.removed = false;
goog.events.Listener.prototype.callOnce = false;
goog.events.Listener.prototype.init = function(listener, proxy, src, type, capture, handler) {
  if(goog.isFunction(listener)) {
    this.isFunctionListener_ = true
  }else if(listener && listener.handleEvent && goog.isFunction(listener.handleEvent)) {
    this.isFunctionListener_ = false
  }else {
    throw Error("Invalid listener argument");
  }this.listener = listener;
  this.proxy = proxy;
  this.src = src;
  this.type = type;
  this.capture = !(!capture);
  this.handler = handler;
  this.callOnce = false;
  this.key = ++goog.events.Listener.counter_;
  this.removed = false
};
goog.events.Listener.prototype.handleEvent = function(eventObject) {
  if(this.isFunctionListener_) {
    return this.listener.call(this.handler || this.src, eventObject)
  }return this.listener.handleEvent.call(this.listener, eventObject)
};goog.structs.SimplePool = function(initialCount, maxCount) {
  goog.Disposable.call(this);
  this.maxCount_ = maxCount;
  this.freeQueue_ = [];
  for(var i = 0;i < initialCount;i++) {
    this.releaseObject(this.createObject())
  }
};
goog.structs.SimplePool.inherits(goog.Disposable);
goog.structs.SimplePool.prototype.createObjectFn_ = null;
goog.structs.SimplePool.prototype.disposeObjectFn_ = null;
goog.structs.SimplePool.prototype.setCreateObjectFn = function(createObjectFn) {
  this.createObjectFn_ = createObjectFn
};
goog.structs.SimplePool.prototype.setDisposeObjectFn = function(disposeObjectFn) {
  this.disposeObjectFn_ = disposeObjectFn
};
goog.structs.SimplePool.prototype.getObject = function() {
  if(this.freeQueue_.length) {
    return this.freeQueue_.pop()
  }return this.createObject()
};
goog.structs.SimplePool.prototype.releaseObject = function(obj) {
  if(this.freeQueue_.length < this.maxCount_) {
    this.freeQueue_.push(obj)
  }else {
    this.disposeObject(obj)
  }
};
goog.structs.SimplePool.prototype.createObject = function() {
  if(this.createObjectFn_) {
    return this.createObjectFn_()
  }else {
    return{}
  }
};
goog.structs.SimplePool.prototype.disposeObject = function(obj) {
  if(this.disposeObjectFn_) {
    this.disposeObjectFn_(obj)
  }else {
    if(goog.isFunction(obj.dispose)) {
      obj.dispose()
    }else {
      for(var i in obj) {
        delete obj[i]
      }
    }
  }
};
goog.structs.SimplePool.prototype.dispose = function() {
  if(!this.getDisposed()) {
    goog.structs.SimplePool.superClass_.dispose.call(this);
    var freeQueue = this.freeQueue_;
    while(freeQueue.length) {
      this.disposeObject(freeQueue.pop())
    }this.freeQueue_ = null
  }
};goog.events.listeners_ = {};
goog.events.listenerTree_ = {};
goog.events.sources_ = {};
goog.events.OBJECT_POOL_INITIAL_COUNT = 0;
goog.events.OBJECT_POOL_MAX_COUNT = 600;
goog.events.objectPool_ = new goog.structs.SimplePool(goog.events.OBJECT_POOL_INITIAL_COUNT, goog.events.OBJECT_POOL_MAX_COUNT);
goog.events.objectPool_.setCreateObjectFn(function() {
  return{count_:0}
});
goog.events.objectPool_.setDisposeObjectFn(function(obj) {
  obj.count_ = 0
});
goog.events.ARRAY_POOL_INITIAL_COUNT = 0;
goog.events.ARRAY_POOL_MAX_COUNT = 600;
goog.events.arrayPool_ = new goog.structs.SimplePool(goog.events.ARRAY_POOL_INITIAL_COUNT, goog.events.ARRAY_POOL_MAX_COUNT);
goog.events.arrayPool_.setCreateObjectFn(function() {
  return[]
});
goog.events.arrayPool_.setDisposeObjectFn(function(obj) {
  obj.length = 0;
  delete obj.locked_;
  delete obj.needsCleanup_
});
goog.events.HANDLE_EVENT_PROXY_POOL_INITIAL_COUNT = 0;
goog.events.HANDLE_EVENT_PROXY_POOL_MAX_COUNT = 600;
goog.events.handleEventProxyPool_ = new goog.structs.SimplePool(goog.events.HANDLE_EVENT_PROXY_POOL_INITIAL_COUNT, goog.events.HANDLE_EVENT_PROXY_POOL_MAX_COUNT);
goog.events.handleEventProxyPool_.setCreateObjectFn(function() {
  var f = function(eventObject) {
    return goog.events.handleBrowserEvent_.call(f.src, f.key, eventObject)
  };
  return f
});
goog.events.LISTENER_POOL_INITIAL_COUNT = 0;
goog.events.LISTENER_POOL_MAX_COUNT = 600;
goog.events.createListenerFunction_ = function() {
  return new goog.events.Listener
};
goog.events.listenerPool_ = new goog.structs.SimplePool(goog.events.LISTENER_POOL_INITIAL_COUNT, goog.events.LISTENER_POOL_MAX_COUNT);
goog.events.listenerPool_.setCreateObjectFn(goog.events.createListenerFunction_);
goog.events.EVENT_POOL_INITIAL_COUNT = 0;
goog.events.EVENT_POOL_MAX_COUNT = 600;
goog.events.createEventFunction_ = function() {
  return new goog.events.BrowserEvent
};
goog.events.createEventPool_ = function() {
  var eventPool = null;
  if(goog.userAgent.IE) {
    eventPool = new goog.structs.SimplePool(goog.events.EVENT_POOL_INITIAL_COUNT, goog.events.EVENT_POOL_MAX_COUNT);
    eventPool.setCreateObjectFn(goog.events.createEventFunction_)
  }return eventPool
};
goog.events.eventPool_ = goog.events.createEventPool_();
goog.events.onString_ = "on";
goog.events.onStringMap_ = {};
goog.events.keySeparator_ = "_";
goog.events.listen = function(src, type, listener, opt_capt, opt_handler) {
  if(goog.isArray(type)) {
    for(var i = 0;i < type.length;i++) {
      goog.events.listen(src, type[i], listener, opt_capt, opt_handler)
    }return null
  }var capture = !(!opt_capt), map = goog.events.listenerTree_;
  if(!(type in map)) {
    map[type] = goog.events.objectPool_.getObject()
  }map = map[type];
  if(!(capture in map)) {
    map[capture] = goog.events.objectPool_.getObject();
    map.count_++
  }map = map[capture];
  var srcHashCode = goog.getHashCode(src), listenerArray, listenerObj;
  if(!map[srcHashCode]) {
    listenerArray = (map[srcHashCode] = goog.events.arrayPool_.getObject());
    map.count_++
  }else {
    listenerArray = map[srcHashCode];
    for(var i = 0;i < listenerArray.length;i++) {
      listenerObj = listenerArray[i];
      if(listenerObj.listener == listener && listenerObj.handler == opt_handler) {
        if(listenerObj.removed) {
          break
        }return listenerArray[i].key
      }
    }
  }var proxy = goog.events.handleEventProxyPool_.getObject();
  proxy.src = src;
  listenerObj = goog.events.listenerPool_.getObject();
  listenerObj.init(listener, proxy, src, type, capture, opt_handler);
  var key = listenerObj.key;
  proxy.key = key;
  listenerArray.push(listenerObj);
  goog.events.listeners_[key] = listenerObj;
  if(!goog.events.sources_[srcHashCode]) {
    goog.events.sources_[srcHashCode] = goog.events.arrayPool_.getObject()
  }goog.events.sources_[srcHashCode].push(listenerObj);
  if(src.addEventListener) {
    if(src == goog.global || !src.customEvent_) {
      src.addEventListener(type, proxy, capture)
    }
  }else {
    src.attachEvent(goog.events.getOnString_(type), proxy)
  }return key
};
goog.events.listenOnce = function(src, type, listener, opt_capt, opt_handler) {
  if(goog.isArray(type)) {
    for(var i = 0;i < type.length;i++) {
      goog.events.listenOnce(src, type[i], listener, opt_capt, opt_handler)
    }return null
  }var key = goog.events.listen(src, type, listener, opt_capt, opt_handler), listenerObj = goog.events.listeners_[key];
  listenerObj.callOnce = true;
  return key
};
goog.events.unlisten = function(src, type, listener, opt_capt, opt_handler) {
  if(goog.isArray(type)) {
    for(var i = 0;i < type.length;i++) {
      goog.events.unlisten(src, type[i], listener, opt_capt, opt_handler)
    }return null
  }var capture = !(!opt_capt), listenerArray = goog.events.getListeners_(src, type, capture);
  if(!listenerArray) {
    return false
  }for(var i = 0;i < listenerArray.length;i++) {
    if(listenerArray[i].listener == listener && listenerArray[i].capture == capture && listenerArray[i].handler == opt_handler) {
      return goog.events.unlistenByKey(listenerArray[i].key)
    }
  }return false
};
goog.events.unlistenByKey = function(key) {
  if(!goog.events.listeners_[key]) {
    return false
  }var listener = goog.events.listeners_[key];
  if(listener.removed) {
    return false
  }var src = listener.src, type = listener.type, proxy = listener.proxy, capture = listener.capture;
  if(src.removeEventListener) {
    if(src == goog.global || !src.customEvent_) {
      src.removeEventListener(type, proxy, capture)
    }
  }else if(src.detachEvent) {
    src.detachEvent(goog.events.getOnString_(type), proxy)
  }var srcHashCode = goog.getHashCode(src), listenerArray = goog.events.listenerTree_[type][capture][srcHashCode];
  if(goog.events.sources_[srcHashCode]) {
    var sourcesArray = goog.events.sources_[srcHashCode];
    goog.array.remove(sourcesArray, listener);
    if(sourcesArray.length == 0) {
      delete goog.events.sources_[srcHashCode]
    }
  }listener.removed = true;
  listenerArray.needsCleanup_ = true;
  goog.events.cleanUp_(type, capture, srcHashCode, listenerArray);
  delete goog.events.listeners_[key];
  return true
};
goog.events.cleanUp_ = function(type, capture, srcHashCode, listenerArray) {
  if(!listenerArray.locked_) {
    if(listenerArray.needsCleanup_) {
      for(var oldIndex = 0, newIndex = 0;oldIndex < listenerArray.length;oldIndex++) {
        if(listenerArray[oldIndex].removed) {
          goog.events.listenerPool_.releaseObject(listenerArray[oldIndex]);
          continue
        }if(oldIndex != newIndex) {
          listenerArray[newIndex] = listenerArray[oldIndex]
        }newIndex++
      }listenerArray.length = newIndex;
      listenerArray.needsCleanup_ = false;
      if(newIndex == 0) {
        goog.events.arrayPool_.releaseObject(listenerArray);
        delete goog.events.listenerTree_[type][capture][srcHashCode];
        goog.events.listenerTree_[type][capture].count_--;
        if(goog.events.listenerTree_[type][capture].count_ == 0) {
          goog.events.objectPool_.releaseObject(goog.events.listenerTree_[type][capture]);
          delete goog.events.listenerTree_[type][capture];
          goog.events.listenerTree_[type].count_--
        }if(goog.events.listenerTree_[type].count_ == 0) {
          goog.events.objectPool_.releaseObject(goog.events.listenerTree_[type]);
          delete goog.events.listenerTree_[type]
        }
      }
    }
  }
};
goog.events.removeAll = function(opt_obj, opt_type, opt_capt) {
  var count = 0, noObj = opt_obj == null, noType = opt_type == null, noCapt = opt_capt == null;
  opt_capt = !(!opt_capt);
  if(!noObj) {
    var srcHashCode = goog.getHashCode(opt_obj);
    if(goog.events.sources_[srcHashCode]) {
      var sourcesArray = goog.events.sources_[srcHashCode];
      for(var i = sourcesArray.length - 1;i >= 0;i--) {
        var listener = sourcesArray[i];
        if((noType || opt_type == listener.type) && (noCapt || opt_capt == listener.capture)) {
          goog.events.unlistenByKey(listener.key);
          count++
        }
      }
    }
  }else {
    goog.object.forEach(goog.events.sources_, function(listeners) {
      for(var i = listeners.length - 1;i >= 0;i--) {
        var listener = listeners[i];
        if((noType || opt_type == listener.type) && (noCapt || opt_capt == listener.capture)) {
          goog.events.unlistenByKey(listener.key);
          count++
        }
      }
    })
  }return count
};
goog.events.getListeners = function(obj, type, capture) {
  return goog.events.getListeners_(obj, type, capture) || []
};
goog.events.getListeners_ = function(obj, type, capture) {
  var map = goog.events.listenerTree_;
  if(type in map) {
    map = map[type];
    if(capture in map) {
      map = map[capture];
      var objHashCode = goog.getHashCode(obj);
      if(map[objHashCode]) {
        return map[objHashCode]
      }
    }
  }return null
};
goog.events.getListener = function(src, type, listener, opt_capt, opt_handler) {
  var capture = !(!opt_capt), listenerArray = goog.events.getListeners_(src, type, capture);
  if(listenerArray) {
    for(var i = 0;i < listenerArray.length;i++) {
      if(listenerArray[i].listener == listener && listenerArray[i].capture == capture && listenerArray[i].handler == opt_handler) {
        return listenerArray[i]
      }
    }
  }return null
};
goog.events.hasListener = function(obj, type, capture) {
  var map = goog.events.listenerTree_;
  if(type in map) {
    map = map[type];
    if(capture in map) {
      map = map[capture];
      var objHashCode = goog.getHashCode(obj);
      if(map[objHashCode]) {
        return true
      }
    }
  }return false
};
goog.events.expose = function(e) {
  var str = [];
  for(var key in e) {
    if(e[key] && e[key].id) {
      str.push(key + " = " + e[key] + " (" + e[key].id + ")")
    }else {
      str.push(key + " = " + e[key])
    }
  }return str.join("\n")
};
goog.events.EventType = {CLICK:"click", DBLCLICK:"dblclick", MOUSEDOWN:"mousedown", MOUSEUP:"mouseup", MOUSEOVER:"mouseover", MOUSEOUT:"mouseout", MOUSEMOVE:"mousemove", KEYPRESS:"keypress", KEYDOWN:"keydown", KEYUP:"keyup", BLUR:"blur", FOCUS:"focus", DEACTIVATE:"deactivate", FOCUSIN:goog.userAgent.IE ? "focusin" : "DOMFocusIn", FOCUSOUT:goog.userAgent.IE ? "focusout" : "DOMFocusOut", CHANGE:"change", SELECT:"select", SUBMIT:"submit", LOAD:"load", UNLOAD:"unload", HELP:"help", RESIZE:"resize", SCROLL:"scroll", 
READYSTATECHANGE:"readystatechange", CONTEXTMENU:"contextmenu"};
goog.events.getOnString_ = function(type) {
  if(type in goog.events.onStringMap_) {
    return goog.events.onStringMap_[type]
  }return goog.events.onStringMap_[type] = goog.events.onString_ + type
};
goog.events.fireListeners = function(obj, type, capture, eventObject) {
  var retval = 1, map = goog.events.listenerTree_;
  if(type in map) {
    map = map[type];
    if(capture in map) {
      map = map[capture];
      var objHashCode = goog.getHashCode(obj);
      if(map[objHashCode]) {
        var listenerArray = map[objHashCode];
        if(!listenerArray.locked_) {
          listenerArray.locked_ = 1
        }else {
          listenerArray.locked_++
        }try {
          var length = listenerArray.length;
          for(var i = 0;i < length;i++) {
            var listener = listenerArray[i];
            if(listener && !listener.removed) {
              retval &= goog.events.fireListener(listener, eventObject) !== false
            }
          }
        }finally {
          listenerArray.locked_--;
          goog.events.cleanUp_(type, capture, objHashCode, listenerArray)
        }
      }
    }
  }return Boolean(retval)
};
goog.events.fireListener = function(listener, eventObject) {
  var rv = listener.handleEvent(eventObject);
  if(listener.callOnce) {
    goog.events.unlistenByKey(listener.key)
  }return rv
};
goog.events.getTotalListenerCount = function() {
  return goog.object.getCount(goog.events.listeners_)
};
goog.events.dispatchEvent = function(src, e) {
  if(goog.isString(e)) {
    e = new goog.events.Event(e, src)
  }else if(!(e instanceof goog.events.Event)) {
    var oldEvent = e;
    e = new goog.events.Event(e.type, src);
    goog.object.extend(e, oldEvent)
  }else {
    e.target = e.target || src
  }var rv = 1, ancestors, type = e.type, map = goog.events.listenerTree_;
  if(!(type in map)) {
    return true
  }map = map[type];
  var hasCapture = true in map, hasBubble = false in map;
  if(hasCapture) {
    ancestors = [];
    for(var parent = src;parent;parent = parent.getParentEventTarget()) {
      ancestors.push(parent)
    }for(var i = ancestors.length - 1;!e.propagationStopped_ && i >= 0;i--) {
      e.currentTarget = ancestors[i];
      rv &= goog.events.fireListeners(ancestors[i], e.type, true, e) && e.returnValue_ != false
    }
  }if(hasBubble) {
    if(hasCapture) {
      for(var i = 0;!e.propagationStopped_ && i < ancestors.length;i++) {
        e.currentTarget = ancestors[i];
        rv &= goog.events.fireListeners(ancestors[i], e.type, false, e) && e.returnValue_ != false
      }
    }else {
      for(var current = src;!e.propagationStopped_ && current;current = current.getParentEventTarget()) {
        e.currentTarget = current;
        rv &= goog.events.fireListeners(current, e.type, false, e) && e.returnValue_ != false
      }
    }
  }return Boolean(rv)
};
goog.events.handleBrowserEvent_ = function(key, opt_evt) {
  if(!goog.events.listeners_[key]) {
    return true
  }var listener = goog.events.listeners_[key], type = listener.type, map = goog.events.listenerTree_;
  if(!(type in map)) {
    return true
  }map = map[type];
  var retval;
  if(goog.userAgent.IE) {
    var ieEvent = opt_evt || goog.getObjectByName("window.event"), hasCapture = true in map;
    if(hasCapture) {
      if(goog.events.isMarkedIeEvent_(ieEvent)) {
        return true
      }goog.events.markIeEvent_(ieEvent)
    }var srcHashCode = goog.getHashCode(listener.src), evt = goog.events.eventPool_.getObject();
    evt.init(ieEvent, this);
    retval = true;
    try {
      if(hasCapture) {
        var ancestors = goog.events.arrayPool_.getObject();
        for(var parent = evt.currentTarget;parent;parent = parent.parentNode) {
          ancestors.push(parent)
        }for(var i = ancestors.length - 1;!evt.propagationStopped_ && i >= 0;i--) {
          evt.currentTarget = ancestors[i];
          retval &= goog.events.fireListeners(ancestors[i], type, true, evt)
        }for(var i = 0;!evt.propagationStopped_ && i < ancestors.length;i++) {
          evt.currentTarget = ancestors[i];
          retval &= goog.events.fireListeners(ancestors[i], type, false, evt)
        }
      }else {
        retval = goog.events.fireListener(listener, evt)
      }
    }finally {
      if(ancestors) {
        ancestors.length = 0;
        goog.events.arrayPool_.releaseObject(ancestors)
      }evt.dispose();
      goog.events.eventPool_.releaseObject(evt)
    }return retval
  }var be = new goog.events.BrowserEvent(opt_evt, this);
  try {
    retval = goog.events.fireListener(listener, be)
  }finally {
    be.dispose()
  }return retval
};
goog.events.markIeEvent_ = function(e) {
  var useReturnValue = false;
  if(e.keyCode == 0) {
    try {
      e.keyCode = -1;
      return
    }catch(ex) {
      useReturnValue = true
    }
  }if(useReturnValue || e.returnValue == undefined) {
    e.returnValue = true
  }
};
goog.events.isMarkedIeEvent_ = function(e) {
  return e.keyCode < 0 || e.returnValue != undefined
};goog.events.EventTarget = function() {
};
goog.events.EventTarget.inherits(goog.Disposable);
goog.events.EventTarget.prototype.getParentEventTarget = function() {
  return null
};
goog.events.EventTarget.prototype.addEventListener = function(type, handler, opt_capture, opt_handlerScope) {
  goog.events.listen(this, type, handler, opt_capture, opt_handlerScope)
};
goog.events.EventTarget.prototype.removeEventListener = function(type, handler, opt_capture, opt_handlerScope) {
  goog.events.unlisten(this, type, handler, opt_capture, opt_handlerScope)
};
goog.events.EventTarget.prototype.dispatchEvent = function(e) {
  return goog.events.dispatchEvent(this, e)
};
goog.events.EventTarget.prototype.dispose = function() {
  if(!this.getDisposed()) {
    goog.Disposable.prototype.dispose.call(this);
    goog.events.removeAll(this)
  }
};
goog.events.EventTarget.prototype.customEvent_ = true;goog.json = {};
goog.json.isValid_ = function(s) {
  if(s == "") {
    return false
  }s = s.replace(/"(\\.|[^"\\])*"/g, "");
  return s == "" || !/[^,:{}\[\]0-9.\-+Eaeflnr-u \n\r\t]/.test(s)
};
goog.json.parse = function(s) {
  s = String(s);
  if(typeof s.parseJSON == "function") {
    return s.parseJSON()
  }if(goog.json.isValid_(s)) {
    try {
      return eval("(" + s + ")")
    }catch(ex) {
    }
  }throw Error("Invalid JSON string: " + s);
};
goog.json.unsafeParse = function(s) {
  return eval("(" + s + ")")
};
goog.json.serializer_ = null;
goog.json.serialize = function(object) {
  if(!goog.json.serializer_) {
    goog.json.serializer_ = new goog.json.Serializer
  }return goog.json.serializer_.serialize(object)
};
goog.json.Serializer = function() {
};
goog.json.Serializer.prototype.serialize = function(object) {
  if(object != null && typeof object.toJSONString == "function") {
    return object.toJSONString()
  }var sb = [];
  this.serialize_(object, sb);
  return sb.join("")
};
goog.json.Serializer.prototype.serialize_ = function(object, sb) {
  switch(typeof object) {
    case "string":
      this.serializeString_(object, sb);
      break;
    case "number":
      this.serializeNumber_(object, sb);
      break;
    case "boolean":
      sb.push(object);
      break;
    case "undefined":
      sb.push("null");
      break;
    case "object":
      if(object == null) {
        sb.push("null");
        break
      }if(goog.isArray(object)) {
        this.serializeArray_(object, sb);
        break
      }this.serializeObject_(object, sb);
      break;
    default:
      throw Error("Unknown type: " + typeof object);
  }
};
goog.json.Serializer.charToJsonCharCache_ = {'"':'\\"', "\\":"\\\\", "/":"\\/", "\u0008":"\\b", "\u000c":"\\f", "\n":"\\n", "\r":"\\r", "\t":"\\t", "\u000b":"\\u000b"};
goog.json.Serializer.prototype.serializeString_ = function(s, sb) {
  sb.push('"', s.replace(/[\\\"\x00-\x1f\x80-\uffff]/g, function(c) {
    if(c in goog.json.Serializer.charToJsonCharCache_) {
      return goog.json.Serializer.charToJsonCharCache_[c]
    }var cc = c.charCodeAt(0), rv = "\\u";
    if(cc < 16) {
      rv += "000"
    }else if(cc < 256) {
      rv += "00"
    }else if(cc < 4096) {
      rv += "0"
    }return goog.json.Serializer.charToJsonCharCache_[c] = rv + cc.toString(16)
  }), '"')
};
goog.json.Serializer.prototype.serializeNumber_ = function(n, sb) {
  sb.push(isFinite(n) && !isNaN(n) ? n : "null")
};
goog.json.Serializer.prototype.serializeArray_ = function(arr, sb) {
  var l = arr.length;
  sb.push("[");
  var sep = "";
  for(var i = 0;i < l;i++) {
    sb.push(sep);
    this.serialize_(arr[i], sb);
    sep = ","
  }sb.push("]")
};
goog.json.Serializer.prototype.serializeObject_ = function(obj, sb) {
  sb.push("{");
  var sep = "";
  for(var key in obj) {
    sb.push(sep);
    this.serializeString_(key, sb);
    sb.push(":");
    this.serialize_(obj[key], sb);
    sep = ","
  }sb.push("}")
};goog.Timer = function(opt_interval, opt_timerObject) {
  goog.events.EventTarget.call(this);
  this.interval_ = opt_interval || 1;
  this.timerObject_ = opt_timerObject || goog.Timer.defaultTimerObject;
  this.boundTick_ = goog.bind(this.tick_, this);
  this.last_ = goog.now()
};
goog.Timer.inherits(goog.events.EventTarget);
goog.Timer.prototype.enabled = false;
goog.Timer.defaultTimerObject = goog.global.window;
goog.Timer.intervalScale = 0.8;
goog.Timer.prototype.timer_ = null;
goog.Timer.prototype.setInterval = function(interval) {
  this.interval_ = interval;
  if(this.timer_ && this.enabled) {
    this.stop();
    this.start()
  }else if(this.timer_) {
    this.stop()
  }
};
goog.Timer.prototype.tick_ = function() {
  if(this.enabled) {
    var elapsed = goog.now() - this.last_;
    if(elapsed > 0 && elapsed < this.interval_ * goog.Timer.intervalScale) {
      this.timer_ = this.timerObject_.setTimeout(this.boundTick_, this.interval_ - elapsed);
      return
    }this.dispatchTick_();
    if(this.enabled) {
      this.timer_ = this.timerObject_.setTimeout(this.boundTick_, this.interval_);
      this.last_ = goog.now()
    }
  }
};
goog.Timer.prototype.dispatchTick_ = function() {
  this.dispatchEvent(goog.Timer.TICK)
};
goog.Timer.prototype.start = function() {
  this.enabled = true;
  if(!this.timer_) {
    this.timer_ = this.timerObject_.setTimeout(this.boundTick_, this.interval_);
    this.last_ = goog.now()
  }
};
goog.Timer.prototype.stop = function() {
  this.enabled = false;
  this.timerObject_.clearTimeout(this.timer_);
  this.timer_ = null
};
goog.Timer.prototype.dispose = function() {
  if(!this.getDisposed()) {
    goog.events.EventTarget.prototype.dispose.call(this);
    this.stop();
    this.timerObject_ = null
  }
};
goog.Timer.TICK = "tick";
goog.Timer.callOnce = function(listener, opt_interval, opt_handler) {
  if(goog.isFunction(listener)) {
    if(opt_handler) {
      listener = goog.bind(listener, opt_handler)
    }
  }else if(listener && typeof listener.handleEvent == "function") {
    listener = goog.bind(listener.handleEvent, listener)
  }else {
    throw Error("Invalid listener argument");
  }return goog.Timer.defaultTimerObject.setTimeout(listener, opt_interval || 0)
};
goog.Timer.clear = function(timerId) {
  goog.Timer.defaultTimerObject.clearTimeout(timerId)
};goog.net.XhrLite = function() {
  goog.events.EventTarget.call(this);
  this.headers = new goog.structs.Map
};
goog.net.XhrLite.inherits(goog.events.EventTarget);
goog.net.XhrLite.prototype.logger_ = goog.debug.Logger.getLogger("goog.net.XhrLite");
goog.net.XhrLite.CONTENT_TYPE_HEADER = "Content-Type";
goog.net.XhrLite.FORM_CONTENT_TYPE = "application/x-www-form-urlencoded;charset=utf-8";
goog.net.XhrLite.sendInstances_ = [];
goog.net.XhrLite.send = function(url, opt_callback, opt_method, opt_content, opt_headers, opt_timeoutInterval) {
  var x = new goog.net.XhrLite;
  goog.net.XhrLite.sendInstances_.push(x);
  if(opt_callback) {
    goog.events.listen(x, goog.net.EventType.COMPLETE, opt_callback)
  }goog.events.listen(x, goog.net.EventType.READY, goog.net.XhrLite.cleanupSend_.partial(x));
  if(opt_timeoutInterval) {
    x.setTimeoutInterval(opt_timeoutInterval)
  }x.send(url, opt_method, opt_content, opt_headers)
};
goog.net.XhrLite.cleanup = function() {
  var instances = goog.net.XhrLite.sendInstances_;
  while(instances.length) {
    instances.pop().dispose()
  }
};
goog.net.XhrLite.cleanupSend_ = function(xhrLite) {
  xhrLite.dispose();
  goog.array.remove(goog.net.XhrLite.sendInstances_, xhrLite)
};
goog.net.XhrLite.prototype.active_ = false;
goog.net.XhrLite.prototype.xhr_ = null;
goog.net.XhrLite.prototype.xhrOptions_ = null;
goog.net.XhrLite.prototype.lastUri_ = "";
goog.net.XhrLite.prototype.lastMethod_ = "";
goog.net.XhrLite.prototype.lastErrorCode_ = goog.net.ErrorCode.NO_ERROR;
goog.net.XhrLite.prototype.lastError_ = "";
goog.net.XhrLite.prototype.timeoutInterval_ = 0;
goog.net.XhrLite.prototype.timeoutId_ = null;
goog.net.XhrLite.prototype.getTimeoutInterval = function() {
  return this.timeoutInterval_
};
goog.net.XhrLite.prototype.setTimeoutInterval = function(ms) {
  this.timeoutInterval_ = Math.max(0, ms)
};
goog.net.XhrLite.prototype.send = function(url, opt_method, opt_content, opt_headers) {
  if(this.active_) {
    throw Error("[goog.net.XhrLite] Object is active with another request");
  }var method = opt_method || "GET";
  this.lastUri_ = url;
  this.lastError_ = "";
  this.lastErrorCode_ = goog.net.ErrorCode.NO_ERROR;
  this.lastMethod_ = method;
  this.active_ = true;
  this.xhr_ = new goog.net.XmlHttp;
  this.xhrOptions_ = goog.net.XmlHttp.getOptions();
  goog.net.xhrMonitor.markXhrOpen(this.xhr_);
  this.xhr_.onreadystatechange = goog.bind(this.onReadyStateChange_, this);
  try {
    this.log_("Opening Xhr");
    this.xhr_.open(method, url, true)
  }catch(err) {
    this.log_("Error opening Xhr: " + err.message);
    this.error_(goog.net.ErrorCode.EXCEPTION, err);
    return
  }var content = opt_content ? String(opt_content) : "", headers = this.headers.clone();
  if(opt_headers) {
    goog.structs.forEach(opt_headers, function(value, key) {
      headers.set(key, value)
    })
  }if(method == "POST" && !headers.containsKey(goog.net.XhrLite.CONTENT_TYPE_HEADER)) {
    headers.set(goog.net.XhrLite.CONTENT_TYPE_HEADER, goog.net.XhrLite.FORM_CONTENT_TYPE)
  }goog.structs.forEach(headers, function(value, key) {
    this.xhr_.setRequestHeader(key, value)
  }, this);
  try {
    if(this.timeoutId_) {
      goog.Timer.defaultTimerObject.clearTimeout(this.timeoutId_);
      this.timeoutId_ = null
    }if(this.timeoutInterval_ > 0) {
      this.log_("Will abort after " + this.timeoutInterval_ + "ms if incomplete");
      this.timeoutId_ = goog.Timer.defaultTimerObject.setTimeout(goog.bind(this.timeout_, this), this.timeoutInterval_)
    }this.log_("Sending request");
    this.xhr_.send(content)
  }catch(err) {
    this.log_("Send error: " + err.message);
    this.error_(goog.net.ErrorCode.EXCEPTION, err)
  }
};
goog.net.XhrLite.prototype.dispatchEvent = function(e) {
  if(this.xhr_) {
    goog.net.xhrMonitor.pushContext(this.xhr_);
    try {
      goog.net.XhrLite.superClass_.dispatchEvent.call(this, e)
    }finally {
      goog.net.xhrMonitor.popContext()
    }
  }else {
    goog.net.XhrLite.superClass_.dispatchEvent.call(this, e)
  }
};
goog.net.XhrLite.prototype.timeout_ = function() {
  if(typeof goog == "undefined") {
  }else if(this.xhr_) {
    this.lastError_ = "Timed out after " + this.timeoutInterval_ + "ms, aborting";
    this.lastErrorCode_ = goog.net.ErrorCode.TIMEOUT;
    this.log_(this.lastError_);
    this.dispatchEvent(goog.net.EventType.TIMEOUT);
    this.abort(goog.net.ErrorCode.TIMEOUT)
  }
};
goog.net.XhrLite.prototype.error_ = function(errorCode, err) {
  this.active_ = false;
  this.xhr_.abort();
  this.lastError_ = err;
  this.lastErrorCode_ = errorCode;
  this.dispatchEvent(goog.net.EventType.COMPLETE);
  this.dispatchEvent(goog.net.EventType.ERROR);
  this.cleanUpXhr_()
};
goog.net.XhrLite.prototype.abort = function(opt_failureCode) {
  if(this.xhr_) {
    this.log_("Aborting");
    this.active_ = false;
    this.xhr_.abort();
    this.lastErrorCode_ = opt_failureCode || goog.net.ErrorCode.ABORT;
    this.dispatchEvent(goog.net.EventType.COMPLETE);
    this.dispatchEvent(goog.net.EventType.ABORT);
    this.cleanUpXhr_()
  }
};
goog.net.XhrLite.prototype.dispose = function() {
  if(!this.getDisposed()) {
    if(this.xhr_) {
      this.active_ = false;
      this.xhr_.abort();
      this.cleanUpXhr_(true)
    }goog.net.XhrLite.superClass_.dispose.call(this)
  }
};
goog.net.XhrLite.prototype.onReadyStateChange_ = function() {
  if(!this.active_) {
    return
  }if(typeof goog == "undefined") {
  }else if(this.xhrOptions_[goog.net.XmlHttp.OptionType.LOCAL_REQUEST_ERROR] && this.getReadyState() == goog.net.XmlHttp.ReadyState.COMPLETE && this.getStatus() == 2) {
    this.log_("Local request error detected and ignored")
  }else {
    this.dispatchEvent(goog.net.EventType.READY_STATE_CHANGE);
    if(this.isComplete()) {
      this.log_("Request complete");
      this.active_ = false;
      if(this.isSuccess()) {
        this.dispatchEvent(goog.net.EventType.COMPLETE);
        this.dispatchEvent(goog.net.EventType.SUCCESS)
      }else {
        this.lastErrorCode_ = goog.net.ErrorCode.HTTP_ERROR;
        this.lastError_ = this.getStatusText() + " [" + this.getStatus() + "]";
        this.dispatchEvent(goog.net.EventType.COMPLETE);
        this.dispatchEvent(goog.net.EventType.ERROR)
      }this.cleanUpXhr_()
    }
  }
};
goog.net.XhrLite.prototype.cleanUpXhr_ = function(opt_fromDispose) {
  if(this.xhr_) {
    this.xhr_.onreadystatechange = this.xhrOptions_[goog.net.XmlHttp.OptionType.USE_NULL_FUNCTION] ? goog.nullFunction : null;
    var xhr = this.xhr_;
    this.xhr_ = null;
    this.xhrOptions_ = null;
    if(this.timeoutId_) {
      goog.Timer.defaultTimerObject.clearTimeout(this.timeoutId_);
      this.timeoutId_ = null
    }if(!opt_fromDispose) {
      goog.net.xhrMonitor.pushContext(xhr);
      this.dispatchEvent(goog.net.EventType.READY);
      goog.net.xhrMonitor.popContext()
    }goog.net.xhrMonitor.markXhrClosed(xhr)
  }
};
goog.net.XhrLite.prototype.isActive = function() {
  return this.active_
};
goog.net.XhrLite.prototype.isComplete = function() {
  return this.getReadyState() == goog.net.XmlHttp.ReadyState.COMPLETE
};
goog.net.XhrLite.prototype.isSuccess = function() {
  switch(this.getStatus()) {
    case 0:
    ;
    case 200:
    ;
    case 304:
      return true;
    default:
      return false
  }
};
goog.net.XhrLite.prototype.getReadyState = function() {
  return this.xhr_ ? this.xhr_.readyState : goog.net.XmlHttp.ReadyState.UNINITIALIZED
};
goog.net.XhrLite.prototype.getStatus = function() {
  try {
    return this.getReadyState() > goog.net.XmlHttp.ReadyState.LOADED ? this.xhr_.status : -1
  }catch(e) {
    this.logger_.warning("Can not get status: " + e.message);
    return-1
  }
};
goog.net.XhrLite.prototype.getStatusText = function() {
  try {
    return this.getReadyState() > goog.net.XmlHttp.ReadyState.LOADED ? this.xhr_.statusText : ""
  }catch(e) {
    this.logger_.fine("Can not get status: " + e.message);
    return""
  }
};
goog.net.XhrLite.prototype.getLastUri = function() {
  return this.lastUri_
};
goog.net.XhrLite.prototype.getResponseText = function() {
  return this.xhr_ ? this.xhr_.responseText : ""
};
goog.net.XhrLite.prototype.getResponseXml = function() {
  return this.xhr_ ? this.xhr_.responseXML : null
};
goog.net.XhrLite.prototype.getResponseJson = function() {
  return this.xhr_ ? goog.json.parse(this.xhr_.responseText) : undefined
};
goog.net.XhrLite.prototype.getResponseHeader = function(key) {
  return this.xhr_ && this.isComplete() ? this.xhr_.getResponseHeader(key) : undefined
};
goog.net.XhrLite.prototype.getLastErrorCode = function() {
  return this.lastErrorCode_
};
goog.net.XhrLite.prototype.getLastError = function() {
  return this.lastError_
};
goog.net.XhrLite.prototype.log_ = function(msg) {
  this.logger_.fine(msg + " [" + this.lastMethod_ + " " + this.lastUri_ + " " + this.getStatus() + "]")
};goog.Uri = function(opt_uri, opt_ignoreCase) {
  var m;
  if(opt_uri instanceof goog.Uri) {
    this.setIgnoreCase(opt_ignoreCase == null ? opt_uri.getIgnoreCase() : opt_ignoreCase);
    this.setScheme(opt_uri.getScheme());
    this.setUserInfo(opt_uri.getUserInfo());
    this.setDomain(opt_uri.getDomain());
    this.setPort(opt_uri.getPort());
    this.setPath(opt_uri.getPath());
    this.setQueryData(opt_uri.getQueryData().clone());
    this.setFragment(opt_uri.getFragment())
  }else if(opt_uri && (m = String(opt_uri).match(goog.Uri.getRE_()))) {
    this.setIgnoreCase(!(!opt_ignoreCase));
    this.setScheme(m[1], true);
    this.setUserInfo(m[2], true);
    this.setDomain(m[3], true);
    this.setPort(m[4]);
    this.setPath(m[5], true);
    this.setQueryData(m[6]);
    this.setFragment(m[7], true)
  }else {
    this.setIgnoreCase(!(!opt_ignoreCase));
    this.queryData_ = new goog.Uri.QueryData(null, this, this.ignoreCase_)
  }
};
goog.Uri.RANDOM_PARAM = "zx";
goog.Uri.prototype.scheme_ = "";
goog.Uri.prototype.userInfo_ = "";
goog.Uri.prototype.domain_ = "";
goog.Uri.prototype.port_ = null;
goog.Uri.prototype.path_ = "";
goog.Uri.prototype.queryData_ = null;
goog.Uri.prototype.fragment_ = "";
goog.Uri.prototype.isReadOnly_ = false;
goog.Uri.prototype.ignoreCase_ = false;
goog.Uri.prototype.toString = function() {
  if(this.cachedToString_) {
    return this.cachedToString_
  }var out = [];
  if(this.scheme_) {
    out.push(goog.Uri.encodeSpecialChars_(this.scheme_, goog.Uri.reDisallowedInSchemeOrUserInfo_), ":")
  }if(this.domain_) {
    out.push("//");
    if(this.userInfo_) {
      out.push(goog.Uri.encodeSpecialChars_(this.userInfo_, goog.Uri.reDisallowedInSchemeOrUserInfo_), "@")
    }out.push(goog.Uri.encodeString_(this.domain_));
    if(this.port_ != null) {
      out.push(":", String(this.getPort()))
    }
  }if(this.path_) {
    out.push(goog.Uri.encodeSpecialChars_(this.path_, goog.Uri.reDisallowedInPath_))
  }var query = String(this.queryData_);
  if(query) {
    out.push("?", query)
  }if(this.fragment_) {
    out.push("#", goog.Uri.encodeString_(this.fragment_))
  }return this.cachedToString_ = out.join("")
};
goog.Uri.prototype.resolve = function(relativeUri) {
  var absoluteUri = this.clone(), overridden = relativeUri.hasScheme();
  if(overridden) {
    absoluteUri.setScheme(relativeUri.getScheme())
  }else {
    overridden = relativeUri.hasUserInfo()
  }if(overridden) {
    absoluteUri.setUserInfo(relativeUri.getUserInfo())
  }else {
    overridden = relativeUri.hasDomain()
  }if(overridden) {
    absoluteUri.setDomain(relativeUri.getDomain())
  }else {
    overridden = relativeUri.hasPort()
  }var path = relativeUri.getPath();
  if(overridden) {
    absoluteUri.setPort(relativeUri.getPort())
  }else {
    overridden = relativeUri.hasPath();
    if(overridden) {
      if(!/^\//.test(path)) {
        path = absoluteUri.getPath().replace(/\/?[^\/]*$/, "/" + path)
      }
    }
  }if(overridden) {
    absoluteUri.setPath(path)
  }else {
    overridden = relativeUri.hasQuery()
  }if(overridden) {
    absoluteUri.setQueryData(relativeUri.getQuery())
  }else {
    overridden = relativeUri.hasFragment()
  }if(overridden) {
    absoluteUri.setFragment(relativeUri.getFragment())
  }return absoluteUri
};
goog.Uri.prototype.clone = function() {
  return new goog.Uri.create(this.scheme_, this.userInfo_, this.domain_, this.port_, this.path_, this.queryData_.clone(), this.fragment_, this.ignoreCase_)
};
goog.Uri.prototype.getScheme = function() {
  return this.scheme_
};
goog.Uri.prototype.setScheme = function(newScheme, opt_decode) {
  this.enforceReadOnly();
  delete this.cachedToString_;
  this.scheme_ = opt_decode ? goog.Uri.decodeOrEmpty_(newScheme) : newScheme;
  if(this.scheme_) {
    this.scheme_ = this.scheme_.replace(/:$/, "")
  }return this
};
goog.Uri.prototype.hasScheme = function() {
  return!(!this.scheme_)
};
goog.Uri.prototype.getUserInfo = function() {
  return this.userInfo_
};
goog.Uri.prototype.setUserInfo = function(newUserInfo, opt_decode) {
  this.enforceReadOnly();
  delete this.cachedToString_;
  this.userInfo_ = opt_decode ? goog.Uri.decodeOrEmpty_(newUserInfo) : newUserInfo;
  return this
};
goog.Uri.prototype.hasUserInfo = function() {
  return!(!this.userInfo_)
};
goog.Uri.prototype.getDomain = function() {
  return this.domain_
};
goog.Uri.prototype.setDomain = function(newDomain, opt_decode) {
  this.enforceReadOnly();
  delete this.cachedToString_;
  this.domain_ = opt_decode ? goog.Uri.decodeOrEmpty_(newDomain) : newDomain;
  return this
};
goog.Uri.prototype.hasDomain = function() {
  return!(!this.domain_)
};
goog.Uri.prototype.getPort = function() {
  return this.port_
};
goog.Uri.prototype.setPort = function(newPort) {
  this.enforceReadOnly();
  delete this.cachedToString_;
  if(newPort) {
    newPort = Number(newPort);
    if(isNaN(newPort) || newPort < 0) {
      throw Error("Bad port number " + newPort);
    }this.port_ = newPort
  }else {
    this.port_ = null
  }return this
};
goog.Uri.prototype.hasPort = function() {
  return this.port_ != null
};
goog.Uri.prototype.getPath = function() {
  return this.path_
};
goog.Uri.prototype.setPath = function(newPath, opt_decode) {
  this.enforceReadOnly();
  delete this.cachedToString_;
  this.path_ = opt_decode ? goog.Uri.decodeOrEmpty_(newPath) : newPath;
  return this
};
goog.Uri.prototype.hasPath = function() {
  return!(!this.path_)
};
goog.Uri.prototype.hasQuery = function() {
  return this.queryData_ !== null && this.queryData_.toString() !== ""
};
goog.Uri.prototype.setQueryData = function(queryData) {
  this.enforceReadOnly();
  delete this.cachedToString_;
  if(queryData instanceof goog.Uri.QueryData) {
    this.queryData_ = queryData;
    this.queryData_.uri_ = this;
    this.queryData_.setIgnoreCase(this.ignoreCase_)
  }else {
    this.queryData_ = new goog.Uri.QueryData(queryData, this, this.ignoreCase_)
  }return this
};
goog.Uri.prototype.getQuery = function() {
  return this.queryData_.toString()
};
goog.Uri.prototype.getQueryData = function() {
  return this.queryData_
};
goog.Uri.prototype.setParameterValue = function(key, value) {
  this.enforceReadOnly();
  delete this.cachedToString_;
  this.queryData_.set(key, value);
  return this
};
goog.Uri.prototype.setParameterValues = function(key, values) {
  this.enforceReadOnly();
  delete this.cachedToString_;
  if(!goog.isArray(values)) {
    values = [String(values)]
  }this.queryData_.setValues(key, values);
  return this
};
goog.Uri.prototype.getParameterValues = function(name) {
  return this.queryData_.getValues(name)
};
goog.Uri.prototype.getParameterValue = function(paramName) {
  return this.queryData_.get(paramName)
};
goog.Uri.prototype.getFragment = function() {
  return this.fragment_
};
goog.Uri.prototype.setFragment = function(newFragment, opt_decode) {
  this.enforceReadOnly();
  delete this.cachedToString_;
  this.fragment_ = opt_decode ? goog.Uri.decodeOrEmpty_(newFragment) : newFragment;
  return this
};
goog.Uri.prototype.hasFragment = function() {
  return!(!this.fragment_)
};
goog.Uri.prototype.hasSameDomainAs = function(uri2) {
  return(!this.hasDomain() && !uri2.hasDomain() || this.getDomain() == uri2.getDomain()) && (!this.hasPort() && !uri2.hasPort() || this.getPort() == uri2.getPort())
};
goog.Uri.prototype.makeUnique = function() {
  this.enforceReadOnly();
  this.setParameterValue(goog.Uri.RANDOM_PARAM, goog.string.getRandomString());
  return this
};
goog.Uri.prototype.setReadOnly = function(isReadOnly) {
  this.isReadOnly_ = isReadOnly
};
goog.Uri.prototype.isReadOnly = function() {
  return this.isReadOnly_
};
goog.Uri.prototype.enforceReadOnly = function() {
  if(this.isReadOnly_) {
    throw Error("Tried to modify a read-only Uri");
  }
};
goog.Uri.prototype.setIgnoreCase = function(ignoreCase) {
  this.ignoreCase_ = ignoreCase;
  if(this.queryData_) {
    this.queryData_.setIgnoreCase(ignoreCase)
  }
};
goog.Uri.prototype.getIgnoreCase = function() {
  return this.ignoreCase_
};
goog.Uri.parse = function(uri, opt_ignoreCase) {
  return uri instanceof goog.Uri ? uri.clone() : new goog.Uri(uri, opt_ignoreCase)
};
goog.Uri.create = function(opt_scheme, opt_userInfo, opt_domain, opt_port, opt_path, opt_query, opt_fragment, opt_ignoreCase) {
  var uri = new goog.Uri(null, opt_ignoreCase);
  uri.setScheme(opt_scheme);
  uri.setUserInfo(opt_userInfo);
  uri.setDomain(opt_domain);
  uri.setPort(opt_port);
  uri.setPath(opt_path);
  uri.setQueryData(opt_query);
  uri.setFragment(opt_fragment);
  return uri
};
goog.Uri.resolve = function(base, rel) {
  if(!(base instanceof goog.Uri)) {
    base = goog.Uri.parse(base)
  }if(!(rel instanceof goog.Uri)) {
    rel = goog.Uri.parse(rel)
  }return base.resolve(rel)
};
goog.Uri.decodeOrEmpty_ = function(val) {
  return val ? goog.string.urlDecode(val) : ""
};
goog.Uri.encodeString_ = function(unescapedPart) {
  if(goog.isString(unescapedPart)) {
    return encodeURIComponent(unescapedPart)
  }return null
};
goog.Uri.encodeSpecialRegExp_ = /^[a-zA-Z0-9\-_.!~*'():\/;?]*$/;
goog.Uri.encodeSpecialChars_ = function(unescapedPart, extra) {
  var ret = null;
  if(goog.isString(unescapedPart)) {
    ret = unescapedPart;
    if(!goog.Uri.encodeSpecialRegExp_.test(ret)) {
      ret = encodeURI(unescapedPart)
    }if(ret.search(extra) >= 0) {
      ret = ret.replace(extra, goog.Uri.encodeChar_)
    }
  }return ret
};
goog.Uri.encodeChar_ = function(ch) {
  var n = ch.charCodeAt(0);
  return"%" + (n >> 4 & 15).toString(16) + (n & 15).toString(16)
};
goog.Uri.re_ = null;
goog.Uri.getRE_ = function() {
  if(!goog.Uri.re_) {
    goog.Uri.re_ = /^(?:([^:\/?#]+):)?(?:\/\/(?:([^\/?#]*)@)?([^\/?#:@]*)(?::([0-9]+))?)?([^?#]+)?(?:\?([^#]*))?(?:#(.*))?$/
  }return goog.Uri.re_
};
goog.Uri.reDisallowedInSchemeOrUserInfo_ = /[#\/\?@]/g;
goog.Uri.reDisallowedInPath_ = /[\#\?]/g;
goog.Uri.haveSameDomain = function(uri1String, uri2String) {
  var uri1 = new goog.Uri(uri1String), uri2 = new goog.Uri(uri2String);
  return uri1.hasSameDomainAs(uri2)
};
goog.Uri.QueryData = function(opt_query, opt_uri, opt_ignoreCase) {
  this.keyMap_ = new goog.structs.Map;
  this.uri_ = opt_uri;
  this.ignoreCase_ = !(!opt_ignoreCase);
  if(opt_query) {
    var pairs = opt_query.split("&");
    for(var i = 0;i < pairs.length;i++) {
      var parts = pairs[i].split("="), name = goog.string.urlDecode(parts[0]);
      name = this.getKeyName_(name);
      this.add(name, parts.length > 1 ? goog.string.urlDecode(parts[1]) : "")
    }
  }
};
goog.Uri.QueryData.prototype.count_ = 0;
goog.Uri.QueryData.prototype.getCount = function() {
  return this.count_
};
goog.Uri.QueryData.prototype.add = function(key, value) {
  this.invalidateCache_();
  key = this.getKeyName_(key);
  if(!this.containsKey(key)) {
    this.keyMap_.set(key, value)
  }else {
    var current = this.keyMap_.get(key);
    if(goog.isArray(current)) {
      current.push(value)
    }else {
      this.keyMap_.set(key, [current, value])
    }
  }this.count_++;
  return this
};
goog.Uri.QueryData.prototype.remove = function(key) {
  key = this.getKeyName_(key);
  if(this.keyMap_.containsKey(key)) {
    this.invalidateCache_();
    var old = this.keyMap_.get(key);
    if(goog.isArray(old)) {
      this.count_ -= old.length
    }else {
      this.count_--
    }return this.keyMap_.remove(key)
  }return false
};
goog.Uri.QueryData.prototype.clear = function() {
  this.invalidateCache_();
  this.keyMap_.clear();
  this.count_ = 0
};
goog.Uri.QueryData.prototype.isEmpty = function() {
  return this.count_ == 0
};
goog.Uri.QueryData.prototype.containsKey = function(key) {
  key = this.getKeyName_(key);
  return this.keyMap_.containsKey(key)
};
goog.Uri.QueryData.prototype.containsValue = function(value) {
  var vals = this.getValues();
  return goog.array.contains(vals, value)
};
goog.Uri.QueryData.prototype.getKeys = function() {
  var vals = this.keyMap_.getValues(), keys = this.keyMap_.getKeys(), rv = [];
  for(var i = 0;i < keys.length;i++) {
    var val = vals[i];
    if(goog.isArray(val)) {
      for(var j = 0;j < val.length;j++) {
        rv.push(keys[i])
      }
    }else {
      rv.push(keys[i])
    }
  }return rv
};
goog.Uri.QueryData.prototype.getValues = function(opt_key) {
  var rv;
  if(opt_key) {
    var key = this.getKeyName_(opt_key);
    if(this.containsKey(key)) {
      var value = this.keyMap_.get(key);
      if(goog.isArray(value)) {
        return value
      }else {
        rv = [];
        rv.push(value)
      }
    }else {
      rv = []
    }
  }else {
    var vals = this.keyMap_.getValues();
    rv = [];
    for(var i = 0;i < vals.length;i++) {
      var val = vals[i];
      if(goog.isArray(val)) {
        goog.array.extend(rv, val)
      }else {
        rv.push(val)
      }
    }
  }return rv
};
goog.Uri.QueryData.prototype.set = function(key, value) {
  this.invalidateCache_();
  key = this.getKeyName_(key);
  if(this.containsKey(key)) {
    var old = this.keyMap_.get(key);
    if(goog.isArray(old)) {
      this.count_ -= old.length
    }else {
      this.count_--
    }
  }this.keyMap_.set(key, value);
  this.count_++;
  return this
};
goog.Uri.QueryData.prototype.get = function(key, opt_default) {
  key = this.getKeyName_(key);
  if(this.containsKey(key)) {
    var val = this.keyMap_.get(key);
    if(goog.isArray(val)) {
      return val[0]
    }else {
      return val
    }
  }else {
    return opt_default
  }
};
goog.Uri.QueryData.prototype.setValues = function(key, values) {
  this.invalidateCache_();
  key = this.getKeyName_(key);
  if(this.containsKey(key)) {
    var old = this.keyMap_.get(key);
    if(goog.isArray(old)) {
      this.count_ -= old.length
    }else {
      this.count_--
    }
  }if(values.length > 0) {
    this.keyMap_.set(key, values);
    this.count_ += values.length
  }
};
goog.Uri.QueryData.prototype.toString = function() {
  if(this.cachedToString_) {
    return this.cachedToString_
  }var sb = [], count = 0, keys = this.keyMap_.getKeys();
  for(var i = 0;i < keys.length;i++) {
    var key = keys[i], encodedKey = goog.string.urlEncode(key), val = this.keyMap_.get(key);
    if(goog.isArray(val)) {
      for(var j = 0;j < val.length;j++) {
        if(count > 0) {
          sb.push("&")
        }sb.push(encodedKey, "=", goog.string.urlEncode(val[j]));
        count++
      }
    }else {
      if(count > 0) {
        sb.push("&")
      }sb.push(encodedKey, "=", goog.string.urlEncode(val));
      count++
    }
  }return this.cachedToString_ = sb.join("")
};
goog.Uri.QueryData.prototype.invalidateCache_ = function() {
  delete this.cachedToString_;
  if(this.uri_) {
    delete this.uri_.cachedToString_
  }
};
goog.Uri.QueryData.prototype.filterKeys = function(keys) {
  goog.structs.forEach(this.keyMap_, function(value, key, map) {
    if(!goog.array.contains(keys, key)) {
      this.remove(key)
    }
  }, this);
  return this
};
goog.Uri.QueryData.prototype.clone = function() {
  var rv = new goog.Uri.QueryData;
  rv.keyMap_ = this.keyMap_.clone();
  return rv
};
goog.Uri.QueryData.prototype.getKeyName_ = function(arg) {
  var keyName = String(arg);
  if(this.ignoreCase_) {
    keyName = keyName.toLowerCase()
  }return keyName
};
goog.Uri.QueryData.prototype.setIgnoreCase = function(ignoreCase) {
  var resetKeys = ignoreCase && !this.ignoreCase_;
  if(resetKeys) {
    this.invalidateCache_();
    goog.structs.forEach(this.keyMap_, function(value, key, map) {
      var lowerCase = key.toLowerCase();
      if(key != lowerCase) {
        this.remove(key);
        this.add(lowerCase, value)
      }
    }, this)
  }this.ignoreCase_ = ignoreCase
};
goog.Uri.QueryData.prototype.extend = function(var_args) {
  for(var i = 0;i < arguments.length;i++) {
    var data = arguments[i];
    goog.structs.forEach(data, function(value, key) {
      this.add(key, value)
    }, this)
  }
};goog.style = {};
goog.style.setStyle = function(element, style, value) {
  element.style[goog.style.toCamelCase(style)] = value
};
goog.style.getStyle = function(element, style) {
  return element.style[goog.style.toCamelCase(style)]
};
goog.style.getComputedStyle = function(element, style) {
  var doc = goog.dom.getOwnerDocument(element);
  if(doc.defaultView && doc.defaultView.getComputedStyle) {
    var styles = doc.defaultView.getComputedStyle(element, "");
    if(styles) {
      return styles[style]
    }
  }return null
};
goog.style.getCascadedStyle = function(element, style) {
  return element.currentStyle ? element.currentStyle[style] : null
};
goog.style.getStyle_ = function(element, style) {
  return goog.style.getComputedStyle(element, style) || goog.style.getCascadedStyle(element, style) || element.style[style]
};
goog.style.getBackgroundColor = function(element) {
  return goog.style.getStyle_(element, "backgroundColor")
};
goog.style.setPosition = function(el, arg1, opt_arg2) {
  var x, y;
  if(arg1 instanceof goog.math.Coordinate) {
    x = arg1.x;
    y = arg1.y
  }else {
    x = arg1;
    y = opt_arg2
  }el.style.left = typeof x == "number" ? Math.round(x) + "px" : x;
  el.style.top = typeof y == "number" ? Math.round(y) + "px" : y
};
goog.style.getPosition = function(element) {
  return new goog.math.Coordinate(element.offsetLeft, element.offsetTop)
};
goog.style.getClientViewportElement = function(opt_node) {
  var doc;
  if(opt_node) {
    if(opt_node.nodeType == goog.dom.NodeType.DOCUMENT) {
      doc = opt_node
    }else {
      doc = goog.dom.getOwnerDocument(opt_node)
    }
  }else {
    doc = goog.dom.getDocument()
  }if(goog.userAgent.IE && doc.compatMode != "CSS1Compat") {
    return doc.body
  }return doc.documentElement
};
goog.style.getPageOffset = function(el) {
  var doc = goog.dom.getOwnerDocument(el), BUGGY_GECKO_BOX_OBJECT = goog.userAgent.GECKO && doc.getBoxObjectFor && goog.style.getStyle_(el, "position") == "absolute" && (el.style.top == "" || el.style.left == "");
  if(typeof goog.style.BUGGY_CAMINO_ == "undefined") {
    goog.style.BUGGY_CAMINO_ = goog.userAgent.CAMINO && !goog.userAgent.isVersion("1.8.0.11")
  }var pos = new goog.math.Coordinate(0, 0), viewportElement = goog.style.getClientViewportElement(doc);
  if(el == viewportElement) {
    return pos
  }var parent = null, box;
  if(el.getBoundingClientRect) {
    box = el.getBoundingClientRect();
    var scrollTop = viewportElement.scrollTop, scrollLeft = viewportElement.scrollLeft;
    pos.x = box.left + scrollLeft;
    pos.y = box.top + scrollTop
  }else if(doc.getBoxObjectFor && !BUGGY_GECKO_BOX_OBJECT && !goog.style.BUGGY_CAMINO_) {
    box = doc.getBoxObjectFor(el);
    var vpBox = doc.getBoxObjectFor(viewportElement);
    pos.x = box.screenX - vpBox.screenX;
    pos.y = box.screenY - vpBox.screenY
  }else {
    pos.x = el.offsetLeft;
    pos.y = el.offsetTop;
    parent = el.offsetParent;
    if(parent != el) {
      while(parent) {
        pos.x += parent.offsetLeft;
        pos.y += parent.offsetTop;
        parent = parent.offsetParent
      }
    }if(goog.userAgent.OPERA || goog.userAgent.SAFARI && goog.style.getStyle_(el, "position") == "absolute") {
      pos.y -= doc.body.offsetTop
    }parent = el.offsetParent;
    while(parent && parent != doc.body) {
      pos.x -= parent.scrollLeft;
      if(!goog.userAgent.OPERA || parent.tagName != "TR") {
        pos.y -= parent.scrollTop
      }parent = parent.offsetParent
    }
  }return pos
};
goog.style.getPageOffsetLeft = function(el) {
  return goog.style.getPageOffset(el).x
};
goog.style.getPageOffsetTop = function(el) {
  return goog.style.getPageOffset(el).y
};
goog.style.getRelativePosition = function(a, b) {
  var ap = goog.style.getClientPosition(a), bp = goog.style.getClientPosition(b);
  return new goog.math.Coordinate(ap.x - bp.x, ap.y - bp.y)
};
goog.style.getClientPosition = function(el) {
  var pos = new goog.math.Coordinate;
  if(el.nodeType == goog.dom.NodeType.ELEMENT) {
    if(el.getBoundingClientRect) {
      var box = el.getBoundingClientRect();
      pos.x = box.left;
      pos.y = box.top
    }else {
      var doc = goog.dom.getOwnerDocument(el), viewportElement = goog.style.getClientViewportElement(doc), pageCoord = goog.style.getPageOffset(el);
      pos.x = pageCoord.x - viewportElement.scrollLeft;
      pos.y = pageCoord.y - viewportElement.scrollTop
    }
  }else {
    pos.x = el.clientX;
    pos.y = el.clientY
  }return pos
};
goog.style.setPageOffset = function(el, x, opt_y) {
  var cur = goog.style.getPageOffset(el);
  if(x instanceof goog.math.Coordinate) {
    opt_y = x.y;
    x = x.x
  }var dx = x - cur.x, dy = opt_y - cur.y;
  goog.style.setPosition(el, el.offsetLeft + dx, el.offsetTop + dy)
};
goog.style.setSize = function(element, w, opt_h) {
  var h;
  if(w instanceof goog.math.Size) {
    h = w.height;
    w = w.width
  }else {
    h = opt_h
  }element.style.width = typeof w == "number" ? Math.round(w) + "px" : w;
  element.style.height = typeof h == "number" ? Math.round(h) + "px" : h
};
goog.style.getSize = function(element) {
  if(goog.style.getStyle_(element, "display") != "none") {
    return new goog.math.Size(element.offsetWidth, element.offsetHeight)
  }var style = element.style, originalVisibility = style.visibility, originalPosition = style.position;
  style.visibility = "hidden";
  style.position = "absolute";
  style.display = "";
  var originalWidth = element.offsetWidth, originalHeight = element.offsetHeight;
  style.display = "none";
  style.position = originalPosition;
  style.visibility = originalVisibility;
  return new goog.math.Size(originalWidth, originalHeight)
};
goog.style.getBounds = function(element) {
  var o = goog.style.getPageOffset(element), s = goog.style.getSize(element);
  return new goog.math.Rect(o.x, o.y, s.width, s.height)
};
goog.style.toCamelCase = function(selector) {
  return String(selector).replace(/\-([a-z])/g, function(all, match) {
    return match.toUpperCase()
  })
};
goog.style.toSelectorCase = function(selector) {
  return selector.replace(/([A-Z])/g, "-$1").toLowerCase()
};
goog.style.setOpacity = function(el, alpha) {
  var style = el.style;
  if("opacity" in style) {
    style.opacity = alpha
  }else if("MozOpacity" in style) {
    style.MozOpacity = alpha
  }else if("KhtmlOpacity" in style) {
    style.KhtmlOpacity = alpha
  }else if("filter" in style) {
    style.filter = "alpha(opacity=" + alpha * 100 + ")"
  }
};
goog.style.setTransparentBackgroundImage = function(el, src) {
  var style = el.style;
  if("filter" in style) {
    style.filter = 'progid:DXImageTransform.Microsoft.AlphaImageLoader(src="' + src + '", sizingMethod="crop")'
  }else {
    style.backgroundImage = "url(" + src + ")";
    style.backgroundPosition = "top left";
    style.backgroundRepeat = "no-repeat"
  }
};
goog.style.showElement = function(el, display) {
  el.style.display = display ? "" : "none"
};
goog.style.installStyles = function(stylesString, opt_element) {
  var dh = goog.dom.getDomHelper(opt_element), styleSheet = null;
  if(goog.userAgent.IE) {
    styleSheet = dh.getDocument().createStyleSheet()
  }else {
    var head = dh.$$("head")[0];
    if(!head) {
      var body = dh.$$("body")[0];
      head = dh.createDom("head");
      body.parentNode.insertBefore(head, body)
    }styleSheet = dh.createDom("style");
    dh.appendChild(head, styleSheet)
  }goog.style.setStyles(styleSheet, stylesString);
  return styleSheet
};
goog.style.setStyles = function(element, stylesString) {
  if(goog.userAgent.IE) {
    element.cssText = stylesString
  }else {
    var propToSet = goog.userAgent.SAFARI ? "innerText" : "innerHTML";
    element[propToSet] = stylesString
  }
};
goog.style.setPreWrap = function(el) {
  if(goog.userAgent.IE) {
    el.style.whiteSpace = "pre";
    el.style.wordWrap = "break-word"
  }else if(goog.userAgent.GECKO) {
    el.style.whiteSpace = "-moz-pre-wrap"
  }else if(goog.userAgent.OPERA) {
    el.style.whiteSpace = "-o-pre-wrap"
  }else {
    el.style.whiteSpace = "pre-wrap"
  }
};
goog.style.setInlineBlock = function(el) {
  el.style.position = "relative";
  if(goog.userAgent.IE) {
    el.style.zoom = "1";
    el.style.display = "inline"
  }else if(goog.userAgent.GECKO) {
    el.style.display = goog.userAgent.isVersion("1.9a") ? "inline-block" : "-moz-inline-box"
  }else {
    el.style.display = "inline-block"
  }
};
goog.style.isRightToLeft = function(el) {
  return"rtl" == goog.style.getStyle_(el, "direction")
};
goog.style.setUnselectable = function(el, unselectable) {
  if(goog.userAgent.IE) {
    var descendents = el.getElementsByTagName("*"), attrValue = unselectable ? "on" : "off";
    for(var i = 0, n = descendents.length;i < n;i++) {
      descendents[i].setAttribute("unselectable", attrValue)
    }el.setAttribute("unselectable", attrValue)
  }
};
goog.style.getBorderBoxSize = function(element) {
  return new goog.math.Size(element.offsetWidth, element.offsetHeight)
};
goog.style.setBorderBoxSize = function(element, size) {
  if(goog.userAgent.IE) {
    var doc = goog.dom.getOwnerDocument(element), style = element.style;
    if(doc.compatMode == "CSS1Compat") {
      var paddingBox = goog.style.getPaddingBox(element), borderBox = goog.style.getBorderBox(element);
      style.pixelWidth = size.width - borderBox.left - paddingBox.left - paddingBox.right - borderBox.right;
      style.pixelHeight = size.height - borderBox.top - paddingBox.top - paddingBox.bottom - borderBox.bottom
    }else {
      style.pixelWidth = size.width;
      style.pixelHeight = size.height
    }
  }else {
    goog.style.setBoxSizingSize_(element, size, "border-box")
  }
};
goog.style.getContentBoxSize = function(element) {
  var doc = goog.dom.getOwnerDocument(element);
  if(goog.userAgent.IE && doc.compatMode == "CSS1Compat") {
    var currentStyle = element.currentStyle, width = goog.style.getIePixelValue_(element, currentStyle.width, "width", "pixelWidth"), height = goog.style.getIePixelValue_(element, currentStyle.height, "height", "pixelHeight");
    return new goog.math.Size(width, height)
  }else {
    var borderBoxSize = goog.style.getBorderBoxSize(element), paddingBox = goog.style.getPaddingBox(element), borderBox = goog.style.getBorderBox(element);
    return new goog.math.Size(borderBoxSize.width - borderBox.left - paddingBox.left - paddingBox.right - borderBox.right, borderBoxSize.height - borderBox.top - paddingBox.top - paddingBox.bottom - borderBox.bottom)
  }
};
goog.style.setContentBoxSize = function(element, size) {
  if(goog.userAgent.IE) {
    var doc = goog.dom.getOwnerDocument(element), style = element.style;
    if(doc.compatMode == "CSS1Compat") {
      style.pixelWidth = size.width;
      style.pixelHeight = size.height
    }else {
      var paddingBox = goog.style.getPaddingBox(element), borderBox = goog.style.getBorderBox(element);
      style.pixelWidth = size.width + borderBox.left + paddingBox.left + paddingBox.right + borderBox.right;
      style.pixelHeight = size.height + borderBox.top + paddingBox.top + paddingBox.bottom + borderBox.bottom
    }
  }else {
    goog.style.setBoxSizingSize_(element, size, "content-box")
  }
};
goog.style.setBoxSizingSize_ = function(element, size, boxSizing) {
  var style = element.style;
  if(goog.userAgent.GECKO) {
    style.MozBoxSizing = boxSizing
  }else if(goog.userAgent.SAFARI) {
    style.WebkitBoxSizing = boxSizing
  }else if(goog.userAgent.OPERA) {
    var value = element.getAttribute("style") || "";
    value = value.replace(/box-sizing:[^;]+/g, "") + ";box-sizing:" + boxSizing;
    element.setAttribute("style", value)
  }else {
    style.boxSizing = boxSizing
  }style.width = size.width + "px";
  style.height = size.height + "px"
};
goog.style.getIePixelValue_ = function(element, value, name, pixelName) {
  if(/^\d+px?$/.test(value)) {
    return parseInt(value, 10)
  }else {
    var oldStyleValue = element.style[name], oldRuntimeValue = element.runtimeStyle[name];
    element.runtimeStyle[name] = element.currentStyle[name];
    element.style[name] = value;
    var pixelValue = element.style[pixelName];
    element.style[name] = oldStyleValue;
    element.runtimeStyle[name] = oldRuntimeValue;
    return pixelValue
  }
};
goog.style.getIePixelPadding_ = function(element, propName) {
  return goog.style.getIePixelValue_(element, goog.style.getCascadedStyle(element, propName), "left", "pixelLeft")
};
goog.style.getPaddingBox = function(element) {
  if(goog.userAgent.IE) {
    var left = goog.style.getIePixelPadding_(element, "paddingLeft"), right = goog.style.getIePixelPadding_(element, "paddingRight"), top = goog.style.getIePixelPadding_(element, "paddingTop"), bottom = goog.style.getIePixelPadding_(element, "paddingBottom");
    return new goog.math.Box(top, right, bottom, left)
  }else {
    var left = goog.style.getComputedStyle(element, "paddingLeft"), right = goog.style.getComputedStyle(element, "paddingRight"), top = goog.style.getComputedStyle(element, "paddingTop"), bottom = goog.style.getComputedStyle(element, "paddingBottom");
    return new goog.math.Box(parseFloat(top), parseFloat(right), parseFloat(bottom), parseFloat(left))
  }
};
goog.style.ieBorderWidthKeywords_ = {thin:2, medium:4, thick:6};
goog.style.getIePixelBorder_ = function(element, prop) {
  if(goog.style.getCascadedStyle(element, prop + "Style") == "none") {
    return 0
  }var width = goog.style.getCascadedStyle(element, prop + "Width");
  if(width in goog.style.ieBorderWidthKeywords_) {
    return goog.style.ieBorderWidthKeywords_[width]
  }return goog.style.getIePixelValue_(element, width, "left", "pixelLeft")
};
goog.style.getBorderBox = function(element) {
  if(goog.userAgent.IE) {
    var left = goog.style.getIePixelBorder_(element, "borderLeft"), right = goog.style.getIePixelBorder_(element, "borderRight"), top = goog.style.getIePixelBorder_(element, "borderTop"), bottom = goog.style.getIePixelBorder_(element, "borderBottom");
    return new goog.math.Box(top, right, bottom, left)
  }else {
    var left = goog.style.getComputedStyle(element, "borderLeftWidth"), right = goog.style.getComputedStyle(element, "borderRightWidth"), top = goog.style.getComputedStyle(element, "borderTopWidth"), bottom = goog.style.getComputedStyle(element, "borderBottomWidth");
    return new goog.math.Box(parseFloat(top), parseFloat(right), parseFloat(bottom), parseFloat(left))
  }
};
goog.style.getFontFamily = function(el) {
  var doc = goog.dom.getOwnerDocument(el), font = "";
  if(doc.createTextRange) {
    var range = doc.body.createTextRange();
    range.moveToElementText(el);
    font = range.queryCommandValue("FontName")
  }if(!font) {
    font = goog.style.getStyle_(el, "fontFamily");
    if(goog.userAgent.OPERA && goog.userAgent.LINUX) {
      font = font.replace(/ \[[^\]]*\]/, "")
    }
  }var fontsArray = font.split(",");
  if(fontsArray.length > 1)font = fontsArray[0];
  return goog.string.stripQuotes(font, '"')
};
goog.style.getLengthUnits = function(value) {
  var units = value.match(/[^\d]+$/);
  return units && units[0] || null
};
goog.style.ABSOLUTE_CSS_LENGTH_UNITS_ = {cm:1, "in":1, mm:1, pc:1, pt:1};
goog.style.CONVERTIBLE_RELATIVE_CSS_UNITS_ = {em:1, ex:1};
goog.style.getFontSize = function(el) {
  var fontSize = goog.style.getStyle_(el, "fontSize"), sizeUnits = goog.style.getLengthUnits(fontSize);
  if(fontSize && "px" == sizeUnits) {
    return parseInt(fontSize, 10)
  }if(goog.userAgent.IE) {
    if(sizeUnits in goog.style.ABSOLUTE_CSS_LENGTH_UNITS_) {
      return goog.style.getIePixelValue_(el, fontSize, "left", "pixelLeft")
    }else if(el.parentNode && sizeUnits in goog.style.CONVERTIBLE_RELATIVE_CSS_UNITS_) {
      return goog.style.getIePixelValue_(el.parentNode, fontSize, "left", "pixelLeft")
    }
  }var sizeElement = goog.dom.createDom("span", {style:"visibility:hidden;position:absolute;padding:0;margin:0;border:0;height:1em;"});
  goog.dom.appendChild(el, sizeElement);
  fontSize = sizeElement.offsetHeight;
  goog.dom.removeNode(sizeElement);
  return fontSize
};//var opensocial = {};
opensocial = function() {
};
opensocial.requestInstallApp = function(appUrl, opt_onSuccess, opt_onFailure) {
  opensocial.Container.get().requestInstallApp(appUrl, opt_onSuccess, opt_onFailure)
};
opensocial.requestUninstallApp = function(appUrl, opt_onSuccess, opt_onFailure) {
  opensocial.Container.get().requestUninstallApp(appUrl, opt_onSuccess, opt_onFailure)
};
opensocial.requestMakePersonAFriend = function(person, opt_onSuccess, opt_onFailure) {
  opensocial.Container.get().requestMakePersonAFriend(person, opt_onSuccess, opt_onFailure)
};
opensocial.requestMakePersonNotAFriend = function(person, opt_onSuccess, opt_onFailure) {
  opensocial.Container.get().requestMakePersonNotAFriend(person, opt_onSuccess, opt_onFailure)
};
opensocial.requestCreateActivity = function(activity, priority, opt_callback) {
  opensocial.Container.get().requestCreateActivity(activity, priority, opt_callback)
};
opensocial.CreateActivityPriority = {HIGH:"HIGH", LOW:"LOW"};
opensocial.newDataRequest = function() {
  return new opensocial.DataRequest
};opensocial.Person = function() {
};
opensocial.Person.Field = {ID:"id", NAME:"name", EMAIL:"email"};
opensocial.Person.prototype.getId = function() {
};
opensocial.Person.prototype.getDisplayName = function() {
};
opensocial.Person.prototype.getField = function(key) {
};
opensocial.Person.prototype.isViewer = function() {
};
opensocial.Person.prototype.isOwner = function() {
};opensocial.Collection = function() {
};
opensocial.Collection.prototype.get = function(key) {
};
opensocial.Collection.prototype.size = function() {
};
opensocial.Collection.prototype.each = function(fn) {
};
opensocial.Collection.prototype.sort = function(fn) {
};
opensocial.Collection.prototype.getValues = function() {
};
opensocial.Collection.prototype.getKeys = function() {
};
opensocial.Collection.prototype.getTotalSize = function() {
};
opensocial.Collection.prototype.getOffset = function() {
};opensocial.Container = function() {
};
opensocial.Container.init = function(opt_baseUrl) {
  opensocial.Container.container_.init(opt_baseUrl)
};
opensocial.Container.container_ = null;
opensocial.Container.setContainer = function(container) {
  opensocial.Container.container_ = container
};
opensocial.Container.get = function() {
  return opensocial.Container.container_
};
opensocial.Container.prototype.requestInstallApp = function(appUrl, opt_onSuccess, opt_onFailure) {
};
opensocial.Container.prototype.requestUninstallApp = function(appUrl, opt_onSuccess, opt_onFailure) {
};
opensocial.Container.prototype.requestMakePersonAFriend = function(person, opt_onSuccess, opt_onFailure) {
};
opensocial.Container.prototype.requestMakePersonNotAFriend = function(person, opt_onSuccess, opt_onFailure) {
};
opensocial.Container.prototype.requestCreateActivity = function(activity, priority, opt_callback) {
};
opensocial.Container.prototype.requestData = function(dataRequest, callback) {
};
opensocial.Container.prototype.getCapabilities = function() {
};opensocial.DataRequest = function() {
};
opensocial.DataRequest.prototype.requestObjects_ = [];
opensocial.DataRequest.prototype.getRequestObjects = function() {
  return this.requestObjects_
};
opensocial.DataRequest.prototype.add = function(request, opt_key) {
  return this.requestObjects_.push({key:opt_key, request:request})
};
opensocial.DataRequest.prototype.send = function(opt_callback) {
  var callback = opt_callback || function() {
  };
  opensocial.Container.get().requestData(this, callback)
};
opensocial.DataRequest.BaseDataRequest = function(type, parameters) {
  this.type = type;
  this.parameters = parameters
};
opensocial.DataRequest.RequestType = {FETCH_PERSON:"FETCH_PERSON", FETCH_PEOPLE:"FETCH_PEOPLE", FETCH_GLOBAL_DATA:"FETCH_GLOBAL_DATA", FETCH_INSTANCE_DATA:"FETCH_INSTANCE_DATA", UPDATE_INSTANCE_DATA:"UPDATE_INSTANCE_DATA", FETCH_PERSON_DATA:"FETCH_PERSON_DATA", UPDATE_PERSON_DATA:"UPDATE_PERSON_DATA", FETCH_ACTIVITIES:"FETCH_ACTIVITIES"};
opensocial.DataRequest.PersonId = {OWNER:"OWNER", VIEWER:"VIEWER"};
opensocial.DataRequest.Group = {OWNER_FRIENDS:"OWNER_FRIENDS", VIEWER_FRIENDS:"VIEWER_FRIENDS"};
opensocial.DataRequest.ProfileDetailType = {BASIC:"basic", MATCHING:"matching", CONTACT:"contact", PERSONAL:"personal", FULL:"full"};
opensocial.DataRequest.SortOrder = {TOP_FRIENDS:"topFriends", NAME:"name"};
opensocial.DataRequest.FilterType = {ALL:"all", HAS_APP:"hasApp"};
opensocial.DataRequest.PeopleRequestFields = {PROFILE_DETAILS:"profileDetail", SORT_ORDER:"sortOrder", FILTER:"filter", FIRST:"first", MAX:"max"};
opensocial.DataRequest.prototype.newFetchPersonRequest = function(id, opt_params) {
  opt_params = opt_params || {};
  var fields = opensocial.DataRequest.PeopleRequestFields, requestParams = {id:id, profileDetail:opt_params[fields.PROFILE_DETAILS] || opensocial.DataRequest.ProfileDetailType.BASIC};
  return new opensocial.DataRequest.BaseDataRequest(opensocial.DataRequest.RequestType.FETCH_PERSON, requestParams)
};
opensocial.DataRequest.prototype.newFetchPeopleRequest = function(idSpec, opt_params) {
  opt_params = opt_params || {};
  var fields = opensocial.DataRequest.PeopleRequestFields, requestParams = {idSpec:idSpec, profileDetail:opt_params[fields.PROFILE_DETAILS] || opensocial.DataRequest.ProfileDetailType.BASIC, sortOrder:opt_params[fields.SORT_ORDER] || opensocial.DataRequest.SortOrder.TOP_FRIENDS, filter:opt_params[fields.FILTER] || opensocial.DataRequest.FilterType.ALL, first:opt_params[fields.FIRST] || 0, max:opt_params[fields.MAX]};
  return new opensocial.DataRequest.BaseDataRequest(opensocial.DataRequest.RequestType.FETCH_PEOPLE, requestParams)
};
opensocial.DataRequest.prototype.newFetchGlobalAppDataRequest = function(keys) {
  var requestParams = {keys:keys};
  return new opensocial.DataRequest.BaseDataRequest(opensocial.DataRequest.RequestType.FETCH_GLOBAL_DATA, requestParams)
};
opensocial.DataRequest.prototype.newFetchInstanceAppDataRequest = function(keys) {
  var requestParams = {keys:keys};
  return new opensocial.DataRequest.BaseDataRequest(opensocial.DataRequest.RequestType.FETCH_INSTANCE_DATA, requestParams)
};
opensocial.DataRequest.prototype.newUpdateInstanceAppDataRequest = function(key, value) {
  var requestParams = {key:key, value:value};
  return new opensocial.DataRequest.BaseDataRequest(opensocial.DataRequest.RequestType.UPDATE_INSTANCE_DATA, requestParams)
};
opensocial.DataRequest.prototype.newFetchPersonAppDataRequest = function(idSpec, keys) {
  var requestParams = {idSpec:idSpec, keys:keys};
  return new opensocial.DataRequest.BaseDataRequest(opensocial.DataRequest.RequestType.FETCH_PERSON_DATA, requestParams)
};
opensocial.DataRequest.prototype.newUpdatePersonAppDataRequest = function(id, key, value) {
  var requestParams = {id:id, key:key, value:value};
  return new opensocial.DataRequest.BaseDataRequest(opensocial.DataRequest.RequestType.UPDATE_PERSON_DATA, requestParams)
};
opensocial.DataRequest.ActivityRequestFields = {APP_ID:"appId", FOLDER_ID:"folderId"};
opensocial.DataRequest.prototype.newFetchActivitiesRequest = function(idSpec, opt_params) {
  opt_params = opt_params || {};
  var fields = opensocial.DataRequest.ActivityRequestFields, requestParams = {idSpec:idSpec, appId:opt_params[fields.APP_ID], folderId:opt_params[fields.FOLDER_ID]};
  return new opensocial.DataRequest.BaseDataRequest(opensocial.DataRequest.RequestType.FETCH_ACTIVITIES, requestParams)
};opensocial.DataResponse = function(responseItems, opt_globalError) {
  this.responseItems_ = responseItems;
  this.globalError_ = opt_globalError
};
opensocial.DataResponse.prototype.hadError = function() {
  return!(!this.globalError_)
};
opensocial.DataResponse.prototype.get = function(key) {
  return this.responseItems_[key]
};opensocial.ResponseItem = function(originalDataRequest, data, opt_error) {
  this.originalDataRequest_ = originalDataRequest;
  this.data_ = data;
  this.error_ = opt_error
};
opensocial.ResponseItem.prototype.hadError = function() {
  return!(!this.error_)
};
opensocial.ResponseItem.prototype.getError = function() {
  return this.error_
};
opensocial.ResponseItem.prototype.getOriginalDataRequest = function() {
  return this.originalDataRequest_
};
opensocial.ResponseItem.prototype.getData = function() {
  return this.data_
};opensocial.Activity = function(stream, title, opt_params) {
};
opensocial.Activity.Field = {ID:"id", EXTERNAL_ID:"externalId", STREAM:"stream", TITLE:"title", SUMMARY:"summary", BODY:"body", URL:"url", MEDIA_ITEMS:"mediaItems", POSTED_TIME:"postedTime", CUSTOM_VALUES:"customValues"};
opensocial.Activity.prototype.getId = function() {
};
opensocial.Activity.prototype.getField = function(key) {
};
opensocial.Activity.MediaItem = function(mimeType, url, opt_params) {
};
opensocial.Activity.MediaItem.Type = {IMAGE:"image", VIDEO:"video", AUDIO:"audio"};
opensocial.Activity.MediaItem.Field = {TYPE:"type", MIME_TYPE:"mimeType", URL:"url"};
opensocial.Activity.MediaItem.prototype.getField = function(key) {
};
opensocial.Activity.Template = function() {
};
opensocial.Activity.Template.prototype.appendString = function(string) {
};
opensocial.Activity.Template.prototype.appendActivityField = function(field) {
};
opensocial.Activity.Template.prototype.appendStreamField = function(field) {
};
opensocial.Activity.Template.prototype.appendUserField = function(id, field) {
};
opensocial.Activity.Template.prototype.appendCustomField = function(key) {
};opensocial.Stream = function(folder, title, opt_params) {
};
opensocial.Stream.Field = {USER_ID:"userId", APP_ID:"appId", FOLDER:"folder", TITLE:"title", URL:"url", SOURCE_URL:"sourceUrl", FAVICON_URL:"faviconUrl"};
opensocial.Stream.prototype.getId = function() {
};
opensocial.Stream.prototype.getField = function(key) {
};opensocial.data = {};
opensocial.data.Request = function(requestParams, path, usePost, onSuccess, onFailure) {
  this.requestParams_ = requestParams;
  this.path_ = path;
  this.onSuccess_ = onSuccess;
  this.onFailure_ = onFailure;
  this.usePost_ = usePost
};
opensocial.data.Request.prototype.createUri_ = function(callbackName) {
  var peopleUri = new goog.Uri(opensocial.data.getBaseUrl() + this.path_);
  this.setParams_(peopleUri.getQueryData());
  peopleUri.setParameterValue("out", "xjs");
  peopleUri.setParameterValue("callback", callbackName);
  return peopleUri
};
opensocial.data.Request.prototype.setParams_ = function(queryData) {
  for(var name in this.requestParams_) {
    queryData.set(name, this.requestParams_[name])
  }var hash = document.location.hash || "#", hashData = new goog.Uri.QueryData(hash.substring(1)), st = hashData.get("st");
  if(st) {
    queryData.set("st", st)
  }
};
opensocial.data.Request.prototype.send = function() {
  var queryData = new goog.Uri.QueryData;
  this.setParams_(queryData);
  queryData.set("out", "js");
  var xhr = new goog.net.XhrLite, peopleUri = new goog.Uri(opensocial.data.getBaseUrl() + this.path_);
  goog.events.listen(xhr, goog.net.EventType.COMPLETE, this.xhrReceived_, false, this);
  xhr.send(peopleUri, "POST", queryData.toString())
};
opensocial.data.Request.prototype.parseProtectedJson_ = function(json) {
  var start = json.indexOf("&&&START&&&") + "&&&START&&&".length, end = json.lastIndexOf("&&&END&&&");
  return goog.json.unsafeParse(json.substring(start, end))
};
opensocial.data.Request.prototype.xhrReceived_ = function(evt) {
  var xhr = evt.target, result = null;
  if(xhr.isSuccess()) {
    result = this.parseProtectedJson_(xhr.getResponseText())
  }if(result && result.Success) {
    this.dataReceived_(result)
  }
};
opensocial.data.Request.prototype.send_EVIL = function() {
  var callbackName = opensocial.data.getUniqueCallbackName_();
  window[callbackName] = this.dataReceived_.bind(this);
  var uri = this.createUri_(callbackName), scriptEl = document.createElement("script");
  scriptEl.src = uri;
  goog.dom.$$("head")[0].appendChild(scriptEl)
};
opensocial.data.Request.prototype.dataReceived_ = function(json) {
  if(this.onSuccess_) {
    this.onSuccess_(json)
  }
};
opensocial.data.GetFriendsRequest = function(uid, params, opt_onSuccess, opt_onFailure) {
  var requestParams = {max:1000, uid:uid, app:params.app};
  if(params.max) {
    requestParams.max = params.max
  }opensocial.data.Request.call(this, requestParams, "/api/friends", false, opt_onSuccess, opt_onFailure)
};
opensocial.data.GetFriendsRequest.inherits(opensocial.data.Request);
opensocial.data.GetContactRequest = function(params, opt_onSuccess, opt_onFailure) {
  var requestParams = {thumb:true, groups:true, enums:true, psort:"Affinity", max:1000, show:"ALL", thumb:true};
  if(params.max) {
    requestParams.max = params.max
  }opensocial.data.Request.call(this, requestParams, "/data/contacts", false, opt_onSuccess, opt_onFailure)
};
opensocial.data.GetContactRequest.inherits(opensocial.data.Request);
opensocial.data.UpdateContactRequest = function(contact, opt_onSuccess, opt_onFailure) {
  var requestParams = {id:contact.getProp("Id"), action:"SET", tok:opensocial.tok_, token:opensocial.tok_};
  (new opensocial.data.ContactToParamConverter(contact, requestParams)).convert();
  opensocial.data.Request.call(this, requestParams, "/update/contact", true, opt_onSuccess, opt_onFailure)
};
opensocial.data.UpdateContactRequest.inherits(opensocial.data.Request);
opensocial.data.ContactToParamConverter = function(contact, params, opt_pathFilter) {
  this.contact_ = contact;
  this.params_ = params;
  this.pathFilter_ = opt_pathFilter
};
opensocial.data.ContactToParamConverter.prototype.convert = function() {
  this.convertObj_(this.contact_, this.params_, "", this.pathFilter_)
};
opensocial.data.ContactToParamConverter.prototype.convertObj_ = function(obj, params, path, opt_pathFilter) {
  var me = this, rootPath = path.length == 0 ? "" : path + ".";
  if(opt_pathFilter && !opt_pathFilter(path)) {
  }else if(obj == null) {
    params[path] = null
  }else if(obj.isCollection && obj.isCollection()) {
    var i = 0;
    obj.each(function(value) {
      me.convertObj_(value, params, rootPath + i++, opt_pathFilter)
    })
  }else if(obj.eachProp) {
    obj.eachProp(function(key, value) {
      me.convertObj_(value, params, rootPath + key, opt_pathFilter)
    })
  }else {
    params[path] = obj.toString()
  }return params
};
opensocial.data.GetMyProfileRequest = function(opt_onSuccess, opt_onFailure, opt_bogusAppName) {
  var requestParams = {app:opt_bogusAppName};
  opensocial.data.Request.call(this, requestParams, "/api/profile", false, opt_onSuccess, opt_onFailure)
};
opensocial.data.GetMyProfileRequest.inherits(opensocial.data.Request);
opensocial.data.GetProfileRequest = function(uid, opt_onSuccess, opt_onFailure, opt_bogusAppName) {
  var requestParams = {app:opt_bogusAppName, uid:uid};
  opensocial.data.Request.call(this, requestParams, "/api/profile", false, opt_onSuccess, opt_onFailure)
};
opensocial.data.GetProfileRequest.inherits(opensocial.data.Request);
opensocial.data.UpdateMyProfileRequest = function(opt_onSuccess, opt_onFailure) {
  var requestParams = {action:"SET", tok:opensocial.tok_, token:opensocial.tok_}, noMoreFields = function(key) {
    return key.indexOf("More") != 0
  };
  (new opensocial.data.ContactToParamConverter(opensocial.profile_, requestParams, noMoreFields)).convert();
  opensocial.data.Request.call(this, requestParams, "/update/profile", true, opt_onSuccess, opt_onFailure)
};
opensocial.data.UpdateMyProfileRequest.inherits(opensocial.data.Request);
opensocial.data.callbackCounter_ = 0;
opensocial.data.setBaseUrl = function(url) {
  opensocial.data.baseUrl_ = url
};
opensocial.data.getBaseUrl = function() {
  return opensocial.data.baseUrl_
};
opensocial.data.baseUrl_ = null;
opensocial.data.getUniqueCallbackName_ = function() {
  return"__p_d_callback" + opensocial.data.callbackCounter_++
};
opensocial.data.UpdateAppDataRequest = function(person, app, key, value, opt_onSuccess, opt_onFailure) {
  var requestParams = {who:person.getId(), app:app, key:key, value:value, tok:opensocial.tok_, token:opensocial.tok_};
  opensocial.data.Request.call(this, requestParams, "/api/update_appdata", true, opt_onSuccess, opt_onFailure)
};
opensocial.data.UpdateAppDataRequest.inherits(opensocial.data.Request);
opensocial.data.GetActivityStreamRequest = function(opt_appId, opt_groupId, opt_onSuccess, opt_onFailure) {
  var requestParams = {};
  if(opt_appId) {
    requestParams.app = opt_appId
  }if(opt_groupId) {
    requestParams.group = opt_groupId
  }opensocial.data.Request.call(this, requestParams, "/data/activities", false, opt_onSuccess, opt_onFailure)
};
opensocial.data.GetActivityStreamRequest.inherits(opensocial.data.Request);
opensocial.data.UpdateActivityRequest = function(appId, title, body, url, summary, folder, opt_onSuccess, opt_onFailure) {
  var requestParams = {title:title, body:body, url:url, app:appId, summary:summary, folder:folder};
  opensocial.data.Request.call(this, requestParams, "/update/activities", true, opt_onSuccess, opt_onFailure)
};
opensocial.data.UpdateActivityRequest.inherits(opensocial.data.Request);opensocial.IGoogleContainer = function() {
  opensocial.Container.call(this)
};
opensocial.IGoogleContainer.inherits(opensocial.Container);
opensocial.IGoogleContainer.prototype.init = function(opt_baseUrl) {
  var baseUrl;
  if(opt_baseUrl) {
    baseUrl = opt_baseUrl
  }else {
    var scripts = document.getElementsByTagName("script");
    for(var i = 0;i < scripts.length;i++) {
      var src = scripts[i].src;
      if(src && src.indexOf("/ui/People/") > -1) {
        var index = src.indexOf("/ui/People");
        baseUrl = src.substring(0, index);
        break
      }
    }
  }opensocial.data.setBaseUrl(baseUrl)
};
opensocial.IGoogleContainer.prototype.requestInstallApp = function(appUrl, opt_onSuccess, opt_onFailure) {
};
opensocial.IGoogleContainer.prototype.requestUninstallApp = function(appUrl, opt_onSuccess, opt_onFailure) {
};
opensocial.IGoogleContainer.prototype.requestMakePersonAFriend = function(id, opt_onSuccess, opt_onFailure) {
};
opensocial.IGoogleContainer.prototype.requestMakePersonNotAFriend = function(id, opt_onSuccess, opt_onFailure) {
};
opensocial.IGoogleContainer.DataResponseCreator = function(numberOfResponses, callback, dataRequest) {
  this.numberOfExpectedResponses = numberOfResponses;
  this.numberOfActualResponses = 0;
  this.callback = callback;
  this.dataResponseValues = {}
};
opensocial.IGoogleContainer.DataResponseCreator.prototype.addResponseValue = function(key, value, dataRequest) {
  this.numberOfActualResponses++;
  this.dataResponseValues[key] = new opensocial.ResponseItem(dataRequest, value);
  this.sendDataResponseIfFinished_()
};
opensocial.IGoogleContainer.DataResponseCreator.prototype.sendDataResponseIfFinished_ = function() {
  if(this.numberOfActualResponses == this.numberOfExpectedResponses) {
    var response = new opensocial.DataResponse(this.dataResponseValues);
    this.callback(response)
  }
};
opensocial.IGoogleContainer.DataResponseCreator.prototype.getSuccessFunction = function(key, dataRequest) {
  var me = this;
  return function(responseObject) {
    me.addResponseValue(key, responseObject, dataRequest)
  }
};
opensocial.IGoogleContainer.prototype.requestData = function(dataRequest, callback) {
  var requestObjects = dataRequest.getRequestObjects(), responseCreator = new opensocial.IGoogleContainer.DataResponseCreator(requestObjects.length, callback, dataRequest), bogusAppName = dataRequest.bogusAppName;
  for(var i = 0;i < requestObjects.length;i++) {
    var request = requestObjects[i].request, requestName = requestObjects[i].key, successResponse = responseCreator.getSuccessFunction(requestName, request);
    switch(request.type) {
      case opensocial.DataRequest.RequestType.FETCH_PEOPLE:
        this.getFriends(request.parameters.idSpec, {app:bogusAppName}, successResponse);
        break;
      case opensocial.DataRequest.RequestType.FETCH_PERSON:
        this.getPerson(request.parameters.id, successResponse, null, bogusAppName);
        break;
      case opensocial.DataRequest.RequestType.FETCH_ACTIVITIES:
        this.getActivities(null, bogusAppName, request.parameters.groupId, successResponse);
        break;
      case opensocial.DataRequest.RequestType.FETCH_PERSON_DATA:
        this.getPerson(request.parameters.idSpec, function(person) {
          successResponse(person.getProp("More"))
        }, null, bogusAppName);
        break;
      case opensocial.DataRequest.RequestType.UPDATE_PERSON_DATA:
        this.updateAppData(request.parameters.id, bogusAppName, request.parameters.key, request.parameters.value, successResponse);
        break
    }
  }
};
opensocial.IGoogleContainer.prototype.updateAppData = function(person, app, key, value, opt_onSuccess, opt_onFailure) {
  (new opensocial.data.UpdateAppDataRequest(person, app, key, value, opt_onSuccess, opt_onFailure)).send()
};
opensocial.IGoogleContainer.prototype.getPerson = function(person, opt_onSuccess, opt_onFailure, opt_bogusAppName) {
  var uid;
  if(person == opensocial.DataRequest.PersonId.VIEWER) {
    uid = "viewer"
  }else if(person == opensocial.DataRequest.PersonId.OWNER) {
    uid = "owner"
  }else {
    throw new Error("not supported yet");
  }(new opensocial.data.GetProfileRequest(uid, function(responseJson) {
    var data = s2.data.from(responseJson), person = new opensocial.JsonPerson(data.evaluate("Body.Profile"), true);
    opensocial.IGoogleContainer.fixPhoto_(person);
    opt_onSuccess(person)
  }, opt_onFailure, opt_bogusAppName)).send()
};
opensocial.IGoogleContainer.fixPhoto_ = function(contact) {
  if(contact.getProp("Photo")) {
    contact.putProp("Photo", opensocial.data.getBaseUrl() + "/photos/private/" + contact.getProp("Photo"))
  }
};
opensocial.IGoogleContainer.prototype.getContacts = function(person, params, opt_onSuccess, opt_onFailure) {
  if(person == opensocial.Container.PersonId.VIEWER) {
    (new opensocial.data.GetContactRequest(params, function(responseJson) {
      var data = s2.data.from(responseJson), contacts = new s2.data.Repository("Friends", "Friend"), contactsJson = data.evaluate("Body.Contacts");
      contactsJson.each(function(contactObj) {
        var contact = new opensocial.JsonPerson(contactObj);
        contacts.putAt(contact.getProp("Id"), contact)
      });
      contacts.each(function(contact) {
        opensocial.IGoogleContainer.fixPhoto_(contact)
      });
      opensocial.tok_ = data.evaluate("Body.AuthToken.Value").getValue();
      opt_onSuccess(contacts)
    }, opt_onFailure)).send()
  }else {
    throw new Error("not supported yet");
  }
};
opensocial.IGoogleContainer.prototype.getFriends = function(person, params, opt_onSuccess, opt_onFailure) {
  var uid;
  if(person == opensocial.DataRequest.Group.VIEWER_FRIENDS) {
    uid = "viewer"
  }else if(person == opensocial.DataRequest.Group.OWNER_FRIENDS) {
    uid = "owner"
  }else {
    throw new Error("not supported yet");
  }(new opensocial.data.GetFriendsRequest(uid, params, function(responseJson) {
    var data = s2.data.from(responseJson), friends = new s2.data.Repository("Friends_" + uid, "Friend"), friendsJson = data.evaluate("Body.Friends");
    friendsJson.each(function(friendObj) {
      var friend = new opensocial.JsonPerson(friendObj);
      friends.putAt(friend.getProp("Id"), friend)
    });
    friends.each(function(friend) {
      opensocial.IGoogleContainer.fixPhoto_(friend)
    });
    opensocial.tok_ = data.evaluate("Body.AuthToken.Value").getValue();
    opt_onSuccess(friends)
  }, opt_onFailure)).send()
};
opensocial.IGoogleContainer.prototype.updatePerson = function(person, opt_onSuccess, opt_onFailure) {
};
opensocial.IGoogleContainer.prototype.updateContact = function(contact, opt_onSuccess, opt_onFailure) {
  (new opensocial.data.UpdateContactRequest(contact, opt_onSuccess, opt_onFailure)).send()
};
opensocial.IGoogleContainer.prototype.getActivities = function(person, opt_appId, opt_groupId, opt_onSuccess, opt_onFailure) {
  (new opensocial.data.GetActivityStreamRequest(opt_appId, opt_groupId, function(response) {
    opt_onSuccess(opensocial.IGoogleContainer.translateFromJsonToActivityObjs_(response))
  }, opt_onFailure)).send()
};
opensocial.IGoogleContainer.translateFromJsonToActivityObjs_ = function(response) {
  var activities = [];
  if(response && response.Body.Activities) {
    var activityResponse = response.Body.Activities;
    for(var i = 0;i < activityResponse.length;i++) {
      var activity = activityResponse[i];
      activities.push(new opensocial.Person.Activity(activity.ApplicationId, activity.Body, activity.Title, activity.Url, activity.Summary, activity.FolderId))
    }
  }return activities
};
opensocial.IGoogleContainer.prototype.requestCreateActivity = function(activity, priority, opt_callback) {
};opensocial.JsonPerson = function(obj, opt_isMe) {
  obj = obj.obj_ || obj;
  s2.data.ObjectWrapper.call(this, obj);
  this.isMe_ = !(!opt_isMe)
};
opensocial.JsonPerson.inherits(s2.data.ObjectWrapper);
opensocial.JsonPerson.prototype.getId = function() {
  return this.getProp("Id")
};
opensocial.JsonPerson.prototype.getDisplayName = function() {
  if(this.getField("Name")) {
    return this.getField("Name")
  }else {
    var email = this.getField("Emails[0].Address");
    if(email && email.indexOf("@") > 0) {
      return email.substring(0, email.indexOf("@"))
    }
  }return""
};
opensocial.JsonPerson.prototype.getField = function(key) {
  return this.getProp(key)
};
opensocial.JsonPerson.prototype.isViewer = function() {
  return this.isMe_
};
opensocial.JsonPerson.prototype.isOwner = function() {
  return false
};


if(window.magic_orkut) {
  opensocial.Container.setContainer(new opensocial.OrkutContainer)
}else {
  opensocial.Container.setContainer(new opensocial.IGoogleContainer)
};opensocial.OrkutContainer = function() {
  opensocial.Container.call(this)
};
opensocial.OrkutContainer.inherits(opensocial.Container);
opensocial.OrkutContainer.prototype.init = function(opt_baseUrl) {
};
