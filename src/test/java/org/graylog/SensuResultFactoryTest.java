package org.graylog;

import static org.junit.Assert.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

public class SensuResultFactoryTest {
	private SensuResultFactory resultFactory;
	
	@Before
	public void setUp() {
		resultFactory = new SensuResultFactory();
	}
	
	@Test
	public void testCreateResultOneHandlerNoSubscribers() throws ParseException{
		
		String result = resultFactory.createResult("check_name", 1, "test_output", "handler", 123456789L, "client", "");
				
		JSONObject json = (JSONObject) new JSONParser().parse(result);
		
		JSONObject check = (JSONObject) json.get("check");
		
		assertEquals(123456789L, check.get("executed"));
		assertEquals(1L, check.get("status"));
		assertEquals(123456789L, check.get("issued"));
		assertEquals("check_name", check.get("name"));
		assertEquals(false, check.get("publish"));
		assertEquals("test_output", check.get("output"));
		assertEquals(true, check.get("standalone"));
		assertEquals("standard", check.get("type"));
		assertEquals("handler", check.get("handler"));
		assertEquals("client", json.get("client"));
	}
	
	@Test
	public void testCreateResultMultipleHandlersOneSubscriber() throws ParseException{
		String result = resultFactory.createResult("check_name ", 1, " test_output ", " handler1, handler2", 123456789L, "client ", "subscriber");
		
		JSONObject json = (JSONObject) new JSONParser().parse(result);
		
		JSONObject check = (JSONObject) json.get("check");
		
		assertEquals(123456789L, check.get("executed"));
		assertEquals(1L, check.get("status"));
		assertEquals(123456789L, check.get("issued"));
		assertEquals("check_name", check.get("name"));
		assertEquals(false, check.get("publish"));
		assertEquals("test_output", check.get("output"));
		assertEquals(true, check.get("standalone"));
		assertEquals("standard", check.get("type"));
		assertEquals("handler1", ((JSONArray) check.get("handlers")).get(0));
		assertEquals("handler2", ((JSONArray) check.get("handlers")).get(1));
		assertEquals("subscriber", ((JSONArray) check.get("subscribers")).get(0));
		assertEquals("client", json.get("client"));

	}
	
	@Test
	public void testCreateResultMultipleSubscribers() throws ParseException{
		String result = resultFactory.createResult("check_name ", 1, " test_output ", " handler1, handler2", 123456789L, "client ", "subscriber1, subscriber2");
		
		JSONObject json = (JSONObject) new JSONParser().parse(result);
		
		JSONObject check = (JSONObject) json.get("check");
		
		assertEquals(123456789L, check.get("executed"));
		assertEquals(1L, check.get("status"));
		assertEquals(123456789L, check.get("issued"));
		assertEquals("check_name", check.get("name"));
		assertEquals(false, check.get("publish"));
		assertEquals("test_output", check.get("output"));
		assertEquals(true, check.get("standalone"));
		assertEquals("standard", check.get("type"));
		assertEquals("handler1", ((JSONArray) check.get("handlers")).get(0));
		assertEquals("handler2", ((JSONArray) check.get("handlers")).get(1));
		assertEquals("subscriber1", ((JSONArray) check.get("subscribers")).get(0));
		assertEquals("subscriber2", ((JSONArray) check.get("subscribers")).get(1));
		assertEquals("client", json.get("client"));
		
	}
}

	
