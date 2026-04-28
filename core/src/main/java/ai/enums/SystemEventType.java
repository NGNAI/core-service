package ai.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SystemEventType {
    SYSTEM_CONNECTED("Sent once immediately after the SSE connection is established successfully"),
    SYSTEM_EVENT("Generic system-level event not tied to a specific domain"),
    DATA_INGESTION_STATUS_UPDATED("Fired when a data ingestion job transitions to an intermediate status (e.g. PROCESSING)"),
    DATA_INGESTION_COMPLETED("Fired when a data ingestion job finishes successfully"),
    DATA_INGESTION_FAILED("Fired when a data ingestion job encounters an error and cannot proceed"),
    DATA_INGESTION_DELETE_QUEUED("Fired when a delete request is accepted and queued for processing"),
    DATA_INGESTION_DELETED("Fired when a data ingestion item is deleted successfully"),
    DATA_INGESTION_DELETE_FAILED("Fired when delete processing fails and will be retried by scheduler"),
    NOTEBOOK_SOURCE_STATUS_UPDATED("Fired when a notebook source transitions to an intermediate ingestion status"),
    NOTEBOOK_SOURCE_COMPLETED("Fired when a notebook source is embedded successfully"),
    NOTEBOOK_SOURCE_FAILED("Fired when notebook source embedding fails"),
    NOTEBOOK_SOURCE_DELETE_QUEUED("Fired when notebook source delete is accepted and queued"),
    NOTEBOOK_SOURCE_DELETED("Fired when notebook source is deleted successfully"),
    NOTEBOOK_SOURCE_DELETE_FAILED("Fired when notebook source delete fails and will be retried by scheduler");

    private final String description;
}