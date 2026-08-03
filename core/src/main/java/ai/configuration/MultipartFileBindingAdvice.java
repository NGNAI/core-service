package ai.configuration;

import java.beans.PropertyEditorSupport;
import java.util.Collection;
import java.util.List;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.multipart.MultipartFile;

/**
 * Xử lý binding multipart cho DTO kiểu {@code @ModelAttribute}.
 *
 * <p>Spring Framework 7 (Spring Boot 4) đã bỏ {@code MultipartFileEditor}/
 * {@code MultipartFileArrayEditor} khỏi {@code ServletRequestDataBinder}, nên khi
 * Swagger UI gửi field file rỗng dưới dạng empty string {@code ""} (không phải file),
 * Spring không có cách nào convert {@code String → MultipartFile} và báo lỗi
 * {@code typeMismatch}. Advice này đăng ký custom editor để:
 * <ul>
 *   <li>empty/blank string → {@code null} (coi như không gửi file)</li>
 *   <li>object file thật (single hoặc collection) → giữ nguyên / chuyển thành array</li>
 * </ul>
 */
@ControllerAdvice
public class MultipartFileBindingAdvice {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(MultipartFile.class, new MultipartFilePropertyEditor());
        binder.registerCustomEditor(MultipartFile[].class, new MultipartFileArrayPropertyEditor());
    }

    private static final class MultipartFilePropertyEditor extends PropertyEditorSupport {
        @Override
        public void setAsText(String text) {
            setValue(null);
        }

        @Override
        public void setValue(Object value) {
            super.setValue(value instanceof MultipartFile ? value : null);
        }
    }

    private static final class MultipartFileArrayPropertyEditor extends PropertyEditorSupport {
        @Override
        public void setAsText(String text) {
            setValue(null);
        }

        @Override
        public void setValue(Object value) {
            if (value == null || value instanceof MultipartFile[]) {
                super.setValue(value);
            } else if (value instanceof MultipartFile file) {
                super.setValue(new MultipartFile[]{file});
            } else if (value instanceof Collection<?> collection) {
                List<MultipartFile> files = collection.stream()
                        .filter(MultipartFile.class::isInstance)
                        .map(MultipartFile.class::cast)
                        .toList();
                super.setValue(files.toArray(new MultipartFile[0]));
            } else {
                // String/array string rỗng hoặc giá trị không convert được -> coi như null
                super.setValue(null);
            }
        }
    }
}
