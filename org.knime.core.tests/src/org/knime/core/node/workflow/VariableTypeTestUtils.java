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
 *   Mar 3, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.node.workflow;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.base.ConfigBooleanEntry;
import org.knime.core.node.config.base.ConfigByteEntry;
import org.knime.core.node.config.base.ConfigCharEntry;
import org.knime.core.node.config.base.ConfigDoubleEntry;
import org.knime.core.node.config.base.ConfigEntries;
import org.knime.core.node.config.base.ConfigFloatEntry;
import org.knime.core.node.config.base.ConfigIntEntry;
import org.knime.core.node.config.base.ConfigLongEntry;
import org.knime.core.node.config.base.ConfigShortEntry;
import org.knime.core.node.config.base.ConfigStringEntry;
import org.knime.core.node.config.base.ConfigTransientStringEntry;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.VariableType.BooleanArrayType;
import org.knime.core.node.workflow.VariableType.BooleanType;
import org.knime.core.node.workflow.VariableType.DoubleArrayType;
import org.knime.core.node.workflow.VariableType.DoubleType;
import org.knime.core.node.workflow.VariableType.IntArrayType;
import org.knime.core.node.workflow.VariableType.IntType;
import org.knime.core.node.workflow.VariableType.LongArrayType;
import org.knime.core.node.workflow.VariableType.LongType;
import org.knime.core.node.workflow.VariableType.StringArrayType;
import org.knime.core.node.workflow.VariableType.StringType;

/**
 * Provides utility method for testing implementations of {@link VariableType}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class VariableTypeTestUtils {

    private VariableTypeTestUtils() {
        // static utility class
    }

    static final Set<ConfigEntries> SIMPLE_CONFIGS = EnumSet.of(ConfigEntries.xboolean, ConfigEntries.xbyte,
        ConfigEntries.xshort, ConfigEntries.xint, ConfigEntries.xlong, ConfigEntries.xfloat, ConfigEntries.xdouble,
        ConfigEntries.xchar, ConfigEntries.xtransientstring, ConfigEntries.xstring);

    static final Set<ConfigEntries> ARRAY_CONFIGS = EnumSet.of(ConfigEntries.xboolean, ConfigEntries.xbyte,
        ConfigEntries.xshort, ConfigEntries.xint, ConfigEntries.xlong, ConfigEntries.xfloat, ConfigEntries.xdouble,
        ConfigEntries.xchar, ConfigEntries.xstring);

    static final String KEY = "key";

    static final String CFG_VALUE = "value";

    static boolean isBoolean(final VariableType<?> type) {
        return BooleanType.INSTANCE.equals(type);
    }

    static boolean isInt(final VariableType<?> type) {
        return IntType.INSTANCE.equals(type);
    }

    static boolean isLong(final VariableType<?> type) {
        return LongType.INSTANCE.equals(type);
    }

    static boolean isDouble(final VariableType<?> type) {
        return DoubleType.INSTANCE.equals(type);
    }

    static boolean isString(final VariableType<?> type) {
        return StringType.INSTANCE.equals(type);
    }

    static boolean isBooleanArray(final VariableType<?> type) {
        return BooleanArrayType.INSTANCE.equals(type);
    }

    static boolean isIntArray(final VariableType<?> type) {
        return IntArrayType.INSTANCE.equals(type);
    }

    static boolean isLongArray(final VariableType<?> type) {
        return LongArrayType.INSTANCE.equals(type);
    }

    static boolean isDoubleArray(final VariableType<?> type) {
        return DoubleArrayType.INSTANCE.equals(type);
    }

    static boolean isStringArray(final VariableType<?> type) {
        return StringArrayType.INSTANCE.equals(type);
    }

    static Config mockBooleanArrayConfig(final boolean stubConfigTypeCheck, final boolean mockDefaultGetter,
        final boolean... args) {
        final Config mock = mock(Config.class);
        if (stubConfigTypeCheck) {
            stubConfigTypeCheck(mock);
        }
        if (mockDefaultGetter) {
            when(mock.getBooleanArray(KEY, null)).thenReturn(args);
        }
        return mock;
    }

    static Config mockByteArrayConfig(final boolean stubConfigTypeCheck, final boolean mockDefaultGetter,
        final byte... args) {
        final Config mock = mock(Config.class);
        if (stubConfigTypeCheck) {
            stubConfigTypeCheck(mock);
        }
        if (mockDefaultGetter) {
            when(mock.getByteArray(KEY, null)).thenReturn(args);
        }
        return mock;
    }

    static Config mockShortArrayConfig(final boolean stubConfigTypeCheck, final boolean mockDefaultGetter,
        final short... args) {
        final Config mock = mock(Config.class);
        if (stubConfigTypeCheck) {
            stubConfigTypeCheck(mock);
        }
        if (mockDefaultGetter) {
            when(mock.getShortArray(KEY, null)).thenReturn(args);
        }
        return mock;
    }

    static Config mockIntArrayConfig(final boolean stubConfigTypeCheck, final boolean mockDefaultGetter,
        final int... args) {
        final Config mock = mock(Config.class);
        if (stubConfigTypeCheck) {
            stubConfigTypeCheck(mock);
        }
        if (mockDefaultGetter) {
            when(mock.getIntArray(KEY, null)).thenReturn(args);
        }
        return mock;
    }

    static Config mockLongArrayConfig(final boolean stubConfigTypeCheck, final boolean mockDefaultGetter,
        final long... args) {
        final Config mock = mock(Config.class);
        if (stubConfigTypeCheck) {
            stubConfigTypeCheck(mock);
        }
        if (mockDefaultGetter) {
            when(mock.getLongArray(KEY, null)).thenReturn(args);
        }
        return mock;
    }

    static Config mockFloatArrayConfig(final boolean stubConfigTypeCheck, final boolean mockDefaultGetter,
        final float... args) {
        final Config mock = mock(Config.class);
        if (stubConfigTypeCheck) {
            stubConfigTypeCheck(mock);
        }
        if (mockDefaultGetter) {
            when(mock.getFloatArray(KEY, null)).thenReturn(args);
        }
        return mock;
    }

    static Config mockDoubleArrayConfig(final boolean stubConfigTypeCheck, final boolean mockDefaultGetter,
        final double... args) {
        final Config mock = mock(Config.class);
        if (stubConfigTypeCheck) {
            stubConfigTypeCheck(mock);
        }
        if (mockDefaultGetter) {
            when(mock.getDoubleArray(KEY, null)).thenReturn(args);
        }
        return mock;
    }

    static Config mockStringArrayConfig(final boolean stubConfigTypeCheck, final boolean mockDefaultGetter,
        final String... args) {
        final Config mock = mock(Config.class);
        if (stubConfigTypeCheck) {
            stubConfigTypeCheck(mock);
        }
        if (mockDefaultGetter) {
            when(mock.getStringArray(KEY, (String[])null)).thenReturn(args);
        }
        return mock;
    }

    static void stubConfigTypeCheck(final Config mock) {
        when(mock.getEntry(KEY)).thenReturn(new NodeSettings(KEY));
    }

    static boolean toBoolean(final Object value) {
        if (value instanceof String) {
            String string = ((String)value).toLowerCase();
            CheckUtils.checkArgument(string.equals("true") || string.equals("false"),
                "The provided value '%s' is not a valid boolean.", value);
            return Boolean.parseBoolean(string);
        }
        CheckUtils.checkArgument(value instanceof Boolean, "Can't convert %s to boolean.", value);
        return (Boolean)value;
    }

    static byte toByte(final Object value) {
        if (value instanceof Integer) {
            return ((Integer)value).byteValue();
        } else if (value instanceof Long) {
            return ((Long)value).byteValue();
        } else if (value instanceof Double) {
            return ((Double)value).byteValue();
        } else {
            throw createConvertException(value, "byte");
        }
    }

    static short toShort(final Object value) {
        if (value instanceof Integer) {
            return ((Integer)value).shortValue();
        } else if (value instanceof Long) {
            return ((Long)value).shortValue();
        } else if (value instanceof Double) {
            return ((Double)value).shortValue();
        } else {
            throw createConvertException(value, "short");
        }
    }

    static int toInt(final Object value) {
        if (value instanceof Integer) {
            return ((Integer)value).intValue();
        } else if (value instanceof Long) {
            return ((Long)value).intValue();
        } else if (value instanceof Double) {
            return ((Double)value).intValue();
        } else {
            throw createConvertException(value, "int");
        }
    }

    static long toLong(final Object value) {
        if (value instanceof Integer) {
            return ((Integer)value).longValue();
        } else if (value instanceof Long) {
            return ((Long)value).longValue();
        } else if (value instanceof Double) {
            return ((Double)value).longValue();
        } else {
            throw createConvertException(value, "long");
        }
    }

    static float toFloat(final Object value) {
        if (value instanceof Integer) {
            return ((Integer)value).floatValue();
        } else if (value instanceof Long) {
            return ((Long)value).floatValue();
        } else if (value instanceof Double) {
            return ((Double)value).floatValue();
        } else {
            throw createConvertException(value, "float");
        }
    }

    static double toDouble(final Object value) {
        if (value instanceof Integer) {
            return ((Integer)value).doubleValue();
        } else if (value instanceof Long) {
            return ((Long)value).doubleValue();
        } else if (value instanceof Double) {
            return ((Double)value).doubleValue();
        } else {
            throw createConvertException(value, "double");
        }
    }

    static char toChar(final Object value) {
        if (value instanceof Character) {
            return (Character)value;
        } else {
            String string = value.toString();
            CheckUtils.checkArgument(string.length() == 1,
                "In order to overwrite a char the length of a string must be 1.");
            return string.charAt(0);
        }
    }

    static boolean[] toBooleans(final Object array) {
        if (array instanceof Boolean) {
            return new boolean[]{(Boolean)array};
        } else if (array instanceof String) {
            return new boolean[] {toBoolean(array)};
        } else if (array instanceof String[]) {
            final String[] strings = (String[])array;
            boolean[] booleans = new boolean[strings.length];
            for (int i = 0; i < booleans.length; i++) {
                booleans[i] = toBoolean(strings[i]);
            }
            return booleans;
        }
        CheckUtils.checkArgument(array instanceof Boolean[], "Only Boolean[] can be converted to boolean[].");
        return ArrayUtils.toPrimitive((Boolean[])array);
    }

    static byte[] toBytes(final Object array) {
        CheckUtils.checkArgument(array instanceof Integer[], "Only Integer[] can be converted to byte[].");
        final Integer[] intArray = (Integer[])array;
        final byte[] result = new byte[intArray.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = intArray[i].byteValue();
        }
        return result;
    }

    static short[] toShorts(final Object array) {
        CheckUtils.checkArgument(array instanceof Integer[], "Only Integer[] can be converted to short[].");
        final Integer[] intArray = (Integer[])array;
        final short[] result = new short[intArray.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = intArray[i].shortValue();
        }
        return result;
    }

    static int[] toInts(final Object array) {
        if (array instanceof Number) {
            return new int[]{((Number)array).intValue()};
        }
        CheckUtils.checkArgument(array instanceof Integer[], "Only Integer[] can be converted to short[].");
        return ArrayUtils.toPrimitive((Integer[])array);
    }

    static long[] toLongs(final Object array) {
        if (array instanceof Number) {
            return new long[]{((Number)array).longValue()};
        }
        CheckUtils.checkArgument(array instanceof Number[], "Only instances of Number[] can be converted to long[].");
        return Arrays.stream((Number[])array).mapToLong(Number::longValue).toArray();
    }

    static float[] toFloats(final Object array) {
        CheckUtils.checkArgument(array instanceof Number[], "Only instances of Number[] can be converted to float[].");
        final Number[] numberArray = (Number[])array;
        final float[] result = new float[numberArray.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = numberArray[i].floatValue();
        }
        return result;
    }

    static double[] toDoubles(final Object array) {
        if (array instanceof Number) {
            return new double[]{((Number)array).doubleValue()};
        }
        CheckUtils.checkArgument(array instanceof Number[], "Only instances of Number[] can be converted to double[].");
        return Arrays.stream((Number[])array).mapToDouble(Number::doubleValue).toArray();
    }

    static char[] toChars(final Object array) {
        CheckUtils.checkArgument(array instanceof Object[], "Only arrays can be converted to char[].");
        final Object[] objectArray = (Object[])array;
        final char[] result = new char[objectArray.length];
        for (int i = 0; i < objectArray.length; i++) {
            result[i] = toChar(objectArray[i]);
        }
        return result;
    }

    static String[] toStrings(final Object array) {
        if (array instanceof Object[]) {
            return Arrays.stream((Object[])array).map(Object::toString).toArray(String[]::new);
        } else {
            return new String[]{array.toString()};
        }
    }

    private static IllegalArgumentException createConvertException(final Object value, final String type) {
        return new IllegalArgumentException(String.format("Can't convert %s to %s.", value, type));
    }

    static final class ConfigMocker {

        enum Options {
                ENTRY_GETTER, DEFAULT_GETTER, GETTER;
        }

        private final Set<Options> m_options;

        ConfigMocker(final Set<Options> options) {
            m_options = options;
        }

        ConfigMocker(final Options... options) {
            this(toEnumSet(options));
        }

        private static EnumSet<Options> toEnumSet(final Options[] options) {
            if (options.length == 0) {
                return EnumSet.noneOf(Options.class);
            } else {
                return EnumSet.copyOf(Arrays.asList(options));
            }
        }

        private boolean mockEntryGetter() {
            return m_options.contains(Options.ENTRY_GETTER);
        }

        private boolean mockDefaultGetter() {
            return m_options.contains(Options.DEFAULT_GETTER);
        }

        private boolean mockGetter() {
            return m_options.contains(Options.GETTER);
        }

        Config mockArrayConfig(final ConfigEntries arrayType) throws InvalidSettingsException {
            switch (arrayType) {
                case xboolean:
                    return mockBooleanArrayConfig(true);
                case xbyte:
                    return mockByteArrayConfig((byte)1);
                case xshort:
                    return mockShortArrayConfig((short)1);
                case xint:
                    return mockIntArrayConfig(1);
                case xlong:
                    return mockLongArrayConfig(1L);
                case xfloat:
                    return mockFloatArrayConfig(1.0f);
                case xdouble:
                    return mockDoubleArrayConfig(1.0);
                case xchar:
                    return mockCharArrayConfig('a');
                case xstring:
                    return mockStringArrayConfig("test");
                default:
                    throw new IllegalArgumentException("Can't mock array config of type " + arrayType);

            }
        }

        Config mockBooleanArrayConfig(final boolean... args) throws InvalidSettingsException {
            if (args.length == 0) {
                return mockBooleanArrayConfig(true);
            }
            final Config mock = mock(Config.class);
            if (mockEntryGetter()) {
                stubConfigTypeCheck(mock);
            }
            if (mockDefaultGetter()) {
                when(mock.getBooleanArray(KEY, null)).thenReturn(args);
            }
            if (mockGetter()) {
                when(mock.getBooleanArray(KEY)).thenReturn(args);
            }
            return mock;
        }

        Config mockByteArrayConfig(final byte... args) throws InvalidSettingsException {
            if (args.length == 0) {
                return mockByteArrayConfig((byte)1);
            }
            final Config mock = mock(Config.class);
            if (mockEntryGetter()) {
                stubConfigTypeCheck(mock);
            }
            if (mockDefaultGetter()) {
                when(mock.getByteArray(KEY, null)).thenReturn(args);
            }
            if (mockGetter()) {
                when(mock.getByteArray(KEY)).thenReturn(args);
            }
            return mock;
        }

        Config mockShortArrayConfig(final short... args) throws InvalidSettingsException {
            if (args.length == 0) {
                return mockShortArrayConfig((short)1);
            }
            final Config mock = mock(Config.class);
            if (mockEntryGetter()) {
                stubConfigTypeCheck(mock);
            }
            if (mockDefaultGetter()) {
                when(mock.getShortArray(KEY, null)).thenReturn(args);
            }
            if (mockGetter()) {
                when(mock.getShortArray(KEY)).thenReturn(args);
            }
            return mock;
        }

        Config mockIntArrayConfig(final int... args) throws InvalidSettingsException {
            if (args.length == 0) {
                return mockIntArrayConfig(1);
            }
            final Config mock = mock(Config.class);
            if (mockEntryGetter()) {
                stubConfigTypeCheck(mock);
            }
            if (mockDefaultGetter()) {
                when(mock.getIntArray(KEY, null)).thenReturn(args);
            }
            if (mockGetter()) {
                when(mock.getIntArray(KEY)).thenReturn(args);
            }
            return mock;
        }

        Config mockLongArrayConfig(final long... args) throws InvalidSettingsException {
            if (args.length == 0) {
                return mockLongArrayConfig(1L);
            }
            final Config mock = mock(Config.class);
            if (mockEntryGetter()) {
                stubConfigTypeCheck(mock);
            }
            if (mockDefaultGetter()) {
                when(mock.getLongArray(KEY, null)).thenReturn(args);
            }
            if (mockGetter()) {
                when(mock.getLongArray(KEY)).thenReturn(args);
            }
            return mock;
        }

        Config mockFloatArrayConfig(final float... args) throws InvalidSettingsException {
            if (args.length == 0) {
                return mockFloatArrayConfig(1.0F);
            }
            final Config mock = mock(Config.class);
            if (mockEntryGetter()) {
                stubConfigTypeCheck(mock);
            }
            if (mockDefaultGetter()) {
                when(mock.getFloatArray(KEY, null)).thenReturn(args);
            }
            if (mockGetter()) {
                when(mock.getFloatArray(KEY)).thenReturn(args);
            }
            return mock;
        }

        Config mockDoubleArrayConfig(final double... args) throws InvalidSettingsException {
            if (args.length == 0) {
                return mockDoubleArrayConfig(1.0);
            }
            final Config mock = mock(Config.class);
            if (mockEntryGetter()) {
                stubConfigTypeCheck(mock);
            }
            if (mockDefaultGetter()) {
                when(mock.getDoubleArray(KEY, null)).thenReturn(args);
            }
            if (mockGetter()) {
                when(mock.getDoubleArray(KEY)).thenReturn(args);
            }
            return mock;
        }

        Config mockCharArrayConfig(final char... args) throws InvalidSettingsException {
            if (args.length == 0) {
                return mockCharArrayConfig('t');
            }
            final Config mock = mock(Config.class);
            if (mockEntryGetter()) {
                stubConfigTypeCheck(mock);
            }
            if (mockDefaultGetter()) {
                when(mock.getCharArray(KEY, null)).thenReturn(args);
            }
            if (mockGetter()) {
                when(mock.getCharArray(KEY)).thenReturn(args);
            }
            return mock;
        }

        Config mockStringArrayConfig(final String... args) throws InvalidSettingsException {
            if (args.length == 0) {
                return mockStringArrayConfig("test");
            }
            final Config mock = mock(Config.class);
            if (mockEntryGetter()) {
                stubConfigTypeCheck(mock);
            }
            if (mockDefaultGetter()) {
                when(mock.getStringArray(KEY, (String[])null)).thenReturn(args);
            }
            if (mockGetter()) {
                when(mock.getStringArray(KEY)).thenReturn(args);
            }
            return mock;
        }

        Config mockSimpleConfig(final ConfigEntries configType) throws InvalidSettingsException {
            switch (configType) {
                case xboolean:
                    return mockBooleanConfig(true);
                case xbyte:
                    return mockByteConfig((byte)1);
                case xshort:
                    return mockShortConfig((short)1);
                case xint:
                    return mockIntConfig(1);
                case xlong:
                    return mockLongConfig(1L);
                case xfloat:
                    return mockFloatConfig(1.0f);
                case xdouble:
                    return mockDoubleConfig(1.0);
                case xchar:
                    return mockCharConfig('a');
                case xstring:
                    return mockStringConfig("test");
                case xtransientstring:
                    return mockTransientStringConfig("test");
                default:
                    throw new IllegalArgumentException(String.format("Can't mock %s config.", configType));
            }
        }

        Config mockBooleanConfig(final boolean value) throws InvalidSettingsException {
            final Config mock = mock(Config.class);
            if (mockEntryGetter()) {
                when(mock.getEntry(KEY)).thenReturn(new ConfigBooleanEntry(KEY, value));
            }
            if (mockGetter()) {
                when(mock.getBoolean(KEY)).thenReturn(value);
            }
            return mock;
        }

        Config mockByteConfig(final byte value) throws InvalidSettingsException {
            final Config mock = mock(Config.class);
            if (mockEntryGetter()) {
                when(mock.getEntry(KEY)).thenReturn(new ConfigByteEntry(KEY, value));
            }
            if (mockGetter()) {
                when(mock.getByte(KEY)).thenReturn(value);
            }
            return mock;
        }

        Config mockShortConfig(final short value) throws InvalidSettingsException {
            final Config mock = mock(Config.class);
            if (mockEntryGetter()) {
                when(mock.getEntry(KEY)).thenReturn(new ConfigShortEntry(KEY, value));
            }
            if (mockGetter()) {
                when(mock.getShort(KEY)).thenReturn(value);
            }
            return mock;
        }

        Config mockIntConfig(final int value) throws InvalidSettingsException {
            final Config mock = mock(Config.class);
            if (mockEntryGetter()) {
                when(mock.getEntry(KEY)).thenReturn(new ConfigIntEntry(KEY, value));
            }
            if (mockGetter()) {
                when(mock.getInt(KEY)).thenReturn(value);
            }
            return mock;
        }

        Config mockLongConfig(final long value) throws InvalidSettingsException {
            final Config mock = mock(Config.class);
            if (mockEntryGetter()) {
                when(mock.getEntry(KEY)).thenReturn(new ConfigLongEntry(KEY, value));
            }
            if (mockGetter()) {
                when(mock.getLong(KEY)).thenReturn(value);
            }
            return mock;
        }

        Config mockFloatConfig(final float value) throws InvalidSettingsException {
            final Config mock = mock(Config.class);
            if (mockEntryGetter()) {
                when(mock.getEntry(KEY)).thenReturn(new ConfigFloatEntry(KEY, value));
            }
            if (mockGetter()) {
                when(mock.getFloat(KEY)).thenReturn(value);
            }
            return mock;
        }

        Config mockDoubleConfig(final double value) throws InvalidSettingsException {
            final Config mock = mock(Config.class);
            if (mockEntryGetter()) {
                when(mock.getEntry(KEY)).thenReturn(new ConfigDoubleEntry(KEY, value));
            }
            if (mockGetter()) {
                when(mock.getDouble(KEY)).thenReturn(value);
            }
            return mock;
        }

        Config mockCharConfig(final char value) throws InvalidSettingsException {
            final Config mock = mock(Config.class);
            if (mockEntryGetter()) {
                when(mock.getEntry(KEY)).thenReturn(new ConfigCharEntry(KEY, value));
            }
            if (mockGetter()) {
                when(mock.getChar(KEY)).thenReturn(value);
            }
            return mock;
        }

        Config mockStringConfig(final String value) throws InvalidSettingsException {
            final Config mock = mock(Config.class);
            if (mockEntryGetter()) {
                when(mock.getEntry(KEY)).thenReturn(new ConfigStringEntry(KEY, value));
            }
            if (mockGetter()) {
                when(mock.getString(KEY)).thenReturn(value);
            }
            return mock;
        }

        Config mockTransientStringConfig(final String value) {
            final Config mock = mock(Config.class);
            if (mockEntryGetter()) {
                when(mock.getEntry(KEY)).thenReturn(new ConfigTransientStringEntry(KEY, value));
            }
            if (mockGetter()) {
                when(mock.getTransientString(KEY)).thenReturn(value);
            }
            return mock;
        }
    }

}
