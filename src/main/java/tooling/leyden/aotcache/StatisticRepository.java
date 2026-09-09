package tooling.leyden.aotcache;

import io.quarkus.arc.Unremovable;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
@Unremovable
public class StatisticRepository implements PanacheRepository<Statistic> {

    public Optional<Statistic> findByName(String name) {
        return find("key", name).firstResultOptional();
    }

    public void createStatistic(String key) {
        if (findByName(key).isEmpty()) {
            Statistic s = new Statistic(key, 0);
            persist(s);
            flush();
        }
    }

    public void incrementStatistic(String key) {
        getEntityManager()
                .createNativeQuery(
                        "MERGE INTO statistic (stat_name, stat_value) " +
                                "KEY (stat_name) " +
                                "VALUES (:key, COALESCE((SELECT stat_value FROM statistic WHERE stat_name = :key), 0) + 1)")
                .setParameter("key", key)
                .executeUpdate();
    }
}