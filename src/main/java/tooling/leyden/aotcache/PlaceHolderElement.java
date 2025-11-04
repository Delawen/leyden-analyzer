package tooling.leyden.aotcache;

//This should not exist after processing the full log, whatever that log is
public class PlaceHolderElement extends Element {
	@Override
	public String getKey() {
		return getAddress();
	}

	public PlaceHolderElement(String address) {
		this.setAddress(address);
		this.setType("Placeholder");
	}
}
