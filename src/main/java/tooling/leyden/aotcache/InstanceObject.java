package tooling.leyden.aotcache;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

public class InstanceObject extends ReferencingElement {
    private Boolean isAOTinited = false;
    private ClassObject instanceOf;

    public InstanceObject(String identifier) {
        super(identifier, "Object");
    }

    public Boolean isAOTinited() {
        return isAOTinited;
    }

    public void setAOTinited(Boolean AOTinited) {
        isAOTinited = AOTinited;
    }

    /**
     * When describing an element, this is the String we are going to use.
     *
     * @return A complete description of this element.
     */
    @Override
    public AttributedString getDescription(String leftPadding, Boolean verbose, Boolean tips) {

        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.append(super.getDescription(leftPadding, verbose, tips));
        sb.append(AttributedString.NEWLINE);
        sb.append(leftPadding).append("This instance object is ");

        if (isAOTinited()) {
            sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN));
            sb.append("AOT inited");
        } else {
            sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED));
            sb.append("not AOT inited");
        }
        sb.style(AttributedStyle.DEFAULT);
        sb.append(".");

        return sb.toAttributedString();
    }

    public ClassObject getInstanceOf() {
        return instanceOf;
    }

    public void setInstanceOf(ClassObject classObject) {
        this.instanceOf = classObject;
        addReference(classObject);
    }
}
