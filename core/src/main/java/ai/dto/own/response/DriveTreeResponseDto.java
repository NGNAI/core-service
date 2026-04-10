package ai.dto.own.response;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DriveTreeResponseDto extends DriveResponseDto {
    List<DriveTreeResponseDto> children = new ArrayList<>();
}
