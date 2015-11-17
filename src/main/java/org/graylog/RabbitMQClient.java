package org.graylog;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;


import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;

public class RabbitMQClient {

	private String userName;
	private String password;
	private String virtualHost;
	private String hostName;
	private int portNumber;
	private boolean useSSL;
	
	
	public RabbitMQClient(String userName, String password, String virtualHost, String hostName, int portNumber, boolean useSSL) {
		this.userName = userName;
		this.password = password;
		this.virtualHost = virtualHost;
		this.hostName = hostName;
		this.portNumber = portNumber;
		this.useSSL = useSSL;
	}
	
	public void send(String message) throws KeyManagementException, NoSuchAlgorithmException, IOException, TimeoutException{
		ConnectionFactory factory = new ConnectionFactory();
		
		if (useSSL){
			factory.useSslProtocol("SSL");
		}

		factory.setUsername(userName);
		factory.setPassword(password);
		factory.setVirtualHost(virtualHost);
		factory.setHost(hostName);
		factory.setPort(portNumber);
		
		Connection conn = factory.newConnection();

		Channel channel = conn.createChannel();

		channel.exchangeDeclare("keepalives", "direct", false);
		channel.queueDeclare("keepalives", false, false, true, null);
		channel.queueBind("keepalives", "keepalives", "");
		
		channel.exchangeDeclare("results", "direct", false);
		channel.queueDeclare("results", false, false, true, null);
		channel.queueBind("results", "results", "");
				
		channel.basicPublish("results", "", null, message.getBytes());

		channel.close();
		conn.close();
	}
}
