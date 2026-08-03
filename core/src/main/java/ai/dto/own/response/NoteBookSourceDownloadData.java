package ai.dto.own.response;

import java.io.InputStream;

public record NoteBookSourceDownloadData(String fileName, String contentType, InputStream inputStream, long size) {
}