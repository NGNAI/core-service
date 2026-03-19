package ai.service.api;

import ai.api.OtpApiCore;
import ai.dto.outer.otp.request.OtpAuthRequestDto;
import ai.dto.outer.otp.response.OtpAuthResponseDto;
import ai.model.OtpApiResponseModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class OtpApiService {
    OtpApiCore apiCore;

    public OtpApiResponseModel<OtpAuthResponseDto> auth(OtpAuthRequestDto requestDto) throws JsonProcessingException {
        return apiCore.post("/register/auth", requestDto, new ParameterizedTypeReference<>(){});
    }
}
