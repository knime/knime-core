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
 *   Mar 4, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.node.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.knime.core.node.workflow.VariableTypeTestUtils.ARRAY_CONFIGS;
import static org.knime.core.node.workflow.VariableTypeTestUtils.KEY;
import static org.knime.core.node.workflow.VariableTypeTestUtils.SIMPLE_CONFIGS;
import static org.knime.core.node.workflow.VariableTypeTestUtils.mockBooleanArrayConfig;
import static org.knime.core.node.workflow.VariableTypeTestUtils.toBoolean;
import static org.knime.core.node.workflow.VariableTypeTestUtils.toByte;
import static org.knime.core.node.workflow.VariableTypeTestUtils.toChar;
import static org.knime.core.node.workflow.VariableTypeTestUtils.toDouble;
import static org.knime.core.node.workflow.VariableTypeTestUtils.toFloat;
import static org.knime.core.node.workflow.VariableTypeTestUtils.toInt;
import static org.knime.core.node.workflow.VariableTypeTestUtils.toLong;
import static org.knime.core.node.workflow.VariableTypeTestUtils.toShort;
import static org.mockito.Mockito.verify;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import javax.swing.Icon;

import org.junit.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.base.ConfigEntries;
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
import org.knime.core.node.workflow.VariableTypeTestUtils.ConfigMocker;
import org.knime.core.node.workflow.VariableTypeTestUtils.ConfigMocker.Options;

/**
 * Unit tests for {@link VariableType} implementations that deal with primitive types such as double, int, long (and
 * string) i.e. non-array types that correspond to atomic {@link ConfigEntries}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> The simple value type that is used by clients (e.g. String, Double, Integer)
 */
public abstract class AbstractSimpleVariableTypeTest<T> extends AbstractVariableTypeTest<T> {

    private final Set<ConfigEntries> m_overwritable;

    private final Set<ConfigEntries> m_creationCompatible;

    /**
     * Constructor.
     *
     * @param expectedIcon the flow variable icon
     * @param expectedType the simple type this {@link VariableType} deals with
     * @param testInstance the singleton instance of the {@link VariableType}
     * @param overwritable the {@link ConfigEntries} that can be overwritten with this {@link VariableType}
     * @param creationCompatible the {@link ConfigEntries} this {@link VariableType} can use to create values
     * @param convertible the types the tested type can be converted to
     */
    protected AbstractSimpleVariableTypeTest(final Icon expectedIcon, final Class<T> expectedType,
        final VariableType<T> testInstance, final Set<ConfigEntries> overwritable,
        final Set<ConfigEntries> creationCompatible, final Set<VariableType<?>> convertible) {
        super(expectedIcon, expectedType, testInstance, convertible);
        m_overwritable = overwritable;
        m_creationCompatible = creationCompatible;
    }

    @Override
    @Test
    public final void testDefaultValue() {
        assertEquals(getExpectedDefaultValue(), m_testInstance.defaultValue().get());
    }


    @Override
    @Test
    public final void testCanOverwrite() throws InvalidSettingsException {
        testCanOverwriteSimpleTypes();
        testCanOverwriteArrays();
    }

    private void testCanOverwriteArrays() {
        // arrays
        // boolean array
        boolean overwritesBoolean = isOverwritable(ConfigEntries.xboolean);
        assertEquals(overwritesBoolean,
            m_testInstance.canOverwrite(mockBooleanArrayConfig(true, overwritesBoolean, true), KEY));
        // byte array
        boolean overwritesByte = isOverwritable(ConfigEntries.xbyte);
        assertEquals(overwritesByte,
            m_testInstance.canOverwrite(VariableTypeTestUtils.mockByteArrayConfig(true, overwritesByte, (byte)1), KEY));
        // short array
        boolean overwritesShort = isOverwritable(ConfigEntries.xshort);
        assertEquals(overwritesShort, m_testInstance
            .canOverwrite(VariableTypeTestUtils.mockShortArrayConfig(true, overwritesShort, (short)1), KEY));
        // int array
        boolean overwritesInt = isOverwritable(ConfigEntries.xint);
        assertEquals(overwritesInt,
            m_testInstance.canOverwrite(VariableTypeTestUtils.mockIntArrayConfig(true, overwritesInt, 1), KEY));
        // long array
        boolean overwritesLong = isOverwritable(ConfigEntries.xlong);
        assertEquals(overwritesLong,
            m_testInstance.canOverwrite(VariableTypeTestUtils.mockLongArrayConfig(true, overwritesLong, 1), KEY));
        // float array
        boolean overwritesFloat = isOverwritable(ConfigEntries.xfloat);
        assertEquals(overwritesFloat,
            m_testInstance.canOverwrite(VariableTypeTestUtils.mockFloatArrayConfig(true, overwritesFloat, 1.0f), KEY));
        // double array
        boolean overwritesDouble = isOverwritable(ConfigEntries.xdouble);
        assertEquals(overwritesDouble,
            m_testInstance.canOverwrite(VariableTypeTestUtils.mockDoubleArrayConfig(true, overwritesDouble, 1.0), KEY));
        // string array
        boolean overwritesString = isOverwritable(ConfigEntries.xstring);
        assertEquals(overwritesString, m_testInstance
            .canOverwrite(VariableTypeTestUtils.mockStringArrayConfig(true, overwritesString, "test"), KEY));
    }

    /**
     * @throws InvalidSettingsException
     */
    private void testCanOverwriteSimpleTypes() throws InvalidSettingsException {
        final ConfigMocker mocker = new ConfigMocker(Options.ENTRY_GETTER);
        // boolean
        assertEquals(isOverwritable(ConfigEntries.xboolean),
            m_testInstance.canOverwrite(mocker.mockBooleanConfig(true), KEY));
        // byte
        assertEquals(isOverwritable(ConfigEntries.xbyte),
            m_testInstance.canOverwrite(mocker.mockByteConfig((byte)1), KEY));
        // short
        assertEquals(isOverwritable(ConfigEntries.xshort),
            m_testInstance.canOverwrite(mocker.mockShortConfig((short)1), KEY));
        // int
        assertEquals(isOverwritable(ConfigEntries.xint), m_testInstance.canOverwrite(mocker.mockIntConfig(1), KEY));
        // long
        assertEquals(isOverwritable(ConfigEntries.xlong), m_testInstance.canOverwrite(mocker.mockLongConfig(1), KEY));
        // float
        assertEquals(isOverwritable(ConfigEntries.xfloat),
            m_testInstance.canOverwrite(mocker.mockFloatConfig(1.0f), KEY));
        // double
        assertEquals(isOverwritable(ConfigEntries.xdouble),
            m_testInstance.canOverwrite(mocker.mockDoubleConfig(1.0), KEY));
        // char
        assertEquals(isOverwritable(ConfigEntries.xchar),
            m_testInstance.canOverwrite(mocker.mockCharConfig((char)1), KEY));
        // string
        assertEquals(isOverwritable(ConfigEntries.xstring),
            m_testInstance.canOverwrite(mocker.mockStringConfig("test"), KEY));
        // transient string
        assertEquals(isOverwritable(ConfigEntries.xtransientstring),
            m_testInstance.canOverwrite(mocker.mockTransientStringConfig("test"), KEY));
    }

    private final boolean isOverwritable(final ConfigEntries type) {
        return m_overwritable.contains(type);
    }

    @Override
    @Test
    public final void testOverwrite() throws InvalidSettingsException, InvalidConfigEntryException {
        testOverwritePrimitives();
        testOverwriteArrays();
    }

    /**
     * Tests if trying to overwrite an incompatible config results in the expected exception.
     *
     * @throws InvalidSettingsException never thrown
     * @throws InvalidConfigEntryException because we try to overwrite a config that is not compatible
     */
    @Test(expected = InvalidConfigEntryException.class)
    public void testOverwriteIncompatibleSimpleConfig() throws InvalidConfigEntryException, InvalidSettingsException {
        final Optional<ConfigEntries> incompatibleType =
            SIMPLE_CONFIGS.stream().filter(c -> !isOverwritable(c)).findAny();
        if (incompatibleType.isPresent()) {
            T testValue = getValueForTesting(StringType.INSTANCE);
            m_testInstance.overwrite(testValue,
                new ConfigMocker(Options.ENTRY_GETTER).mockSimpleConfig(incompatibleType.get()), KEY);
        }
    }

    /**
     * Tests that an incompatible array type results in an {@link InvalidConfigEntryException}.
     *
     * @throws InvalidSettingsException never thrown
     * @throws InvalidConfigEntryException because we try to overwrite an incompatible array config
     */
    @Test(expected = InvalidConfigEntryException.class)
    public void testOverwriteIncompatibleArrayConfig() throws InvalidConfigEntryException, InvalidSettingsException {
        final Optional<ConfigEntries> incompatibleType =
            ARRAY_CONFIGS.stream().filter(c -> !isOverwritable(c)).findAny();
        if (incompatibleType.isPresent()) {
            T testValue = getValueForTesting(StringType.INSTANCE);
            m_testInstance.overwrite(testValue,
                new ConfigMocker(EnumSet.of(Options.ENTRY_GETTER)).mockArrayConfig(incompatibleType.get()), KEY);
        }
    }

    private void testOverwriteArrays() throws InvalidSettingsException, InvalidConfigEntryException {
        ConfigMocker mocker = new ConfigMocker(EnumSet.of(Options.ENTRY_GETTER, Options.DEFAULT_GETTER));
        Config mock;
        if (isOverwritable(ConfigEntries.xboolean)) {
            mock = mocker.mockBooleanArrayConfig(true);
            T testValue = getValueForTesting(BooleanArrayType.INSTANCE);
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addBooleanArray(KEY, toBoolean(testValue));
        }
        // byte array
        if (isOverwritable(ConfigEntries.xbyte)) {
            T testValue = getValueForTesting(IntArrayType.INSTANCE);
            mock = mocker.mockByteArrayConfig((byte)1);
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addByteArray(KEY, toByte(testValue));
        }
        // short array
        if (isOverwritable(ConfigEntries.xshort)) {
            T testValue = getValueForTesting(IntArrayType.INSTANCE);
            mock = mocker.mockShortArrayConfig((short)1);
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addShortArray(KEY, toShort(testValue));
        }
        // int array
        if (isOverwritable(ConfigEntries.xint)) {
            T testValue = getValueForTesting(IntArrayType.INSTANCE);
            mock = mocker.mockIntArrayConfig(1);
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addIntArray(KEY, toInt(testValue));
        }
        // long array
        if (isOverwritable(ConfigEntries.xlong)) {
            T testValue = getValueForTesting(LongArrayType.INSTANCE);
            mock = mocker.mockLongArrayConfig(1);
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addLongArray(KEY, toLong(testValue));
        }
        // float array
        if (isOverwritable(ConfigEntries.xfloat)) {
            T testValue = getValueForTesting(DoubleArrayType.INSTANCE);
            mock = mocker.mockFloatArrayConfig(1);
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addFloatArray(KEY, toFloat(testValue));
        }
        // double array
        if (isOverwritable(ConfigEntries.xdouble)) {
            T testValue = getValueForTesting(DoubleArrayType.INSTANCE);
            mock = mocker.mockDoubleArrayConfig(1);
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addDoubleArray(KEY, toDouble(testValue));
        }
        // char array
        if (isOverwritable(ConfigEntries.xchar)) {
            T testValue = getValueForTesting(StringArrayType.INSTANCE);
            mock = mocker.mockCharArrayConfig((char)1);
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addCharArray(KEY, toChar(testValue));
        }
        // string array
        if (isOverwritable(ConfigEntries.xstring)) {
            T testValue = getValueForTesting(StringArrayType.INSTANCE);
            mock = mocker.mockStringArrayConfig("test");
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addStringArray(KEY, testValue.toString());
        }
    }

    private void testOverwritePrimitives() throws InvalidSettingsException, InvalidConfigEntryException {
        final ConfigMocker mocker = new ConfigMocker(Options.ENTRY_GETTER);
        Config mock;
        // boolean
        if (isOverwritable(ConfigEntries.xboolean)) {
            T testValue = getValueForTesting(BooleanType.INSTANCE);
            mock = mocker.mockBooleanConfig(true);
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addBoolean(KEY, toBoolean(testValue));
        }
        // byte
        if (isOverwritable(ConfigEntries.xbyte)) {
            T testValue = getValueForTesting(IntType.INSTANCE);
            mock = mocker.mockByteConfig((byte)1);
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addByte(KEY, toByte(testValue));
        }
        // short
        if (isOverwritable(ConfigEntries.xshort)) {
            T testValue = getValueForTesting(IntType.INSTANCE);
            mock = mocker.mockShortConfig((short)1);
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addShort(KEY, toShort(testValue));
        }
        // int
        if (isOverwritable(ConfigEntries.xint)) {
            T testValue = getValueForTesting(IntType.INSTANCE);
            mock = mocker.mockIntConfig(1);
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addInt(KEY, toInt(testValue));
        }
        // long
        if (isOverwritable(ConfigEntries.xlong)) {
            T testValue = getValueForTesting(LongType.INSTANCE);
            mock = mocker.mockLongConfig(1);
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addLong(KEY, toLong(testValue));
        }
        // float
        if (isOverwritable(ConfigEntries.xfloat)) {
            T testValue = getValueForTesting(DoubleType.INSTANCE);
            mock = mocker.mockFloatConfig(1);
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addFloat(KEY, toFloat(testValue));
        }
        // double
        if (isOverwritable(ConfigEntries.xdouble)) {
            T testValue = getValueForTesting(DoubleType.INSTANCE);
            mock = mocker.mockDoubleConfig(1);
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addDouble(KEY, toDouble(testValue));
        }
        // char
        if (isOverwritable(ConfigEntries.xchar)) {
            T testValue = getValueForTesting(StringType.INSTANCE);
            mock = mocker.mockCharConfig((char)1);
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addChar(KEY, toChar(testValue));
        }
        // string
        if (isOverwritable(ConfigEntries.xstring)) {
            T testValue = getValueForTesting(StringType.INSTANCE);
            mock = mocker.mockStringConfig("test");
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addString(KEY, testValue.toString());
        }
        // transient string
        if (isOverwritable(ConfigEntries.xtransientstring)) {
            T testValue = getValueForTesting(StringType.INSTANCE);
            mock = mocker.mockTransientStringConfig("test");
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addTransientString(KEY, testValue.toString());
        }
    }

    private boolean isCreationCompatible(final ConfigEntries type) {
        return m_creationCompatible.contains(type);
    }

    @Override
    @Test
    public final void testCanCreateFrom() throws InvalidSettingsException {
        testCanCreateFromSimpleType();
        testCanCreateFromArray();
    }

    private void testCanCreateFromSimpleType() throws InvalidSettingsException {
        final ConfigMocker mocker = new ConfigMocker(Options.ENTRY_GETTER);
        // boolean
        assertEquals(isCreationCompatible(ConfigEntries.xboolean),
            m_testInstance.canCreateFrom(mocker.mockBooleanConfig(true), KEY));
        // byte
        assertEquals(isCreationCompatible(ConfigEntries.xbyte),
            m_testInstance.canCreateFrom(mocker.mockByteConfig((byte)1), KEY));
        // short
        assertEquals(isCreationCompatible(ConfigEntries.xshort),
            m_testInstance.canCreateFrom(mocker.mockShortConfig((short)1), KEY));
        // int
        assertEquals(isCreationCompatible(ConfigEntries.xint),
            m_testInstance.canCreateFrom(mocker.mockIntConfig(1), KEY));
        // long
        assertEquals(isCreationCompatible(ConfigEntries.xlong),
            m_testInstance.canCreateFrom(mocker.mockLongConfig(1), KEY));
        // float
        assertEquals(isCreationCompatible(ConfigEntries.xfloat),
            m_testInstance.canCreateFrom(mocker.mockFloatConfig(1.0f), KEY));
        // double
        assertEquals(isCreationCompatible(ConfigEntries.xdouble),
            m_testInstance.canCreateFrom(mocker.mockDoubleConfig(1.0), KEY));
        // char
        assertEquals(isCreationCompatible(ConfigEntries.xchar),
            m_testInstance.canCreateFrom(mocker.mockCharConfig((char)1), KEY));
        // string
        assertEquals(isCreationCompatible(ConfigEntries.xstring),
            m_testInstance.canCreateFrom(mocker.mockStringConfig("test"), KEY));
        // transient string
        assertEquals(isCreationCompatible(ConfigEntries.xtransientstring),
            m_testInstance.canCreateFrom(mocker.mockTransientStringConfig("test"), KEY));
    }

    private void testCanCreateFromArray() throws InvalidSettingsException {
        ConfigMocker mocker = new ConfigMocker(EnumSet.of(Options.ENTRY_GETTER));
        // boolean
        cannotCreateFrom(mocker.mockBooleanArrayConfig());
        // byte
        cannotCreateFrom(mocker.mockByteArrayConfig());
        // short
        cannotCreateFrom(mocker.mockShortArrayConfig());
        // int
        cannotCreateFrom(mocker.mockIntArrayConfig());
        // long
        cannotCreateFrom(mocker.mockLongArrayConfig());
        // float
        cannotCreateFrom(mocker.mockFloatArrayConfig());
        // double
        cannotCreateFrom(mocker.mockDoubleArrayConfig());
        // char
        cannotCreateFrom(mocker.mockCharArrayConfig());
        // string
        cannotCreateFrom(mocker.mockStringArrayConfig());
    }

    private void cannotCreateFrom(final Config mock) {
        assertFalse(m_testInstance.canCreateFrom(mock, KEY));
    }

    @Override
    @Test
    public final void testCreateFrom() throws InvalidSettingsException, InvalidConfigEntryException {
        final ConfigMocker mocker = new ConfigMocker(Options.ENTRY_GETTER, Options.GETTER);
        // boolean
        if (isCreationCompatible(ConfigEntries.xboolean)) {
            T testValue = getValueForTesting(BooleanType.INSTANCE);
            assertEquals(testValue,
                m_testInstance.createFrom(mocker.mockBooleanConfig(toBoolean(testValue)), KEY));
        }
        // byte
        if (isCreationCompatible(ConfigEntries.xbyte)) {
            T testValue = getValueForTesting(IntType.INSTANCE);
            assertEquals(testValue, m_testInstance.createFrom(mocker.mockByteConfig(toByte(testValue)), KEY));
        }
        // short
        if (isCreationCompatible(ConfigEntries.xshort)) {
            T testValue = getValueForTesting(IntType.INSTANCE);
            assertEquals(testValue,
                m_testInstance.createFrom(mocker.mockShortConfig(toShort(testValue)), KEY));
        }
        // int
        if (isCreationCompatible(ConfigEntries.xint)) {
            T testValue = getValueForTesting(IntType.INSTANCE);
            assertEquals(testValue, m_testInstance.createFrom(mocker.mockIntConfig(toInt(testValue)), KEY));
        }
        // long
        if (isCreationCompatible(ConfigEntries.xlong)) {
            T testValue = getValueForTesting(LongType.INSTANCE);
            assertEquals(testValue, m_testInstance.createFrom(mocker.mockLongConfig(toLong(testValue)), KEY));
        }
        // float
        if (isCreationCompatible(ConfigEntries.xfloat)) {
            T testValue = getValueForTesting(DoubleType.INSTANCE);
            assertEquals(testValue,
                m_testInstance.createFrom(mocker.mockFloatConfig(toFloat(testValue)), KEY));
        }
        // double
        if (isCreationCompatible(ConfigEntries.xdouble)) {
            T testValue = getValueForTesting(DoubleType.INSTANCE);
            assertEquals(testValue,
                m_testInstance.createFrom(mocker.mockDoubleConfig(toDouble(testValue)), KEY));
        }
        // char
        if (isCreationCompatible(ConfigEntries.xchar)) {
            T testValue = getValueForTesting(StringType.INSTANCE);
            assertEquals(testValue, m_testInstance.createFrom(mocker.mockCharConfig(toChar(testValue)), KEY));
        }
        // string
        if (isCreationCompatible(ConfigEntries.xstring)) {
            T testValue = getValueForTesting(StringType.INSTANCE);
            assertEquals(testValue,
                m_testInstance.createFrom(mocker.mockStringConfig(testValue.toString()), KEY));
        }
        // transient string
        if (isCreationCompatible(ConfigEntries.xtransientstring)) {
            T testValue = getValueForTesting(StringType.INSTANCE);
            assertEquals(testValue,
                m_testInstance.createFrom(mocker.mockTransientStringConfig(testValue.toString()), KEY));
        }
    }

    /**
     * Tests if trying to create a variable from an incompatible config results in the expected exception.
     *
     * @throws InvalidConfigEntryException because we try to create a variable from an incompatible config
     * @throws InvalidSettingsException never thrown
     */
    @Test(expected = InvalidConfigEntryException.class)
    public void testCreateFromIncompatibleSimpleConfig() throws InvalidSettingsException, InvalidConfigEntryException {
        final Optional<ConfigEntries> incompatible =
            SIMPLE_CONFIGS.stream().filter(c -> !m_creationCompatible.contains(c)).findAny();
        if (incompatible.isPresent()) {
            m_testInstance.createFrom(new ConfigMocker(Options.ENTRY_GETTER).mockSimpleConfig(incompatible.get()), KEY);
        }
    }

    /**
     * Tests if creating a variable of this type from an array variable fails.
     *
     * @throws InvalidConfigEntryException because it is (currently) not possible to create a simple variable from an
     *             array variable
     * @throws InvalidSettingsException never thrown
     */
    @Test(expected = InvalidConfigEntryException.class)
    public void testCreateFromArray() throws InvalidSettingsException, InvalidConfigEntryException {
        // it's currently not possible to create a simple flow variable from an array config
        // therefore any such attempt must fail
        m_testInstance.createFrom(new ConfigMocker(Options.ENTRY_GETTER).mockBooleanArrayConfig(), KEY);
    }

}
