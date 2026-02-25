package ai.entity.postgres;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "users")
@Entity
public class UserEntity {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", name = "user_id",updatable = false, nullable = false)
    UUID userId;

    @Column(name = "user_name", nullable = false, unique = true, updatable = false)
    String userName;

    @Column(name = "password", nullable = false)
    String password;

    @Column(name = "full_name", nullable = false)
    String fullName;

    @Column(name = "email", nullable = false)
    String email;

    @Column(name = "source", nullable = false)
    String source;

    @ManyToMany
    Set<RoleEntity> roles;
}
