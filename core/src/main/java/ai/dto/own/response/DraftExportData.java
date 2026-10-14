package ai.dto.own.response;

/**
 * Dữ liệu file export của draft (nhị phân) trả về cho client.
 *
 * @param fileName    tên file đã chuẩn hóa (luôn kết thúc bằng .docx)
 * @param contentType MIME type của file
 * @param bytes       nội dung file
 */
public record DraftExportData(
        String fileName,
        String contentType,
        byte[] bytes) {
}
