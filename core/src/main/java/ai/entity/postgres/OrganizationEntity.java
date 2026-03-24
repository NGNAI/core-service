package ai.entity.postgres;

import ai.entity.postgres.embeddable.AuditEmbed;
import ai.util.LTreeUtil;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Set;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "organizations")
@EntityListeners(AuditingEntityListener.class)
@Entity
public class OrganizationEntity {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id",updatable = false, nullable = false)
    UUID id;

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "description")
    String description;

    @Column(name = "path")
    String path;

    @Builder.Default
    @Embedded
    AuditEmbed audit = new AuditEmbed();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    OrganizationEntity parent;

    @OneToMany(mappedBy = "parent",orphanRemoval = true)
    Set<OrganizationEntity> children;

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<OrganizationUserRoleEntity> orgUsersRole;

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<MediaEntity> medias;

    @PrePersist
    public void prePersist() {
        String parentPath = (parent != null) ? parent.getPath() : null;
        this.path = LTreeUtil.buildPath(parentPath, this.id);
    }
}
