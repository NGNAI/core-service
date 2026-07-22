package ai.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.annotation.Audited;
import ai.constant.CacheName;
import ai.dto.own.request.SystemSettingCreateRequestDto;
import ai.dto.own.request.SystemSettingUpdateRequestDto;
import ai.dto.own.response.SystemSettingGroupResponseDto;
import ai.dto.own.response.SystemSettingResponseDto;
import ai.entity.postgres.SystemSettingEntity;
import ai.enums.ApiResponseStatus;
import ai.enums.AuditAction;
import ai.enums.AuditResource;
import ai.exception.AppException;
import ai.mapper.SystemSettingMapper;
import ai.repository.SystemSettingRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class SystemSettingService {

    SystemSettingRepository settingRepository;
    SystemSettingMapper settingMapper;

    public SystemSettingResponseDto getByKey(String key) {
        return settingRepository.findByKey(key)
                .map(settingMapper::entityToResponseDto)
                .orElseThrow(() -> new AppException(ApiResponseStatus.SETTING_KEY_NOT_EXISTS));
    }

    public List<SystemSettingGroupResponseDto> getAllGrouped() {
        List<SystemSettingEntity> settings = settingRepository.findByIsActiveTrueOrderByGroupNameAscDisplayOrderAsc();

        Map<String, List<SystemSettingResponseDto>> grouped = settings.stream()
                .collect(Collectors.groupingBy(
                        SystemSettingEntity::getGroupName,
                        LinkedHashMap::new,
                        Collectors.mapping(settingMapper::entityToResponseDto, Collectors.toList())
                ));

        return grouped.entrySet().stream()
                .map(entry -> {
                    SystemSettingGroupResponseDto group = new SystemSettingGroupResponseDto();
                    group.setGroupName(entry.getKey());
                    group.setSettings(entry.getValue());
                    return group;
                })
                .collect(Collectors.toList());
    }

    public List<SystemSettingResponseDto> getByGroup(String groupName) {
        return settingRepository.findByGroupNameAndIsActiveTrueOrderByDisplayOrderAsc(groupName)
                .stream()
                .map(settingMapper::entityToResponseDto)
                .collect(Collectors.toList());
    }

    public List<SystemSettingResponseDto> getPublicSettings() {
        return settingRepository.findByIsPublicTrueAndIsActiveTrueOrderByGroupNameAscDisplayOrderAsc()
                .stream()
                .map(settingMapper::entityToResponseDto)
                .collect(Collectors.toList());
    }

    public Map<String, String> getPublicSettingsMap() {
        return settingRepository.findByIsPublicTrueAndIsActiveTrueOrderByGroupNameAscDisplayOrderAsc()
                .stream()
                .collect(Collectors.toMap(SystemSettingEntity::getKey, SystemSettingEntity::getValue));
    }

    // ========================================================================
    // Helper methods: đọc giá trị setting theo kiểu dữ liệu (typed)
    // ========================================================================

    /**
     * Lấy giá trị setting dạng String. Trả về defaultValue nếu setting không tồn tại hoặc không active.
     * Kết quả được cache theo key để giảm tải DB.
     * @param key khóa setting
     * @param defaultValue giá trị mặc định nếu không tìm thấy
     * @return giá trị setting hoặc defaultValue
     */
    @Cacheable(value = CacheName.SYSTEM_SETTINGS, key = "#key", unless = "#result == null")
    public String getString(String key, String defaultValue) {
        return settingRepository.findByKey(key)
                .filter(entity -> entity.getIsActive() != null && entity.getIsActive())
                .map(SystemSettingEntity::getValue)
                .filter(v -> v != null && !v.isBlank())
                .orElse(defaultValue);
    }

    /**
     * Lấy giá trị setting dạng int. Trả về defaultValue nếu setting không tồn tại, không active, hoặc không parse được.
     * @param key khóa setting
     * @param defaultValue giá trị mặc định nếu không tìm thấy hoặc không hợp lệ
     * @return giá trị setting hoặc defaultValue
     */
    public int getInt(String key, int defaultValue) {
        try {
            String value = getString(key, null);
            if (value == null) return defaultValue;
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Setting '{}' không phải số nguyên hợp lệ, sử dụng mặc định: {}", key, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Lấy giá trị setting dạng long. Trả về defaultValue nếu setting không tồn tại, không active, hoặc không parse được.
     * @param key khóa setting
     * @param defaultValue giá trị mặc định nếu không tìm thấy hoặc không hợp lệ
     * @return giá trị setting hoặc defaultValue
     */
    public long getLong(String key, long defaultValue) {
        try {
            String value = getString(key, null);
            if (value == null) return defaultValue;
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Setting '{}' không phải số nguyên hợp lệ, sử dụng mặc định: {}", key, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Lấy giá trị setting dạng double. Trả về defaultValue nếu setting không tồn tại, không active, hoặc không parse được.
     * @param key khóa setting
     * @param defaultValue giá trị mặc định nếu không tìm thấy hoặc không hợp lệ
     * @return giá trị setting hoặc defaultValue
     */
    public double getDouble(String key, double defaultValue) {
        try {
            String value = getString(key, null);
            if (value == null) return defaultValue;
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Setting '{}' không phải số thực hợp lệ, sử dụng mặc định: {}", key, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Lấy giá trị setting dạng boolean. Trả về defaultValue nếu setting không tồn tại hoặc không active.
     * Chấp nhận "true"/"false" (không phân biệt hoa thường).
     * @param key khóa setting
     * @param defaultValue giá trị mặc định nếu không tìm thấy hoặc không hợp lệ
     * @return giá trị setting hoặc defaultValue
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getString(key, null);
        if (value == null) return defaultValue;
        return Boolean.parseBoolean(value.trim());
    }

    @Audited(action = AuditAction.CREATE, resource = AuditResource.SYSTEM_SETTING, description = "Tạo cấu hình hệ thống: {0}")
    @CacheEvict(value = CacheName.SYSTEM_SETTINGS, key = "#requestDto.key")
    @Transactional
    public SystemSettingResponseDto create(SystemSettingCreateRequestDto requestDto) {
        if (settingRepository.existsByKey(requestDto.getKey())) {
            throw new AppException(ApiResponseStatus.SETTING_KEY_EXISTED);
        }

        SystemSettingEntity entity = settingMapper.createRequestDtoToEntity(requestDto);
        return settingMapper.entityToResponseDto(settingRepository.save(entity));
    }

    @Audited(action = AuditAction.UPDATE, resource = AuditResource.SYSTEM_SETTING, resourceIdExpression = "#arg0", description = "Cập nhật cấu hình hệ thống: {0}")
    @CacheEvict(value = CacheName.SYSTEM_SETTINGS, key = "#key")
    @Transactional
    public SystemSettingResponseDto update(String key, SystemSettingUpdateRequestDto requestDto) {
        SystemSettingEntity entity = settingRepository.findByKey(key)
                .orElseThrow(() -> new AppException(ApiResponseStatus.SETTING_KEY_NOT_EXISTS));

        settingMapper.updateEntity(entity, requestDto);
        return settingMapper.entityToResponseDto(settingRepository.save(entity));
    }

    @Audited(action = AuditAction.UPDATE, resource = AuditResource.SYSTEM_SETTING, description = "Cập nhật hàng loạt cấu hình hệ thống")
    @CacheEvict(value = CacheName.SYSTEM_SETTINGS, allEntries = true)
    @Transactional
    public List<SystemSettingResponseDto> bulkUpdate(List<SystemSettingUpdateRequestDto> requestDtos) {
        return requestDtos.stream().map(dto -> {
            if (dto.getKey() == null || dto.getKey().isBlank()) {
                throw new AppException(ApiResponseStatus.SETTING_KEY_CAN_NOT_BE_NULL_OR_EMPTY);
            }
            SystemSettingEntity entity = settingRepository.findByKey(dto.getKey())
                    .orElseThrow(() -> new AppException(ApiResponseStatus.SETTING_KEY_NOT_EXISTS));
            settingMapper.updateEntity(entity, dto);
            return settingMapper.entityToResponseDto(settingRepository.save(entity));
        }).collect(Collectors.toList());
    }

    @Audited(action = AuditAction.DELETE, resource = AuditResource.SYSTEM_SETTING, resourceIdExpression = "#arg0", description = "Xoá cấu hình hệ thống: {0}")
    @CacheEvict(value = CacheName.SYSTEM_SETTINGS, key = "#key")
    @Transactional
    public void delete(String key) {
        if (!settingRepository.existsByKey(key)) {
            throw new AppException(ApiResponseStatus.SETTING_KEY_NOT_EXISTS);
        }
        settingRepository.deleteByKey(key);
    }

    @Audited(action = AuditAction.DELETE, resource = AuditResource.SYSTEM_SETTING, description = "Xoá cấu hình hệ thống theo ID")
    @CacheEvict(value = CacheName.SYSTEM_SETTINGS, allEntries = true)
    @Transactional
    public void deleteById(UUID id) {
        if (!settingRepository.existsById(id)) {
            throw new AppException(ApiResponseStatus.SETTING_KEY_NOT_EXISTS);
        }
        settingRepository.deleteById(id);
    }
}
