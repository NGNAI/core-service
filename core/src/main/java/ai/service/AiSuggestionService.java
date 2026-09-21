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

import ai.constant.AiPromptTemplates;
import ai.constant.AiSuggestionConfig;
import ai.constant.CacheName;
import ai.util.AiTextSanitizer;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Sinh gợi ý câu hỏi (ask-autocomplete) cho ô chat.
 *
 * <p><b>Không gọi model trực tiếp.</b> Việc sinh text đi qua đúng cơ chế chung của hệ
 * thống — {@link RagService#generalQuestionsRaw(String)} → {@code RagApiService.general()}
 * → {@code POST /generate/v1/chat/completions_simple}. Nhờ vậy:
 * <ul>
 *   <li>Model do RAG service quyết định ({@code ai.suggestion.model}, fallback {@code ai.model}).
 *       RAG có thêm model nhỏ hơn → đổi setting, không cần sửa core-service.</li>
 *   <li>Cấu hình AI (temperature tất định, maxTokens) áp dụng nhất quán với các tác vụ
 *       phụ trợ khác (sinh tiêu đề, nén summary).</li>
 * </ul>
 *
 * <p>Lớp này chỉ chịu trách nhiệm phần <b>đặc thù của autocomplete</b>:
 * <ol>
 *   <li>Chặn khi prefix quá ngắn (không có giá trị gợi ý, chỉ tốn tài nguyên).</li>
 *   <li>Cache Redis theo {@code prefix + ngôn ngữ} để chặn spam model khi người dùng gõ liên tục.</li>
 *   <li>Semaphore giới hạn số request đồng thời.</li>
 *   <li>Parse output của model thành danh sách gợi ý: lọc bỏ mục không bắt đầu bằng prefix,
 *       khử trùng lặp, clamp độ dài.</li>
 * </ol>
 *
 * <p>Đây là tác vụ <b>best-effort</b>: mọi lỗi (model lỗi, RAG down, Redis down, setting lỗi)
 * đều được nuốt và trả về danh sách rỗng để FE degrade êm — autocomplete không bao giờ
 * được phép làm hỏng trải nghiệm chat.
 */
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Service
public class AiSuggestionService {

    final RagService ragService;

    final ObjectMapper objectMapper;

    final SystemSettingService systemSettingService;

    final CacheManager cacheManager;

    /** Giới hạn request đồng thời tới model. Khởi tạo trong {@link #init()} theo config. */
    Semaphore generationSemaphore;

    @PostConstruct
    void init() {
        generationSemaphore = new Semaphore(maxConcurrentRequests());
    }

    /**
     * Entry point cho controller: sinh danh sách gợi ý câu hỏi cho prefix đang gõ.
     *
     * <p>Toàn bộ thân method được bọc trong {@code try/catch} — không chỉ riêng lời gọi model.
     * Ngoài model/RAG, luồng này còn phụ thuộc Redis (cache) và DB (System Setting); nếu chỉ
     * bắt lỗi quanh lời gọi model thì Redis/DB down sẽ ném ra ngoài và
     * {@code GlobalExceptionHandler} trả 500, phá vỡ cam kết "autocomplete không bao giờ 5xx".
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

            String cacheKey = normalizedPrefix + "|" + resolveLanguage();

            List<String> cached = readCache(cacheKey);
            if (cached != null) {
                return cached;
            }

            List<String> generated = generateSuggestions(normalizedPrefix);
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

    private List<String> generateSuggestions(String normalizedPrefix) {
        // tryAcquire() không tham số = thử lấy permit ngay lập tức (timeout 0):
        // hết permit thì fail fast thay vì xếp hàng, tránh dồn request vào model.
        if (!generationSemaphore.tryAcquire()) {
            log.warn("Bỏ qua gợi ý cho prefix '{}': đã đạt giới hạn {} request đồng thời",
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
                    languageHint(resolveLanguage()),
                    maxSuggestions(),
                    maxSuggestionChars());

            String raw = ragService.generalQuestionsRaw(prompt);
            List<String> suggestions = parseSuggestions(raw, effectivePrefix);

            log.debug("Sinh được {} gợi ý cho prefix '{}'", suggestions.size(), effectivePrefix);
            return suggestions;
        } catch (Exception e) {
            log.warn("Không sinh được gợi ý cho prefix '{}': {}", normalizedPrefix, e.getMessage());
            return List.of();
        } finally {
            generationSemaphore.release();
        }
    }

    // ------------------------------------------------------------------
    // Parse & làm sạch output
    // ------------------------------------------------------------------

    /**
     * Parse output của model thành danh sách gợi ý đã làm sạch.
     *
     * <p>Chiến lược: thử parse JSON trước (nhánh chính vì prompt đã yêu cầu trả JSON
     * {@code {"suggestions": [...]}}), nếu thất bại thì fallback tách theo dòng — model nhỏ
     * đôi khi vẫn trả text thay vì JSON.
     *
     * @param effectivePrefix prefix đã dùng trong prompt — cũng là căn cứ để kiểm tra
     *                        gợi ý có phải là phần hoàn thiện của prefix hay không
     */
    private List<String> parseSuggestions(String raw, String effectivePrefix) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        String cleaned = AiTextSanitizer.sanitize(raw, false);
        if (cleaned == null || cleaned.isBlank()) {
            return List.of();
        }

        return filterCandidates(extractCandidates(cleaned), effectivePrefix);
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
            // Model đôi khi bọc JSON trong code fence -> bỏ fence trước khi parse.
            String candidate = text.trim();
            if (candidate.startsWith("```")) {
                candidate = candidate.replaceFirst("^```[a-zA-Z]*\\s*", "")
                        .replaceFirst("\\s*```$", "");
            }
            JsonNode node = objectMapper.readTree(candidate);
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
    // Cache (optional — Redis down chỉ mất phần tăng tốc)
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
            String language = systemSettingService.getString("system.language",
                    AiSuggestionConfig.DEFAULT_LANGUAGE);
            return (language == null || language.isBlank())
                    ? AiSuggestionConfig.DEFAULT_LANGUAGE
                    : language.trim();
        } catch (Exception e) {
            log.debug("Không đọc được system.language, dùng mặc định '{}': {}",
                    AiSuggestionConfig.DEFAULT_LANGUAGE, e.getMessage());
            return AiSuggestionConfig.DEFAULT_LANGUAGE;
        }
    }

    private String languageHint(String language) {
        if (AiSuggestionConfig.DEFAULT_LANGUAGE.equalsIgnoreCase(language)) {
            return "Tiếng Việt — giữ đúng ngôn ngữ của phần văn bản người dùng đã gõ.";
        }
        return "Giữ đúng ngôn ngữ của phần văn bản người dùng đã gõ.";
    }

    private int maxSuggestions() {
        return readPositiveConfig(AiSuggestionConfig.KEY_MAX_SUGGESTIONS,
                AiSuggestionConfig.DEFAULT_MAX_SUGGESTIONS);
    }

    private int minPrefixChars() {
        return readPositiveConfig(AiSuggestionConfig.KEY_MIN_PREFIX_CHARS,
                AiSuggestionConfig.DEFAULT_MIN_PREFIX_CHARS);
    }

    private int maxPrefixChars() {
        return readPositiveConfig(AiSuggestionConfig.KEY_MAX_PREFIX_CHARS,
                AiSuggestionConfig.DEFAULT_MAX_PREFIX_CHARS);
    }

    private int maxSuggestionChars() {
        return readPositiveConfig(AiSuggestionConfig.KEY_MAX_SUGGESTION_CHARS,
                AiSuggestionConfig.DEFAULT_MAX_SUGGESTION_CHARS);
    }

    private int maxConcurrentRequests() {
        return readPositiveConfig(AiSuggestionConfig.KEY_MAX_CONCURRENT_REQUESTS,
                AiSuggestionConfig.DEFAULT_MAX_CONCURRENT_REQUESTS);
    }

    /**
     * Đọc setting dạng số. Giá trị null/rỗng/{@code <= 0}/không parse được → dùng default
     * (theo quy ước chung của repo).
     */
    private int readPositiveConfig(String key, int defaultValue) {
        try {
            int configured = systemSettingService.getInt(key, -1);
            return configured > 0 ? configured : defaultValue;
        } catch (Exception e) {
            log.debug("Không đọc được setting '{}', dùng mặc định {}: {}", key, defaultValue, e.getMessage());
            return defaultValue;
        }
    }
}
