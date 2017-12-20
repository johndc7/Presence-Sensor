/**
 *  Improved Mobile Presence
 *
 *  Copyright 2017 John Callahan
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
	}


	simulator {
    	standardTile("presence", "device.presence", width: 2, height: 2, canChangeBackground: true) {
            state "present", labelIcon:"st.presence.tile.present", backgroundColor:"#00a0dc"
            state "not present", labelIcon:"st.presence.tile.not-present", backgroundColor:"#ffffff"
        }
		status "present":  "set-present"
        status "not present": "set-away"
	}
    
    preferences {
		section {
			image(name: 'educationalcontent', multiple: true, images: [
				"http://cdn.device-gse.smartthings.com/Arrival/Arrival1.jpg",
				"http://cdn.device-gse.smartthings.com/Arrival/Arrival2.jpg"
				])
			input("id", "text", title: "Device ID", description: "Device ID from app", displayDuringSetup: true, required: true)
            input("timeout", "number", title: "Presence timeout (minutes)", description: "Minutes until considered not present after leaving", defaultValue: 0, required: true)
		}
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

// handle commands
def configure() {
	state.leftCounter = 0
	runEvery1Minute(checkPresence)
    checkPresence()
}

def updateState(response, data){
	if (response.hasError()) {
	        log.error "Response has error: $response.errorMessage"
	    } else {
	        try {
            	def slurper = new JsonSlurper()
            	log.debug "Recieved \"$response.data\" from server"
                log.debug "Current Location: " + device.currentValue("currentLocation")
                log.debug "Previous Location: " + device.currentValue("previousLocation")
                //log.debug "Location: " + response.data.location.getClass()
            	def json = slurper.parseText(response.data)
                setPresence(json.present)
                setPresenceLocation(json.location)
	        } catch (e) {
	            log.error "Error setting presence: $e"
                setPresence(false)
	        }
    	}
}
def checkPresence(){
	log.debug "Checking presence"
	asynchttp_v1.get(updateState, [uri:"https://st.callahtech.com/detailedpresence?id=$id"])
}

def setPresence(boolean present){
	log.debug "setPresence(" + present + ")"
    log.debug "leftCounter: " + state.leftCounter
    if(present)
    	state.leftCounter = 0
	else
    	state.leftCounter = state.leftCounter+1
    if(present != (device.currentValue("presence") == "present")){
		if(present)
	   		sendEvent(displayed: true,  isStateChange: true, name: "presence", value: "present", descriptionText: "$device.displayName has arrived")
		else if(state.leftCounter > timeout)
	    	sendEvent(displayed: true,  isStateChange: true, name: "presence", value: "not present", descriptionText: "$device.displayName has left")
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
