package ai.constant;

/**
 * Khai báo tập trung các System Setting và giá trị mặc định cho tác vụ gợi ý câu hỏi
 * (ask-autocomplete).
 *
 * <p>Theo cùng pattern với {@link NotebookSourceSummaryConfig}: lớp này chỉ giữ <b>key</b>
 * và <b>default</b>; giá trị thực tế được đọc qua {@code SystemSettingService} để admin
 * chỉnh không cần deploy. Quy ước chung của repo: giá trị rỗng, {@code <= 0} hoặc không
 * parse được → dùng default trong lớp này.
 *
 * <p><b>Vì sao dùng System Setting thay vì YAML:</b> việc chọn model là của RAG service
 * (nó sở hữu danh sách model). Khi RAG bổ sung model nhỏ/nhanh hơn, chỉ cần đổi
 * {@link #KEY_MODEL} trong System Settings là autocomplete dùng model mới — core-service
 * không phải sửa code hay deploy lại.
 */
public final class AiSuggestionConfig {

    private AiSuggestionConfig() {
    }

    // ------------------------------------------------------------------
    // Model & timeout (áp dụng ở RagService khi dựng request completion)
    // ------------------------------------------------------------------

    /**
     * Model riêng cho tác vụ gợi ý câu hỏi. <b>Để rỗng = dùng {@code ai.model}</b>
     * (model chung của các tác vụ AI).
     */
    public static final String KEY_MODEL = "ai.suggestion.model";

    /** Timeout (giây) cho lời gọi sinh gợi ý. Người dùng đang gõ nên không thể chờ lâu. */
    public static final String KEY_TIMEOUT_SECONDS = "ai.suggestion.timeoutSeconds";

    public static final int DEFAULT_TIMEOUT_SECONDS = 15;

    // ------------------------------------------------------------------
    // Số lượng & độ dài gợi ý
    // ------------------------------------------------------------------

    /** Số gợi ý tối đa trả về. */
    public static final String KEY_MAX_SUGGESTIONS = "ai.suggestion.maxSuggestions";

    /** Độ dài prefix tối thiểu để gọi model; ngắn hơn → trả rỗng, không gọi. */
    public static final String KEY_MIN_PREFIX_CHARS = "ai.suggestion.minPrefixChars";

    /** Độ dài prefix tối đa đưa vào prompt (dài hơn → cắt lấy phần đuôi). */
    public static final String KEY_MAX_PREFIX_CHARS = "ai.suggestion.maxPrefixChars";

    /** Độ dài tối đa của mỗi gợi ý (ký tự). */
    public static final String KEY_MAX_SUGGESTION_CHARS = "ai.suggestion.maxSuggestionChars";

    public static final int DEFAULT_MAX_SUGGESTIONS = 5;
    public static final int DEFAULT_MIN_PREFIX_CHARS = 3;
    public static final int DEFAULT_MAX_PREFIX_CHARS = 200;
    public static final int DEFAULT_MAX_SUGGESTION_CHARS = 120;

    // ------------------------------------------------------------------
    // Chống lạm dụng (cache + giới hạn đồng thời)
    // ------------------------------------------------------------------

    /**
     * Số request đồng thời tối đa gửi tới model cho tác vụ gợi ý. Hết permit → trả rỗng
     * ngay (fail fast) để không dồn request.
     */
    public static final String KEY_MAX_CONCURRENT_REQUESTS = "ai.suggestion.maxConcurrentRequests";

    public static final int DEFAULT_MAX_CONCURRENT_REQUESTS = 4;

    /** Ngôn ngữ mặc định khi system setting {@code system.language} chưa có. */
    public static final String DEFAULT_LANGUAGE = "vi";
}
