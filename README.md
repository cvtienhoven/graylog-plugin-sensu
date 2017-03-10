# SensuAlarmCallback Plugin for Graylog

[![Build Status](https://travis-ci.org/cvtienhoven/graylog-plugin-sensu.svg?branch=master)](https://travis-ci.org/cvtienhoven/graylog-plugin-sensu)


**Required Graylog version:** 2.0 and later


This plugin enables you to send events to a Sensu server via the RabbitMQ broker. The structure of
the event data is much like the data you would receive in the `Email Alert Callback`.

![](https://github.com/cvtienhoven/graylog-plugin-sensu/blob/master/images/uchiwa.png)

![](https://github.com/cvtienhoven/graylog-plugin-sensu/blob/master/images/callback.png)

## Installation

[Download the plugin](https://github.com/cvtienhoven/graylog-plugin-sensu/releases)
and place the `.jar` file in your Graylog plugin directory. The plugin directory
is the `plugins/` folder relative from your `graylog-server` directory by default
and can be configured in your `graylog.conf` file.

Restart `graylog-server` and you are done.

## Use cases

This plugin is useful when your organization has adopted the Sensu platform for monitoring/alerting
and you'd like Sensu to handle your Graylog stream alerts as well. This way, you can keep your alert 
handling all in one place.

## Usage

### Configure the alarm callback

You can configure an alert condition in Graylog and add the `Sensu Alarm Callback` as the Callback Type. 
In the popup that occurs you can configure the connection to the RabbitMQ broker. The following settings
manage how an alert will be presented in the main dashboard (client name/check name).

**check_client**: The name of the client as shown in Sensu, e.g. `Graylog Production`. You can enter `[source]` 
to make the plugin try to fetch the source name from the first message of the supplied message backlog. To 
make this work, you'll need to make the alert condition include 1 or more messages of the stream. If set to 
`[source]`, but no source could be extracted, the client name will be set to `graylog`.

**check_name**: The name of the check as shown in Sensu. In the case of alerts from Graylog, there are no pre-defined
Sensu checks. However, the plugin sends events that need a name for the a check. You can enter a name for the 
check, but to give it the same name as the stream that triggers the alert, enter `[stream]` for this parameter. 

For more information on checks, handlers, subscribers etc., [visit the Sensu website](https://sensuapp.org).

### Auto resolve alerts in Sensu

The plugin now also supports tags. You can use the tag ```auto_resolve_time=X``` and the handler ```auto_resolve``` in combination
with this Sensu handler script: https://github.com/cvtienhoven/sensu-plugin-auto-resolve. With this configuration, you can set ```X``` 
to a number of seconds after which you'd like the alert to be resolved in Sensu.


Getting started
---------------

This project is using Maven 3 and requires Java 8 or higher.

* Clone this repository.
* Run `mvn package` to build a JAR file.
* Optional: Run `mvn jdeb:jdeb` and `mvn rpm:rpm` to create a DEB and RPM package respectively.
* Copy generated JAR file in target directory to your Graylog plugin directory.
* Restart the Graylog.

Plugin Release
--------------

We are using the maven release plugin:

```
$ mvn release:prepare
[...]
$ mvn release:perform
```

This sets the version numbers, creates a tag and pushes to GitHub. Travis CI will build the release artifacts and upload to GitHub automatically.
