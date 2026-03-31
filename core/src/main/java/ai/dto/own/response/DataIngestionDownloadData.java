package ai.dto.own.response;

public record DataIngestionDownloadData(String fileName, String contentType, byte[] bytes) {
    
}
