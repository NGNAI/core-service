package ai.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Semaphore;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.AppProperties;
import ai.api.OllamaApiCore;
import ai.constant.AiPromptTemplates;
import ai.constant.CacheName;
import ai.dto.outer.ollama.OllamaChatRequestDto;
import ai.util.AiTextSanitizer;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Sinh gợi ý câu hỏi (ask-autocomplete) cho ô chat bằng model mini chạy trên Ollama.
 *
 * <p>Đây là tác vụ <b>phụ trợ, best-effort</b>: mọi lỗi (Ollama down, timeout, output
 * không parse được) đều được nuốt và trả về danh sách rỗng để FE degrade êm —
 * autocomplete không bao giờ được phép làm hỏng trải nghiệm chat.
 *
 * <p>Các chốt chống lạm dụng, theo thứ tự kiểm tra:
 * <ol>
 *   <li>Prefix ngắn hơn {@code suggestion.min-prefix-chars} → trả rỗng, không gọi model.</li>
 *   <li>Redis cache theo {@code prefix + ngôn ngữ} (TTL {@code suggestion.cache-ttl-minutes}).</li>
 *   <li>Semaphore {@code suggestion.max-concurrent-requests} — hết permit thì trả rỗng ngay
 *       (fail fast, không xếp hàng) để không dồn request vào Ollama.</li>
 * </ol>
 *
 * <p>Cache được thao tác thủ công qua {@link CacheManager} (theo pattern
 * {@code DataIngestionService}) thay vì {@code @Cacheable}: luồng này kiểm tra điều kiện
 * guard TRƯỚC khi tra cache, và {@code @Cacheable} trên lời gọi nội bộ trong cùng bean
 * sẽ không đi qua proxy nên không có hiệu lực.
 */
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Service
public class AiSuggestionService {

    /** Ngôn ngữ mặc định khi system setting {@code system.language} chưa có. */
    static final String DEFAULT_LANGUAGE = "vi";

    /** Giá trị mặc định khi config chưa khai báo — mirror của application.yml. */
    static final int DEFAULT_MAX_SUGGESTIONS = 5;
    static final int DEFAULT_MIN_PREFIX_CHARS = 3;
    static final int DEFAULT_MAX_PREFIX_CHARS = 200;
    static final int DEFAULT_MAX_SUGGESTION_CHARS = 120;
    static final int DEFAULT_MAX_TOKENS = 256;
    static final int DEFAULT_NUM_CTX = 2048;
    static final double DEFAULT_TEMPERATURE = 0.2;
    static final int DEFAULT_MAX_CONCURRENT_REQUESTS = 4;

    final OllamaApiCore ollamaApiCore;

    final ObjectMapper objectMapper;

    final AppProperties appProperties;

    final SystemSettingService systemSettingService;

    final CacheManager cacheManager;

    /** Giới hạn request đồng thời tới Ollama. Khởi tạo trong {@link #init()} theo config. */
    Semaphore ollamaSemaphore;

    @PostConstruct
    void init() {
        ollamaSemaphore = new Semaphore(maxConcurrentRequests());
    }

    /**
     * Entry point cho controller: sinh danh sách gợi ý câu hỏi cho prefix đang gõ.
     *
     * <p>Toàn bộ thân method được bọc trong {@code try/catch} — không chỉ riêng lời gọi
     * Ollama. Lý do: ngoài Ollama, luồng này còn phụ thuộc Redis (cache) và System Setting
     * (ngôn ngữ). Nếu chỉ bắt lỗi quanh lời gọi Ollama thì Redis down hoặc DB setting lỗi
     * sẽ ném ra ngoài → {@code GlobalExceptionHandler} trả 500, phá vỡ cam kết
     * "autocomplete không bao giờ trả 5xx".
     *
     * @param prefix phần văn bản người dùng đã nhập
     * @return danh sách gợi ý, rỗng nếu không đủ điều kiện hoặc có lỗi
     */
    public List<String> suggest(String prefix) {
        try {
            String normalizedPrefix = normalizePrefix(prefix);
            if (normalizedPrefix == null || normalizedPrefix.length() < minPrefixChars()) {
                return List.of();
            }

            String language = resolveLanguage();
            String cacheKey = normalizedPrefix + "|" + language;

            List<String> cached = readCache(cacheKey);
            if (cached != null) {
                return cached;
            }

            List<String> generated = generateSuggestions(normalizedPrefix, language);
            if (!generated.isEmpty()) {
                writeCache(cacheKey, generated);
            }
            return generated;
        } catch (Exception e) {
            log.warn("Không lấy được gợi ý cho prefix '{}': {}", prefix, e.getMessage());
            return List.of();
        }
    }

    // ------------------------------------------------------------------
    // Sinh gợi ý
    // ------------------------------------------------------------------

    private List<String> generateSuggestions(String normalizedPrefix, String language) {
        // tryAcquire() không tham số = thử lấy permit ngay lập tức (timeout 0):
        // hết permit thì fail fast thay vì xếp hàng, tránh dồn request vào Ollama.
        if (!ollamaSemaphore.tryAcquire()) {
            log.warn("Bỏ qua gợi ý cho prefix '{}': đã đạt giới hạn {} request đồng thời tới Ollama",
                    normalizedPrefix, maxConcurrentRequests());
            return List.of();
        }

        try {
            // Prefix dài hơn ngân sách thì cắt bớt, và dùng CHÍNH giá trị đã cắt này cho
            // cả prompt lẫn bước kiểm tra "gợi ý phải bắt đầu bằng prefix" — nếu không,
            // model chỉ thấy phần bị cắt nên mọi gợi ý sẽ bị lọc sạch.
            String effectivePrefix = truncatePrefix(normalizedPrefix);

            String prompt = AiPromptTemplates.questionSuggestionPrompt(
                    effectivePrefix,
                    languageHint(language),
                    maxSuggestions(),
                    maxSuggestionChars());

            String raw = ollamaApiCore.postChat(buildRequest(prompt));
            List<String> suggestions = parseSuggestions(raw, effectivePrefix);

            log.debug("Sinh được {} gợi ý cho prefix '{}'", suggestions.size(), effectivePrefix);
            return suggestions;
        } catch (Exception e) {
            log.warn("Không sinh được gợi ý cho prefix '{}': {}", normalizedPrefix, e.getMessage());
            return List.of();
        } finally {
            ollamaSemaphore.release();
        }
    }

    /** Dựng request native Ollama cho tác vụ sinh gợi ý. */
    private OllamaChatRequestDto buildRequest(String prompt) {
        AppProperties.Suggestion suggestion = suggestionConfig();

        OllamaChatRequestDto.OllamaMessage message = OllamaChatRequestDto.OllamaMessage.builder()
                .role("user")
                .content(prompt)
                .build();

        OllamaChatRequestDto.Options options = OllamaChatRequestDto.Options.builder()
                .temperature(temperature())
                .numPredict(maxTokens())
                .numCtx(numCtx())
                .build();

        return OllamaChatRequestDto.builder()
                .model(suggestion != null ? suggestion.getModel() : null)
                .messages(List.of(message))
                .stream(false)
                // Buộc Ollama đảm bảo output là JSON hợp lệ -> parse an toàn.
                .format("json")
                .think(suggestion != null ? suggestion.getThink() : null)
                .keepAlive(suggestion != null ? suggestion.getKeepAlive() : null)
                .options(options)
                .build();
    }

    // ------------------------------------------------------------------
    // Parse & làm sạch output
    // ------------------------------------------------------------------

    /**
     * Parse response của Ollama thành danh sách gợi ý đã làm sạch.
     *
     * <p>Chiến lược: thử parse JSON trước (nhánh chính vì đã ép {@code format=json}),
     * nếu thất bại thì fallback tách theo dòng — model nhỏ đôi khi vẫn trả về text
     * thay vì JSON dù đã ép format.
     *
     * @param effectivePrefix prefix đã dùng trong prompt — cũng là căn cứ để kiểm tra
     *                        gợi ý có phải là phần hoàn thiện của prefix hay không
     */
    private List<String> parseSuggestions(String raw, String effectivePrefix) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        String content = extractMessageContent(raw);
        if (content == null || content.isBlank()) {
            return List.of();
        }

        String cleaned = AiTextSanitizer.sanitize(content, false);
        if (cleaned == null || cleaned.isBlank()) {
            return List.of();
        }

        return filterCandidates(extractCandidates(cleaned), effectivePrefix);
    }

    /**
     * Lấy nội dung message từ response JSON của Ollama:
     * {@code {"message": {"role": "assistant", "content": "..."}}}.
     * Không dùng DTO cứng để chịu được field thừa/thiếu giữa các phiên bản Ollama.
     */
    private String extractMessageContent(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode contentNode = root.path("message").path("content");
            if (contentNode.isTextual()) {
                return contentNode.asText();
            }
        } catch (Exception e) {
            // Ollama lỗi giữa chừng có thể trả NDJSON nhiều object -> thử nhánh fallback.
            log.warn("Không parse được JSON response từ Ollama, thử fallback dạng dòng: {}", e.getMessage());
        }
        return raw;
    }

    /** Trích danh sách ứng viên từ text model trả về (JSON array/object hoặc text nhiều dòng). */
    private List<String> extractCandidates(String cleaned) {
        JsonNode json = tryParseJson(cleaned);

        if (json != null) {
            JsonNode arrayNode = json.isArray() ? json : json.path("suggestions");
            if (arrayNode.isArray()) {
                List<String> fromJson = new ArrayList<>();
                for (JsonNode item : arrayNode) {
                    if (item.isTextual()) {
                        fromJson.add(item.asText());
                    }
                }
                if (!fromJson.isEmpty()) {
                    return fromJson;
                }
            }
        }

        // Fallback: một gợi ý mỗi dòng.
        List<String> lines = new ArrayList<>();
        for (String line : cleaned.split("\\R")) {
            lines.add(line);
        }
        return lines;
    }

    /** Thử parse JSON từ text, trả về {@code null} nếu không phải JSON hợp lệ. */
    private JsonNode tryParseJson(String text) {
        try {
            JsonNode node = objectMapper.readTree(text);
            return (node == null || node.isMissingNode() || node.isNull()) ? null : node;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Làm sạch, kiểm tra và khử trùng lặp từng ứng viên.
     *
     * <p>Quy tắc loại bỏ: rỗng; trùng với chính prefix; KHÔNG bắt đầu bằng prefix
     * (vì FE dùng gợi ý để thay thế text — gợi ý phải là phần hoàn thiện của prefix);
     * dài hơn {@code maxSuggestionChars}; hoặc đã xuất hiện trước đó (không phân biệt
     * hoa/thường).
     */
    private List<String> filterCandidates(List<String> candidates, String effectivePrefix) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> result = new ArrayList<>();
        String prefixLower = effectivePrefix.toLowerCase(Locale.ROOT);

        for (String candidate : candidates) {
            String item = AiTextSanitizer.sanitize(stripListMarker(candidate), true);
            if (item == null || item.isBlank()) {
                continue;
            }

            if (item.length() > maxSuggestionChars()) {
                continue;
            }

            String itemLower = item.toLowerCase(Locale.ROOT);
            if (itemLower.equals(prefixLower) || !itemLower.startsWith(prefixLower)) {
                continue;
            }

            if (seen.add(itemLower)) {
                result.add(item);
            }

            if (result.size() >= maxSuggestions()) {
                break;
            }
        }

        return result;
    }

    /** Bỏ tiền tố đánh số/bullet mà model nhỏ hay tự thêm: {@code 1. }, {@code - }, {@code * }. */
    private String stripListMarker(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().replaceFirst("^(?:\\d+[.)]|[-*+\u2022])\\s+", "");
    }

    // ------------------------------------------------------------------
    // Cache
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private List<String> readCache(String cacheKey) {
        try {
            Cache cache = cacheManager.getCache(CacheName.AI_SUGGESTION);
            if (cache == null) {
                return null;
            }

            Cache.ValueWrapper wrapper = cache.get(cacheKey);
            if (wrapper == null || wrapper.get() == null) {
                return null;
            }

            Object value = wrapper.get();
            return value instanceof List<?> list ? new ArrayList<>((List<String>) list) : null;
        } catch (Exception e) {
            // Cache là thành phần OPTIONAL: Redis down không được làm mất tính năng.
            // Bỏ qua cache và sinh gợi ý trực tiếp từ model.
            log.debug("Không đọc được cache gợi ý '{}', bỏ qua cache: {}", cacheKey, e.getMessage());
            return null;
        }
    }

    private void writeCache(String cacheKey, List<String> suggestions) {
        try {
            Cache cache = cacheManager.getCache(CacheName.AI_SUGGESTION);
            if (cache != null) {
                // Bọc lại thành ArrayList để chắc chắn serialize được với JdkSerializationRedisSerializer.
                cache.put(cacheKey, new ArrayList<>(suggestions));
            }
        } catch (Exception e) {
            // Ghi cache thất bại chỉ làm mất tác dụng tăng tốc, không phải lỗi nghiệp vụ.
            log.debug("Không ghi được cache gợi ý '{}', bỏ qua: {}", cacheKey, e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Guard & config
    // ------------------------------------------------------------------

    /** Trim + gộp khoảng trắng. Trả {@code null} nếu rỗng. */
    private String normalizePrefix(String prefix) {
        if (prefix == null) {
            return null;
        }
        String normalized = prefix.strip().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * Cắt prefix dài quá ngân sách, giữ phần ĐUÔI — với autocomplete, phần người dùng
     * vừa gõ (cuối chuỗi) chứa nhiều ngữ cảnh hơn phần đầu.
     */
    private String truncatePrefix(String prefix) {
        int maxChars = maxPrefixChars();
        if (prefix.length() <= maxChars) {
            return prefix;
        }
        return prefix.substring(prefix.length() - maxChars);
    }

    /**
     * Đọc ngôn ngữ mong muốn từ System Setting. Best-effort: setting đọc lỗi (DB down)
     * chỉ làm mất phần "khớp ngôn ngữ", không được chặn tính năng.
     */
    private String resolveLanguage() {
        try {
            String language = systemSettingService.getString("system.language", DEFAULT_LANGUAGE);
            return (language == null || language.isBlank()) ? DEFAULT_LANGUAGE : language.trim();
        } catch (Exception e) {
            log.debug("Không đọc được system.language, dùng mặc định '{}': {}", DEFAULT_LANGUAGE, e.getMessage());
            return DEFAULT_LANGUAGE;
        }
    }

    private String languageHint(String language) {
        if (DEFAULT_LANGUAGE.equalsIgnoreCase(language)) {
            return "Tiếng Việt — giữ đúng ngôn ngữ của phần văn bản người dùng đã gõ.";
        }
        return "Giữ đúng ngôn ngữ của phần văn bản người dùng đã gõ.";
    }

    private AppProperties.Suggestion suggestionConfig() {
        return appProperties.getSuggestion();
    }

    private int maxSuggestions() {
        return readPositive(value(suggestion -> suggestion.getMaxSuggestions()), DEFAULT_MAX_SUGGESTIONS);
    }

    private int minPrefixChars() {
        return readPositive(value(suggestion -> suggestion.getMinPrefixChars()), DEFAULT_MIN_PREFIX_CHARS);
    }

    private int maxPrefixChars() {
        return readPositive(value(suggestion -> suggestion.getMaxPrefixChars()), DEFAULT_MAX_PREFIX_CHARS);
    }

    private int maxSuggestionChars() {
        return readPositive(value(suggestion -> suggestion.getMaxSuggestionChars()), DEFAULT_MAX_SUGGESTION_CHARS);
    }

    private int maxTokens() {
        return readPositive(value(suggestion -> suggestion.getMaxTokens()), DEFAULT_MAX_TOKENS);
    }

    private int numCtx() {
        return readPositive(value(suggestion -> suggestion.getNumCtx()), DEFAULT_NUM_CTX);
    }

    private int maxConcurrentRequests() {
        return readPositive(value(suggestion -> suggestion.getMaxConcurrentRequests()),
                DEFAULT_MAX_CONCURRENT_REQUESTS);
    }

    private double temperature() {
        Double configured = value(suggestion -> suggestion.getTemperature());
        return (configured != null && configured >= 0) ? configured : DEFAULT_TEMPERATURE;
    }

    /** Đọc một thuộc tính từ config group {@code suggestion}, trả {@code null} nếu group chưa khai báo. */
    private <T> T value(java.util.function.Function<AppProperties.Suggestion, T> extractor) {
        AppProperties.Suggestion suggestion = suggestionConfig();
        return suggestion != null ? extractor.apply(suggestion) : null;
    }

    /** Giá trị null/<= 0 coi như chưa cấu hình → dùng default (theo convention của repo). */
    private int readPositive(Integer configured, int defaultValue) {
        return (configured != null && configured > 0) ? configured : defaultValue;
    }
}
