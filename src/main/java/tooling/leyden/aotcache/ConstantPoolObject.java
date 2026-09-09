package tooling.leyden.aotcache;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * This element represents an Object of the ConstantPool(Cache) inside the AOT Cache.
 */
@Entity
@DiscriminatorValue("ConstantPoolObject")
public class ConstantPoolObject extends Element {
    private String constantPoolCacheAddress;

    @Column(name = "cp_pool_holder_key")
    private String poolHolderKey;

    public ConstantPoolObject() {
    }

    ConstantPoolObject(String key) {
        setKey(key);
        this.setType("ConstantPool");
    }

    public String getConstantPoolCacheAddress() {
        return constantPoolCacheAddress;
    }

    public void setConstantPoolCacheAddress(String constantPoolCacheAddress) {
        this.constantPoolCacheAddress = constantPoolCacheAddress;
    }

    public ClassObject getPoolHolder() {
        if (poolHolderKey == null) return null;
        return (ClassObject) Information.getMyself()
                .getElements(poolHolderKey, null, null, true, "Class")
                .findAny().orElse(null);
    }

    public void setPoolHolder(ClassObject poolHolder) {
        this.poolHolderKey = poolHolder.getKey();
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
