package ai.entity.postgres;

import ai.entity.postgres.embeddable.AuditEmbed;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "organizations")
@EntityListeners(AuditingEntityListener.class)
@Entity
public class OrganizationEntity {
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    OrganizationEntity parent;

    @OneToMany(mappedBy = "parent",orphanRemoval = true)
    Set<OrganizationEntity> children = new HashSet<>();

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<OrganizationUserRoleEntity> orgUsersRole;
}
