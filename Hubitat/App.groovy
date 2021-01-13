/**
 *  Improved Mobile Presence
 *
 *  Copyright 2018 John Callahan
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
import groovy.json.JsonSlurper;

definition(
    name: "Presence Sensor",
    namespace: "johndc7",
    author: "John Callahan",
    description: "Improved Presence Sensor integration for Hubitat",
    category: "Convenience",
    iconUrl: "https://st.callahtech.com/icons/icon.png",
    iconX2Url: "https://st.callahtech.com/icons/icon@2x.png",
    iconX3Url: "https://st.callahtech.com/icons/icon@2x.png",
    oauth: [displayName: "Presence Sensor", displayLink: "https://st.callahtech.com"])


preferences {
  page(name: "default", title: "Presence Sensor", uninstall: true, install: true) {
    section("Create a device") {
      href(name: "newDeviceHref",
        title: "New Device",
        page: "createPage",
        description: "Create new device")
    }
    section("Pair an existing device") {
      href(name: "repairDeviceHref",
        title: "Existing Device",
        page: "repairPage",
        description: "Pair an existing device (For Presence Sensor app reinstalls, etc.)")
    }
      
    section("Assign lock codes") {
      href(name: "lockCodesHref",
        title: "Lock Codes",
        page: "lockPage",
        description: "Assign a lock code to a device. When that code is used, device will be assumed present for a few minutes.")
    }
      
    section("") {
       input "logEnable", "bool", title: "Enable Debug Logging", required: false, multiple: false, defaultValue: false, submitOnChange: true
    }
    //section("Other settings") {
      //input "notify", "bool", title: "Notifications"
    //}
    footer()
  }
  page(name: "createPage")
  page(name: "repairPage")
  page(name: "lockPage")
}

def createPage() {
  String deviceId = getDeviceId().toString();
  dynamicPage(name: "createPage", install: false) {
  	section("Instructions"){
  	  paragraph "To create a new device, enter a name for the device below and choose one of the options to pair the Presence Sensor app."
    }
    section("Name") {
      input "deviceName", "text", title: "Device name", required: false, submitOnChange: true
    }
    if(deviceName){
    section("Pair Presence Sensor app") {
      paragraph "New device id: ${deviceId}"
      href(name: "pairCurrent",
        title: "Pair another device",
        url: "https://st.callahtech.com/pair?stId=hubitat%3A${hubUID}&id=${deviceId}&name=${deviceName}",
        description: "Pair another device")
      href(name: "newDevice",
        title: "Pair this device",
        style: "external",
        url: "https://st.callahtech.com/pair?stId=hubitat%3A${hubUID}&id=${deviceId}&name=${deviceName}&current=true",
        description: "Pair this device")
    }
    }
  }
}

def repairPage() {
	dynamicPage(name: "repairPage", install: false) {
  	section("Instructions"){
  	  paragraph "Select an existing device, and choose one of the options to pair the Presence Sensor app."
    }
    section("Device") {
      input "repairDevice", "capability.presenceSensor", multiple: false, required: false, submitOnChange: true
    }
    if(repairDevice){
    section("Pair Presence Sensor app") {
      href(name: "repairCurrent",
        title: "Pair different device",
        url: "https://st.callahtech.com/pair?stId=hubitat%3A${hubUID}&id=${repairDevice.getDeviceNetworkId()}",
        description: "Pair another device")
      href(name: "repairOther",
        title: "Pair this device",
        style: "external",
        url: "https://st.callahtech.com/pair?stId=hubitat%3A${hubUID}&id=${repairDevice.getDeviceNetworkId()}&current=true",
        description: "Pair this device")
    }
    }
  }
}

def lockPage() {
    dynamicPage(name: "lockPage", install: true) {
    section("Locks"){
      paragraph "Select what lock to listen for events on. This option is used for all devices."
      input "locks", "capability.lockCodes", multiple: true, required: false, submitOnChange: true
    }
    section("Device") {
      def codeNames = []
      if(locks != null)
        for(lock in locks){
          def slurper = new JsonSlurper()
          def lockCodes = slurper.parseText(lock.currentValue('lockCodes'))
          for(code in lockCodes.keySet())
            if(lock.getLabel() != null)
                codeNames.push("(${lock.getId()})${lock.getLabel()}: ${lockCodes[code].name}")
        }
      paragraph "Choose a device and select the codes you would like to assign. When any of a device's assigned codes are used, the device will be assumed present for a few minutes. The amount of time can be set in the device preferences."
      input "lockPresenceDevice", "capability.presenceSensor", multiple: false, required: false, submitOnChange: true
      
      if(lockPresenceDevice != null) input "codes-${lockPresenceDevice.getDeviceNetworkId()}", "enum", multiple: true, required: false, submitOnChange: true, options: codeNames, description: "Select lock codes"
    }
  }
}

def footer() {
    section(){
        paragraph "<hr style='background-color:rgba(54,54,54,.87); height: 1px; border: 0;'>"
        paragraph "<div style='text-align:center;color:rgba(54,54,54,.87)'><a href='https://play.google.com/store/apps/details?id=com.callahtech.presencesensor&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1' target='_blank'><img style='max-width: 150px' alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'/></a><br>If you are on Android 8.1 or later, the patch app is also needed: <a href=\"https://presence.callahtech.com/latest-patch.apk\" target='_blank'>Download</a><br><br>Lots of time has been spent creating and maintaining this app.<br>If you find it useful, please consider donating.<br><a href='https://paypal.me/johndc7' target='_blank'><img src='https://www.paypalobjects.com/webstatic/mktg/logo/pp_cc_mark_37x23.jpg' border='0' alt='PayPal Logo'></a></div>"
    }
}

mappings {
  path("/update") {
    action: [
      POST: "updatePresence"
    ]
  }
  path("/devices") {
  	action: [
    	GET: "listDevices"
    ]
  }
}

def setToken(){
	if(!state.accessToken) {
    	createAccessToken()
	}
	try {
    	httpPost([
        	uri: "https://st.callahtech.com",
    		path: "/updateLocation",
            body: [
            	access_token: state.accessToken,
                id: "hubitat:${hubUID}",
                token_type: "bearer",
                scope: "app",
                uri: "${getApiServerUrl()}/${hubUID}/apps/${app.id}",
                name: location.getName()
            ]
        ]) { resp ->
        	if(logEnable) log.debug(resp.data);
        	return resp.data;	    	
    	}
	} catch (e) {
	    log.error "Could not set location auth: $e"
	}
}

def getDeviceId(){
	try {
    	httpGet([
        	uri: "https://st.callahtech.com",
    		path: "/newid"
        ]) { resp ->
        	if(logEnable) log.debug(resp.data);
        	return resp.data;	    	
    	}
	} catch (e) {
	    log.error "Could not get new ID: $e"
	}
}

def listDevices() {
	def resp = []
    getChildDevices().each {
      resp << [id: it.getDeviceNetworkId(), name: it.displayName]
    }
    return resp
}

def updatePresence() {
    def body = request.JSON;
    if(logEnable) log.debug("Received push from server");
    if(logEnable) log.debug(body);
    if(body == null || body.toString() == "{}"){
    	getChildDevices().checkPresence();
        log.error("No JSON data received. Requesting update of ${getChildDevices().size()} device(s) at location.");
    	return [error:true,type:"No Data",message:"No JSON data received. Requesting update of ${getChildDevices().size()} device(s) at location."];
	}
    for(int i = 0; i < getChildDevices().size(); i++)
    	if(getChildDevices().get(i).getDeviceNetworkId() == body.id){
        	getChildDevices().get(i).setPresence(body.present,body.location);
            if(body.battery && body.charging != null)
            	getChildDevices().get(i).setBattery(body.battery,body.charging);
            if(logEnable) log.debug("Updating: ${body.id}");
            return [error:false,type:"Device updated",message:"Sucessfully updated device: ${body.id}"];
        }
    if(logEnable) log.debug("Creating new device ${body.name} with an id of: ${body.id}");
    addChildDevice("johndc7", "Improved Mobile Presence", body.id, [name: body.name ? body.name : "Presence Sensor"]);
    return [error:false, type:"Device created", message:"Created new device ${body.name} with an id of: ${body.id}"];
    //return [error:true,type:"Invalid ID",message:"No device with an id of ${body.id} could be found. Requesting update of ${getChildDevices().size()} device(s) at location."];
}

def installed() {
	if(logEnable) log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
  if(logEnable) log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	setToken();
	if(logEnable) log.debug("Subscribing to events");
  subscribe(getChildDevices(), "presence", presenceNotifier);
  subscribe(settings.locks, "lastCodeName", lockHandler)
}
              
def lockHandler(evt){
  if(logEnable) log.debug("${evt.getDevice()} unlocked with code '${evt.value}'")
  if(logEnable) log.debug "(${evt.getDeviceId()})${evt.getDevice()}: ${evt.value}"
  for(key in settings.keySet())
    if(key.startsWith('codes-') && settings."$key".find({i -> i == "(${evt.getDeviceId()})${evt.getDevice()}: ${evt.value}"}) != null){
      def child = getChildDevice(key.split('-')[1])
      if(child){
        if(logEnable) log.debug "${child.getName()} temporary present"
        child.temporaryPresent()
      }
    }
}

def presenceNotifier(evt) {
	if(logEnable) log.debug "Event: " + evt.descriptionText;
    //if(notify)
    	//sendNotification(evt.descriptionText)
}
