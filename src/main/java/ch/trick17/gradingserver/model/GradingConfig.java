package ch.trick17.gradingserver.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS;

@JsonTypeInfo(use = CLASS, property = "@class")
public sealed interface GradingConfig
        permits ImplGradingConfig, TestSuiteGradingConfig {
}
