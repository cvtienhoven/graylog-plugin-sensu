package org.graylog;

import org.graylog.RabbitMQClient;
import org.graylog.SensuAlarmCallback;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.alarms.AlertCondition.CheckResult;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackConfigurationException;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class SensuAlarmCallbackTest {

	private static final ImmutableMap<String, Object> VALID_CONFIG = ImmutableMap.<String, Object> builder()
			.put("check_name", "test_check_name")
			.put("check_client", "test_check_client")
			.put("check_handlers", "test_check_handler1,test_check_handler2")
			.put("check_severity", "2")
			.put("check_subscribers", "test_check_subscribers")
			.put("rabbitmq_user", "test_rabbitmq_user")
			.put("rabbitmq_password", "test_rabbitmq_password")
			.put("rabbitmq_virtual_host", "test_rabbitmq_virtual_host")
			.put("rabbitmq_hostname", "test_rabbitmq_hostname")
			.put("rabbitmq_port", 5671)
			.put("rabbitmq_use_ssl", false).build();

	private static final ImmutableMap<String, Object> VALID_CONFIG_DYNAMIC_CHECK = ImmutableMap
			.<String, Object> builder()
			.put("check_name", "[stream]")
			.put("check_client", "[source]")
			.put("check_handlers", "test_check_handler1,test_check_handler2")
			.put("check_severity", "2")
			.put("check_subscribers", "test_check_subscribers")
			.put("rabbitmq_user", "test_rabbitmq_user")
			.put("rabbitmq_password", "test_rabbitmq_password")
			.put("rabbitmq_virtual_host", "test_rabbitmq_virtual_host")
			.put("rabbitmq_hostname", "test_rabbitmq_hostname")
			.put("rabbitmq_port", 5671)
			.put("rabbitmq_use_ssl", false).build();

	private static final ImmutableMap<String, Object> INVALID_CONFIG = ImmutableMap.<String, Object> builder()
			.put("check_name", "test_check_name")
			.put("check_client", "test_check_client")
			.put("check_handlers", ",test_check_handler1,test_check_handler2")
			.put("check_level", "2")
			.put("rabbitmq_user", "test_rabbitmq_user")
			.put("rabbitmq_password", "test_rabbitmq_password")
			.put("rabbitmq_virtual_host", "test_rabbitmq_virtual_host")
			.put("rabbitmq_hostname", "test_rabbitmq_hostname")
			.put("rabbitmq_port", 5671)
			.put("rabbitmq_use_ssl", false).build();

	private static final Configuration VALID_CONFIGURATION = new Configuration(VALID_CONFIG);
	private static final Configuration VALID_CONFIGURATION_DYNAMIC_CHECK = new Configuration(VALID_CONFIG_DYNAMIC_CHECK);

	private SensuAlarmCallback alarmCallback;

	@Before
	public void setUp() {
		alarmCallback = new SensuAlarmCallback();
	}

	@Test
	public void testInitialize() throws AlarmCallbackConfigurationException {
		final Configuration configuration = new Configuration(VALID_CONFIG);
		alarmCallback.initialize(configuration);
	}

	@Test(expected = ConfigurationException.class)
	public void testConfigurationSucceedsWithInvalidConfiguration()
			throws AlarmCallbackConfigurationException, ConfigurationException {
		alarmCallback.initialize(new Configuration(INVALID_CONFIG));
		alarmCallback.checkConfiguration();
	}

	@Test
	public void testConfigurationSucceedsWithValidConfiguration()
			throws AlarmCallbackConfigurationException, ConfigurationException {
		alarmCallback.initialize(new Configuration(VALID_CONFIG));
		alarmCallback.checkConfiguration();
	}

	@Test
	public void testGetName() {
		assertEquals("Sensu Alarm Callback", alarmCallback.getName());
	}

	@Test
	public void testCall() throws AlarmCallbackConfigurationException, ConfigurationException, AlarmCallbackException,
			KeyManagementException, NoSuchAlgorithmException, IOException, TimeoutException {
		DateTime dateTime = new DateTime(2015, 11, 18, 12, 7, DateTimeZone.UTC);
		
		final RabbitMQClient client = mock(RabbitMQClient.class);
		final Stream stream = mockStream();
		final AlertCondition.CheckResult checkResult = mockCheckResult(dateTime);//mock(AlertCondition.CheckResult.class);
		final SensuResultFactory resultFactory = mock(SensuResultFactory.class);
		final AlertCondition alertcondition = mockAlertCondition();

		when(checkResult.getTriggeredCondition()).thenReturn(alertcondition);
		when(checkResult.getTriggeredCondition()).thenReturn(alertcondition);

		alarmCallback.initialize(VALID_CONFIGURATION);
		alarmCallback.setClient(client);
		alarmCallback.setResultFactory(resultFactory);
		alarmCallback.checkConfiguration();
		alarmCallback.call(stream, checkResult);

		
		String title = "Stream \"Stream title\" raised alert. \n";
		String description= "Alert description: Result description\n";
		String time = "Triggered at: 2015-11-18T12:07:00.000Z\n";
		String streamURL = "Stream URL: Parameter transport_email_web_interface_url not set in Sensu Alarm Callback Plugin\n\n";
		String messageBacklog = "Last messages accounting for this alert: \ntest_message1\n\n\ntest_message2\n\n\n";
		
		String output = title + description + time + streamURL + messageBacklog;
		
		verify(resultFactory).createResult(Mockito.eq("test_check_name"), Mockito.eq(2), Mockito.eq(output),
				Mockito.eq("test_check_handler1,test_check_handler2"), Mockito.anyLong(), Mockito.eq("test_check_client"), Mockito.eq("test_check_subscribers"));

		verify(client).send(Mockito.anyString());
	}

	@Test
	public void testCallDynamicCheck() throws AlarmCallbackConfigurationException, ConfigurationException, AlarmCallbackException,
			KeyManagementException, NoSuchAlgorithmException, IOException, TimeoutException {
		DateTime dateTime = new DateTime(2015, 11, 17, 12, 9, DateTimeZone.UTC);
		
		final RabbitMQClient client = mock(RabbitMQClient.class);
		final Stream stream = mockStream();
		final AlertCondition.CheckResult checkResult = mockCheckResult(dateTime);
		final SensuResultFactory resultFactory = mock(SensuResultFactory.class);
		final AlertCondition alertcondition = mockAlertCondition();

		when(checkResult.getTriggeredCondition()).thenReturn(alertcondition);

		alarmCallback.initialize(VALID_CONFIGURATION_DYNAMIC_CHECK);
		alarmCallback.setClient(client);
		alarmCallback.setResultFactory(resultFactory);
		alarmCallback.checkConfiguration();
		alarmCallback.call(stream, checkResult);

		String title = "Stream \"Stream title\" raised alert. \n";
		String description= "Alert description: Result description\n";
		String time = "Triggered at: 2015-11-17T12:09:00.000Z\n";
		String streamURL = "Stream URL: Parameter transport_email_web_interface_url not set in Sensu Alarm Callback Plugin\n\n";
		String messageBacklog = "Last messages accounting for this alert: \ntest_message1\n\n\ntest_message2\n\n\n";
		
		String output = title + description + time + streamURL + messageBacklog;
		
		verify(resultFactory).createResult(Mockito.eq("Stream-title"), Mockito.eq(2), Mockito.eq(output),
				Mockito.eq("test_check_handler1,test_check_handler2"), Mockito.anyLong(), Mockito.eq("test_source1"), Mockito.eq("test_check_subscribers"));
		
		verify(client).send(Mockito.anyString());
	}
	
	private AlertCondition mockAlertCondition() {
		final String alertConditionId = "alertConditionId";
		final AlertCondition alertCondition = mock(AlertCondition.class);
		when(alertCondition.getId()).thenReturn(alertConditionId);
		when(alertCondition.getDescription()).thenReturn("alert description");		
		return alertCondition;
	}

	private Stream mockStream() {
		// final String alertConditionId = "alertConditionId";
		final Stream stream = mock(Stream.class);
		when(stream.getTitle()).thenReturn("Stream title");
		return stream;
	}
	
	private CheckResult mockCheckResult(DateTime dateTime){
		final CheckResult result = mock(CheckResult.class);
		List<MessageSummary> messages = new ArrayList<MessageSummary>();
		
		Message message1 = mock(Message.class);
		when(message1.getId()).thenReturn("test_id1");
		when(message1.getSource()).thenReturn("test_source1");
		when(message1.getMessage()).thenReturn("test_message1");
		
		Message message2 = mock(Message.class);
		when(message2.getId()).thenReturn("test_id2");
		when(message2.getSource()).thenReturn("test_source2");
		when(message2.getMessage()).thenReturn("test_message2");

		
		MessageSummary messageSummary1 = new MessageSummary("index1", message1);
		messages.add(messageSummary1);
		
		MessageSummary messageSummary2 = new MessageSummary("index2", message2);
		messages.add(messageSummary2);
		
		when(result.getMatchingMessages()).thenReturn(messages);
		when(result.getTriggeredAt()).thenReturn(dateTime);
		when(result.getResultDescription()).thenReturn("Result description");
		return result;
	}
	
}
