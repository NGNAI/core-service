package ai.entity.postgres;

import ai.entity.postgres.embeddable.AuditEmbed;
import ai.mapper.GeneralMapper;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.mapstruct.Mapping;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "permission")
@EntityListeners(AuditingEntityListener.class)
@Entity
public class PermissionEntity {
    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false)
    int id;

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "description")
    String description;

    @Embedded
    AuditEmbed audit = new AuditEmbed();

    @OneToMany(mappedBy = "permission", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<RolePermissionEntity> rolePermissions = new HashSet<>();
}
