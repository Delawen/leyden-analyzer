package tooling.leyden.aotcache;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

//This should not exist after processing the full log, whatever that log is
@Entity
@DiscriminatorValue("PlaceHolderElement")
public class PlaceHolderElement extends Element {

    public PlaceHolderElement() {
    }

    public PlaceHolderElement(String address) {
        this.setAddress(address);
        this.setKey(address);
        this.setType("Placeholder");
    }

    @Override
    public String toString() {
        return "[" + this.isInCache() + "]" + this.getKey();
    }
}
