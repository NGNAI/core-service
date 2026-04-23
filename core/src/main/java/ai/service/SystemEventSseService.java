package ai.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ai.dto.own.response.SystemSseEventResponseDto;
import ai.enums.SystemEventSource;
import ai.enums.SystemEventType;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SystemEventSseService {
    // FE khi subscribe sẽ tạo một emitter với timeout khá dài (ví dụ 30 phút) để đảm bảo kết nối SSE được duy trì ổn định, tránh tình trạng timeout giữa chừng khi FE đang mở kết nối để nhận sự kiện realtime. Nếu cần thiết, FE có thể chủ động gửi yêu cầu mới để tạo emitter mới sau khi emitter cũ timeout hoặc bị đóng.
    static long DEFAULT_SSE_TIMEOUT_MILLIS = 30L * 60L * 1000L;

    // Key của map là orgId:userId để đảm bảo mỗi user trong mỗi org sẽ có một kênh SSE riêng biệt, tránh việc gửi nhầm sự kiện giữa các user khác nhau
    ConcurrentMap<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>>();

    /**
     * Hàm subscribe sẽ được gọi khi FE mở một kết nối SSE mới. Hàm sẽ tạo một SseEmitter mới, lưu vào map emitters với key là orgId:userId để sau này có thể gửi sự kiện đến đúng user đó. Hàm cũng đăng ký các callback để tự động xóa emitter khỏi map khi kết nối bị đóng hoặc gặp lỗi, tránh tình trạng rò rỉ bộ nhớ do emitter cũ không còn sử dụng nhưng vẫn tồn tại trong map.
     * @param orgId
     * @param userId
     * @return
     */
    public SseEmitter subscribe(UUID orgId, UUID userId) {
        String key = key(orgId, userId);
        SseEmitter emitter = new SseEmitter(DEFAULT_SSE_TIMEOUT_MILLIS);

        CopyOnWriteArrayList<SseEmitter> keyEmitters = emitters.computeIfAbsent(key, k -> new CopyOnWriteArrayList<SseEmitter>());
        keyEmitters.add(emitter);

        emitter.onCompletion(() -> removeEmitter(key, emitter));
        emitter.onTimeout(() -> removeEmitter(key, emitter));
        emitter.onError(exception -> removeEmitter(key, emitter));

        publishToSingleEmitter(key, emitter, SystemEventType.SYSTEM_CONNECTED, SystemEventSource.SYSTEM, null, "connected");
        return emitter;
    }

    /**
     * Hàm publish sẽ được gọi khi có một sự kiện mới cần gửi đến FE. Dựa vào orgId và userId của sự kiện, hàm sẽ tìm đúng kênh SSE của user đó và gửi sự kiện đi. Nếu type hoặc source của sự kiện không được cung cấp, sẽ mặc định là SYSTEM_EVENT và SYSTEM để đảm bảo tính nhất quán trong contract sự kiện.
     * @param orgId
     * @param userId
     * @param type
     * @param source
     * @param sourceId
     * @param data
     */
    public void publish(UUID orgId, UUID userId, SystemEventType type, SystemEventSource source, String sourceId, Object data) {
        if (orgId == null || userId == null) {
            return;
        }

        String key = key(orgId, userId);
        List<SseEmitter> keyEmitters = emitters.get(key);
        if (keyEmitters == null || keyEmitters.isEmpty()) {
            return;
        }

        SystemEventType eventType = type == null ? SystemEventType.SYSTEM_EVENT : type;
        SystemEventSource eventSource = source == null ? SystemEventSource.SYSTEM : source;

        for (SseEmitter emitter : keyEmitters) {
            publishToSingleEmitter(key, emitter, eventType, eventSource, sourceId, data);
        }
    }

    /**
     * Hàm publishToSingleEmitter sẽ gửi một sự kiện đến một emitter cụ thể. Nếu quá trình gửi gặp lỗi (ví dụ do kết nối bị mất), hàm sẽ tự động xóa emitter đó khỏi map để tránh rò rỉ bộ nhớ và gọi completeWithError để đóng kết nối SSE một cách an toàn.
     * @param key
     * @param emitter
     * @param type
     * @param source
     * @param sourceId
     * @param data
     */
    private void publishToSingleEmitter(String key, SseEmitter emitter, SystemEventType type, SystemEventSource source, String sourceId, Object data) {
        try {
            SystemSseEventResponseDto payload = SystemSseEventResponseDto.builder()
                    .type(type)
                    .source(source)
                    .sourceId(sourceId)
                    .data(data)
                    .timestamp(System.currentTimeMillis())
                    .build();
            emitter.send(SseEmitter.event().name(type.name()).data(payload));
        } catch (IOException exception) {
            removeEmitter(key, emitter);
            emitter.completeWithError(exception);
        }
    }

    /**
     * Hàm removeEmitter sẽ xóa một emitter khỏi map emitters dựa vào key và emitter cụ thể. Hàm sẽ được gọi khi kết nối SSE bị đóng hoặc gặp lỗi, giúp đảm bảo rằng các emitter không còn sử dụng sẽ được dọn dẹp khỏi bộ nhớ một cách hiệu quả.
     * @param key
     * @param emitter
     */
    private void removeEmitter(String key, SseEmitter emitter) {
        List<SseEmitter> keyEmitters = emitters.get(key);
        if (keyEmitters == null) {
            return;
        }

        keyEmitters.remove(emitter);
        if (keyEmitters.isEmpty()) {
            emitters.remove(key);
        }
    }

    /**
     * Hàm key sẽ tạo một chuỗi key duy nhất dựa trên orgId và userId theo định dạng orgId:userId. Hàm này giúp đảm bảo rằng mỗi user trong mỗi org sẽ có một kênh SSE riêng biệt, tránh việc gửi nhầm sự kiện giữa các user khác nhau.
     * @param orgId
     * @param userId
     * @return
     */
    private String key(UUID orgId, UUID userId) {
        return orgId + ":" + userId;
    }
}