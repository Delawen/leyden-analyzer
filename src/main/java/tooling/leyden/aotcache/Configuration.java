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
@Table(name = "configuration")
public class Configuration {

    @Id
    @Column(name = "config_name", nullable = false, unique = true)
    private String key;


    @Column(name = "config_value")
    private String value;

    public Configuration() {
    }

    public Configuration(String configName, String value) {
        this.key = configName;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
