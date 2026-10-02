package tooling.leyden.aotcache;

import java.util.Optional;

import io.quarkus.arc.Unremovable;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Unremovable
public class ConfigurationRepository implements PanacheRepository<Configuration> {

    public Optional<Configuration> findByName(String name) {
        return find("configName", name).firstResultOptional();
    }
}
