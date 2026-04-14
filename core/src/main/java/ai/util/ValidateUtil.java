package ai.util;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class ValidateUtil {
    private static final String PHONE_REGEX = "^(0|\\+84|84)(3|5|7|8|9)([0-9]{8})$";

    public static boolean isValidVietnamesePhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }

        Pattern pattern = Pattern.compile(PHONE_REGEX);
        Matcher matcher = pattern.matcher(phone);

        return matcher.matches();
    }
}
