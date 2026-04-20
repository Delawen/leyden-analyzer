package tooling.leyden.aotcache;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * This class represents errors in storing or loading elements to/from the cache.
 */
public class Warning {

    private String id;

    private final WarningType type;

    /**
     * Element that suffered the problem.
     */
    private final List<Element> element;

    /**
     * String ready to be printed regarding this error.
     */
    private final AttributedString message;

    private final static AtomicInteger idGenerator = new AtomicInteger();

    public Warning(List<Element> e, AttributedString message, WarningType type) {
        this.element = new ArrayList<>();
        this.element.addAll(e);
        //If the element is a Symbol with a class, add the class to the list too:
        List.copyOf(this.element).stream()
                .filter(el -> el.getType().equalsIgnoreCase("Symbol"))
                .forEach(el -> ((ReferencingElement) el).getReferences().stream()
                        .filter(c -> c.getType().equalsIgnoreCase("Class"))
                        .forEach(this.element::add));
        this.type = type;
        this.message = message;
        this.setId(idGenerator.getAndIncrement());
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

    public String getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = String.format("%04d", id);
    }

    public WarningType getType() {
        return type;
    }

    public boolean affects(String id) {
        return !this.element.isEmpty()
                && this.element.stream().anyMatch(element -> element.getKey().equalsIgnoreCase(id));
    }

    public AttributedString getDescription() {

        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.CYAN));
        sb.append(this.getId());
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
}
