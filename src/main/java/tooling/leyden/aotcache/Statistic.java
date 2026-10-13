package tooling.leyden.aotcache;

import jakarta.persistence.*;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import tooling.leyden.QuarkusPicocliLineApp;
import tooling.leyden.StatusMessage;

import java.util.Set;

@Entity
@Table(name = "statistic")
@IdClass(ConfigPK.class)
public class Statistic {

    @Id
    @Column(name = "stat_name", nullable = false)
    private String key;

    @Column(name = "stat_value")
    private Integer value;

    @Id
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "playground")
    private Playground playground;

    public Statistic() {
    }

    public Statistic(String configName, Integer value, Playground playground) {
        this.key = configName;
        this.value = value;
        this.playground = playground;
    }

    public Statistic(String configName, String value) {
        this(configName, Integer.valueOf(value), null);
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

    public Playground getPlayground() {
        return playground;
    }

    public void setPlayground(Playground playground) {
        this.playground = playground;
    }
}
