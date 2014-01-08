package cz.cesnet.shongo.connector.api;

import org.joda.time.Duration;
import org.joda.time.Period;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Device options for connectors.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ConnectorOptions
{
    private Map<String, Object> options = new HashMap<String, Object>();

    /**
     * Sets an option to a given value.
     *
     * @param key   name of the option
     * @param value value to be set
     */
    public void set(String key, Object value)
    {
        options.put(key, value);
    }

    /**
     * Sets all options from a given option map.
     *
     * @param options map of option names to corresponding values
     */
    public void set(Map<String, Object> options)
    {
        for (Map.Entry<String, Object> entry : options.entrySet()) {
            set(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Unsets an option, i.e. deletes the option from configuration.
     *
     * @param key name of the option
     */
    public void unset(String key)
    {
        options.remove(key);
    }

    public Object getObject(String key)
    {
        return options.get(key);
    }

    public Object getObject(String key, Object defaultValue)
    {
        Object value = options.get(key);
        if (value == null) {
            return defaultValue;
        }
        else {
            return value;
        }
    }

    public String getString(String key)
    {
        return (String) getObject(key);
    }

    public String getString(String key, String defaultValue)
    {
        return (String) getObject(key, defaultValue);
    }

    public Integer getInt(String key)
    {
        return (Integer) getObject(key);
    }

    public Integer getInt(String key, Integer defaultValue)
    {
        return (Integer) getObject(key, defaultValue);
    }

    public Pattern getPattern(String key)
    {
        String h323Number = getString(key);
        if (h323Number != null) {
            return Pattern.compile(h323Number);
        }
        return null;
    }

    public Duration getDuration(String key, Duration defaultValue)
    {
        String duration = getString(key);
        if (duration != null) {
            return Duration.parse(duration);
        }
        return defaultValue;
    }
}
