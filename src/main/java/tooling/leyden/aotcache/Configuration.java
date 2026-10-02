package tooling.leyden.aotcache;

import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import tooling.leyden.QuarkusPicocliLineApp;
import tooling.leyden.StatusMessage;

@Entity
@Table(name = "configurations")
public class Configuration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dbId;

    /** Logical name to distinguish configuration from statistics instances. */
    @Column(name = "config_name", nullable = false, unique = true)
    private String configName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "configuration_entries", joinColumns = @JoinColumn(name = "config_id"))
    @MapKeyColumn(name = "entry_key")
    @Column(name = "entry_value", length = 4096)
    private java.util.Map<String, String> entries = new java.util.concurrent.ConcurrentHashMap<>();

    public Configuration() {
    }

    public Configuration(String configName) {
        this.configName = configName;
    }

    public void addValue(String key, Object value) {
        String strValue = String.valueOf(value);
        String existing = entries.get(key.trim());
        if (existing != null && !existing.equals(strValue)) {
            QuarkusPicocliLineApp.addStatusMessage(new StatusMessage(System.currentTimeMillis(),
                    new AttributedString(
                            "Rewriting value for '" + key + "' previously it was '" + existing + "'.",
                            AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED))));
        }
        entries.put(key.trim(), strValue);
    }

    public void incrementValue(String key) {
        entries.merge(key, "1", (old, one) -> String.valueOf(Integer.parseInt(old) + 1));
    }

    public Object getValue(String key) {
        return entries.getOrDefault(key, "unknown");
    }

    public Object getValue(String key, Object defaultValue) {
        return entries.getOrDefault(key, String.valueOf(defaultValue));
    }

    public Set<String> getKeys() {
        return entries.keySet();
    }

    public void clear() {
        entries.clear();
    }
}
