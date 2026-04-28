package ai.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SystemEventSource {
    SYSTEM("Events emitted by the internal system (e.g. connection lifecycle)"),
    DATA_INGESTION("Events emitted by the data ingestion pipeline"),
    NOTEBOOK_SOURCE("Events emitted by notebook source ingestion pipeline");

    private final String description;
}