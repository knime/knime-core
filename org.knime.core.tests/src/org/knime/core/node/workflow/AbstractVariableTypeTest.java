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

import static org.apache.commons.lang3.ArrayUtils.toObject;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.knime.core.node.workflow.VariableTypeTestUtils.toBoolean;
import static org.knime.core.node.workflow.VariableTypeTestUtils.toBooleans;
import static org.knime.core.node.workflow.VariableTypeTestUtils.toDouble;
import static org.knime.core.node.workflow.VariableTypeTestUtils.toDoubles;
import static org.knime.core.node.workflow.VariableTypeTestUtils.toInt;
import static org.knime.core.node.workflow.VariableTypeTestUtils.toInts;
import static org.knime.core.node.workflow.VariableTypeTestUtils.toLong;
import static org.knime.core.node.workflow.VariableTypeTestUtils.toLongs;
import static org.knime.core.node.workflow.VariableTypeTestUtils.toStrings;

import java.util.Optional;
import java.util.Set;

import javax.swing.Icon;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
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
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Defines unit tests for the default implementations of {@link VariableType}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> the simple type the tested variable type represents
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractVariableTypeTest<T> {

    /**
     * Mock of NodeSettingsRO that should be used for {@link #testLoadValue()}.
     */
    @Mock
    protected NodeSettingsRO readSettings;

    /**
     * Mock of NodeSettingsWO that should be used for {@link #testSaveValue()}.
     */
    @Mock
    protected NodeSettingsWO writeSettings;

    private final Icon m_expectedIcon;

    private final Class<T> m_expectedSimpleType;

    /**
     * Instance used for testing (VariableTypes should be singletons anyway).
     */
    protected final VariableType<T> m_testInstance;

    private final Set<VariableType<?>> m_convertible;

    /**
     * Constructor.
     *
     * @param expectedIcon the expected icon of the flow variable type
     * @param expectedSimpleType the expected simple type of the flow variable type
     * @param testInstance the instance to test
     * @param convertible the set of types the tested type can be converted to
     */
    protected AbstractVariableTypeTest(final Icon expectedIcon, final Class<T> expectedSimpleType,
        final VariableType<T> testInstance, final Set<VariableType<?>> convertible) {
        m_expectedIcon = expectedIcon;
        m_expectedSimpleType = expectedSimpleType;
        m_testInstance = testInstance;
        m_convertible = convertible;
    }

    /**
     * @param type for which to test conversion
     * @return a value that can be used to test the conversion into {@link VariableType type}
     */
    protected abstract T getValueForTesting(final VariableType<?> type);

    /**
     * @return the expected default value
     */
    protected abstract T getExpectedDefaultValue();

    /**
     * Tests if {@link VariableType#defaultValue()} works as expected.
     */
    @Test
    public abstract void testDefaultValue();

    /**
     * Tests if {@link VariableType#isConvertible(VariableType)} works as expected.
     */
    @Test
    public final void testIsConvertible() {
        for (VariableType<?> convertible : m_convertible) {
            assertTrue(m_testInstance.isConvertible(convertible));
        }
        Optional<VariableType<?>> incompatible =
            VariableTypeRegistry.getInstance().stream().filter(t -> !m_testInstance.isConvertible(t)).findAny();
        if (incompatible.isPresent()) {
            assertFalse(m_testInstance.isConvertible(incompatible.get()));
        }
    }

    /**
     * Tests if {@link VariableType#getConvertibleTypes()} works as expected.
     */
    @Test
    public final void testGetConvertibleTypes() {
        assertEquals(m_convertible, m_testInstance.getConvertibleTypes());
    }

    private boolean isConvertible(final VariableType<?> type) {
        return m_convertible.contains(type);
    }

    /**
     * Tests if {@link VariableType#getAs(Object, VariableType)} works as expected.
     */
    @Test
    public final void testGetAs() {
        testGetAsSimpleTypes();
        testGetAsArrayTypes();
    }

    private void testGetAsArrayTypes() {
        if (isConvertible(BooleanArrayType.INSTANCE)) {
            T testValue = getValueForTesting(BooleanArrayType.INSTANCE);
            assertArrayEquals(ArrayUtils.toObject(toBooleans(getValueForTesting(BooleanArrayType.INSTANCE))),
                m_testInstance.getAs(testValue, BooleanArrayType.INSTANCE));
        }
        if (isConvertible(IntArrayType.INSTANCE)) {
            T testValue = getValueForTesting(IntArrayType.INSTANCE);
            assertArrayEquals(toObject(toInts(testValue)), m_testInstance.getAs(testValue, IntArrayType.INSTANCE));
        }
        if (isConvertible(LongArrayType.INSTANCE)) {
            T testValue = getValueForTesting(LongArrayType.INSTANCE);
            assertArrayEquals(toObject(toLongs(testValue)), m_testInstance.getAs(testValue, LongArrayType.INSTANCE));
        }
        if (isConvertible(DoubleArrayType.INSTANCE)) {
            T testValue = getValueForTesting(DoubleArrayType.INSTANCE);
            assertArrayEquals(toObject(toDoubles(testValue)),
                m_testInstance.getAs(testValue, DoubleArrayType.INSTANCE));
        }
        if (isConvertible(StringArrayType.INSTANCE)) {
            T testValue = getValueForTesting(StringArrayType.INSTANCE);
            assertArrayEquals(toStrings(testValue), m_testInstance.getAs(testValue, StringArrayType.INSTANCE));
        }
    }

    private void testGetAsSimpleTypes() {
        if (isConvertible(BooleanType.INSTANCE)) {
            T testValue = getValueForTesting(BooleanType.INSTANCE);
            assertEquals(toBoolean(testValue), m_testInstance.getAs(testValue, BooleanType.INSTANCE));
        }
        if (isConvertible(IntType.INSTANCE)) {
            T testValue = getValueForTesting(IntType.INSTANCE);
            assertEquals(Integer.valueOf(toInt(testValue)), m_testInstance.getAs(testValue, IntType.INSTANCE));
        }
        if (isConvertible(LongType.INSTANCE)) {
            T testValue = getValueForTesting(LongType.INSTANCE);
            assertEquals(Long.valueOf(toLong(testValue)), m_testInstance.getAs(testValue, LongType.INSTANCE));
        }
        if (isConvertible(DoubleType.INSTANCE)) {
            T testValue = getValueForTesting(DoubleType.INSTANCE);
            assertEquals(Double.valueOf(toDouble(testValue)), m_testInstance.getAs(testValue, DoubleType.INSTANCE));
        }
        if (isConvertible(StringType.INSTANCE)) {
            T testValue = getValueForTesting(StringType.INSTANCE);
            assertEquals(testValue.toString(), m_testInstance.getAs(testValue, StringType.INSTANCE));
        }
    }

    /**
     * Tests if {@link VariableType#getAs(Object, VariableType)} fails with the appropriate exception if called with an
     * incompatible {@link VariableType}.
     */
    @Test(expected = IllegalArgumentException.class)
    public final void testGetAsIncompatibleType() {
        final Optional<VariableType<?>> incompatible =
            VariableTypeRegistry.getInstance().stream().filter(t -> !isConvertible(t)).findAny();
        if (incompatible.isPresent()) {
            T testValue = getValueForTesting(incompatible.get());
            m_testInstance.getAs(testValue, incompatible.get());
        }
    }

    /**
     * Tests if the icon returned by {@link VariableType#getIcon()} is correct.
     */
    @Test
    public final void testGetIcon() {
        assertEquals(m_expectedIcon, m_testInstance.getIcon());
    }

    /**
     * Tests if the type returned by {@link VariableType#getSimpleType()} is correct.
     */
    @Test
    public final void testGetSimpleType() {
        assertEquals(m_expectedSimpleType, m_testInstance.getSimpleType());
    }

    /**
     * Tests if {@link VariableType#newValue(Object)} works as expected.
     */
    @Test
    public final void testNewValue() {
        T testValue = getValueForTesting(StringType.INSTANCE);
        assertEquals(testValue, m_testInstance.newValue(testValue).get());
    }

    /**
     * Tests if {@link VariableType#loadValue(NodeSettingsRO)} works as expected.
     *
     * @throws InvalidSettingsException shouldn't be thrown
     */
    @Test
    public abstract void testLoadValue() throws InvalidSettingsException;

    /**
     * Tests if {@link VariableType#saveValue(NodeSettingsWO, org.knime.core.node.workflow.VariableType.VariableValue)} works as expected.
     */
    @Test
    public abstract void testSaveValue();

    /**
     * Tests if {@link VariableType#canOverwrite(org.knime.core.node.config.Config, String)} works as expected.
     *
     * @throws InvalidSettingsException shouldn't be thrown
     */
    @Test
    public abstract void testCanOverwrite() throws InvalidSettingsException;

    /**
     * Tests if {@link VariableType#overwrite(Object, org.knime.core.node.config.Config, String)} works as expected.
     *
     * @throws InvalidSettingsException shouldn't be thrown
     * @throws InvalidConfigEntryException shouldn't be thrown
     */
    @Test
    public abstract void testOverwrite() throws InvalidSettingsException, InvalidConfigEntryException;

    /**
     * Tests if {@link VariableType#canCreateFrom(org.knime.core.node.config.Config, String)} works as expected.
     *
     * @throws InvalidSettingsException shouldn't be thrown
     */
    @Test
    public abstract void testCanCreateFrom() throws InvalidSettingsException;

    /**
     * Tests if {@link VariableType#createFrom(org.knime.core.node.config.Config, String)} wors as expected.
     *
     * @throws InvalidSettingsException shouldn't be thrown
     * @throws InvalidConfigEntryException shouldn't be thrown
     */
    @Test
    public abstract void testCreateFrom() throws InvalidSettingsException, InvalidConfigEntryException;

}
