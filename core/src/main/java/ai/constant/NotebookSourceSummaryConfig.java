package ai.constant;

/**
 * Cấu hình chọn cách lấy summary cho notebook source (bật/tắt bằng biến trong class, không cần application.yml).
 * <ul>
 *   <li>{@code USE_RAG_SOURCE_GUIDE = true}  → dùng RAG service <code>/notebook/v2/source-guide</code> (cách mới, có callback + scheduler)</li>
 *   <li>{@code USE_RAG_SOURCE_GUIDE = false} → dùng IngestionService <code>/summarize</code> (cách cũ, giữ nguyên)</li>
 * </ul>
 */
public final class NotebookSourceSummaryConfig {
    public static final boolean USE_RAG_SOURCE_GUIDE = true;

    private NotebookSourceSummaryConfig() {
    }
}
