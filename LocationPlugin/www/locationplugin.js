 // Empty constructor
  function LocationPlugin() {}
  // The function that passes work along to native shells
  LocationPlugin.prototype.startSer = function(options, successCallback, errorCallback) {
  //   var option = {};
  //   options.key = autKey;
  //   options.url = url;
  //   options.contentType = contentType;
  //   options.timer = timer;
  //   options.debug = debug;
	// options.startOnBoot = startOnBoot;
    
    cordova.exec(successCallback, errorCallback, 'LocationPlugin', 'start', [options]);
  }
  
  LocationPlugin.prototype.stopSer = function(successCallback, errorCallback) {
   var options = {};
    cordova.exec(successCallback, errorCallback, 'LocationPlugin', 'stop', [options]);
  }
  
  // Installation constructor that binds LocationPlugin to window
  LocationPlugin.install = function() {
    if (!window.plugins) {
      window.plugins = {};
    }
    window.plugins.locationPlugin = new LocationPlugin();
    return window.plugins.locationPlugin;
  };
  cordova.addConstructor(LocationPlugin.install);

  

