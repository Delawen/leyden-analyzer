package tooling.leyden.aotcache;


import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Elements that refer to other types of elements. For example: An element in the ConstantPool may be of certain
 * class, which is defined and loaded on the Information independently.
 **/
public class ReferencingElement extends Element {
	private Set<Element> references = new HashSet<>();
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ReferencingElement(){

	}

	public ReferencingElement(String name, String type) {
		this.setName(name);
		this.setType(type);
	}

	@Override
	public String getKey() {
		return name;
	}

	public List<Element> getReferences() {
		return this.references.stream().sorted(Comparator.comparing(Element::getType)).toList();
	}

	public void addReference(Element reference) {
		if (!this.references.contains(reference) && this != reference) {
			this.references.add(reference);
			reference.markAsReferenced(this);
		}
	}

	@Override
	public AttributedString getDescription(String leftPadding) {

		AttributedStringBuilder sb = new AttributedStringBuilder();
		sb.append(super.getDescription(leftPadding));

		if (!this.getReferences().isEmpty()) {
			sb.append('\n' + leftPadding + "This element refers to " + getReferences().size() + " other elements.");
		}

		return sb.toAttributedString();
	}
	public void resolvePlaceholders() {
		List<Element> refs = new ArrayList<>();
		refs.addAll(references);
		refs.replaceAll(element ->
						(element instanceof PlaceHolderElement) ?
								Information.getMyself().getByAddress(element.getAddress()) : element);
		references.clear();
		references.addAll(refs);
		references.remove(null);
	}
}
