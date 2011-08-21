var tamings___ = tamings___ || [];
tamings___.push(function(imports) {
  ___.grantRead(gadgets.actions, 'addAction');
  ___.grantRead(gadgets.actions, 'updateAction');
  ___.grantRead(gadgets.actions, 'removeAction');
  ___.grantRead(gadgets.actions, 'runAction');
  ___.grantRead(gadgets.actions, 'getActionsByPath');
  ___.grantRead(gadgets.actions, 'getActionsByDataType');
  ___.grantRead(gadgets.actions, 'registerShowActionsListener');
  ___.grantRead(gadgets.actions, 'registerHideActionsListener');
});
