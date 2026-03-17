package ai.dto.outer.otp.request;

public record OtpAuthRequestDto(String userId, String password, String customerCode) {
}
