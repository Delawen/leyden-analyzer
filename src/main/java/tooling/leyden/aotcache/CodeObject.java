package tooling.leyden.aotcache;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * This class represents an element on the Code Cache
 */
public class CodeObject extends ReferencingElement {

    private Integer id = null;

    CodeObject(String identifier, String type) {
        super(identifier, type);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public AttributedString getDescription(String leftPadding, Boolean verbose, Boolean tips) {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.append(super.getDescription(leftPadding, verbose, tips));
        sb.append(AttributedString.NEWLINE);
        sb.append(leftPadding).append("This is part of the Code Cache. Code Cache ID is " + this.getId());

        return sb.toAttributedString();
    }

}
