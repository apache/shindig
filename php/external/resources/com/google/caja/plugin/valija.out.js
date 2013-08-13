{
  ___.loadModule({
      'instantiate': function (___, IMPORTS___) {
        var Array = ___.readImport(IMPORTS___, 'Array');
        var Object = ___.readImport(IMPORTS___, 'Object');
        var ReferenceError = ___.readImport(IMPORTS___, 'ReferenceError', {});
        var RegExp = ___.readImport(IMPORTS___, 'RegExp', {});
        var TypeError = ___.readImport(IMPORTS___, 'TypeError', {});
        var cajita = ___.readImport(IMPORTS___, 'cajita', {
            'beget': { '()': {} },
            'freeze': { '()': {} },
            'newTable': { '()': {} },
            'getFuncCategory': { '()': {} },
            'enforceType': { '()': {} },
            'getSuperCtor': { '()': {} },
            'getOwnPropertyNames': { '()': {} },
            'getProtoPropertyNames': { '()': {} },
            'getProtoPropertyValue': { '()': {} },
            'inheritsFrom': { '()': {} },
            'readOwn': { '()': {} },
            'directConstructor': { '()': {} },
            'USELESS': {},
            'construct': { '()': {} },
            'forAllKeys': { '()': {} },
            'Token': { '()': {} },
            'args': { '()': {} }
          });
        var loader = ___.readImport(IMPORTS___, 'loader');
        var outers = ___.readImport(IMPORTS___, 'outers');
        var moduleResult___, valijaMaker;
        moduleResult___ = ___.NO_RESULT;
        valijaMaker = (function () {
            function valijaMaker$_var(outers) {
              var ObjectPrototype, DisfunctionPrototype, Disfunction,
              ObjectShadow, x0___, FuncHeader, x1___, myPOE, x2___, pumpkin,
              x3___, x4___, x5___, t, undefIndicator;
              function disfuncToString($dis) {
                var callFn, printRep, match, name;
                callFn = $dis.call_canRead___? $dis.call: ___.readPub($dis,
                  'call');
                if (callFn) {
                  printRep = callFn.toString_canCall___? callFn.toString():
                  ___.callPub(callFn, 'toString', [ ]);
                  match = FuncHeader.exec_canCall___? FuncHeader.exec(printRep)
                    : ___.callPub(FuncHeader, 'exec', [ printRep ]);
                  if (null !== match) {
                    name = $dis.name_canRead___? $dis.name: ___.readPub($dis,
                      'name');
                    if (name === void 0) {
                      name = match[ 1 ];
                    }
                    return 'function ' + name + '(' + match[ 2 ] +
                      ') {\n  [cajoled code]\n}';
                  }
                  return printRep;
                }
                return 'disfunction(var_args){\n   [cajoled code]\n}';
              }
              disfuncToString.FUNC___ = 'disfuncToString';
              function getShadow(func) {
                var cat, result, parentFunc, parentShadow, proto, statics, i,
                k, meths, i, k, v;
                cajita.enforceType(func, 'function');
                cat = cajita.getFuncCategory(func);
                result = myPOE.get_canCall___? myPOE.get(cat):
                ___.callPub(myPOE, 'get', [ cat ]);
                if (void 0 === result) {
                  result = cajita.beget(DisfunctionPrototype);
                  parentFunc = cajita.getSuperCtor(func);
                  if (___.typeOf(parentFunc) === 'function') {
                    parentShadow = getShadow.CALL___(parentFunc);
                  } else {
                    parentShadow = ObjectShadow;
                  }
                  proto = cajita.beget(parentShadow.prototype_canRead___?
                    parentShadow.prototype: ___.readPub(parentShadow,
                      'prototype'));
                  result.prototype_canSet___ === result? (result.prototype =
                    proto): ___.setPub(result, 'prototype', proto);
                  proto.constructor_canSet___ === proto? (proto.constructor =
                    func): ___.setPub(proto, 'constructor', func);
                  statics = cajita.getOwnPropertyNames(func);
                  for (i = 0; i < statics.length; i++) {
                    k = ___.readPub(statics, i);
                    if (k !== 'valueOf') {
                      ___.setPub(result, k, ___.readPub(func, k));
                    }
                  }
                  meths = cajita.getProtoPropertyNames(func);
                  for (i = 0; i < meths.length; i++) {
                    k = ___.readPub(meths, i);
                    if (k !== 'valueOf') {
                      v = cajita.getProtoPropertyValue(func, k);
                      if (___.typeOf(v) === 'object' && v !== null &&
                        ___.typeOf(v.call_canRead___? v.call: ___.readPub(v,
                            'call')) === 'function') {
                        v = dis.CALL___(v.call_canRead___? v.call:
                          ___.readPub(v, 'call'), k);
                      }
                      ___.setPub(proto, k, v);
                    }
                  }
                  myPOE.set_canCall___? myPOE.set(cat, result):
                  ___.callPub(myPOE, 'set', [ cat, result ]);
                }
                return result;
              }
              getShadow.FUNC___ = 'getShadow';
              function getFakeProtoOf(func) {
                var shadow;
                if (___.typeOf(func) === 'function') {
                  shadow = getShadow.CALL___(func);
                  return shadow.prototype_canRead___? shadow.prototype:
                  ___.readPub(shadow, 'prototype');
                } else if (___.typeOf(func) === 'object' && func !== null) {
                  return func.prototype_canRead___? func.prototype:
                  ___.readPub(func, 'prototype');
                } else { return void 0; }
              }
              getFakeProtoOf.FUNC___ = 'getFakeProtoOf';
              function typeOf(obj) {
                var result;
                result = ___.typeOf(obj);
                if (result !== 'object') { return result; }
                if (cajita.isPseudoFunc_canCall___? cajita.isPseudoFunc(obj):
                  ___.callPub(cajita, 'isPseudoFunc', [ obj ])) { return 'function'; }
                return result;
              }
              typeOf.FUNC___ = 'typeOf';
              function instanceOf(obj, func) {
                if (___.typeOf(func) === 'function' && obj instanceof func) {
                  return true; } else {
                  return cajita.inheritsFrom(obj, getFakeProtoOf.CALL___(func))
                    ;
                }
              }
              instanceOf.FUNC___ = 'instanceOf';
              function read(obj, name) {
                var result, stepParent;
                result = cajita.readOwn(obj, name, pumpkin);
                if (result !== pumpkin) { return result; }
                if (___.typeOf(obj) === 'function') {
                  return ___.readPub(getShadow.CALL___(obj), name);
                }
                if (obj === null || obj === void 0) {
                  throw ___.construct(TypeError, [ 'Cannot read property \"' +
                        name + '\" from ' + obj ]);
                }
                if (___.inPub(name, ___.construct(Object, [ obj ]))) {
                  return ___.readPub(obj, name);
                }
                stepParent =
                  getFakeProtoOf.CALL___(cajita.directConstructor(obj));
                if (stepParent !== void 0 && ___.inPub(name,
                    ___.construct(Object, [ stepParent ])) && name !==
                  'valueOf') {
                  return ___.readPub(stepParent, name);
                }
                return ___.readPub(obj, name);
              }
              read.FUNC___ = 'read';
              function set(obj, name, newValue) {
                if (___.typeOf(obj) === 'function') {
                  ___.setPub(getShadow.CALL___(obj), name, newValue);
                } else {
                  ___.setPub(obj, name, newValue);
                }
                return newValue;
              }
              set.FUNC___ = 'set';
              function callFunc(func, args) {
                var x0___;
                return x0___ = cajita.USELESS, func.apply_canCall___?
                  func.apply(x0___, args): ___.callPub(func, 'apply', [ x0___,
                    args ]);
              }
              callFunc.FUNC___ = 'callFunc';
              function callMethod(obj, name, args) {
                var m;
                m = read.CALL___(obj, name);
                if (!m) {
                  throw ___.construct(TypeError, [ 'callMethod: ' + obj +
                        ' has no method ' + name ]);
                }
                return m.apply_canCall___? m.apply(obj, args): ___.callPub(m,
                  'apply', [ obj, args ]);
              }
              callMethod.FUNC___ = 'callMethod';
              function construct(ctor, args) {
                var result, altResult;
                if (___.typeOf(ctor) === 'function') {
                  return cajita.construct(ctor, args);
                }
                result = cajita.beget(ctor.prototype_canRead___?
                  ctor.prototype: ___.readPub(ctor, 'prototype'));
                altResult = ctor.apply_canCall___? ctor.apply(result, args):
                ___.callPub(ctor, 'apply', [ result, args ]);
                switch (___.typeOf(altResult)) {
                case 'object':
                  if (null !== altResult) { return altResult; }
                  break;
                case 'function':
                  return altResult;
                }
                return result;
              }
              construct.FUNC___ = 'construct';
              function dis(callFn, opt_name) {
                var template, result, x0___, disproto, x1___;
                template = cajita.PseudoFunction_canCall___?
                  cajita.PseudoFunction(callFn): ___.callPub(cajita,
                  'PseudoFunction', [ callFn ]);
                result = cajita.beget(DisfunctionPrototype);
                result.call_canSet___ === result? (result.call = callFn):
                ___.setPub(result, 'call', callFn);
                x0___ = template.apply_canRead___? template.apply:
                ___.readPub(template, 'apply'), result.apply_canSet___ ===
                  result? (result.apply = x0___): ___.setPub(result, 'apply',
                  x0___);
                disproto = cajita.beget(ObjectPrototype);
                result.prototype_canSet___ === result? (result.prototype =
                  disproto): ___.setPub(result, 'prototype', disproto);
                disproto.constructor_canSet___ === disproto?
                  (disproto.constructor = result): ___.setPub(disproto,
                  'constructor', result);
                x1___ = template.length, result.length_canSet___ === result?
                  (result.length = x1___): ___.setPub(result, 'length', x1___);
                if (opt_name !== void 0 && opt_name !== '') {
                  result.name_canSet___ === result? (result.name = opt_name):
                  ___.setPub(result, 'name', opt_name);
                }
                return result;
              }
              dis.FUNC___ = 'dis';
              function disfuncCall($dis, self, var_args) {
                var a___ = ___.args(arguments);
                var x0___;
                return x0___ = Array.slice(a___, 2), $dis.apply_canCall___?
                  $dis.apply(self, x0___): ___.callPub($dis, 'apply', [ self,
                    x0___ ]);
              }
              disfuncCall.FUNC___ = 'disfuncCall';
              function disfuncApply($dis, self, args) {
                return $dis.apply_canCall___? $dis.apply(self, args):
                ___.callPub($dis, 'apply', [ self, args ]);
              }
              disfuncApply.FUNC___ = 'disfuncApply';
              function disfuncBind($dis, self, var_args) {
                var a___ = ___.args(arguments);
                var leftArgs;
                function disfuncBound(var_args) {
                  var a___ = ___.args(arguments);
                  var x0___, x1___;
                  return x1___ = (x0___ = Array.slice(a___, 0),
                    leftArgs.concat_canCall___? leftArgs.concat(x0___):
                    ___.callPub(leftArgs, 'concat', [ x0___ ])),
                  $dis.apply_canCall___? $dis.apply(self, x1___):
                  ___.callPub($dis, 'apply', [ self, x1___ ]);
                }
                disfuncBound.FUNC___ = 'disfuncBound';
                leftArgs = Array.slice(a___, 2);
                return ___.primFreeze(disfuncBound);
              }
              disfuncBind.FUNC___ = 'disfuncBind';
              function getOuters() {
                cajita.enforceType(outers, 'object');
                return outers;
              }
              getOuters.FUNC___ = 'getOuters';
              function readOuter(name) {
                var result;
                result = cajita.readOwn(outers, name, pumpkin);
                if (result !== pumpkin) { return result; }
                if (canReadRev.CALL___(name, outers)) {
                  return read.CALL___(outers, name);
                } else {
                  throw ___.construct(ReferenceError, [
                      'Outer variable not found: ' + name ]);
                }
              }
              readOuter.FUNC___ = 'readOuter';
              function readOuterSilent(name) {
                if (canReadRev.CALL___(name, outers)) {
                  return read.CALL___(outers, name);
                } else { return void 0; }
              }
              readOuterSilent.FUNC___ = 'readOuterSilent';
              function setOuter(name, val) {
                return ___.setPub(outers, name, val);
              }
              setOuter.FUNC___ = 'setOuter';
              function initOuter(name) {
                if (canReadRev.CALL___(name, outers)) { return; }
                set.CALL___(outers, name, void 0);
              }
              initOuter.FUNC___ = 'initOuter';
              function remove(obj, name) {
                var shadow;
                if (___.typeOf(obj) === 'function') {
                  shadow = getShadow.CALL___(obj);
                  return ___.deletePub(shadow, name);
                } else {
                  return ___.deletePub(obj, name);
                }
              }
              remove.FUNC___ = 'remove';
              function keys(obj) {
                var result;
                result = [ ];
                cajita.forAllKeys(obj, ___.markFuncFreeze(function (name) {
                      result.push_canCall___? result.push(name):
                      ___.callPub(result, 'push', [ name ]);
                    }));
                cajita.forAllKeys(getSupplement.CALL___(obj),
                  ___.markFuncFreeze(function (name) {
                      if (!___.inPub(name, obj) && name !== 'constructor') {
                        result.push_canCall___? result.push(name):
                        ___.callPub(result, 'push', [ name ]);
                      }
                    }));
                return result;
              }
              keys.FUNC___ = 'keys';
              function canReadRev(name, obj) {
                if (___.inPub(name, obj)) { return true; }
                return ___.inPub(name, getSupplement.CALL___(obj));
              }
              canReadRev.FUNC___ = 'canReadRev';
              function getSupplement(obj) {
                var ctor;
                if (___.typeOf(obj) === 'function') {
                  return getShadow.CALL___(obj);
                } else {
                  ctor = cajita.directConstructor(obj);
                  return getFakeProtoOf.CALL___(ctor);
                }
              }
              getSupplement.FUNC___ = 'getSupplement';
              function exceptionTableSet(ex) {
                var result, x0___;
                result = cajita.Token('' + ex);
                x0___ = ex === void 0? undefIndicator: ex, t.set_canCall___?
                  t.set(result, x0___): ___.callPub(t, 'set', [ result, x0___ ]
                );
                return result;
              }
              exceptionTableSet.FUNC___ = 'exceptionTableSet';
              function exceptionTableRead(key) {
                var v, x0___;
                v = t.get_canCall___? t.get(key): ___.callPub(t, 'get', [ key ]
                );
                x0___ = void 0, t.set_canCall___? t.set(key, x0___):
                ___.callPub(t, 'set', [ key, x0___ ]);
                return v === void 0? key: v === undefIndicator? void 0: v;
              }
              exceptionTableRead.FUNC___ = 'exceptionTableRead';
              function disArgs(original) {
                return cajita.args(Array.slice(original, 1));
              }
              disArgs.FUNC___ = 'disArgs';
              ObjectPrototype = ___.iM([ 'constructor', Object ]);
              DisfunctionPrototype =
                cajita.beget(cajita.PseudoFunctionProto_canRead___?
                cajita.PseudoFunctionProto: ___.readPub(cajita,
                  'PseudoFunctionProto'));
              Disfunction = cajita.beget(DisfunctionPrototype);
              Disfunction.prototype_canSet___ === Disfunction?
                (Disfunction.prototype = DisfunctionPrototype):
              ___.setPub(Disfunction, 'prototype', DisfunctionPrototype);
              Disfunction.length_canSet___ === Disfunction? (Disfunction.length
                = 1): ___.setPub(Disfunction, 'length', 1);
              DisfunctionPrototype.constructor_canSet___ ===
                DisfunctionPrototype? (DisfunctionPrototype.constructor =
                Disfunction): ___.setPub(DisfunctionPrototype, 'constructor',
                Disfunction);
              outers.Function_canSet___ === outers? (outers.Function =
                Disfunction): ___.setPub(outers, 'Function', Disfunction);
              ObjectShadow = cajita.beget(DisfunctionPrototype);
              ObjectShadow.prototype_canSet___ === ObjectShadow?
                (ObjectShadow.prototype = ObjectPrototype):
              ___.setPub(ObjectShadow, 'prototype', ObjectPrototype);
              x0___ = (function () {
                  function freeze$_meth(obj) {
                    if (___.typeOf(obj) === 'function') {
                      cajita.freeze(getShadow.CALL___(obj));
                    } else {
                      cajita.freeze(obj);
                    }
                    return obj;
                  }
                  return ___.markFuncFreeze(freeze$_meth, 'freeze$_meth');
                })(), ObjectShadow.freeze_canSet___ === ObjectShadow?
                (ObjectShadow.freeze = x0___): ___.setPub(ObjectShadow,
                'freeze', x0___);
              FuncHeader = ___.construct(RegExp, [
                  '^\\s*function\\s*([^\\s\\(]*)\\s*\\(' + '(?:\\$dis,?\\s*)?'
                    + '([^\\)]*)\\)' ]);
              x1___ = dis.CALL___(___.primFreeze(disfuncToString), 'toString'),
              DisfunctionPrototype.toString_canSet___ === DisfunctionPrototype?
                (DisfunctionPrototype.toString = x1___):
              ___.setPub(DisfunctionPrototype, 'toString', x1___);
              outers.Function_canSet___ === outers? (outers.Function =
                Disfunction): ___.setPub(outers, 'Function', Disfunction);
              myPOE = cajita.newTable();
              x2___ = cajita.getFuncCategory(Object), myPOE.set_canCall___?
                myPOE.set(x2___, ObjectShadow): ___.callPub(myPOE, 'set', [
                  x2___, ObjectShadow ]);
              pumpkin = ___.iM([ ]);
              x3___ = dis.CALL___(___.primFreeze(disfuncCall), 'call'),
              DisfunctionPrototype.call_canSet___ === DisfunctionPrototype?
                (DisfunctionPrototype.call = x3___):
              ___.setPub(DisfunctionPrototype, 'call', x3___);
              x4___ = dis.CALL___(___.primFreeze(disfuncApply), 'apply'),
              DisfunctionPrototype.apply_canSet___ === DisfunctionPrototype?
                (DisfunctionPrototype.apply = x4___):
              ___.setPub(DisfunctionPrototype, 'apply', x4___);
              x5___ = dis.CALL___(___.primFreeze(disfuncBind), 'bind'),
              DisfunctionPrototype.bind_canSet___ === DisfunctionPrototype?
                (DisfunctionPrototype.bind = x5___):
              ___.setPub(DisfunctionPrototype, 'bind', x5___);
              t = cajita.newTable();
              undefIndicator = ___.iM([ ]);
              return cajita.freeze(___.iM([ 'typeOf', ___.primFreeze(typeOf),
                    'instanceOf', ___.primFreeze(instanceOf), 'tr',
                    ___.primFreeze(exceptionTableRead), 'ts',
                    ___.primFreeze(exceptionTableSet), 'r',
                    ___.primFreeze(read), 's', ___.primFreeze(set), 'cf',
                    ___.primFreeze(callFunc), 'cm', ___.primFreeze(callMethod),
                    'construct', ___.primFreeze(construct), 'getOuters',
                    ___.primFreeze(getOuters), 'ro', ___.primFreeze(readOuter),
                    'ros', ___.primFreeze(readOuterSilent), 'so',
                    ___.primFreeze(setOuter), 'initOuter',
                    ___.primFreeze(initOuter), 'remove', ___.primFreeze(remove)
                      , 'keys', ___.primFreeze(keys), 'canReadRev',
                    ___.primFreeze(canReadRev), 'disArgs',
                    ___.primFreeze(disArgs), 'dis', ___.primFreeze(dis) ]));
            }
            return ___.markFuncFreeze(valijaMaker$_var, 'valijaMaker$_var');
          })();
        if (___.typeOf(loader) !== 'undefined') {
          loader.provide_canCall___? loader.provide(valijaMaker):
          ___.callPub(loader, 'provide', [ valijaMaker ]);
        }
        if (___.typeOf(outers) !== 'undefined') {
          moduleResult___ = valijaMaker.CALL___(outers);
        }
        return moduleResult___;
      },
      'cajolerName': 'com.google.caja',
      'cajolerVersion': '4250',
      'cajoledDate': 1282577826648
});
}