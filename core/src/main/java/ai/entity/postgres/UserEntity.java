package ai.entity.postgres;

import ai.entity.postgres.embeddable.AuditEmbed;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Entity
public class UserEntity {
    @Id
    @GeneratedValue
    @Column(name = "id",updatable = false, nullable = false)
    int id;

    @Column(name = "user_name", nullable = false, unique = true, updatable = false)
    String userName;

    @Column(name = "password", nullable = false)
    String password;

    @Column(name = "first_name", nullable = false)
    String firstName;

    @Column(name = "last_name")
    String lastName;

    @Column(name = "gender", nullable = false)
    int gender;

    @Column(name = "email", nullable = false)
    String email;

    @Column(name = "phone_number")
    String phoneNumber;

    @Embedded
    AuditEmbed audit;

    @Column(name = "source", nullable = false)
    String source;

    @OneToMany(mappedBy = "user")
    Set<OrganizationUserRoleEntity> orgUsersRole;
}
