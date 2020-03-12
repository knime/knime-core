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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.knime.core.node.workflow.VariableTypeTestUtils.ARRAY_CONFIGS;
import static org.knime.core.node.workflow.VariableTypeTestUtils.KEY;
import static org.knime.core.node.workflow.VariableTypeTestUtils.mockBooleanArrayConfig;
import static org.knime.core.node.workflow.VariableTypeTestUtils.toBooleans;
import static org.knime.core.node.workflow.VariableTypeTestUtils.toBytes;
import static org.knime.core.node.workflow.VariableTypeTestUtils.toChars;
import static org.knime.core.node.workflow.VariableTypeTestUtils.toDoubles;
import static org.knime.core.node.workflow.VariableTypeTestUtils.toFloats;
import static org.knime.core.node.workflow.VariableTypeTestUtils.toInts;
import static org.knime.core.node.workflow.VariableTypeTestUtils.toLongs;
import static org.knime.core.node.workflow.VariableTypeTestUtils.toShorts;
import static org.knime.core.node.workflow.VariableTypeTestUtils.toStrings;
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
import org.knime.core.node.workflow.VariableType.IntArrayType;
import org.knime.core.node.workflow.VariableType.InvalidConfigEntryException;
import org.knime.core.node.workflow.VariableType.LongArrayType;
import org.knime.core.node.workflow.VariableType.StringArrayType;
import org.knime.core.node.workflow.VariableType.StringType;
import org.knime.core.node.workflow.VariableTypeTestUtils.ConfigMocker;
import org.knime.core.node.workflow.VariableTypeTestUtils.ConfigMocker.Options;

/**
 * Unit tests for array flow variable types such as {@link IntArrayType}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> the simple type represented by the tested variable type
 */
public abstract class AbstractArrayVariableTypeTest<T> extends AbstractVariableTypeTest<T> {

    private final Set<ConfigEntries> m_overwritable;

    private final Set<ConfigEntries> m_creationCompatible;

    /**
     * Constructor.
     *
     * @param expectedIcon the icon of the variable type
     * @param expectedSimpleType the simple type represented by the tested variable type
     * @param testInstance the singleton instance of the tested variable type
     * @param overwritable the types of config entries the tested variable type can overwrite
     * @param creationCompatible the types of config entries the tested variable type can create variables from
     * @param convertible the types the tested variable type can be converted to
     */
    protected AbstractArrayVariableTypeTest(final Icon expectedIcon, final Class<T> expectedSimpleType,
        final VariableType<T> testInstance, final Set<ConfigEntries> overwritable,
        final Set<ConfigEntries> creationCompatible, final Set<VariableType<?>> convertible) {
        super(expectedIcon, expectedSimpleType, testInstance, convertible);
        m_overwritable = overwritable;
        m_creationCompatible = creationCompatible;
    }

    @Override
    @Test
    public final void testDefaultValue() {
        assertArrayEquals((Object[])getExpectedDefaultValue(), (Object[])m_testInstance.defaultValue().get());
    }

    @Override
    public final void testCanOverwrite() throws InvalidSettingsException {
        testCanOverwriteSimpleTypes();
        testCanOverwriteArrays();
    }

    private void testCanOverwriteSimpleTypes() throws InvalidSettingsException {
        // all non-array types should return false
        final ConfigMocker mocker = new ConfigMocker();
        // boolean
        cannotOverwriteSimpleConfig(mocker.mockBooleanConfig(false));
        // byte
        cannotOverwriteSimpleConfig(mocker.mockByteConfig((byte)1));
        // short
        cannotOverwriteSimpleConfig(mocker.mockShortConfig((short)1));
        // int
        cannotOverwriteSimpleConfig(mocker.mockIntConfig(1));
        // long
        cannotOverwriteSimpleConfig(mocker.mockLongConfig(1));
        // float
        cannotOverwriteSimpleConfig(mocker.mockFloatConfig(1));
        // double
        cannotOverwriteSimpleConfig(mocker.mockDoubleConfig(1));
        // char
        cannotOverwriteSimpleConfig(mocker.mockCharConfig('1'));
        // string
        cannotOverwriteSimpleConfig(mocker.mockStringConfig("test"));
        // transient string
        cannotOverwriteSimpleConfig(mocker.mockTransientStringConfig("test"));
    }

    private void cannotOverwriteSimpleConfig(final Config config) {
        assertFalse(m_testInstance.canOverwrite(config, KEY));
    }

    private boolean isOverwritable(final ConfigEntries type) {
        return m_overwritable.contains(type);
    }

    private void testCanOverwriteArrays() throws InvalidSettingsException {
        // arrays
        // boolean array
        boolean overwritesBoolean = isOverwritable(ConfigEntries.xboolean);
        assertEquals(overwritesBoolean,
            m_testInstance.canOverwrite(mockBooleanArrayConfig(false, overwritesBoolean, true), KEY));
        // byte array
        boolean overwritesByte = isOverwritable(ConfigEntries.xbyte);
        assertEquals(overwritesByte, m_testInstance
            .canOverwrite(VariableTypeTestUtils.mockByteArrayConfig(false, overwritesByte, (byte)1), KEY));
        // short array
        boolean overwritesShort = isOverwritable(ConfigEntries.xshort);
        assertEquals(overwritesShort, m_testInstance
            .canOverwrite(VariableTypeTestUtils.mockShortArrayConfig(false, overwritesShort, (short)1), KEY));
        // int array
        boolean overwritesInt = isOverwritable(ConfigEntries.xint);
        assertEquals(overwritesInt,
            m_testInstance.canOverwrite(VariableTypeTestUtils.mockIntArrayConfig(false, overwritesInt, 1), KEY));
        // long array
        boolean overwritesLong = isOverwritable(ConfigEntries.xlong);
        assertEquals(overwritesLong,
            m_testInstance.canOverwrite(VariableTypeTestUtils.mockLongArrayConfig(false, overwritesLong, 1), KEY));
        // float array
        boolean overwritesFloat = isOverwritable(ConfigEntries.xfloat);
        assertEquals(overwritesFloat,
            m_testInstance.canOverwrite(VariableTypeTestUtils.mockFloatArrayConfig(false, overwritesFloat, 1.0f), KEY));
        // double array
        boolean overwritesDouble = isOverwritable(ConfigEntries.xdouble);
        assertEquals(overwritesDouble, m_testInstance
            .canOverwrite(VariableTypeTestUtils.mockDoubleArrayConfig(false, overwritesDouble, 1.0), KEY));
        // string array
        boolean overwritesString = isOverwritable(ConfigEntries.xstring);
        assertEquals(overwritesString, m_testInstance
            .canOverwrite(VariableTypeTestUtils.mockStringArrayConfig(false, overwritesString, "test"), KEY));
    }

    @Override
    public final void testOverwrite() throws InvalidSettingsException, InvalidConfigEntryException {
        final ConfigMocker mocker = new ConfigMocker(EnumSet.of(Options.DEFAULT_GETTER));
        Config mock;
        // boolean array
        if (isOverwritable(ConfigEntries.xboolean)) {
            T testValue = getValueForTesting(BooleanArrayType.INSTANCE);
            mock = mocker.mockBooleanArrayConfig(true);
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addBooleanArray(KEY, toBooleans(testValue));
        }
        // byte array
        if (isOverwritable(ConfigEntries.xbyte)) {
            T testValue = getValueForTesting(IntArrayType.INSTANCE);
            mock = mocker.mockByteArrayConfig((byte)1);
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addByteArray(KEY, toBytes(testValue));
        }
        // short array
        if (isOverwritable(ConfigEntries.xshort)) {
            T testValue = getValueForTesting(IntArrayType.INSTANCE);
            mock = mocker.mockShortArrayConfig((short)1);
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addShortArray(KEY, toShorts(testValue));
        }
        // int array
        if (isOverwritable(ConfigEntries.xint)) {
            T testValue = getValueForTesting(IntArrayType.INSTANCE);
            mock = mocker.mockIntArrayConfig(1);
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addIntArray(KEY, toInts(testValue));
        }
        // long array
        if (isOverwritable(ConfigEntries.xlong)) {
            T testValue = getValueForTesting(LongArrayType.INSTANCE);
            mock = mocker.mockLongArrayConfig(1);
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addLongArray(KEY, toLongs(testValue));
        }
        // float array
        if (isOverwritable(ConfigEntries.xfloat)) {
            T testValue = getValueForTesting(DoubleArrayType.INSTANCE);
            mock = mocker.mockFloatArrayConfig(1);
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addFloatArray(KEY, toFloats(testValue));
        }
        // double array
        if (isOverwritable(ConfigEntries.xdouble)) {
            T testValue = getValueForTesting(DoubleArrayType.INSTANCE);
            mock = mocker.mockDoubleArrayConfig(1);
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addDoubleArray(KEY, toDoubles(testValue));
        }
        // char array
        if (isOverwritable(ConfigEntries.xchar)) {
            T testValue = getValueForTesting(StringArrayType.INSTANCE);
            mock = mocker.mockCharArrayConfig((char)1);
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addCharArray(KEY, toChars(testValue));
        }
        // string array
        if (isOverwritable(ConfigEntries.xstring)) {
            T testValue = getValueForTesting(StringArrayType.INSTANCE);
            mock = mocker.mockStringArrayConfig("test");
            m_testInstance.overwrite(testValue, mock, KEY);
            verify(mock).addStringArray(KEY, toStrings(testValue));
        }
    }

    /**
     * Tests if the array type fails when asked to overwrite a simple config.
     *
     * @throws InvalidSettingsException never actually thrown
     * @throws InvalidConfigEntryException because no array type can create a simple type
     */
    @Test(expected = InvalidConfigEntryException.class)
    public void testOverwriteSimpleConfig() throws InvalidSettingsException, InvalidConfigEntryException {
        T testValue = getValueForTesting(BooleanType.INSTANCE);
        // no array type can be created from a simple type such as boolean
        m_testInstance.overwrite(testValue, new ConfigMocker(Options.ENTRY_GETTER).mockBooleanConfig(true), KEY);
    }

    /**
     * Tests if the array type fails when asked to overwrite an incompatible array config.
     *
     * @throws InvalidSettingsException never actually thrown
     * @throws InvalidConfigEntryException because we explicitly test an incompatible array type
     */
    @Test(expected = InvalidConfigEntryException.class)
    public void testOverwriteIncompatibleArrayType() throws InvalidSettingsException, InvalidConfigEntryException {
        final Optional<ConfigEntries> incompatible = ARRAY_CONFIGS.stream().filter(c -> !isOverwritable(c)).findAny();
        if (incompatible.isPresent()) {
            T testValue = getValueForTesting(StringType.INSTANCE);
            m_testInstance.overwrite(testValue,
                new ConfigMocker(Options.ENTRY_GETTER).mockArrayConfig(incompatible.get()), KEY);
        }
    }

    @Override
    public final void testCanCreateFrom() throws InvalidSettingsException {
        testCanCreateFromSimpleConfigs();
        testCanCreateFromArrays();
    }

    private void testCanCreateFromSimpleConfigs() throws InvalidSettingsException {
        // all non-array types should return false
        final ConfigMocker mocker = new ConfigMocker();
        // boolean
        cannotCreateFromSimpleConfig(mocker.mockBooleanConfig(false));
        // byte
        cannotCreateFromSimpleConfig(mocker.mockByteConfig((byte)1));
        // short
        cannotCreateFromSimpleConfig(mocker.mockShortConfig((short)1));
        // int
        cannotCreateFromSimpleConfig(mocker.mockIntConfig(1));
        // long
        cannotCreateFromSimpleConfig(mocker.mockLongConfig(1));
        // float
        cannotCreateFromSimpleConfig(mocker.mockFloatConfig(1));
        // double
        cannotCreateFromSimpleConfig(mocker.mockDoubleConfig(1));
        // char
        cannotCreateFromSimpleConfig(mocker.mockCharConfig('1'));
        // string
        cannotCreateFromSimpleConfig(mocker.mockStringConfig("test"));
        // transient string
        cannotCreateFromSimpleConfig(mocker.mockTransientStringConfig("test"));
    }

    private boolean isCreationCompatible(final ConfigEntries type) {
        return m_creationCompatible.contains(type);
    }

    private void testCanCreateFromArrays() throws InvalidSettingsException {
        // boolean array
        boolean canCreateFromBoolean = isCreationCompatible(ConfigEntries.xboolean);
        assertEquals(canCreateFromBoolean,
            m_testInstance.canOverwrite(mockBooleanArrayConfig(false, canCreateFromBoolean, true), KEY));
        // byte array
        boolean canCreateFromByte = isCreationCompatible(ConfigEntries.xbyte);
        assertEquals(canCreateFromByte, m_testInstance
            .canOverwrite(VariableTypeTestUtils.mockByteArrayConfig(false, canCreateFromByte, (byte)1), KEY));
        // short array
        boolean canCreateFromShort = isCreationCompatible(ConfigEntries.xshort);
        assertEquals(canCreateFromShort, m_testInstance
            .canOverwrite(VariableTypeTestUtils.mockShortArrayConfig(false, canCreateFromShort, (short)1), KEY));
        // int array
        boolean canCreateFromInt = isCreationCompatible(ConfigEntries.xint);
        assertEquals(canCreateFromInt,
            m_testInstance.canOverwrite(VariableTypeTestUtils.mockIntArrayConfig(false, canCreateFromInt, 1), KEY));
        // long array
        boolean canCreateFromLong = isCreationCompatible(ConfigEntries.xlong);
        assertEquals(canCreateFromLong,
            m_testInstance.canOverwrite(VariableTypeTestUtils.mockLongArrayConfig(false, canCreateFromLong, 1), KEY));
        // float array
        boolean canCreateFromFloat = isCreationCompatible(ConfigEntries.xfloat);
        assertEquals(canCreateFromFloat, m_testInstance
            .canOverwrite(VariableTypeTestUtils.mockFloatArrayConfig(false, canCreateFromFloat, 1.0f), KEY));
        // double array
        boolean canCreateFromDouble = isCreationCompatible(ConfigEntries.xdouble);
        assertEquals(canCreateFromDouble, m_testInstance
            .canOverwrite(VariableTypeTestUtils.mockDoubleArrayConfig(false, canCreateFromDouble, 1.0), KEY));
        // string array
        boolean canCreateFromString = isCreationCompatible(ConfigEntries.xstring);
        assertEquals(canCreateFromString, m_testInstance
            .canOverwrite(VariableTypeTestUtils.mockStringArrayConfig(false, canCreateFromString, "test"), KEY));

    }

    private void cannotCreateFromSimpleConfig(final Config config) {
        assertFalse(m_testInstance.canCreateFrom(config, KEY));
    }

    @Override
    public final void testCreateFrom() throws InvalidSettingsException, InvalidConfigEntryException {
        final ConfigMocker mocker = new ConfigMocker(EnumSet.of(Options.DEFAULT_GETTER, Options.GETTER));
        // boolean array
        if (isCreationCompatible(ConfigEntries.xboolean)) {
            T testValue = getValueForTesting(BooleanArrayType.INSTANCE);
            T created = m_testInstance.createFrom(mocker.mockBooleanArrayConfig(toBooleans(testValue)), KEY);
            assertArrayEquals(toBooleans(testValue), toBooleans(created));
        }
        // byte array
        if (isCreationCompatible(ConfigEntries.xbyte)) {
            T testValue = getValueForTesting(IntArrayType.INSTANCE);
            assertArrayEquals(toBytes(testValue),
                toBytes(m_testInstance.createFrom(mocker.mockByteArrayConfig(toBytes(testValue)), KEY)));
        }
        // short array
        if (isCreationCompatible(ConfigEntries.xshort)) {
            T testValue = getValueForTesting(IntArrayType.INSTANCE);
            assertArrayEquals(toShorts(testValue),
                toShorts(m_testInstance.createFrom(mocker.mockShortArrayConfig(toShorts(testValue)), KEY)));
        }
        // int array
        if (isCreationCompatible(ConfigEntries.xint)) {
            T testValue = getValueForTesting(IntArrayType.INSTANCE);
            assertArrayEquals(toInts(testValue),
                toInts(m_testInstance.createFrom(mocker.mockIntArrayConfig(toInts(testValue)), KEY)));
        }
        // long array
        if (isCreationCompatible(ConfigEntries.xlong)) {
            T testValue = getValueForTesting(LongArrayType.INSTANCE);
            assertArrayEquals(toLongs(testValue),
                toLongs(m_testInstance.createFrom(mocker.mockLongArrayConfig(toLongs(testValue)), KEY)));
        }
        // float array
        if (isCreationCompatible(ConfigEntries.xfloat)) {
            T testValue = getValueForTesting(DoubleArrayType.INSTANCE);
            assertArrayEquals(toFloats(testValue),
                toFloats(m_testInstance.createFrom(mocker.mockFloatArrayConfig(toFloats(testValue)), KEY)),
                1e-5F);
        }
        // double array
        if (isCreationCompatible(ConfigEntries.xdouble)) {
            T testValue = getValueForTesting(DoubleArrayType.INSTANCE);
            assertArrayEquals(toDoubles(testValue),
                toDoubles(m_testInstance.createFrom(mocker.mockDoubleArrayConfig(toDoubles(testValue)), KEY)),
                1e-5);
        }
        // char array
        if (isCreationCompatible(ConfigEntries.xchar)) {
            T testValue = getValueForTesting(StringArrayType.INSTANCE);
            assertArrayEquals(toChars(testValue),
                toChars(m_testInstance.createFrom(mocker.mockCharArrayConfig(toChars(testValue)), KEY)));
        }
        // string array
        if (isCreationCompatible(ConfigEntries.xstring)) {
            T testValue = getValueForTesting(StringArrayType.INSTANCE);
            assertArrayEquals(toStrings(testValue),
                toStrings(m_testInstance.createFrom(mocker.mockStringArrayConfig(toStrings(testValue)), KEY)));
        }
    }

    /**
     * Checks if attempting to create a variable of the tested array variable type from a simple config results in the
     * expected exception.
     *
     * @throws InvalidConfigEntryException because we attempt to create an array variable from a simple config
     * @throws InvalidSettingsException never thrown
     */
    @Test(expected = InvalidConfigEntryException.class)
    public void testCreateFromSimpleConfig() throws InvalidSettingsException, InvalidConfigEntryException {
        // Currently no array type can be created from a simple config
        m_testInstance.createFrom(new ConfigMocker(Options.ENTRY_GETTER).mockBooleanConfig(true), KEY);
    }

    /**
     * Checks if creating a variable from an incompatible array config results in the expected exception.
     *
     * @throws InvalidConfigEntryException because we attempt to create a variable from an incompatible config
     * @throws InvalidSettingsException never thrown
     */
    @Test(expected = InvalidConfigEntryException.class)
    public void testCreateFromIncompatibleArrayConfig() throws InvalidSettingsException, InvalidConfigEntryException {
        final Optional<ConfigEntries> incompatible =
            ARRAY_CONFIGS.stream().filter(c -> !isCreationCompatible(c)).findAny();
        if (incompatible.isPresent()) {
            m_testInstance.createFrom(new ConfigMocker(Options.ENTRY_GETTER).mockArrayConfig(incompatible.get()), KEY);
        }
    }

}
