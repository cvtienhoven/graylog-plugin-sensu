package org.graylog;

import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import java.util.Arrays;
import java.util.HashMap;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.Tools;

import org.graylog2.plugin.alarms.AlertCondition.CheckResult;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackConfigurationException;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import static com.google.common.base.Strings.isNullOrEmpty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;

public class SensuAlarmCallback implements AlarmCallback {
	private static final String CHECK_NAME = "check_name";
	private static final String CHECK_CLIENT = "check_client";
	private static final String CHECK_HANDLERS = "check_handlers";
	private static final String CHECK_SEVERITY = "check_severity";
	private static final String CHECK_SUBSCRIBERS = "check_subscribers";

	private static final String TRANSPORT_EMAIL_WEB_INTERFACE_URL = "transport_email_web_interface_url";

	private static final String RABBITMQ_USER = "rabbitmq_user";
	private static final String RABBITMQ_PASSWORD = "rabbitmq_password";
	private static final String RABBITMQ_VIRTUAL_HOST = "rabbitmq_virtual_host";
	private static final String RABBITMQ_HOSTNAME = "rabbitmq_hostname";
	private static final String RABBITMQ_PORT = "rabbitmq_port";
	private static final String RABBITMQ_USE_SSL = "rabbitmq_use_ssl";

	private Configuration configuration;
	private RabbitMQClient client;
	private SensuResultFactory resultFactory;

	@Override
	public void call(Stream stream, CheckResult result) throws AlarmCallbackException {
		String title = "Stream \"" + stream.getTitle() + "\" raised alert. \n";
		String alert_description = "Alert description: " + result.getResultDescription() + "\n";
		String time = "Triggered at: " + result.getTriggeredAt() + "\n";

		String streamURL = "Stream URL: Parameter transport_email_web_interface_url not set in Sensu Alarm Callback Plugin\n\n";
		if (!isNullOrEmpty(configuration.getString(TRANSPORT_EMAIL_WEB_INTERFACE_URL))) {
			streamURL = "Stream URL: " + buildStreamDetailsURL(
					URI.create(configuration.getString(TRANSPORT_EMAIL_WEB_INTERFACE_URL)), result, stream) + "\n";
		}

		String messageBacklog = "Last messages accounting for this alert: \n";
		if (result.getMatchingMessages().size() == 0) {
			messageBacklog += "No message backlog available.\n";
		} else {

			for (MessageSummary message : result.getMatchingMessages()) {
				messageBacklog += message.getMessage() + "\n\n\n";
			}
		}

		String output = title + alert_description + time + streamURL + messageBacklog;

		// if set to [source], try to extract the source from the first message
		// of the backlog and use it for the client name.
		String client_name = configuration.getString(CHECK_CLIENT);
		if ("[source]".equals(client_name)) {
			if (result.getMatchingMessages().size() > 0) {
				MessageSummary message = result.getMatchingMessages().get(0);
				client_name = message.getSource();
			} else {
				// no messages found - let's set it to "graylog" for now.
				client_name = "graylog";
			}
		}

		// if set to [stream], set the value of check_name to the stream title.
		String check_name = configuration.getString(CHECK_NAME);
		if ("[stream]".equals(check_name)) {
			check_name = stream.getTitle();
		}
		
		long timestamp = new DateTime().getMillis()/1000;
		
		String sensuResult = resultFactory.createResult(check_name, Integer.parseInt(configuration.getString(CHECK_SEVERITY)), output,
				configuration.getString(CHECK_HANDLERS), timestamp, client_name,			
				configuration.getString(CHECK_SUBSCRIBERS));

		try {
			client.send(sensuResult);
		} catch (KeyManagementException | NoSuchAlgorithmException | IOException | TimeoutException e) {
			e.printStackTrace();
			throw new AlarmCallbackException(e.getMessage());
		}

	}

	

	@Override
	public void checkConfiguration() throws ConfigurationException {
		if (configuration.getString(CHECK_HANDLERS).contains(",")) {
			String[] handlers = configuration.getString(CHECK_HANDLERS).split(",");
			for (String handler : handlers) {
				if (handler.trim().equals("")) {
					throw new ConfigurationException("Cannot submit empty handler.");
				}
			}
		}
		if (configuration.getString(CHECK_SUBSCRIBERS).contains(",")) {
			String[] subscribers = configuration.getString(CHECK_SUBSCRIBERS).split(",");
			for (String subscriber : subscribers) {
				if (subscriber.trim().equals("")) {
					throw new ConfigurationException("Cannot submit empty subscriber.");
				}
			}
		}
		if (!configuration.stringIsSet(CHECK_NAME)) {
			throw new ConfigurationException(CHECK_NAME + " is mandatory and must be not be null or empty.");
		}
		if (!configuration.stringIsSet(CHECK_CLIENT)) {
			throw new ConfigurationException(CHECK_CLIENT + " is mandatory and must be not be null or empty.");
		}
		if (!configuration.stringIsSet(CHECK_HANDLERS)) {
			throw new ConfigurationException(CHECK_HANDLERS + " is mandatory and must be not be null or empty.");
		}
		if (!configuration.stringIsSet(CHECK_SEVERITY)) {
			throw new ConfigurationException(CHECK_SEVERITY + " is mandatory and must be not be null or empty.");
		}
		if (!configuration.stringIsSet(RABBITMQ_USER)) {
			throw new ConfigurationException(RABBITMQ_USER + " is mandatory and must be not be null or empty.");
		}
		if (!configuration.stringIsSet(RABBITMQ_VIRTUAL_HOST)) {
			throw new ConfigurationException(RABBITMQ_VIRTUAL_HOST + " is mandatory and must be not be null or empty.");
		}
		if (!configuration.stringIsSet(RABBITMQ_HOSTNAME)) {
			throw new ConfigurationException(RABBITMQ_HOSTNAME + " is mandatory and must be not be null or empty.");
		}
		if (!configuration.intIsSet(RABBITMQ_PORT)) {
			throw new ConfigurationException(RABBITMQ_PORT + " is mandatory and must be not be null or empty.");
		}

	}

	@Override
	public Map<String, Object> getAttributes() {
		return Maps.transformEntries(configuration.getSource(), new Maps.EntryTransformer<String, Object, Object>() {
			@Override
			public Object transformEntry(String key, Object value) {
				if (RABBITMQ_PASSWORD.equals(key)) {
					return "****";
				}
				return value;
			}
		});
	}

	@Override
	public String getName() {
		return "Sensu Alarm Callback";
	}

	@Override
	public ConfigurationRequest getRequestedConfiguration() {
		final ConfigurationRequest configurationRequest = new ConfigurationRequest();
		configurationRequest.addField(new TextField(RABBITMQ_HOSTNAME, "RabbitMQ Host", "",
				"The hostname or IP address of the RabbitMQ server.", ConfigurationField.Optional.NOT_OPTIONAL));
		configurationRequest.addField(new NumberField(RABBITMQ_PORT, "RabbitMQ Port", 5671,
				"The port on which RabbitMQ listens.", ConfigurationField.Optional.NOT_OPTIONAL));
		configurationRequest.addField(new TextField(RABBITMQ_USER, "RabbitMQ User", "sensu",
				"The user to connect to RabbitMQ.", ConfigurationField.Optional.NOT_OPTIONAL));
		configurationRequest.addField(
				new TextField(RABBITMQ_PASSWORD, "RabbitMQ Password", "", "The password to connect to RabbitMQ.",
						ConfigurationField.Optional.OPTIONAL, TextField.Attribute.IS_PASSWORD));
		configurationRequest.addField(new TextField(RABBITMQ_VIRTUAL_HOST, "RabbitMQ Virtual Host", "/sensu",
				"The VHost for Sensu on RabbitMQ.", ConfigurationField.Optional.NOT_OPTIONAL));
		configurationRequest.addField(
				new BooleanField(RABBITMQ_USE_SSL, "Use SSL", false, "Enable to use SSL for connecting to RabbitMQ"));
		configurationRequest.addField(new TextField(CHECK_NAME, "Check title", "",
				"The name of the check as shown in Sensu. Enter \"[stream]\" to use the stream name.",
				ConfigurationField.Optional.NOT_OPTIONAL));
		configurationRequest.addField(new TextField(CHECK_CLIENT, "Check client", "",
				"The name of the client as shown in Sensu. Enter \"[source]\" to try to fetch the source from the first message of the backlog.", ConfigurationField.Optional.NOT_OPTIONAL));
		configurationRequest.addField(new TextField(CHECK_HANDLERS, "Sensu handlers", "default",
				"The handler(s) in Sensu that takes care of this result (comma separated if multiple handlers).",
				ConfigurationField.Optional.NOT_OPTIONAL));
		configurationRequest.addField(new TextField(CHECK_SUBSCRIBERS, "Sensu subscribers", "",
				"The subscriber(s) in Sensu that takes care of this result (comma separated if multiple subscribers).",
				ConfigurationField.Optional.OPTIONAL));

		Map<String, String> levels = new HashMap<String, String>();
		levels.put("0", "OK");
		levels.put("1", "Warning");
		levels.put("2", "Critical");

		configurationRequest.addField(new DropdownField(CHECK_SEVERITY, "Severity", "Critical", levels,
				"The severity of the event.", ConfigurationField.Optional.NOT_OPTIONAL));

		// this has to be added because the global config containing the mail
		// stream url is not accessible in the plugin API.
		configurationRequest.addField(new TextField(TRANSPORT_EMAIL_WEB_INTERFACE_URL,
				"Transport Email Web Interface Url", "http://graylog.example.com",
				"Should be the same as transport_email_web_interface_url config parameter. If not supplied, stream link won't be included.",
				ConfigurationField.Optional.OPTIONAL));

		return configurationRequest;
	}

	@Override
	public void initialize(Configuration config) throws AlarmCallbackConfigurationException {
		this.configuration = config;

		setClient(new RabbitMQClient(configuration.getString(RABBITMQ_USER), configuration.getString(RABBITMQ_PASSWORD),
				configuration.getString(RABBITMQ_VIRTUAL_HOST), configuration.getString(RABBITMQ_HOSTNAME),
				configuration.getInt(RABBITMQ_PORT), configuration.getBoolean(RABBITMQ_USE_SSL)));
		
		setResultFactory(new SensuResultFactory());
	}

	@VisibleForTesting
	void setClient(RabbitMQClient client) {
		this.client = client;
	}
	
	@VisibleForTesting
	void setResultFactory(SensuResultFactory resultFactory) {
		this.resultFactory = resultFactory;
	}

	protected String buildStreamDetailsURL(URI baseUri, CheckResult checkResult, Stream stream) {

		int time = 5;
		if (checkResult.getTriggeredCondition().getParameters().get("time") != null) {
			time = (int) checkResult.getTriggeredCondition().getParameters().get("time");
		}

		DateTime dateAlertEnd = checkResult.getTriggeredAt();
		DateTime dateAlertStart = dateAlertEnd.minusMinutes(time);
		String alertStart = Tools.getISO8601String(dateAlertStart);
		String alertEnd = Tools.getISO8601String(dateAlertEnd);

		return baseUri + "/streams/" + stream.getId() + "/messages?rangetype=absolute&from=" + alertStart + "&to="
				+ alertEnd + "&q=*";
	}
}
