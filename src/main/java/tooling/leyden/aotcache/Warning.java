package tooling.leyden.aotcache;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents errors in storing or loading elements to/from the cache.
 */
@Entity
public class Warning {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	private WarningType type;

	/**
	 * Element that suffered the problem.
	 */
	@OneToMany
	private List<Element> element;

	/**
	 * String ready to be printed regarding this error.
	 */
	@Transient
	private AttributedString message;

	private Boolean auto;

	public Warning(List<Element> e, AttributedString message, WarningType type) {
		this.element = new ArrayList<>();
		this.element.addAll(e);
		//If the element is a Symbol with a class, add the class to the list too:
		List.copyOf(this.element).stream()
				.filter(el -> el.getType().equalsIgnoreCase("Symbol"))
				.forEach(el -> ((ReferencingElement)el).getReferences().stream()
						.filter(c -> c.getType().equalsIgnoreCase("Class"))
						.forEach(this.element::add));
		this.type = type;
		this.message = message;
	}

	public Warning(Element e, AttributedString message, WarningType type) {
		this((e != null ? List.of(e) : List.of()), message, type);
	}

	public Warning(Element element, String description, WarningType type) {
		this((element != null ? List.of(element) : List.of()), new AttributedString(description), type);
	}

	public Warning(String description) {
		this(List.of(), new AttributedString(description), WarningType.Unknown);
	}

	public Warning() {

	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public WarningType getType() {
		return type;
	}

	public boolean affects(String id) {
		return !this.element.isEmpty()
				&& this.element.stream().anyMatch(element -> element.getIdentifier().equalsIgnoreCase(id));
	}

	public AttributedString getDescription() {

		AttributedStringBuilder sb = new AttributedStringBuilder();
		sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.CYAN));
		sb.append(String.format("%04d", id));
		sb.style(AttributedStyle.DEFAULT);
		sb.append(" [");
		sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.YELLOW));
		sb.append(this.type.name());
		sb.style(AttributedStyle.DEFAULT);
		sb.append("] ");
		sb.append(this.message);
		return sb.toAttributedString();
	}

	public String toString() {
		return message.toString();
	}

	public Boolean getAuto() {
		return auto;
	}

	public void setAuto(Boolean auto) {
		this.auto = auto;
	}
}
