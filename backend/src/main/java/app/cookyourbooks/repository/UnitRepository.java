package app.cookyourbooks.repository;

import app.cookyourbooks.domain.Unit;
import app.cookyourbooks.domain.UnitKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UnitRepository extends JpaRepository<Unit, Long> {
    Optional<Unit> findByCode(String code);
    List<Unit> findByKind(UnitKind kind);
}
