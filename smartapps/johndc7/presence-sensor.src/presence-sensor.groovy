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
definition(
    name: "Presence Sensor",
    namespace: "johndc7",
    author: "John Callahan",
    description: "Improves presence sensor integration with SmartThings",
    category: "Convenience",
    iconUrl: "https://st.callahtech.com/icons/icon.png",
    iconX2Url: "https://st.callahtech.com/icons/icon@2x.png",
    iconX3Url: "https://st.callahtech.com/icons/icon@2x.png",
    oauth: [displayName: "Presence Sensor", displayLink: "https://st.callahtech.com"])


preferences {
  section ("Link the following devices") {
    input "devices", "capability.presenceSensor", multiple: true, required: true
    input "notify", "bool", title: "Notifications"
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

def listDevices() {
	def resp = []
    devices.each {
      resp << [id: it.currentValue("deviceId"), name: it.displayName]
    }
    return resp
}

def updatePresence() {
    def body = request.JSON;
    log.debug("Received push from server");
    log.debug(body);
    if(body == null || body.toString() == "{}"){
    	devices.checkPresence();
        log.error("No JSON data received. Requesting update of ${devices.size()} device(s) at location.");
    	return [error:true,type:"No Data",message:"No JSON data received. Requesting update of ${devices.size()} device(s) at location."];
	}
    log.debug("Updating: " + body.id);
    for(int i = 0; i < devices.size(); i++)
    	if(devices.get(i).currentValue("deviceId") == body.id){
        	devices.get(i).setPresence(body.present,body.location);
            log.debug("Updating: ${body.id}");
            return [error:false,type:"Device updated",message:"Sucessfully updated device: ${body.id}"];
        }
    devices.checkPresence();
    return [error:true,type:"Invalid ID",message:"No device with an id of ${body.id} could be found. Requesting update of ${devices.size()} device(s) at location."];
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	log.debug("Subscribing to events");
	subscribe(devices, "presence", presenceNotifier)
}

def presenceNotifier(evt) {
	log.debug "Event: " + evt.descriptionText;
    if(notify)
    	sendNotification(evt.descriptionText)
}
