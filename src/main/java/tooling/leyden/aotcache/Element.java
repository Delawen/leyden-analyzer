package tooling.leyden.aotcache;


import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Elements that can be found on the Information.
 **/
public abstract class Element {

	private String type;
	private Boolean isHeapRoot = false;
	private WhichRun loaded = WhichRun.None;
	private List<String> whereDoesItComeFrom = new ArrayList<>();
	private List<String> source = new ArrayList<>();
	private Set<Element> whoReferencesMe = new HashSet<>();
	/**
	 * Address where an element can be found
	 */
	private String address;

	public Boolean isHeapRoot() {
		return isHeapRoot;
	}

	public void setHeapRoot(Boolean heapRoot) {
		isHeapRoot = heapRoot;
	}

	/**
	 * Do we know why this element was stored in the cache?
	 *
	 * @return reason why it was stored
	 */
	public List<String> getWhereDoesItComeFrom() {
		return whereDoesItComeFrom;
	}

	public final void addWhereDoesItComeFrom(String whereDoesItComeFrom) {
		this.whereDoesItComeFrom.add(whereDoesItComeFrom);
	}

	/**
	 * Is this a class, a method,...?
	 *
	 * @return The type of element
	 */
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	/**
	 * When describing an element, this is the String we are going to use.
	 *
	 * @return A complete description of this element.
	 */
	public AttributedString getDescription(String leftPadding, Boolean verbose, Boolean tips) {

		AttributedStringBuilder sb = new AttributedStringBuilder();
		sb.append(leftPadding);
		sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.YELLOW));
		sb.append(getType());
		sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.CYAN));
		sb.append(" " + getKey());
		sb.style(AttributedStyle.DEFAULT);
		if (getAddress() != null) {
			sb.append(" on address ");
			sb.style(AttributedStyle.DEFAULT.bold());
			sb.append(address);
			sb.style(AttributedStyle.DEFAULT);
		}
		if (getSize() != null) {
			sb.append(" with size ");
			sb.style(AttributedStyle.DEFAULT.bold());
			sb.append(getSize().toString());
			sb.style(AttributedStyle.DEFAULT);
		}
		sb.append(".");
		if (isHeapRoot()) {
			sb.append(AttributedString.NEWLINE);
			sb.append(leftPadding + "This is a ");
			sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN));
			sb.append("HEAP ROOT");
			sb.style(AttributedStyle.DEFAULT);
			sb.append(" element.");
		}

		if (wasLoaded() != WhichRun.None) {
			sb.append(AttributedString.NEWLINE);
			sb.append(leftPadding + "This asset was loaded into memory for usage during");
			sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN));
			switch (wasLoaded()) {
				case Training -> sb.append(" training run");
				case Production -> sb.append(" production run");
				default -> sb.append(" both training and production runs");
			}
			sb.style(AttributedStyle.DEFAULT);
			sb.append(".");
		}

		return sb.toAttributedString();
	}

	/**
	 * Size that is written on the description of the object. On the following example, 600:
	 * 0x0000000800001d80: @@ TypeArrayU1       600
	 */
	private Integer size = null;

	public Integer getSize() {
		return size;
	}

	public void setSize(Integer size) {
		this.size = size;
	}

	public Collection<Element> getWhoReferencesMe() {
		return whoReferencesMe;
	}

	public final void markAsReferenced(Element e) {
		if (e != this) {
			this.whoReferencesMe.add(e);
		}
	}

	/**
	 * Used to search for this element. For example, on classes this would be the full qualified name of the class.
	 *
	 * @return The key that identifies the element
	 */
	public abstract String getKey();


	public void addSource(String source) {
		if (!this.source.contains(source)) {
			this.source.add(source);
		}
	}

	/**
	 * Used to understand why this element is added to the cache. There may be more than one source of information
	 * for this element.
	 *
	 * @return Where this element comes from
	 */
	public List<String> getSources() {
		return this.source;
	}

	public WhichRun wasLoaded() {
		return loaded;
	}

	public void setLoaded(WhichRun loaded) {
		if ((loaded == WhichRun.Production && this.loaded == WhichRun.Training)
				|| (this.loaded == WhichRun.Production && loaded == WhichRun.Training)) {
			this.loaded = WhichRun.Both;
		} else if (this.loaded != WhichRun.Both){
			this.loaded = loaded;
		}
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public boolean isTrained() {
		return false;
	}

	public boolean isTraineable() {
		return false;
	}

	public AttributedString toAttributedString() {
		AttributedStringBuilder sb = new AttributedStringBuilder();

		String padding = "";

		if (Information.getMyself().cacheContains(this)) {
			sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN));
			sb.append("[Cached]");
			padding += "  ";
		} else {
			sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED));
			sb.append("[Uncached]");
		}

		if (isTraineable()) {
			if (isTrained()) {
				sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN));
				sb.append("[Trained]");
				padding += "  ";
			} else {
				sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED));
				sb.append("[Untrained]");
			}
		}

		sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.YELLOW));
		sb.append("[" + getType() + "] ");
		sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.CYAN));
		sb.append(padding);
		sb.append(getKey());
		return sb.toAttributedString();
	}

	@Override
	public String toString() {
		return toAttributedString().toString();
	}

	@Override
	public int hashCode() {
		int result = Objects.hashCode(getType());
		result = 31 * result + Objects.hashCode(getKey());
		return result;
	}

	@Override
	public final boolean equals(Object o) {
		if (!(o instanceof Element element))
			return false;

		return Objects.equals(getType(), element.getType()) && Objects.equals(getKey(), element.getKey());
	}

	public enum WhichRun {
		None, Training, Production, Both
	}
}
