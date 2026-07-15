package ai.util;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import lombok.experimental.UtilityClass;

/**
 * Utility class for exporting data to CSV format.
 * Supports writing CSV with BOM for Excel compatibility.
 */
@UtilityClass
public class CsvUtil {

    /**
     * Write CSV data to an OutputStream with BOM (UTF-8) for Excel compatibility.
     *
     * @param out     the OutputStream to write to
     * @param headers the column headers
     * @param rows    the data rows, each row is a list of cell values
     */
    public static void writeCsv(OutputStream out, List<String> headers, List<List<String>> rows) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            // Write BOM for Excel UTF-8 compatibility
            writer.write('\uFEFF');

            // Write headers
            writer.write(escapeCsvLine(headers));
            writer.newLine();

            // Write data rows
            for (List<String> row : rows) {
                writer.write(escapeCsvLine(row));
                writer.newLine();
            }

            writer.flush();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to write CSV", e);
        }
    }

    /**
     * Escape a list of values for CSV format.
     * Values containing commas, quotes, or newlines are wrapped in double quotes.
     * Double quotes inside values are escaped by doubling them.
     */
    private static String escapeCsvLine(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            String value = values.get(i) != null ? values.get(i) : "";
            if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
                sb.append('"').append(value.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(value);
            }
        }
        return sb.toString();
    }
}