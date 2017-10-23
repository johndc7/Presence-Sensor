/**
 *  DCS-933L
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
metadata {
	definition (name: "DCS-933L", namespace: "johndc7", author: "John Callahan") {
	capability "Configuration"
		capability "Video Camera"
		capability "Video Capture"
		capability "Refresh"
		capability "Switch"

		command "start"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale: 2) {
		multiAttributeTile(name: "videoPlayer", type: "videoPlayer", width: 6, height: 4) {
			tileAttribute("device.switch", key: "CAMERA_STATUS") {
				attributeState("on", label: "Active", icon: "st.camera.dlink-indoor", action: "switch.off", backgroundColor: "#79b821", defaultState: true)
				attributeState("off", label: "Inactive", icon: "st.camera.dlink-indoor", action: "switch.on", backgroundColor: "#ffffff")
				attributeState("restarting", label: "Connecting", icon: "st.camera.dlink-indoor", backgroundColor: "#53a7c0")
				attributeState("unavailable", label: "Unavailable", icon: "st.camera.dlink-indoor", action: "refresh.refresh", backgroundColor: "#F22000")
			}

			tileAttribute("device.errorMessage", key: "CAMERA_ERROR_MESSAGE") {
				attributeState("errorMessage", label: "", value: "", defaultState: true)
			}

			tileAttribute("device.camera", key: "PRIMARY_CONTROL") {
				attributeState("on", label: "Active", icon: "st.camera.dlink-indoor", backgroundColor: "#79b821", defaultState: true)
				attributeState("off", label: "Inactive", icon: "st.camera.dlink-indoor", backgroundColor: "#ffffff")
				attributeState("restarting", label: "Connecting", icon: "st.camera.dlink-indoor", backgroundColor: "#53a7c0")
				attributeState("unavailable", label: "Unavailable", icon: "st.camera.dlink-indoor", backgroundColor: "#F22000")
			}

			tileAttribute("device.startLive", key: "START_LIVE") {
				attributeState("live", action: "start", defaultState: true)
			}

			tileAttribute("device.stream", key: "STREAM_URL") {
				attributeState("activeURL", defaultState: true)
			}
		}
		

		main("videoPlayer")
		details(["videoPlayer"])
	}
}

preferences {
	section("Camera Info"){
    	input("camIP", "text", title: "Camera IP", description: "Camera IP", required: true)
        input("camPort", "text", title: "Camera Port", description: "Camera Port", required: true)
        input("camUser", "text", title: "Camera Username", description: "Camera Username", required: false)
        input("camPass", "text", title: "Camera Password", description: "Camera Password", required: false)
	}
    
    section("Stream settings"){
    	input("streamType", "enum", title: "Stream type", description: "What would you like to stream from the camera?", required: true, options: ["Audio / Video","Video Only","Audio Only"])
    }
}

mappings {
   path("/getInHomeURL") {
       action:
       [GET: "getInHomeURL"]
   }
}


def installed() {
	configure()
}

def updated() {
	configure()
}
// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"

}

// handle commands
def configure() {
	def stream = "h264.flv"
	switch("$streamType"){
    	case "Audio / Video":
        	stream = "h264.flv"
            break
		case "Audio Only":
        	stream = "audio.cgi"
            break
		case "Video Only":
        	stream = "mjpeg.cgi?channel=1.mjpeg"
            break
    }
	if("$camUser" == null || "$camPass" == null)
    	state.streamURL = "http://$camIP:$camPort/" + stream
	else
		state.streamURL = "http://$camUser:$camPass@$camIP:$camPort/" + stream
	log.debug "Executing 'configure'"
    log.debug "Stream: " + state.streamURL
    sendEvent(name:"switch", value: "on")
}

def start() {
	log.trace "start()"
	def dataLiveVideo = [
		OutHomeURL  : state.streamURL,
		InHomeURL   : state.streamURL,
		ThumbnailURL: "http://cdn.device-icons.smartthings.com/camera/dlink-indoor@2x.png",
		cookie      : [key: "key", value: "value"]
	]

	def event = [
		name           : "stream",
		value          : groovy.json.JsonOutput.toJson(dataLiveVideo).toString(),
		data		   : groovy.json.JsonOutput.toJson(dataLiveVideo),
		descriptionText: "Starting the livestream",
		eventType      : "VIDEO",
		displayed      : false,
		isStateChange  : true
	]
	sendEvent(event)
}

def getInHomeURL() {
	 [InHomeURL: state.streamURL]
}