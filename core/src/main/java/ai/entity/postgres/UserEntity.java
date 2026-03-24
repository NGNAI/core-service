package ai.entity.postgres;

import ai.entity.postgres.embeddable.AuditEmbed;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id",updatable = false, nullable = false)
    UUID id;

    @Column(name = "user_name", nullable = false, unique = true, updatable = false)
    String userName;

    @Column(name = "password")
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

    @Builder.Default
    @Embedded
    AuditEmbed audit = new AuditEmbed();

    @Column(name = "active")
    boolean active;

    @Column(name = "last_login")
    Instant lastLogin;

    @Column(name = "source", nullable = false)
    String source;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    Set<OrganizationUserRoleEntity> orgUsersRole;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    List<TopicEntity> topics;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    List<MediaEntity> medias;
}
