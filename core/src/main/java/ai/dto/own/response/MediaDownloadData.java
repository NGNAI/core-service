package ai.dto.own.response;

public record MediaDownloadData(String fileName, String contentType, byte[] bytes) {
    
}
