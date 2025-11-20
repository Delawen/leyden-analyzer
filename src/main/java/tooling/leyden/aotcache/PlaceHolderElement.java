package tooling.leyden.aotcache;

import jakarta.persistence.Entity;

//This should not exist after processing the full log, whatever that log is
@Entity
public class PlaceHolderElement extends Element {
	public PlaceHolderElement() {
	}

	public PlaceHolderElement(String address) {
		this.setAddress(address);
		this.setIdentifier(address);
		this.setType("Placeholder");
	}
}
