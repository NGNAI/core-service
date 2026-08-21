package ai.dto.own.response;

import java.io.InputStream;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class DraftSourceDownloadData {
    String fileName;
    String contentType;
    InputStream inputStream;
    long size;
}
