package ai.service.api;

import ai.AppProperties;
import ai.api.OtpApiCore;
import ai.dto.outer.otp.response.OtpUserResponseDto;
import ai.model.OtpApiResponseModel;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class OtpApiService {
    OtpApiCore apiCore;
    AppProperties appProperties;

    /**
     * Xác thực user LDAP qua OTP Service.
     * POST /user/authLdap body: { userId, password, customerCode }
     */
    public OtpApiResponseModel<OtpUserResponseDto> auth(String userId, String password) {
       try {
            Map<String, Object> body = new HashMap<>();
            body.put("userId", userId);
            body.put("password", password);
            body.put("customerCode", appProperties.getOtp().getCustomerCode());
            return apiCore.post("/user/authLdap", body, new ParameterizedTypeReference<>(){});
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to serialize request body for authLdap", e);
        }
    }

    /**
     * Tìm kiếm / lấy toàn bộ user LDAP qua OTP Service.
     * POST /user/searchInLdap body: { search, customerCode }
     * Truyền keyword rỗng ("") để lấy tất cả (không phân trang).
     */
    public OtpApiResponseModel<List<OtpUserResponseDto>> searchUsers(String keyword) {
        try {
            // Nếu lấy tất cả mặc định thì keyword = "*" để tránh lỗi từ OTP Service.
            if (keyword == null || keyword.isBlank()) {
                keyword = "*";
            }
            Map<String, Object> body = new HashMap<>();
            body.put("userId", keyword);
            body.put("customerCode", appProperties.getOtp().getCustomerCode());
            return apiCore.post("/user/searchInLdap", body, new ParameterizedTypeReference<>(){});
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to serialize request body for searchUsers", e);
        }
    }

    /**
     * Lấy chi tiết 1 user LDAP theo userId.
     * POST /users/get{userId} body: { customerCode }
     */
    public OtpApiResponseModel<OtpUserResponseDto> getUserDetail(String userId) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("userId", userId);
            body.put("customerCode", appProperties.getOtp().getCustomerCode());
            return apiCore.post("/user/get", body, new ParameterizedTypeReference<>(){});
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to serialize request body for getUserDetail", e);
        }
    }
}
