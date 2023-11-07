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
 *   6 Nov 2023 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.node.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.core.node.workflow.CredentialsStore.CredentialsFlowVariableValue.CFG_LOGIN;
import static org.knime.core.node.workflow.CredentialsStore.CredentialsFlowVariableValue.CFG_NAME;
import static org.knime.core.node.workflow.CredentialsStore.CredentialsFlowVariableValue.CFG_PWD;
import static org.knime.core.node.workflow.CredentialsStore.CredentialsFlowVariableValue.SECRET;
import static org.knime.core.node.workflow.VariableTypeTestUtils.CFG_VALUE;
import static org.knime.core.node.workflow.VariableTypeTestUtils.KEY;

import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.util.SharedIcons;
import org.knime.core.node.workflow.CredentialsStore.CredentialsFlowVariableValue;
import org.knime.core.node.workflow.VariableType.CredentialsType;
import org.knime.core.node.workflow.VariableType.InvalidConfigEntryException;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public class CredentialsTypeTest extends AbstractVariableTypeTest<CredentialsFlowVariableValue> {

    private static final CredentialsFlowVariableValue VARIABLE = new CredentialsFlowVariableValue("foo", "bar", "baz");

    static final NodeSettings createCredentialsSettings(final String name, final String login, final String password) {
        final var settings = new NodeSettings(CFG_VALUE);
        settings.addString(CFG_NAME, name);
        settings.addString(CFG_LOGIN, login);
        if (password != null) {
            settings.addPassword(CFG_PWD, SECRET, password);
        }
        return settings;
    }

    /**
     * Constructor
     */
    public CredentialsTypeTest() {
        super(SharedIcons.FLOWVAR_GENERAL.get(), CredentialsFlowVariableValue.class, CredentialsType.INSTANCE,
            Set.of(CredentialsType.INSTANCE));
    }

    @Override
    protected CredentialsFlowVariableValue getValueForTesting(final VariableType<?> type) {
        return VARIABLE;
    }

    @Override
    protected CredentialsFlowVariableValue getExpectedDefaultValue() {
        throw new NotImplementedException("The CredentialsType does not support the creation of default values.");
    }

    @Override
    public void testDefaultValue() {
        assertThrows(NotImplementedException.class, () -> m_testInstance.defaultValue().get());
    }

    @Override
    public void testLoadValue() throws InvalidSettingsException {
        final var settings = new NodeSettings(KEY);
        settings.addNodeSettings(createCredentialsSettings("foo", "bar", "baz"));
        assertEquals(VARIABLE, m_testInstance.loadValue(settings).get());
    }

    @Override
    public void testSaveValue() {
        final var settings = new NodeSettings(KEY);
        m_testInstance.saveValue(settings, m_testInstance.newValue(VARIABLE));
        assertEquals(createCredentialsSettings("foo", "bar", null), settings.getEntry(CFG_VALUE));
    }

    @Override
    public void testCanOverwrite() throws InvalidSettingsException {
        final var settings = new NodeSettings(KEY);
        assertFalse(m_testInstance.canOverwrite(settings, CFG_VALUE));
        final var subSettings = settings.addNodeSettings(CFG_VALUE);
        assertFalse(m_testInstance.canOverwrite(settings, CFG_VALUE));
        subSettings.addString(CFG_NAME, "foo");
        assertFalse(m_testInstance.canOverwrite(settings, CFG_VALUE));
        subSettings.addString(CFG_LOGIN, "bar");
        assertFalse(m_testInstance.canOverwrite(settings, CFG_VALUE));
        subSettings.addPassword(CFG_PWD, SECRET, "baz");
        assertTrue(m_testInstance.canOverwrite(settings, CFG_VALUE));
    }

    @Override
    public void testOverwrite() throws InvalidSettingsException, InvalidConfigEntryException {
        final var settings = new NodeSettings(KEY);
        assertThrows(InvalidConfigEntryException.class, () -> m_testInstance.overwrite(VARIABLE, settings, CFG_VALUE));
        try {
            m_testInstance.overwrite(VARIABLE, settings, CFG_VALUE);
        } catch (InvalidConfigEntryException e) {
            assertThat(e.getErrorMessageWithVariableName("").isPresent());
        }
        settings.addNodeSettings(createCredentialsSettings("", "", ""));
        m_testInstance.overwrite(VARIABLE, settings, CFG_VALUE);
        assertEquals(createCredentialsSettings("foo", "bar", "baz"), settings.getConfig(CFG_VALUE));
    }

    @Override
    public void testCanCreateFrom() throws InvalidSettingsException {
        final var settings = new NodeSettings(KEY);
        assertFalse(m_testInstance.canCreateFrom(settings, CFG_VALUE));
        final var subSettings = settings.addNodeSettings(CFG_VALUE);
        assertFalse(m_testInstance.canCreateFrom(settings, CFG_VALUE));
        subSettings.addString(CFG_NAME, "foo");
        assertFalse(m_testInstance.canCreateFrom(settings, CFG_VALUE));
        subSettings.addString(CFG_LOGIN, "bar");
        assertFalse(m_testInstance.canCreateFrom(settings, CFG_VALUE));
        subSettings.addPassword(CFG_PWD, SECRET, "baz");
        assertTrue(m_testInstance.canCreateFrom(settings, CFG_VALUE));
    }

    @Override
    public void testCreateFrom() throws InvalidSettingsException, InvalidConfigEntryException {
        final var settings = new NodeSettings(KEY);
        assertThrows(InvalidConfigEntryException.class, () -> m_testInstance.createFrom(settings, CFG_VALUE));
        try {
            m_testInstance.createFrom(settings, CFG_VALUE);
        } catch (InvalidConfigEntryException e) {
            assertThat(e.getErrorMessageWithVariableName("").isPresent());
        }
        settings.addNodeSettings(createCredentialsSettings("foo", "bar", "baz"));
        assertEquals(VARIABLE, m_testInstance.createFrom(settings, CFG_VALUE));
    }

}
