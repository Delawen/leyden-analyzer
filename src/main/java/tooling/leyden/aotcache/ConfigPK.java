package tooling.leyden.aotcache;

import java.io.Serializable;
import java.util.Objects;

public class ConfigPK implements Serializable {
    protected String key;
    protected Playground playground;

    public ConfigPK() {}

    public ConfigPK(String key, Playground playground) {
        this.key = key;
        this.playground = playground;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ConfigPK configPK)) return false;
        return Objects.equals(key, configPK.key) && Objects.equals(playground, configPK.playground);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, playground);
    }
}