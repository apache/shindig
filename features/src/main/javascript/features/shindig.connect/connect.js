
gadgets.rpc.register('resize_iframe', function(height) {
  var element = document.getElementById(this.f);
  if (element) {
    element.style.height = height + 'px';
  }
});
