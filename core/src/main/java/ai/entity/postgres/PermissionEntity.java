package ai.entity.postgres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "permission")
@Entity
public class PermissionEntity {
    @Column(name = "name", nullable = false)
    @Id
    String name;

    @Column(name = "description")
    String description;
}
