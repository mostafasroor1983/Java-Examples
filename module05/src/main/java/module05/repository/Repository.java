package module05.repository;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Generic repository contract: T is the entity type, ID its identifier type. */
public interface Repository<T extends Entity<ID>, ID> {
    T save(T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    List<T> findWhere(Predicate<? super T> predicate);

    boolean deleteById(ID id);

    long count();
}
