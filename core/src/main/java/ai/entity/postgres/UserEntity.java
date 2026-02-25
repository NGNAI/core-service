package ai.entity.postgres;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "user")
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
}
