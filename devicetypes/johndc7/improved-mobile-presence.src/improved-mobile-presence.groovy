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
include 'asynchttp_v1'
import groovy.json.JsonSlurper
 
metadata {
	definition (name: "Improved Mobile Presence", namespace: "johndc7", author: "John Callahan") {
		capability "Presence Sensor"
        capability "Sensor"
        
        attribute "currentLocation", "String"
        attribute "previousLocation", "String"
        
        command "checkPresence"
        command "setPresence",["boolean","string"]
	}


	simulator {
    	standardTile("presence", "device.presence", width: 2, height: 2, canChangeBackground: true) {
            state "present", labelIcon:"st.presence.tile.present", backgroundColor:"#00a0dc"
            state "not present", labelIcon:"st.presence.tile.not-present", backgroundColor:"#ffffff"
        }
		status "present":  "set-present"
        status "not present": "set-away"
	}

	tiles {
        standardTile("presence", "device.presence", width: 2, height: 2, canChangeBackground: true) {
            state "present", labelIcon:"st.presence.tile.present", backgroundColor:"#00a0dc"
            state "not present", labelIcon:"st.presence.tile.not-present", backgroundColor:"#ffffff"
        }
        valueTile("location", "device.currentLocation", width: 2, height: 2) {
        	state "Home", label: '${currentValue}', backgroundColor:"#00a0dc"
            state "default", label: '${currentValue}', backgroundColor:"#ffffff"
		}
        main "location"
        details(["presence"])
    }
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	if(description == "set-present")
    	setPresence(true);
	if(description == "set-away")
    	setPresence(false);
}

def installed() {
	configure()
}

def updated() {
	configure()
}

def configure() {
	log.debug "Running config with settings: ${settings}"
	runEvery15Minutes(checkPresence)
    checkPresence();
}

def updateState(response, data){
	if (response.hasError()) {
	        log.error "Response has error: $response.errorMessage"
	    } else {
	        try {
            	def slurper = new JsonSlurper();
            	def json = slurper.parseText(response.data);
                if(json.error) log.error('Error checking presence - ' + json.message);
                else setPresence(json.present, json.location);
                //setPresenceLocation(json.location);
                log.debug "Recieved \"$response.data\" from server"
                log.debug "Current Location: " + device.currentValue("currentLocation");
                log.debug "Previous Location: " + device.currentValue("previousLocation");
                //log.debug "Location: " + response.data.location.getClass()
	        } catch (e) {
	            log.error "Error setting presence: $e"
                setPresence(false, "Away");
	        }
    	}
}
def checkPresence(){
	log.debug "Checking presence"
	asynchttp_v1.get(updateState, [uri:"https://st.callahtech.com/detailedpresence?id=${device.getDeviceNetworkId()}"])
}

def setPresence(boolean present, String location){
	log.debug "setPresence(" + present + ")"
    if(location != device.currentValue("currentLocation")){
		if(present)
	   		sendEvent(displayed: true,  isStateChange: true, name: "presence", value: "present", descriptionText: "$device.displayName has arrived at " + location)
		else {
        	if(location == "Away")
	    		sendEvent(displayed: true,  isStateChange: true, name: "presence", value: "not present", descriptionText: "$device.displayName has left " + device.currentValue("currentLocation"))
            else
                sendEvent(displayed: true,  isStateChange: true, name: "presence", value: "not present", descriptionText: "$device.displayName has arrived at " + location)
        }
        setPresenceLocation(location);
        log.debug "Presence set"
	}
}

def setPresenceLocation(String location){
	if(location != device.currentValue("currentLocation")){
		log.debug "Setting location to: " + location
		sendEvent(name: "previousLocation", value: device.currentValue("currentLocation"), isStateChange: true, displayed: false)
		sendEvent(name: "currentLocation", value: location, isStateChange: true, displayed: false)
		log.debug "Current location: " + device.currentValue("currentLoaction")
		log.debug "Previous location: " + device.currentValue("previousLocation")
    }
}