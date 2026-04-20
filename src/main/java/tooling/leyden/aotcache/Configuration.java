package tooling.leyden.aotcache;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import tooling.leyden.QuarkusPicocliLineApp;
import tooling.leyden.StatusMessage;

public class Configuration {

    private final Map<String, Object> configuration = new ConcurrentHashMap<>();

    public void addValue(String key, Object value) {
        if (configuration.containsKey(key) && !configuration.get(key).equals(value)) {
            QuarkusPicocliLineApp.addStatusMessage(new StatusMessage(System.currentTimeMillis(),
                    new AttributedString(
                            "Rewriting value for '" + key + "' previously it was '" + configuration.get(key) + "'.",
                            AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED))));
        }
        configuration.put(key.trim(), value);
    }

    public void incrementValue(String key) {
        if (!configuration.containsKey(key)) {
            configuration.put(key, 0);
        }
        configuration.compute(key, (k, val) -> ((Integer) val) + 1);
    }

    public Object getValue(String key) {
        return configuration.getOrDefault(key, "unknown");
    }

    public Object getValue(String key, Object defaultValue) {
        return configuration.getOrDefault(key, defaultValue);
    }

    public Set<String> getKeys() {
        return configuration.keySet();
    }

    public void clear() {
        configuration.clear();
    }
}
