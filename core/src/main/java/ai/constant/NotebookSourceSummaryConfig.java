package ai.constant;

/**
 * Cấu hình chọn cách lấy summary cho notebook source (bật/tắt bằng biến trong class, không cần application.yml).
 * <ul>
 *   <li>{@code USE_RAG_SOURCE_GUIDE = true}  → dùng RAG service <code>/notebook/v2/source-guide</code> (cách mới, có callback + scheduler)</li>
 *   <li>{@code USE_RAG_SOURCE_GUIDE = false} → dùng IngestionService <code>/summarize</code> (cách cũ, giữ nguyên)</li>
 * </ul>
 *
 * <p>Các tham số retry dưới đây chặn tình trạng scheduler poll vô hạn với source mà RAG
 * service không bao giờ sinh được summary (trigger thất bại, hoặc source tạo trước khi
 * bật tính năng source-guide).
 */
public final class NotebookSourceSummaryConfig {
    public static final boolean USE_RAG_SOURCE_GUIDE = true;

    /** Key setting cho số lần thử sinh summary tối đa (admin chỉnh được qua UI). */
    public static final String KEY_MAX_SUMMARY_RETRY_ATTEMPTS = "ai.sourceGuide.maxRetryAttempts";

    /**
     * Key setting cho chỉ dẫn sinh summary gửi kèm khi trigger source-guide.
     * Cho phép admin tinh chỉnh chất lượng/tone summary mà không cần deploy lại.
     */
    public static final String KEY_SUMMARY_GENERATION_INSTRUCTION = "ai.sourceGuide.generationInstruction";

    /** Số lần thử sinh summary tối đa mặc định trước khi đánh dấu FAILED và dừng retry. */
    public static final int DEFAULT_MAX_SUMMARY_RETRY_ATTEMPTS = 5;

    /**
     * Chỉ dẫn sinh nội dung mặc định, nhằm neo chất lượng summary cho model nhỏ
     * ({@code gpt-oss-20b}): chỉ trả về nội dung summary, không kèm phần suy luận hay lời dẫn.
     */
    public static final String DEFAULT_SUMMARY_GENERATION_INSTRUCTION = """
            Hãy tạo bản tóm tắt nội dung của tài liệu này cho mục đích học tập và tra cứu nhanh.

            Yêu cầu bắt buộc:
            - Chỉ trả về DUY NHẤT nội dung tóm tắt, không thêm lời chào, lời dẫn hay giải thích.
            - Không trình bày quá trình suy luận, không dùng thẻ <thinking>.
            - Tóm tắt trung thành với tài liệu: không thêm thông tin không có trong tài liệu.
            - Nêu rõ chủ đề chính, các ý lớn và kết luận quan trọng; giữ lại các số liệu,
              tên riêng và thuật ngữ then chốt.
            - Trả lời bằng bullet ngắn gọn; mỗi bullet một ý.
            - Độ dài 8 đến 15 bullet.
            """;

    private NotebookSourceSummaryConfig() {
    }
}
