package app.cookyourbooks.repository;

import app.cookyourbooks.domain.UnitConversion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UnitConversionRepository extends JpaRepository<UnitConversion, Long> {

    @Query("""
        SELECT c FROM UnitConversion c
        WHERE c.fromUnit.id = :fromId
          AND c.toUnit.id   = :toId
          AND (c.ingredient.id = :ingredientId
               OR (c.ingredient IS NULL AND :ingredientId IS NULL))
    """)
    Optional<UnitConversion> findExact(@Param("fromId") Long fromId,
                                       @Param("toId") Long toId,
                                       @Param("ingredientId") Long ingredientId);

    @Query("""
        SELECT c FROM UnitConversion c
        WHERE (c.fromUnit.id = :unitA AND c.toUnit.id = :unitB)
           OR (c.fromUnit.id = :unitB AND c.toUnit.id = :unitA)
    """)
    List<UnitConversion> findBetween(@Param("unitA") Long unitA,
                                     @Param("unitB") Long unitB);
}
