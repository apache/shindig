shindig.defer = (function() {
  function callback(callback) {
    var args = [].slice.call(arguments, 1);
    callback.apply(null, args);
  }

  return {
    callback: callback
  };
})();
