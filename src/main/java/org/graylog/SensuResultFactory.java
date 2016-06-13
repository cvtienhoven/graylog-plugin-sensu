package org.graylog;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class SensuResultFactory {

	@SuppressWarnings("unchecked")
	public String createResult(String name, int status, String output, String handlers, long timestamp, String client,
			String subscribers) {
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
			String[] handlersArray = handlers.split(",");
			JSONArray jsonArray = new JSONArray();
			for (String handler:handlersArray){
				jsonArray.add(handler.trim());
			}
			check.put("handlers", jsonArray);
		} else {
			check.put("handler", handlers);
		}

		JSONArray jsonArray = new JSONArray();
		if (subscribers.contains(",")) {
			String[] subscribersArray = subscribers.split(",");
			for (String subscriber:subscribersArray){
				jsonArray.add(subscriber.trim());
			}			
		} else if (!"".equals(subscribers)) {
			jsonArray.add(subscribers.trim());
		}

		if (jsonArray.size() > 0) {
			check.put("subscribers", jsonArray);
		}

		check.put("publish", false);

		JSONObject result = new JSONObject();
		result.put("client", client.trim());
		
		result.put("check", check);

		return result.toJSONString();
	}
}
