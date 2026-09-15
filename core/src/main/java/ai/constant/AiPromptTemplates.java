package ai.constant;

/**
 * Prompt template tập trung cho các tác vụ sinh văn bản phụ trợ của AI
 * (đặt tiêu đề và nén hội thoại thành rolling summary).
 *
 * <p>Nguyên tắc thiết kế — tối ưu cho các model nhỏ / model reasoning như
 * {@code gpt-oss-20b}:
 * <ul>
 *   <li><b>Tách bạch chỉ dẫn và dữ liệu</b>: phần nội dung đầu vào được bọc trong
 *       delimiter {@code <<<...>>>} để model không nhầm nội dung người dùng với luật.</li>
 *   <li><b>Mỗi luật một dòng riêng</b> (tránh lỗi nối chuỗi thiếu {@code \n} khiến
 *       các heading dính vào nhau như trước đây).</li>
 *   <li><b>Output contract tường minh</b>: chỉ trả về đúng nội dung, không thêm lời dẫn,
 *       không trình bày quá trình suy luận (reasoning models rất hay thêm phần này).</li>
 *   <li><b>Ngân sách độ dài</b> cho cả input lẫn output để tránh summary phình to
 *       làm tràn context của các lượt chat sau.</li>
 *   <li><b>Chống bịa</b>: cấm thêm thông tin không có trong dữ liệu đầu vào.</li>
 * </ul>
 *
 * <p>Lớp này chỉ chứa chuỗi prompt (không phụ thuộc Spring) để dễ đọc, dễ review và
 * dễ tinh chỉnh độc lập với logic gọi API.
 */
public final class AiPromptTemplates {

    /** Nội dung thay thế khi chưa có summary cũ. */
    public static final String NO_EXISTING_SUMMARY = "(chưa có)";

    /** Nội dung thay thế khi không có tin nhắn mới nào. */
    public static final String NO_MESSAGES = "(không có)";

    private static final String DELIM_INPUT_OPEN = "<<<INPUT";
    private static final String DELIM_INPUT_CLOSE = "INPUT>>>";
    private static final String DELIM_EXISTING_OPEN = "<<<EXISTING_SUMMARY";
    private static final String DELIM_EXISTING_CLOSE = "EXISTING_SUMMARY>>>";
    private static final String DELIM_MESSAGES_OPEN = "<<<NEW_MESSAGES";
    private static final String DELIM_MESSAGES_CLOSE = "NEW_MESSAGES>>>";

    private AiPromptTemplates() {
    }

    /**
     * Prompt sinh tiêu đề (dùng chung cho topic chat và note).
     *
     * @param subjectLabel mô tả đối tượng cần đặt tiêu đề, ví dụ {@code "một ghi chú"}
     * @param languageHint câu chỉ dẫn ngôn ngữ đầu ra
     * @param content      nội dung nguồn (đã được cắt ngắn trước khi truyền vào)
     * @return prompt hoàn chỉnh
     */
    public static String titlePrompt(String subjectLabel, String languageHint, String content) {
        return """
                Bạn là biên tập viên nội dung chuyên nghiệp. Nhiệm vụ: đặt tiêu đề ngắn gọn và mô tả đúng nội dung của %s.

                Quy tắc bắt buộc:
                - Ngôn ngữ: %s
                - Độ dài: 4 đến 10 từ, chỉ một dòng duy nhất.
                - Không bọc trong dấu nháy, không có dấu chấm ở cuối, không có tiền tố kiểu "Tiêu đề:".
                - Không dùng markdown, không thêm lời dẫn hay giải thích.
                - Không trình bày quá trình suy luận.
                - Chỉ trả về DUY NHẤT nội dung tiêu đề.

                %s
                %s
                %s
                """.formatted(
                subjectLabel,
                languageHint,
                DELIM_INPUT_OPEN,
                content,
                DELIM_INPUT_CLOSE);
    }

    /**
     * Prompt cập nhật rolling summary cho chat Topic.
     *
     * @param existingSummary summary hiện tại (đã chuẩn hóa, có thể là {@link #NO_EXISTING_SUMMARY})
     * @param messageBlock    các tin nhắn mới, mỗi dòng dạng {@code User: ...} / {@code Assistant: ...}
     * @param targetWords     số từ tối đa cho summary đầu ra
     * @return prompt hoàn chỉnh
     */
    public static String topicSummaryPrompt(String existingSummary, String messageBlock, int targetWords) {
        return """
                Bạn là bộ nén bộ nhớ hội thoại (conversation memory compressor) cho chat Topic.
                Nhiệm vụ: cập nhật rolling summary để các lượt chat sau vẫn giữ được ngữ cảnh quan trọng.

                Quy tắc bắt buộc:
                - Giữ nguyên ngôn ngữ của cuộc hội thoại (hội thoại tiếng Việt thì summary phải là tiếng Việt).
                - Hợp nhất summary cũ với tin nhắn mới: giữ thông tin còn đúng, bổ sung thông tin mới, loại bỏ thông tin đã bị phủ định hoặc thay thế.
                - Chỉ giữ thông tin bền vững: sự kiện, quyết định, ràng buộc, sở thích của người dùng, thực thể có tên (người, tổ chức, tệp, số liệu), câu hỏi chưa được giải quyết, trạng thái tiến độ.
                - Không đưa vào: lời chào, cảm ơn, câu xác nhận, câu đệm, ý lặp lại, nhận xét về việc tóm tắt.
                - Không được thêm thông tin không xuất hiện trong dữ liệu đầu vào.
                - Độ dài tối đa %d từ. Ưu tiên các dòng bắt đầu bằng "- ", không dùng tiêu đề mục.
                - Nếu tin nhắn mới không có thông tin bền vững nào mới, trả lại nguyên văn summary cũ.
                - Không trình bày quá trình suy luận.
                - Chỉ trả về DUY NHẤT nội dung summary, không thêm câu dẫn kiểu "Dưới đây là...".

                %s
                %s
                %s

                %s
                %s
                %s
                """.formatted(
                targetWords,
                DELIM_EXISTING_OPEN,
                existingSummary,
                DELIM_EXISTING_CLOSE,
                DELIM_MESSAGES_OPEN,
                messageBlock,
                DELIM_MESSAGES_CLOSE);
    }

    /**
     * Prompt cập nhật rolling summary cho chat Notebook (NotebookLM).
     *
     * @param existingSummary summary hiện tại (đã chuẩn hóa, có thể là {@link #NO_EXISTING_SUMMARY})
     * @param messageBlock    các tin nhắn mới, mỗi dòng dạng {@code User: ...} / {@code Assistant: ...}
     * @param targetWords     số từ tối đa cho summary đầu ra
     * @return prompt hoàn chỉnh
     */
    public static String noteBookSummaryPrompt(String existingSummary, String messageBlock, int targetWords) {
        return """
                Bạn là bộ nén bộ nhớ hội thoại (conversation memory compressor) cho chat Notebook (NotebookLM).
                Nhiệm vụ: cập nhật rolling summary để các lượt chat sau vẫn giữ được ngữ cảnh quan trọng.

                Quy tắc bắt buộc:
                - Giữ nguyên ngôn ngữ của cuộc hội thoại (hội thoại tiếng Việt thì summary phải là tiếng Việt).
                - Hợp nhất summary cũ với tin nhắn mới: giữ thông tin còn đúng, bổ sung thông tin mới, loại bỏ thông tin đã bị phủ định hoặc thay thế.
                - Chỉ giữ thông tin bền vững: yêu cầu, nhiệm vụ, kế hoạch, giả định, quyết định, việc còn dang dở, và các tham chiếu quan trọng từ tài liệu nguồn của notebook.
                - Không đưa vào: lời chào, cảm ơn, câu xác nhận, câu đệm, ý lặp lại, nhận xét về việc tóm tắt.
                - Không được thêm thông tin không xuất hiện trong dữ liệu đầu vào.
                - Độ dài tối đa %d từ. Ưu tiên các dòng bắt đầu bằng "- ", không dùng tiêu đề mục.
                - Nếu tin nhắn mới không có thông tin bền vững nào mới, trả lại nguyên văn summary cũ.
                - Không trình bày quá trình suy luận.
                - Chỉ trả về DUY NHẤT nội dung summary, không thêm câu dẫn kiểu "Dưới đây là...".

                %s
                %s
                %s

                %s
                %s
                %s
                """.formatted(
                targetWords,
                DELIM_EXISTING_OPEN,
                existingSummary,
                DELIM_EXISTING_CLOSE,
                DELIM_MESSAGES_OPEN,
                messageBlock,
                DELIM_MESSAGES_CLOSE);
    }
}
