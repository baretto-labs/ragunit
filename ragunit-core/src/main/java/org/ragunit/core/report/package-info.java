/**
 * Reporting — silent observer pattern for assertion results.
 *
 * <p>{@code RagReporter} is the observer interface. After each assertion,
 * {@code RagAssert} notifies all registered reporters without interrupting the test.
 *
 * <p>Default implementation: {@code JsonFileReporter} — appends a JSON entry
 * to {@code target/ragunit-report.json} using only {@code java.io} and {@code java.time}.
 */
package org.ragunit.core.report;
