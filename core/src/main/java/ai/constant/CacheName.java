package ai.constant;

public class CacheName {
    public static final String DATA_INGESTION_DTO_DETAILS = "data-ingestion:dto:details";
    public static final String ROLE_AND_PERMISSION = "role:permission:map";
    public static final String USER_PERMISSION_IN_ORG = "org:user:permission";
    public static final String SYSTEM_SETTINGS = "system:settings";
    /** Cache gợi ý câu hỏi (ask-autocomplete) theo prefix + ngôn ngữ. TTL ngắn, xem RedisCacheConfig. */
    public static final String AI_SUGGESTION = "ai:suggestion";
}
