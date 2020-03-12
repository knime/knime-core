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
import static org.knime.core.node.workflow.VariableTypeTestUtils.CFG_VALUE;
import static org.knime.core.node.workflow.VariableTypeTestUtils.KEY;
import static org.knime.core.node.workflow.VariableTypeTestUtils.isBoolean;
import static org.knime.core.node.workflow.VariableTypeTestUtils.isBooleanArray;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.Set;

import org.junit.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.base.ConfigEntries;
import org.knime.core.node.config.base.ConfigPasswordEntry;
import org.knime.core.node.util.SharedIcons;
import org.knime.core.node.workflow.VariableType.BooleanArrayType;
import org.knime.core.node.workflow.VariableType.BooleanType;
import org.knime.core.node.workflow.VariableType.InvalidConfigEntryException;
import org.knime.core.node.workflow.VariableType.StringArrayType;
import org.knime.core.node.workflow.VariableType.StringType;
import org.knime.core.node.workflow.VariableTypeTestUtils.ConfigMocker;
import org.knime.core.node.workflow.VariableTypeTestUtils.ConfigMocker.Options;
import org.mockito.Mockito;

import com.google.common.collect.Sets;

/**
 * Unit tests for {@link StringType}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class StringTypeTest extends AbstractSimpleVariableTypeTest<String> {

    private static final Set<ConfigEntries> OVERWRITABLE =
        EnumSet.of(ConfigEntries.xboolean, ConfigEntries.xchar, ConfigEntries.xstring, ConfigEntries.xtransientstring);

    private static final Set<ConfigEntries> CREATION_COMPATIBLE =
        EnumSet.of(ConfigEntries.xchar, ConfigEntries.xstring);

    /**
     * Constructor.
     */
    public StringTypeTest() {
        super(SharedIcons.FLOWVAR_STRING.get(), String.class, StringType.INSTANCE, OVERWRITABLE, CREATION_COMPATIBLE,
            Sets.newHashSet(StringType.INSTANCE, StringArrayType.INSTANCE, BooleanType.INSTANCE,
                BooleanArrayType.INSTANCE));
    }

    @Override
    public void testLoadValue() throws InvalidSettingsException {
        when(readSettings.getString(CFG_VALUE)).thenReturn("foo");
        assertEquals("foo", m_testInstance.loadValue(readSettings).get());
    }

    @Override
    public void testSaveValue() {
        m_testInstance.saveValue(writeSettings, m_testInstance.newValue("foo"));
        verify(writeSettings).addString(CFG_VALUE, "foo");
    }

    /**
     * Tests if a string variable can be created from a password config.
     *
     * @throws InvalidConfigEntryException never thrown
     * @throws InvalidSettingsException never thrown
     */
    @Test
    public void testCreateFromPassword() throws InvalidSettingsException, InvalidConfigEntryException {
        final Config config = Mockito.mock(Config.class);
        when(config.getEntry(KEY)).thenReturn(new ConfigPasswordEntry(KEY, "pwd"));
        assertEquals("pwd", m_testInstance.createFrom(config, KEY));
    }

    @Override
    protected String getValueForTesting(final VariableType<?> type) {
        if (isBoolean(type) || isBooleanArray(type)) {
            return "true";
        } else {
            return "t";
        }
    }

    /**
     * Tests if overwriting char configs fails if the string has not exactly one character.
     *
     * @throws InvalidSettingsException never thrown
     * @throws InvalidConfigEntryException because strings are not exactly 1 character long can't be converted to char
     */
    @Test(expected = InvalidConfigEntryException.class)
    public void testOverwriteInvalidChar() throws InvalidConfigEntryException, InvalidSettingsException {
        m_testInstance.overwrite("foo", new ConfigMocker(Options.ENTRY_GETTER).mockBooleanConfig(true), KEY);
    }

    @Override
    protected String getExpectedDefaultValue() {
        return "";
    }

}
