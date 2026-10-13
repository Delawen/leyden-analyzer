package tooling.leyden.aotcache;

import java.util.Optional;

import io.quarkus.arc.Unremovable;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Unremovable
public class ConfigurationRepository implements PanacheRepository<Configuration> {

    public Optional<Configuration> findByName(String name) {
        return find("key", name).firstResultOptional();
    }

    public void deleteAll(Playground playground) {
        getEntityManager()
                .createNativeQuery("DELETE FROM Configuration WHERE playground = :playground")
                .setParameter("playground", playground.getDbId())
                .executeUpdate();
        flush();
    }
}
