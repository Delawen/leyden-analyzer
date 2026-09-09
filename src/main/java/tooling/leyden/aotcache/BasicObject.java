package tooling.leyden.aotcache;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * This element represents a basic object like a record, annotation,... inside the AOT Cache.
 * They don't offer much information on the AOT map file.
 *
 */
@Entity
@DiscriminatorValue("BasicObject")
public class BasicObject extends Element {

    public BasicObject() {
        setKey(null);
    }

    BasicObject(String key) {
        setKey(key);
    }
}
