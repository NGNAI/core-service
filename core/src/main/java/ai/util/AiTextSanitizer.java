package ai.util;

import java.util.regex.Pattern;

/**
 * Chuẩn hóa văn bản do LLM sinh ra trước khi lưu DB hoặc trả về client.
 *
 * <p>Vấn đề thực tế: các model nhỏ và đặc biệt là <b>reasoning model</b>
 * ({@code gpt-oss-20b}, DeepSeek-R1, QwQ...) thường trả về kết quả kèm phần suy luận
 * hoặc các thành phần trình bày dư thừa, dù prompt đã yêu cầu "chỉ trả về nội dung".
 * Nếu lưu thẳng vào DB, dữ liệu sẽ bẩn (summary lẫn {@code <thinking>}, tiêu đề chứa
 * {@code "Title:"} hoặc câu dẫn "Dưới đây là...").
 *
 * <p>Lớp này xử lý tập trung các trường hợp phổ biến:
 * <ul>
 *   <li>Bỏ khối suy luận có thẻ đóng: {@code <thinking>...</thinking>},
 *       {@code  thinking...</think>}, {@code <reasoning>...</reasoning>}.</li>
 *   <li>Bỏ nhãn suy luận dạng text ở đầu: {@code Thought:}, {@code Reasoning:},
 *       {@code Phân tích:}...</li>
 *   <li>Bỏ nhãn kết quả do model tự thêm: {@code Title:}, {@code Tiêu đề:},
 *       {@code Summary:}, {@code Tóm tắt:}...</li>
 *   <li>Bỏ câu dẫn: {@code Here is the title:}, {@code Dưới đây là...}.</li>
 *   <li>Bỏ dấu nháy/backtick bao quanh toàn bộ nội dung.</li>
 *   <li>Chuẩn hóa khoảng trắng; tùy chọn gộp về một dòng (cho tiêu đề).</li>
 * </ul>
 */
public final class AiTextSanitizer {

    /** Khối suy luận có thẻ đóng, có thể trải nhiều dòng. */
    private static final Pattern REASONING_BLOCK_PATTERN = Pattern.compile(
            "(?is)<(thinking|think|reasoning|analysis)>.*?</\\1>");

    /** Nhãn mở đầu phần suy luận dạng text (không có thẻ đóng). */
    private static final Pattern LEADING_REASONING_LABEL_PATTERN = Pattern.compile(
            "(?is)^\\s*(thought|thinking|reasoning|analysis|phân tích|suy luận)\\s*[:\\-]\\s*");

    /** Nhãn kết quả mà model hay tự thêm trước nội dung. */
    private static final Pattern LEADING_RESULT_LABEL_PATTERN = Pattern.compile(
            "(?is)^\\s*(generated\\s+title|title|tiêu\\s*đề|updated\\s+summary|final\\s+summary|summary|"
                    + "tóm\\s*tắt|nội\\s*dung\\s*tóm\\s*tắt)\\s*[:\\-]\\s*");

    /** Câu dẫn tiếng Anh / tiếng Việt đứng trước nội dung thật. */
    private static final Pattern LEADING_FILLER_PATTERN = Pattern.compile(
            "(?is)^\\s*(here\\s+is|here's|below\\s+is|dưới\\s+đây\\s+là)[^:\\n]*[:\\-]\\s*");

    /**
     * Đuôi bịa thêm sau nội dung (câu xác nhận của model nhỏ).
     * <p>
     * Yêu cầu phải có xuống dòng trước câu này để tránh cắt nhầm khi cụm từ xuất hiện
     * ngay trong nội dung hợp lệ (vd bullet kết thúc bằng "liên hệ khi cần").
     */
    private static final Pattern TRAILING_FILLER_PATTERN = Pattern.compile(
            "(?im)(?:\\R\\s*)(let\\s+me\\s+know\\s+if[^\\n]*|hope\\s+this\\s+helps[^\\n]*|"
                    + "hy\\s+vọng[^\\n]*|nếu\\s+bạn\\s+cần[^\\n]*)\\s*$");

    /** Ký tự dấu câu kết thúc cần bỏ khi sinh tiêu đề. */
    private static final Pattern TRAILING_PUNCTUATION_PATTERN = Pattern.compile("[.,;:!。！、]+$");

    /** Markdown heading ở đầu tiêu đề. */
    private static final Pattern LEADING_MARKDOWN_HEADING_PATTERN = Pattern.compile("^#{1,6}\\s*");

    /** Markdown bullet ở đầu tiêu đề. */
    private static final Pattern LEADING_MARKDOWN_BULLET_PATTERN = Pattern.compile("^[-*+]\\s+");

    private AiTextSanitizer() {
    }

    /**
     * Làm sạch văn bản sinh bởi LLM.
     *
     * @param raw               nội dung thô từ model (có thể null)
     * @param collapseToOneLine {@code true} để gộp về một dòng duy nhất và bỏ dấu câu
     *                          kết thúc (dùng cho tiêu đề); {@code false} để giữ cấu
     *                          trúc nhiều dòng (dùng cho summary)
     * @return nội dung đã làm sạch, hoặc {@code null} nếu không còn nội dung
     */
    public static String sanitize(String raw, boolean collapseToOneLine) {
        return sanitize(raw, collapseToOneLine, 0);
    }

    /**
     * Làm sạch văn bản sinh bởi LLM, kèm chặn độ dài đầu ra.
     *
     * <p>Chặn độ dài là rào chắn cuối cùng chống việc model nhỏ phình to output dù
     * prompt đã giới hạn số từ — nhất là với rolling summary: nếu summary không ngừng
     * dài thêm qua từng lần nén thì ngân sách input sẽ bị lấp đầy bởi summary cũ,
     * chặn luôn các tin nhắn mới.
     *
     * @param raw               nội dung thô từ model (có thể null)
     * @param collapseToOneLine {@code true} để gộp về một dòng duy nhất (dùng cho tiêu đề)
     * @param maxLength         độ dài tối đa của nội dung sau làm sạch; {@code <= 0} là không giới hạn
     * @return nội dung đã làm sạch, hoặc {@code null} nếu không còn nội dung
     */
    public static String sanitize(String raw, boolean collapseToOneLine, int maxLength) {
        String text = sanitizeInternal(raw, collapseToOneLine);
        if (text == null) {
            return null;
        }

        if (maxLength > 0 && text.length() > maxLength) {
            // Cắt theo ranh giới từ để không chém giữa chừng một từ/tiếng có dấu
            text = cutAtWordBoundary(text, maxLength);
        }

        return text.isEmpty() ? null : text;
    }

    /**
     * Thực hiện làm sạch phần lõi (không chặn độ dài).
     */
    private static String sanitizeInternal(String raw, boolean collapseToOneLine) {
        if (raw == null) {
            return null;
        }

        String text = raw.trim();
        if (text.isEmpty()) {
            return null;
        }

        text = REASONING_BLOCK_PATTERN.matcher(text).replaceAll("").trim();
        text = LEADING_REASONING_LABEL_PATTERN.matcher(text).replaceAll("").trim();
        text = LEADING_RESULT_LABEL_PATTERN.matcher(text).replaceAll("").trim();
        text = LEADING_FILLER_PATTERN.matcher(text).replaceAll("").trim();
        text = TRAILING_FILLER_PATTERN.matcher(text).replaceAll("").trim();
        text = stripWrappingQuotes(text);

        if (text.isEmpty()) {
            return null;
        }

        if (collapseToOneLine) {
            text = text.replaceAll("\\s*\\R+\\s*", " ").trim();
            text = LEADING_MARKDOWN_HEADING_PATTERN.matcher(text).replaceAll("");
            text = LEADING_MARKDOWN_BULLET_PATTERN.matcher(text).replaceAll("");
            text = TRAILING_PUNCTUATION_PATTERN.matcher(text).replaceAll("").trim();
        } else {
            // Giới hạn tối đa 1 dòng trống liên tiếp để summary gọn gàng
            text = text.replaceAll("\\R{3,}", "\n\n").trim();
        }

        return text.isEmpty() ? null : text;
    }

    /**
     * Cắt chuỗi tại ranh giới khoảng trắng gần {@code maxLength} nhất (không cắt giữa
     * một từ), dùng cho cả văn bản tiếng Việt có dấu.
     */
    private static String cutAtWordBoundary(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }

        int cutIndex = maxLength;
        while (cutIndex > 0 && !Character.isWhitespace(text.charAt(cutIndex - 1))) {
            cutIndex--;
        }
        if (cutIndex == 0) {
            // Không có khoảng trắng nào trong đoạn đầu — cắt cứng
            cutIndex = maxLength;
        }

        return text.substring(0, cutIndex).trim() + "...";
    }

    /**
     * Cắt ngắn nội dung theo ngân sách ký tự, thêm hậu tố đánh dấu phần bị lược bỏ
     * để model biết dữ liệu không đầy đủ (giảm nguy cơ model bịa thêm cho đủ).
     *
     * @param value     nội dung gốc (có thể null)
     * @param maxLength số ký tự tối đa; {@code <= 0} nghĩa là không giới hạn
     * @return nội dung đã cắt (không bao giờ null)
     */
    public static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if (maxLength <= 0 || normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "\n...(nội dung đã được lược bớt)";
    }

    /**
     * Bỏ dấu nháy đơn/kép và backtick bao quanh nội dung (một hoặc nhiều lớp).
     */
    private static String stripWrappingQuotes(String text) {
        String result = text;
        boolean changed = true;
        while (changed && result.length() >= 2) {
            changed = false;
            char first = result.charAt(0);
            char last = result.charAt(result.length() - 1);
            if (first == last && (first == '"' || first == '\'' || first == '`'
                    || first == '\u201C' || first == '\u2018')) {
                result = result.substring(1, result.length() - 1).trim();
                changed = true;
            }
            // Trường hợp mở bằng “ và đóng bằng ” (không đối xứng)
            if (!changed && result.length() >= 2
                    && (result.charAt(0) == '\u201C' || result.charAt(0) == '\u2018')
                    && (result.charAt(result.length() - 1) == '\u201D'
                            || result.charAt(result.length() - 1) == '\u2019')) {
                result = result.substring(1, result.length() - 1).trim();
                changed = true;
            }
        }
        return result;
    }
}
