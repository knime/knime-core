/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 12, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.node.workflow;

import java.util.Arrays;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.knime.core.node.config.Config;
import org.knime.core.node.config.base.ConfigEntries;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.VariableType.BooleanArrayType;
import org.knime.core.node.workflow.VariableType.BooleanType;
import org.knime.core.node.workflow.VariableType.DoubleArrayType;
import org.knime.core.node.workflow.VariableType.DoubleType;
import org.knime.core.node.workflow.VariableType.IntArrayType;
import org.knime.core.node.workflow.VariableType.IntType;
import org.knime.core.node.workflow.VariableType.InvalidConfigEntryException;
import org.knime.core.node.workflow.VariableType.LongArrayType;
import org.knime.core.node.workflow.VariableType.LongType;
import org.knime.core.node.workflow.VariableType.StringArrayType;
import org.knime.core.node.workflow.VariableType.StringType;

/**
 * Contains utility methods used by the various {@link VariableType} implementations.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class VariableTypeUtils {

    private VariableTypeUtils() {
        // static utility class
    }

    static <S, T> BiPredicate<S,T> alwaysFalse() {
        return (s, t) -> false;
    }

    static <S, T> BiPredicate<S, T> wrap(final BiPredicate<S, T> predicate) {
        return predicate;
    }

    static boolean isBoolean(final VariableType<?> type) {
        return BooleanType.INSTANCE.equals(type);
    }

    static boolean isBooleanArray(final VariableType<?> type) {
        return BooleanArrayType.INSTANCE.equals(type);
    }

    static boolean isInt(final VariableType<?> type) {
        return IntType.INSTANCE.equals(type);
    }

    static boolean isIntArray(final VariableType<?> type) {
        return IntArrayType.INSTANCE.equals(type);
    }

    static boolean isLong(final VariableType<?> type) {
        return LongType.INSTANCE.equals(type);
    }

    static boolean isLongArray(final VariableType<?> type) {
        return LongArrayType.INSTANCE.equals(type);
    }

    static boolean isDouble(final VariableType<?> type) {
        return DoubleType.INSTANCE.equals(type);
    }

    static boolean isDoubleArray(final VariableType<?> type) {
        return DoubleArrayType.INSTANCE.equals(type);
    }

    static boolean isString(final VariableType<?> type) {
        return StringType.INSTANCE.equals(type);
    }

    static boolean isStringArray(final VariableType<?> type) {
        return StringArrayType.INSTANCE.equals(type);
    }

    static boolean isDoubleArray(final Config config, final String configKey) {
        return config.getDoubleArray(configKey, null) != null;
    }

    static boolean isFloatArray(final Config config, final String configKey) {
        return config.getFloatArray(configKey, null) != null;
    }

    static boolean isByteArray(final Config config, final String configKey) {
        return config.getByteArray(configKey, null) != null;
    }

    static boolean isShortArray(final Config config, final String configKey) {
        return config.getShortArray(configKey, null) != null;
    }

    static boolean isIntArray(final Config config, final String configKey) {
        return config.getIntArray(configKey, null) != null;
    }

    static boolean isBooleanArray(final Config config, final String configKey) {
        return config.getBooleanArray(configKey, null) != null;
    }

    static boolean isLongArray(final Config config, final String configKey) {
        return config.getLongArray(configKey, null) != null;
    }

    static boolean isStringArray(final Config config, final String configKey) {
        return config.getStringArray(configKey, (String[])null) != null;
    }

    static boolean isCharArray(final Config config, final String configKey) {
        return config.getCharArray(configKey, null) != null;
    }

    static void checkWithinRange(final int value, final String configKey, final ConfigEntries configType,
        final int minValue, final int maxValue) throws InvalidConfigEntryException {
        if (value < minValue || value > maxValue) {
            throw new InvalidConfigEntryException(
                String.format("The value '%s' is out of the range [%s, %s].", value, minValue, maxValue),
                n -> String.format(
                    "Value of variable \"%s\" can't be cast to %s(settings parameter \"%s\"), out of range: %s", n,
                    configType, configKey, value));
        }
    }

    static short toShort(final int value, final String configKey) throws InvalidConfigEntryException {
        checkWithinRange(value, configKey, ConfigEntries.xshort, Short.MIN_VALUE, Short.MAX_VALUE);
        return (short)value;
    }

    static byte toByte(final int value, final String configKey) throws InvalidConfigEntryException {
        checkWithinRange(value, configKey, ConfigEntries.xbyte, Byte.MIN_VALUE, Byte.MAX_VALUE);
        return (byte)value;
    }

    static float[] toFloats(final Number[] numbers) {
        final float[] floats = new float[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            floats[i] = numbers[i].floatValue();
        }
        return floats;
    }

    static char toChar(final Object value, final String configKey) throws InvalidConfigEntryException {
        checkConfigEntry(value != null, varName -> String
            .format("Unable to parse null (variable \"%s\") as char (settings parameter \"%s\")", varName, configKey),
            "Can't convert null value to char");
        @SuppressWarnings("null") // the previous line ensures that value != null
        String string = value.toString();
        checkConfigEntry(string.length() == 1,
            varName -> String.format("Unable to parse \"%s\" (variable \"%s\") as char (settings parameter \"%s\")",
                value, varName, configKey),
            "Only values corresponding to a single character can be converted to char.");
        return string.charAt(0);
    }

    static char[] toChars(final Object[] objectArray, final String configKey) throws InvalidConfigEntryException {
        final char[] chars = new char[objectArray.length];
        for (int i = 0; i < objectArray.length; i++) {
            chars[i] = toChar(objectArray[i], configKey);
        }
        return chars;
    }

    static boolean toBoolean(final String value, final String configKey) throws InvalidConfigEntryException {
        final String lowerCaseBool = value.toLowerCase();
        checkConfigEntry(lowerCaseBool.equals("true") || lowerCaseBool.equals("false"),
            varName -> String.format(
                "Unable to parse \"%s\" (variable \"%s\") as boolean expression (settings parameter \"%s\")",
                lowerCaseBool, varName, configKey),
            "Can't convert '%s' to boolean.", value);
        return Boolean.parseBoolean(lowerCaseBool);
    }

    static boolean toBoolean(final String value) {
        final String lowerCaseBool = value.toLowerCase();
        CheckUtils.checkArgument(lowerCaseBool.equals("true") || lowerCaseBool.equals("false"),
            "Can't convert the value '%s' to boolean.");
        return Boolean.parseBoolean(lowerCaseBool);
    }

    static boolean[] toBooleans(final String[] strings, final String configKey) throws InvalidConfigEntryException {
        final boolean[] booleans = new boolean[strings.length];
        for (int i = 0; i < strings.length; i++) {
            booleans[i] = toBoolean(strings[i], configKey);
        }
        return booleans;
    }

    static Boolean[] toBooleans(final String[] strings) {
        final Boolean[] booleans = new Boolean[strings.length];
        for (int i = 0; i < strings.length; i++) {
            booleans[i] = toBoolean(strings[i]);
        }
        return booleans;
    }

    static String[] toStrings(final Object... objects) {
        return Arrays.stream(objects).map(Object::toString).toArray(String[]::new);
    }

    static Long[] toLongs(final Number... values) {
        return Arrays.stream(values).mapToLong(Number::longValue).boxed().toArray(Long[]::new);
    }

    static Double[] toDoubles(final Number... values) {
        return Arrays.stream(values).mapToDouble(Number::doubleValue).boxed().toArray(Double[]::new);
    }

    static BiPredicate<Config, String> createTypePredicate(final Set<ConfigEntries> types) {
        return (c, k) -> types.contains(c.getEntry(k).getType());
    }

    static <X> X createFormattedException(final Function<String, X> constructor, final String format,
        final Object... args) {
        return constructor.apply(String.format(format, args));
    }

    static InvalidConfigEntryException createFormattedICEE(final UnaryOperator<String> varNameToError,
        final String format, final Object... args) {
        return new InvalidConfigEntryException(String.format(format, args), varNameToError);
    }

    static void checkConfigEntry(final boolean predicate, final UnaryOperator<String> varNameToError,
        final String format, final Object... args) throws InvalidConfigEntryException {
        if (!predicate) {
            throw createFormattedICEE(varNameToError, format, args);
        }
    }

}
