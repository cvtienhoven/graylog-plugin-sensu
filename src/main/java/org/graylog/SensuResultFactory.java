package org.graylog;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class SensuResultFactory {

	@SuppressWarnings("unchecked")
	public String createResult(String name, int status, String output, String handlers, long timestamp, String client,
			String subscribers, String tags) {
		JSONObject check = new JSONObject();
		check.put("name", name.trim());
		check.put("type", "standard");
		check.put("command", "none");
		check.put("standalone", true);
		check.put("status", new Integer(status));
		check.put("output", output.trim());
		check.put("issued", timestamp);
		check.put("executed", timestamp);
				
		
		if (handlers.contains(",")) {			
			check.put("handlers", stringToJSONArray(handlers));
		} else {
			check.put("handler", handlers);
		}

		JSONArray jsonArray = stringToJSONArray(subscribers);
		
		if (jsonArray.size() > 0) {
			check.put("subscribers", jsonArray);
		}

		jsonArray = stringToJSONArray(tags);		

		if (jsonArray.size() > 0) {
			check.put("tags", jsonArray);
		}
						
		check.put("publish", false);

		JSONObject result = new JSONObject();
		result.put("client", client.trim());
		
		result.put("check", check);

		return result.toJSONString();
	}
	@SuppressWarnings("unchecked")
	private JSONArray stringToJSONArray(String input){
		JSONArray jsonArray = new JSONArray();
		if (input.contains(",")) {
			String[] stringArray = input.split(",");
			for (String string:stringArray){
				jsonArray.add(string.trim());
			}			
		} else if (!"".equals(input)) {
			jsonArray.add(input.trim());
		}
		
		return jsonArray;
	}
	
}
