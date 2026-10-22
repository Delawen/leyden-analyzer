package tooling.leyden.aotcache;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "playground")
public class Playground {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "dbId")
    private Long dbId;

    @Column(name = "identifier", nullable = false, unique = true)
    private String identifier;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "playground_sources",
            joinColumns = @JoinColumn(name = "identifier"),
            indexes = {
                    @Index(name = "playground_sources_identifier_index", columnList = "identifier")
            })
    @Column(name = "source", length = 1000, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<String> sources = new HashSet<>();

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Set<String> getSources() {
        return sources;
    }

    public void setSources(Set<String> sources) {
        this.sources = sources;
    }

    public Long getDbId() {
        return dbId;
    }

    public void setDbId(Long dbId) {
        this.dbId = dbId;
    }

    @Override
    public String toString() {
        return "Playground(" + this.getDbId() + ")[" + this.getIdentifier()  + "]";
    }
}
