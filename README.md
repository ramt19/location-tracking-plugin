# location-tracking-plugin

Name:  

		Location Tracking Plugin
    
    
Synopsis:

    This is a Cordova plugin that is used to find latitude and longitude of the phone with the help of 
    Cell Network (MCC, MNC, LAC, CID).

Description:

    Location Tracking Plugin is a plugin made for Cordova/ Ionic in Native Android. It uses native 	
    android to find the Latitude and Longitude through GPS and with Cell Network.
    
    Native Android provides us a feature to access the location through GPS but Cordova/Ionic in 
    itself cannot do that. So, a plugin is needed to provide that functionality to the Cordova/Ionic 
    Application.
    
    This Plugin gets the Latitude and Longitude of the phone through the GPS when it is enabled.
    
    When the GPS is disabled then the plugin uses the Cell Network to get the nearest tower location
    to which the cell is connected. 
    
    Cell Tower location is determined by the help of the following parameters: -
    
    MCC - Mobile Country Code
    MNC - Mobile Network Code
    LAC - Local Area Code
    CID - Cell ID
    
    It calls Unwired Labs API with the following data and return the Latitude and Longitude of the 
    nearest tower.
    
    
Requirements:


        Location Tracker Plugin is mainly built in JAVA so JDK needs to installed. As it a plugin made
        for Cordova that is also need to be installed. 
        
        Installation:
        
         Download the tarball from GitHub and unpack it in your plugin folder of the Ionic App.
         
            C:/cordova-app/plugins/
            
         Install the plugin by typing the following in the Cordova CLI
         
            ->cordova plugins add </location/of/plugin>


Arguments:

      The data is to be passed in JSON Format to the plugin which includes the following:
      
      URL:  The Server URL where to send the collected data.
      Method: The Method of Request
      Headers:  JSON Object which will contain the header values
      Interval: The interval at which the location is to be requested. (in milliseconds)
      Debug:  To start the debug mode which will show Toast notification for debugging.
      StartOnBoot:  Option to whether start the plugin automatically when the Device Reboots. 

      The following changes are to be made in index.js file in the button which will start the plugin.
      
      
        For E.g.
        {
        "URL" : "SERVER_URL”,
        "method" : "POST",
        "headers”: 	`	{
              "contentType" : "application/x-www-form-urlencoded",
              "version" : "1.0"
              },
        "interval" : 60000, 
        "debug" : true,
        "StartOnBoot" : true
        }


GITHUB LINK:
	https://github.com/ramt19/location-tracking-plugin.git
