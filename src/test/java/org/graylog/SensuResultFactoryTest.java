package org.graylog;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class SensuResultFactoryTest {
	private SensuResultFactory resultFactory;
	
	@Before
	public void setUp() {
		resultFactory = new SensuResultFactory();
	}
	
	@Test
	public void testCreateResultOneHandlerNoSubscribers(){
		
		String result = resultFactory.createResult("check_name", 1, "test_output", "handler", 123456789L, "client", "");
		assertEquals("{\"check\":"
				+ "{"
				+ "\"executed\":123456789,"
				+ "\"status\":1,"
				+ "\"issued\":123456789,"
				+ "\"name\":\"check_name\","
				+ "\"command\":\"none\","
				+ "\"publish\":false,"
				+ "\"output\":\"test_output\","
				+ "\"standalone\":true,"
				+ "\"type\":\"standard\","
				+ "\"handler\":\"handler\"},"
				+ "\"client\":\"client\"}",
				result
				);
	}
	
	@Test
	public void testCreateResultMultipleHandlersOneSubscriber(){
		String result = resultFactory.createResult("check_name ", 1, " test_output ", " handler1, handler2", 123456789L, "client ", "subscriber");
		assertEquals("{\"check\":"
				+ "{"
				+ "\"executed\":123456789,"
				+ "\"status\":1,"
				+ "\"issued\":123456789,"
				+ "\"name\":\"check_name\","
				+ "\"command\":\"none\","
				+ "\"publish\":false,"
				+ "\"subscribers\":[\"subscriber\"],"
				+ "\"output\":\"test_output\","
				+ "\"standalone\":true,"
				+ "\"type\":\"standard\","
				+ "\"handlers\":[\"handler1\",\"handler2\"]},"
				+ "\"client\":\"client\"}",
				result
				);
	}
	
	@Test
	public void testCreateResultMultipleSubscribers(){
		String result = resultFactory.createResult("check_name ", 1, " test_output ", " handler1, handler2", 123456789L, "client ", "subscriber1, subscriber2");
		assertEquals("{\"check\":"
				+ "{"
				+ "\"executed\":123456789,"
				+ "\"status\":1,"
				+ "\"issued\":123456789,"
				+ "\"name\":\"check_name\","
				+ "\"command\":\"none\","
				+ "\"publish\":false,"
				+ "\"subscribers\":[\"subscriber1\",\"subscriber2\"],"
				+ "\"output\":\"test_output\","
				+ "\"standalone\":true,"
				+ "\"type\":\"standard\","
				+ "\"handlers\":[\"handler1\",\"handler2\"]},"
				+ "\"client\":\"client\"}",
				result
				);
	}
}

	
