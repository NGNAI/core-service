package ai.entity.postgres;

import ai.entity.postgres.embeddable.AuditEmbed;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.hibernate.type.SqlTypes;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "permission")
@EntityListeners(AuditingEntityListener.class)
@Entity
public class PermissionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    UUID id;

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "description")
    String description;

    @Column(name = "code", nullable = false)
    String code;

    @Column(name = "resource", nullable = false)
    String resource;

    @Column(name = "action", nullable = false)
    String action;

    @Column(name = "target_resource")
    String targetResource;

    @JdbcTypeCode(SqlTypes.ARRAY)
    List<String> scopes;

    @Embedded
    AuditEmbed audit = new AuditEmbed();

    @OneToMany(mappedBy = "permission", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<RolePermissionEntity> rolePermissions = new HashSet<>();

    @PrePersist
    public void prePersist(){
        if(resource!=null && action!=null)
            if(targetResource==null)
                code = String.format("%s:%s",resource,action);
            else
                code =  String.format("%s:%s:%s",resource,action,targetResource);
    }
}
