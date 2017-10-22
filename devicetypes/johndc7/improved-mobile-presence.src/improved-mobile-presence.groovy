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
 
metadata {
	definition (name: "Improved Mobile Presence", namespace: "johndc7", author: "John Callahan") {
		capability "Presence Sensor"
        capability "Sensor"
	}


	simulator {
		// TODO: define status and reply messages here
	}
    
    preferences {
		section {
			image(name: 'educationalcontent', multiple: true, images: [
				"http://cdn.device-gse.smartthings.com/Arrival/Arrival1.jpg",
				"http://cdn.device-gse.smartthings.com/Arrival/Arrival2.jpg"
				])
			input("id", "text", title: "Device ID", description: "Device ID from app", displayDuringSetup: true, required: true)
            input("timeout", "number", title: "Timeout", description: "Timeout", defaultValue: 0, required: true)
		}
	}

	tiles {
        standardTile("presence", "device.presence", width: 2, height: 2, canChangeBackground: true) {
            state "present", labelIcon:"st.presence.tile.present", backgroundColor:"#00a0dc"
            state "not present", labelIcon:"st.presence.tile.not-present", backgroundColor:"#ffffff"
        }
        main "presence"
        details(["presence", "beep", "battery"])
    }
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'presence' attribute
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
	    } else {+6
	        try {
                log.debug "Recieved \"$response.data\" from server"
                setPresence(response.data == "true")
	        } catch (e) {
	            log.error "Error setting presence: $e"
                setPresence(false)
	        }
    	}
}
def checkPresence(){
	log.debug "Checking presence"
	asynchttp_v1.get(updateState, [uri:"https://st.callahtech.com/presence?id=$id"])
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