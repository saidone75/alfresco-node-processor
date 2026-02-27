/*
 *  Alfresco Node Processor - Do things with nodes
 *  Copyright (C) 2023-2026 Saidone
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.saidone.processors;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.alfresco.core.model.NodeBodyUpdate;
import org.apache.logging.log4j.util.Strings;
import org.saidone.model.config.ProcessorConfig;
import org.saidone.utils.CastUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Metadata normalization processor.
 * <p>
 * This processor reads the configured list of operations for each source property and applies
 * them in order, allowing each operation to consume the output from the previous one.
 *
 * <h2>Supported operations</h2>
 * <ul>
 *     <li>{@code trim}: remove leading and trailing blanks from string values.</li>
 *     <li>{@code collapse-whitespace}: replace any run of whitespace with a single space.</li>
 *     <li>{@code case}: apply a case mode with {@code value=start|lower|upper}.</li>
 *     <li>{@code regex}: replace text with {@code pattern} and optional {@code replace}.</li>
 *     <li>{@code copy-to}: copy the current value to another property named in {@code value}.</li>
 *     <li>{@code delete}: set the source property to {@code null}.</li>
 *     <li>{@code parse-date-to}: parse a textual date and assign the resulting {@link Date}
 *     value to the target property named in {@code value}.</li>
 * </ul>
 *
 * <h2>Date parsing details</h2>
 * Date parsing first attempts {@link Instant#parse(CharSequence)} (ISO-8601), then falls back to
 * {@code yyyy-MM-dd HH:mm:ss.SSS}, {@code yyyy-MM-dd HH:mm:ss.SS}, and
 * {@code yyyy-MM-dd HH:mm:ss.S} interpreted in the JVM system default timezone.
 */
@Component
@Slf4j
public class NormalizeMetadataProcessor extends AbstractNodeProcessor {

    private static final String OP = "op";
    private static final String OP_TRIM = "trim";
    private static final String OP_COLLAPSE_WHITESPACE = "collapse-whitespace";
    private static final String OP_CASE = "case";
    private static final String OP_CASE_MODE_START = "start";
    private static final String OP_CASE_MODE_LOWER = "lower";
    private static final String OP_CASE_MODE_UPPER = "upper";
    private static final String OP_REGEX = "regex";
    private static final String OP_REGEX_PATTERN = "pattern";
    private static final String OP_REGEX_REPLACE = "replace";
    private static final String OP_COPY_TO = "copy-to";
    private static final String OP_DELETE = "delete";
    private static final String OP_PARSE_DATE = "parse-date-to";
    private static final String VALUE = "value";

    /**
     * Loads a node, applies normalization operations in configuration order, and updates
     * the node with the transformed properties.
     *
     * @param nodeId target node identifier.
     * @param config processor configuration containing normalization operations per property.
     */
    @Override
    @SneakyThrows
    public void processNode(String nodeId, ProcessorConfig config) {
        val opMap = parseArgs(config.getArgs());
        val node = Objects.requireNonNull(nodesApi.getNode(nodeId, null, null, null).getBody()).getEntry();
        val actualProperties = CastUtils.castToMapOfObjectObject(node.getProperties(), String.class, Object.class);
        val normalizedProperties = new HashMap<String, Object>();
        opMap.forEach((k, v) -> v.forEach(op -> apply(op, k, actualProperties, normalizedProperties)));
        val nodeBodyUpdate = new NodeBodyUpdate();
        nodeBodyUpdate.setProperties(normalizedProperties);
        nodesApi.updateNode(nodeId, nodeBodyUpdate, null, null);
    }

    /**
     * Converts raw processor arguments into an ordered map of property names and operations.
     *
     * @param args raw processor arguments.
     * @return ordered map where each property points to the list of operations to run.
     */
    private LinkedHashMap<String, List<Map<String, String>>> parseArgs(Map<String, Object> args) {
        val opMap = new LinkedHashMap<String, List<Map<String, String>>>();
        CastUtils.castToMapOfObjectObject(args, String.class, List.class).forEach((k, v) -> {
            val op = new ArrayList<Map<String, String>>();
            for (val e : v) {
                op.add(CastUtils.castToMapOfObjectObject(e, String.class, String.class));
            }
            opMap.put(k, op);
        });
        return opMap;
    }

    /**
     * Applies a single normalization operation for one property.
     * <p>
     * The source value is looked up from {@code normalizedProperties} first so that chained
     * operations can build on previous outputs, then falls back to the original value from
     * {@code actualProperties}. Unsupported operations are ignored and logged as warnings.
     *
     * @param op                   operation descriptor (must include {@code op}).
     * @param k                    source property key.
     * @param actualProperties     original node properties as loaded from Alfresco.
     * @param normalizedProperties map collecting resulting property updates.
     */
    private static void apply(Map<String, String> op, String k, Map<String, Object> actualProperties, HashMap<String, Object> normalizedProperties) {
        val v = normalizedProperties.get(k) != null ? normalizedProperties.get(k) : actualProperties.get(k);
        if (v == null) return;
        switch (op.get(OP)) {
            case OP_TRIM -> normalizedProperties.put(k, trim(v));
            case OP_COLLAPSE_WHITESPACE -> normalizedProperties.put(k, collapseWhitespace(v));
            case OP_CASE -> normalizedProperties.put(k, fixCase(v, op.get(VALUE)));
            case OP_REGEX -> normalizedProperties.put(k, regex(v, op.get(OP_REGEX_PATTERN), op.get(OP_REGEX_REPLACE)));
            case OP_COPY_TO -> normalizedProperties.put(op.get(VALUE), v);
            case OP_DELETE -> normalizedProperties.put(k, null);
            case OP_PARSE_DATE -> normalizedProperties.put(op.get(VALUE), parseDate(v));
            default -> log.warn("Unsupported metadata normalization operation '{}' for property '{}'", op.get(OP), k);
        }
    }

    /**
     * Trims leading and trailing whitespace for string values.
     *
     * @param v candidate value.
     * @return trimmed string or the original value when it is not a string.
     */
    private static Object trim(Object v) {
        if (v instanceof String) return ((String) v).trim();
        else return v;
    }

    /**
     * Replaces any sequence of whitespace characters with a single space for string values.
     *
     * @param v candidate value.
     * @return normalized string or the original value when it is not a string.
     */
    private static Object collapseWhitespace(Object v) {
        if (v instanceof String) return ((String) v).replaceAll("\\s+", " ");
        else return v;
    }

    /**
     * Applies case normalization on string values.
     *
     * @param v candidate value.
     * @param c case mode ({@code start}, {@code lower}, or {@code upper}).
     * @return transformed string or the original value when unsupported/non-string.
     */
    private static Object fixCase(Object v, String c) {
        if (v instanceof String) {
            switch (c) {
                case OP_CASE_MODE_START -> {
                    return Arrays.stream(((String) v).toLowerCase().split("\\s"))
                            .map(StringUtils::capitalize)
                            .collect(Collectors.joining(" "));
                }
                case OP_CASE_MODE_LOWER -> {
                    return ((String) v).toLowerCase();
                }
                case OP_CASE_MODE_UPPER -> {
                    return ((String) v).toUpperCase();
                }
                default -> {
                    return v;
                }
            }
        } else return v;
    }

    /**
     * Applies a regular-expression replacement to string values.
     *
     * @param v       candidate value.
     * @param pattern regular expression to match.
     * @param replace replacement string.
     * @return transformed string or the original value when it is not a string.
     */
    private static Object regex(Object v, String pattern, String replace) {
        if (v instanceof String && pattern != null)
            return ((String) v).replaceAll(pattern, replace != null ? replace : Strings.EMPTY);
        else return v;
    }

    /**
     * Parses a value into a {@link Date} for the {@code parse-date-to} operation.
     *
     * @param v candidate value to parse.
     * @return parsed date for string input, otherwise {@code null}.
     */
    private static Date parseDate(Object v) {
        if (v instanceof String) {
            return parseDateString((String) v);
        } else return null;
    }

    /**
     * Parses supported textual date representations into {@link Date}.
     *
     * @param v date string.
     * @return parsed date or {@code null} if blank/unparseable.
     */
    private static Date parseDateString(String v) {
        if (v == null || v.trim().isEmpty()) {
            return null;
        }
        try {
            return Date.from(Instant.parse(v));
        } catch (DateTimeParseException e) {
            // try with other patterns
            String[] datePatterns = {
                    "yyyy-MM-dd HH:mm:ss.SSS",
                    "yyyy-MM-dd HH:mm:ss.SS",
                    "yyyy-MM-dd HH:mm:ss.S"
            };
            for (val datePattern : datePatterns) {
                try {
                    val formatter = DateTimeFormatter.ofPattern(datePattern);
                    val localDateTime = LocalDateTime.parse(v, formatter);
                    return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
                } catch (Exception ignored) {
                }
            }
            log.warn("Unable to parse date: '{}', will be set to null", v);
            return null;
        }
    }

}
