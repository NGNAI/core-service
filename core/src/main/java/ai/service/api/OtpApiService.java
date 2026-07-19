package ai.service.api;

import ai.api.OtpApiCore;
import ai.dto.outer.otp.request.OtpAuthRequestDto;
import ai.dto.outer.otp.response.OtpAuthResponseDto;
import ai.dto.outer.otp.response.OtpUserResponseDto;
import ai.model.OtpApiResponseModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class OtpApiService {
    OtpApiCore apiCore;

    public OtpApiResponseModel<OtpAuthResponseDto> auth(OtpAuthRequestDto requestDto) throws JsonProcessingException {
        return apiCore.post("/register/auth", requestDto, new ParameterizedTypeReference<>(){});
    }

    /**
     * Tìm kiếm user LDAP qua OTP Service.
     * Endpoint: GET /register/users?search={keyword}
     */
    public OtpApiResponseModel<List<OtpUserResponseDto>> searchUsers(String keyword) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("search", keyword);
        return apiCore.get("/register/users", params, new ParameterizedTypeReference<>(){});
    }

    /**
     * Lấy chi tiết 1 user LDAP theo userId.
     * Endpoint: GET /register/users/{userId}
     */
    public OtpApiResponseModel<OtpUserResponseDto> getUserDetail(String userId) {
        return apiCore.get("/register/users/" + userId, new ParameterizedTypeReference<>(){});
    }

    /**
     * Lấy danh sách tất cả user LDAP (phân trang).
     * Endpoint: GET /register/users?page={page}&size={size}
     */
    public OtpApiResponseModel<List<OtpUserResponseDto>> getAllUsers(int page, int size) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", String.valueOf(page));
        params.add("size", String.valueOf(size));
        return apiCore.get("/register/users", params, new ParameterizedTypeReference<>(){});
    }
}
