package tooling.leyden.aotcache;

import io.quarkus.arc.Unremovable;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
@Unremovable
public class PlaygroundRepository implements PanacheRepository<Playground> {

    public Optional<Playground> findByIdentifier(String name) {
        return find("identifier", name).firstResultOptional();
    }

}