package com.angrysurfer.beats.diagnostic;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for building diagnostic logs
 */
public class DiagnosticLogBuilder {
    private final StringBuilder log = new StringBuilder();
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String title;
    private static final Logger logger = LoggerFactory.getLogger(DiagnosticLogBuilder.class);

    /**
     * Constructor with title
     * 
     * @param title The diagnostic title
     */
    public DiagnosticLogBuilder(String title) {
        this.title = title;
        // Add header with timestamp
        log.append("=== ").append(title).append(" ===\n");
        log.append("Date/Time: ").append(LocalDateTime.now().format(dateFormatter)).append("\n\n");
    }

    /**
     * Add a section header
     * 
     * @param sectionTitle The section title
     * @return This builder for chaining
     */
    public DiagnosticLogBuilder addSection(String sectionTitle) {
        log.append("\n").append(sectionTitle).append("\n");
        log.append("-".repeat(sectionTitle.length())).append("\n");
        return this;
    }

    /**
     * Add a line of text
     * 
     * @param text The text to add
     * @return This builder for chaining
     */
    public DiagnosticLogBuilder addLine(String text) {
        log.append(text).append("\n");
        return this;
    }

    /**
     * Add multiple lines of text
     * 
     * @param lines The lines to add
     * @return This builder for chaining
     */
    public DiagnosticLogBuilder addLines(String... lines) {
        for (String line : lines) {
            addLine(line);
        }
        return this;
    }

    /**
     * Add an indented line of text
     * 
     * @param text        The text to add
     * @param indentLevel The indentation level (number of spaces = 3 * indentLevel)
     * @return This builder for chaining
     */
    public DiagnosticLogBuilder addIndentedLine(String text, int indentLevel) {
        log.append(" ".repeat(3 * indentLevel)).append(text).append("\n");
        return this;
    }

    /**
     * Add an error message to the log and error collection
     * 
     * @param errorMessage The error message
     * @return This builder for chaining
     */
    public DiagnosticLogBuilder addError(String errorMessage) {
        String message = "ERROR: " + errorMessage;
        log.append(message).append("\n");
        errors.add(errorMessage);
        return this;
    }

    /**
     * Add an exception to the log and error collection
     * 
     * @param e The exception
     * @return This builder for chaining
     */
    public DiagnosticLogBuilder addException(Exception e) {
        // Add the error message
        addError(e.getMessage());

        // Add the stack trace
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        log.append("Stack trace:\n").append(sw.toString()).append("\n");

        // Also emit to the logger so the exception is visible in configured logs
        logger.error("Captured exception for diagnostics: {}", e.getMessage(), e);

        return this;
    }

    /**
     * Add a subsection header (slightly less prominent than a section)
     * 
     * @param subSectionTitle The subsection title
     * @return This builder for chaining
     */
    public DiagnosticLogBuilder addSubSection(String subSectionTitle) {
        log.append("\n").append(subSectionTitle).append("\n");
        log.append("~".repeat(subSectionTitle.length())).append("\n");
        return this;
    }

    /**
     * Add a warning message to the log
     * 
     * @param warningMessage The warning message
     * @return This builder for chaining
     */
    public DiagnosticLogBuilder addWarning(String warningMessage) {
        log.append("WARNING: ").append(warningMessage).append("\n");
        warnings.add(warningMessage);
        return this;
    }

    /**
     * Check if the log contains any errors
     * 
     * @return True if errors are present
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Check if the log contains any warnings
     * 
     * @return True if warnings are present
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * Get all errors found during diagnostics
     * 
     * @return List of error messages
     */
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    /**
     * Get all warnings found during diagnostics
     * 
     * @return List of warning messages
     */
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }

    /**
     * Build the final log text
     * 
     * @return The complete log as a string
     */
    public String build() {
        StringBuilder result = new StringBuilder(log);

        // Add summary of errors and warnings
        boolean hasIssues = !errors.isEmpty() || !warnings.isEmpty();

        if (hasIssues) {
            result.append("\n=== ISSUE SUMMARY ===\n");

            if (!errors.isEmpty()) {
                result.append("Found ").append(errors.size()).append(" errors:\n");
                for (int i = 0; i < errors.size(); i++) {
                    result.append(i + 1).append(". ").append(errors.get(i)).append("\n");
                }
            }

            if (!warnings.isEmpty()) {
                if (!errors.isEmpty()) {
                    result.append("\n");
                }
                result.append("Found ").append(warnings.size()).append(" warnings:\n");
                for (int i = 0; i < warnings.size(); i++) {
                    result.append(i + 1).append(". ").append(warnings.get(i)).append("\n");
                }
            }
        } else {
            result.append("\n=== DIAGNOSTICS COMPLETED SUCCESSFULLY ===\n");
            result.append("No issues found.\n");
        }

        return result.toString();
    }

    /**
     * Build the log without the header and summary
     * 
     * @return The log content without header and summary
     */
    public String buildWithoutHeader() {
        return log.toString();
    }

    /**
     * Get the diagnostic title
     * 
     * @return The title
     */
    public String getTitle() {
        return title;
    }
}