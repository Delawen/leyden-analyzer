package tooling.leyden.aotcache;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * This element represents an Object of the ConstantPool(Cache) inside the AOT Cache.
 */
public class ConstantPoolObject extends Element {
    private String constantPoolCacheAddress;
    private ClassObject poolHolder;
    private final String key;

    ConstantPoolObject(String key) {
        this.key = key;
        this.setType("ConstantPool");
    }

    public String getConstantPoolCacheAddress() {
        return constantPoolCacheAddress;
    }

    public void setConstantPoolCacheAddress(String constantPoolCacheAddress) {
        this.constantPoolCacheAddress = constantPoolCacheAddress;
    }

    public ClassObject getPoolHolder() {
        return poolHolder;
    }

    public void setPoolHolder(ClassObject poolHolder) {
        this.poolHolder = poolHolder;
        poolHolder.markAsReferenced(this);
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public AttributedString getDescription(String leftPadding, Boolean verbose, Boolean tips) {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.append(super.getDescription(leftPadding, verbose, tips));
        sb.append(AttributedString.NEWLINE);
        sb.append(leftPadding).append("ConstantPoolCache on address ");
        sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.CYAN));
        sb.append(getConstantPoolCacheAddress());
        sb.style(AttributedStyle.DEFAULT);
        return sb.toAttributedString();
    }
}
