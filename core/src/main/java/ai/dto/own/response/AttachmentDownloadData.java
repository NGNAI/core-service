package ai.dto.own.response;

public record AttachmentDownloadData(
        String fileName,
        String contentType,
        byte[] bytes) {
}
