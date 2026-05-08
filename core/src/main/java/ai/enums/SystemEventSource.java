package ai.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SystemEventSource {
    SYSTEM("Events emitted by the internal system (e.g. connection lifecycle)"),
    DATA_INGESTION("Events emitted by the data ingestion pipeline"),
    NOTEBOOK_SOURCE("Events emitted by notebook source ingestion pipeline"),
    TOPIC("Events emitted by topic operations (e.g. title generation)");

    private final String description;
}