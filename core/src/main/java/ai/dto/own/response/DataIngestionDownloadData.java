package ai.dto.own.response;

import java.io.InputStream;

public record DataIngestionDownloadData(String fileName, String contentType, InputStream inputStream, long size) {
}
