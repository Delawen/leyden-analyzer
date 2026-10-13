package tooling.leyden.aotcache;

import java.util.Set;

import jakarta.persistence.*;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import tooling.leyden.QuarkusPicocliLineApp;
import tooling.leyden.StatusMessage;

@Entity
@Table(name = "configuration")
@IdClass(ConfigPK.class)
public class Configuration {

    @Id
    @Column(name = "config_name", nullable = false)
    private String key;

    @Column(name = "config_value")
    private String value;

    @Id
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "playground")
    private Playground playground;

    public Configuration() {
    }


    public Configuration(String configName, String value) {
        this(configName, value, null);
    }

    public Configuration(String configName, String value, Playground playground) {
        this.key = configName;
        this.value = value;
        this.playground = playground;
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

    public Playground getPlayground() {
        return playground;
    }

    public void setPlayground(Playground playground) {
        this.playground = playground;
    }
}
