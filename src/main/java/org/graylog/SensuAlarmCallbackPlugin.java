package org.graylog;

import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;

import java.util.Arrays;
import java.util.Collection;

/**
 * Implement the Plugin interface here.
 */
public class SensuAlarmCallbackPlugin implements Plugin {
    @Override
    public PluginMetaData metadata() {
        return new SensuAlarmCallbackMetaData();
    }

    @Override
    public Collection<PluginModule> modules () {
        return Arrays.<PluginModule>asList(new SensuAlarmCallbackModule());
    }
}