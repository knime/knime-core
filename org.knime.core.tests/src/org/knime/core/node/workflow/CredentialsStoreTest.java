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
 *   8 Nov 2023 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.node.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.workflow.CredentialsStore.CredentialsFlowVariableValue;
import org.knime.testing.util.WorkflowManagerUtil;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public class CredentialsStoreTest {

    private WorkflowManager m_wfm;

    @BeforeEach
    void createEmptyWorkflow() throws IOException {
        m_wfm = WorkflowManagerUtil.createEmptyWorkflow();
    }

    @AfterEach
    void disposeWorkflow() {
        WorkflowManagerUtil.disposeWorkflow(m_wfm);
    }

    @Test
    void testAddFromVariable() throws InvalidSettingsException {
        final var credStore = new CredentialsStore(m_wfm);
        credStore.addFromFlowVariable(
            CredentialsStore.newCredentialsFlowVariable("name", "login", "password", false, false));
        assertEquals(new Credentials("name", "login", "password"), credStore.get("name"));
    }

    @Test
    void testAddFromVariableWithSecondFactor() {
        final var credStore = new CredentialsStore(m_wfm);
        credStore.addFromFlowVariable(
            CredentialsStore.newCredentialsFlowVariable("name", "login", "password", "second factor"));
        assertEquals(new Credentials("name", "login", "password", "second factor"), credStore.get("name"));
    }

    @Test
    void testUpdate() {
        final var credStore = new CredentialsStore(m_wfm);
        final var credProvider = new CredentialsProvider(m_wfm, credStore);
        CredentialsStore.update(credProvider, "name", "login", "password");
        assertEquals(new Credentials("name", "login", "password"), credStore.get("name"));
        CredentialsStore.update(credProvider, "name", "new login", "new password");
        assertEquals(new Credentials("name", "new login", "new password"), credStore.get("name"));
    }

    @Test
    void testUpdateWithSecondFactor() {
        final var credStore = new CredentialsStore(m_wfm);
        final var credProvider = new CredentialsProvider(m_wfm, credStore);
        CredentialsStore.update(credProvider, "name", "login", "password", "second factor");
        assertEquals(new Credentials("name", "login", "password", "second factor"), credStore.get("name"));
        CredentialsStore.update(credProvider, "name", "new login", "new password", "new second factor");
        assertEquals(new Credentials("name", "new login", "new password", "new second factor"), credStore.get("name"));
    }

    @Nested
    @SuppressWarnings("unused")
    class CredentialsStoreCredentialsFlowVariableValue {

        @Test
        void testEqualsHashCodeContracts() {
            EqualsVerifier.forClass(CredentialsFlowVariableValue.class).suppress(Warning.NONFINAL_FIELDS).verify();
        }

        @Test
        void testToString() {
            assertEquals(new CredentialsFlowVariableValue("name", "login", null, null).toString(),
                "name [login: login, password set: false]");
            assertEquals(new CredentialsFlowVariableValue("name", "login", "password", null).toString(),
                "name [login: login, password set: true]");
            assertEquals(new CredentialsFlowVariableValue("name", "login", null, "second factor").toString(),
                    "name [login: login, password set: false, second factor set: true]");
            assertEquals(new CredentialsFlowVariableValue("name", "login", "password", "second factor").toString(),
                "name [login: login, password set: true, second factor set: true]");
        }

    }

    @Nested
    class CredentialsPropertiesTest {

        @Test
        void testPropertiesWithPassword() throws InvalidSettingsException {
            final var credStore = new CredentialsStore(m_wfm);
            final var fv = CredentialsStore.newCredentialsFlowVariable("a", "b", "c", "2FA");
            credStore.addFromFlowVariable(fv);
            final var optProps = CredentialsStore.CredentialsProperties.of(fv);
            assertEquals(true, optProps.isPresent(), "Properties are not created.");
            final var props = optProps.get();
            assertEquals("a", props.name());
            assertEquals("b", props.login());
            assertEquals(true, props.isPasswordSet());
            assertEquals(true, props.isSecondAuthenticationFactorSet());
        }

        @Test
        void testPropertiesWithNullPassword() throws InvalidSettingsException {
            final var credStore = new CredentialsStore(m_wfm);
            final var fv = CredentialsStore.newCredentialsFlowVariable("a", "b", null, null);
            credStore.addFromFlowVariable(fv);
            final var optProps = CredentialsStore.CredentialsProperties.of(fv);
            assertEquals(true, optProps.isPresent(), "Properties are not created.");
            final var props = optProps.get();
            assertEquals("a", props.name());
            assertEquals("b", props.login());
            assertEquals(false, props.isPasswordSet());
            assertEquals(false, props.isSecondAuthenticationFactorSet());
        }

        @Test
        void testPropertiesWithEmptyPassword() throws InvalidSettingsException {
            final var credStore = new CredentialsStore(m_wfm);
            final var fv = CredentialsStore.newCredentialsFlowVariable("a", "b", "", "");
            credStore.addFromFlowVariable(fv);
            final var optProps = CredentialsStore.CredentialsProperties.of(fv);
            assertEquals(true, optProps.isPresent(), "Properties are not created.");
            final var props = optProps.get();
            assertEquals("a", props.name());
            assertEquals("b", props.login());
            assertEquals(false, props.isPasswordSet());
            assertEquals(false, props.isSecondAuthenticationFactorSet());
        }
    }

}
