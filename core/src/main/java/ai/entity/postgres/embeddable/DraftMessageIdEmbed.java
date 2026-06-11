package ai.entity.postgres.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Embeddable
public class DraftMessageIdEmbed {
    @Column(name = "draft_id", nullable = false)
    UUID draftId;

    @Column(name = "message_id", nullable = false)
    UUID messageId;
}
