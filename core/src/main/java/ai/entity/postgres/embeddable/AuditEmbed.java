package ai.entity.postgres.embeddable;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.time.LocalDateTime;

@JsonPropertyOrder({
        "createdAt",
        "createdBy",
        "updatedAt",
        "updatedBy"
})
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Embeddable
public class AuditEmbed {
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    Integer createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by")
    Integer updatedBy;
}
