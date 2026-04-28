package ai.dto.own.response;

public record NoteBookSourceDownloadData(String fileName, String contentType, byte[] bytes) {
}