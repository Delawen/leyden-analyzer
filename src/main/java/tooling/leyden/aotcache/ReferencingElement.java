package tooling.leyden.aotcache;

import java.util.*;

import jakarta.persistence.*;

/**
 * Elements that refer to other types of elements. For example: An element in the ConstantPool may be of certain
 * class, which is defined and loaded on the Information independently.
 **/
@Entity
@DiscriminatorValue("ReferencingElement")
public class ReferencingElement extends Element {

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "element_references",
        joinColumns = @JoinColumn(name = "source_id"),
        inverseJoinColumns = @JoinColumn(name = "target_id"),
            indexes = {
                    @Index(name = "idx_references_source_id", columnList = "source_id"),
                    @Index(name = "idx_references_target_id", columnList = "target_id"),
                    @Index(name = "idx_references", columnList = "source_id, target_id"),
        }
    )
    private final Set<Element> references = new HashSet<>();

    @jakarta.persistence.Column(length = 4096)
    protected String name;

    public ReferencingElement() {
    }

    public ReferencingElement(String name, String type) {
        this.setName(name);
        this.setKey(name);
        this.setType(type);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Element> getReferences() {
        return this.references;
    }

    public void addReference(Element reference) {
        Information.getMyself().addRelationship(this, reference);
    }
}
