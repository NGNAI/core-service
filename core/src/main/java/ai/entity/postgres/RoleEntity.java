package ai.entity.postgres;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "role")
@Entity
public class RoleEntity {
    @Column(name = "name", nullable = false)
    @Id
    String name;

    @Column(name = "description")
    String description;

    @ManyToMany
    Set<PermissionEntity> permissions;
}
