package ai.entity.postgres.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Embeddable
public class AuditEmbed {
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @Column(name = "create_by", updatable = false)
    int createBy;

    @LastModifiedDate
    @Column(name = "updated", updatable = false)
    LocalDateTime updatedAt;

    @CreatedDate
    @Column(name = "updated_by", updatable = false)
    int updatedBy;
}
