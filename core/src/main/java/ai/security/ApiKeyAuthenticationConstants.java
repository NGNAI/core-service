package ai.security;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ApiKeyAuthenticationConstants {
    public static final String ATTACHMENT_API_KEY_PRINCIPAL = "ATTACHMENT_API_KEY";
    public static final String ATTACHMENT_API_KEY_AUTHORITY = "ATTACHMENT_API_KEY";
    public static final String DATA_INGESTION_API_KEY_PRINCIPAL = "DATA_INGESTION_API_KEY";
    public static final String DATA_INGESTION_API_KEY_AUTHORITY = "DATA_INGESTION_API_KEY";
    public static final String DATA_INGESTION_WEBHOOK_SIGNATURE_PRINCIPAL = "DATA_INGESTION_WEBHOOK_SIGNATURE";
    public static final String DATA_INGESTION_WEBHOOK_SIGNATURE_AUTHORITY = "DATA_INGESTION_WEBHOOK_SIGNATURE";
}