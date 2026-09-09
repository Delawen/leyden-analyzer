package tooling.leyden.aotcache;

import jakarta.persistence.*;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import tooling.leyden.QuarkusPicocliLineApp;
import tooling.leyden.StatusMessage;

import java.util.Set;

@Entity
@Table(name = "statistic")
public class Statistic {

    @Id
    @Column(name = "stat_name", nullable = false, unique = true)
    private String key;


    @Column(name = "stat_value")
    private Integer value;

    public Statistic() {
    }

    public Statistic(String configName, Integer value) {
        this.key = configName;
        this.value = value;
    }

    public Statistic(String configName, String value) {
        this(configName, Integer.valueOf(value));
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }
}
