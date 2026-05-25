package app.cookyourbooks.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "units")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 64)
    private String plural;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UnitKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UnitSystem system;

    @Column(name = "base_factor", nullable = false, precision = 18, scale = 9)
    private BigDecimal baseFactor;

    @Column(name = "is_base", nullable = false)
    private boolean isBase;
}
