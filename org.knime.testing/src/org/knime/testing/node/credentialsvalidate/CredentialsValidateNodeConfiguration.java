/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *   Mar 13, 2015 (wiswedel): created
 */
package org.knime.testing.node.credentialsvalidate;

import java.util.Objects;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.ICredentials;

/**
 * Config to node.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class CredentialsValidateNodeConfiguration {

    private String m_credentialsID;
    private String m_username;
    private String m_password;

    private boolean m_passwordExpectedToBeSet;

    /**
     * @return the credId
     */
    String getCredId() {
        return m_credentialsID;
    }

    /**
     * @param credId the credId to set
     */
    void setCredentialsID(final String credId) {
        m_credentialsID = credId;
    }

    /**
     * @return the username
     */
    String getUsername() {
        return m_username;
    }

    /**
     * @param username the username to set
     */
    void setUsername(final String username) {
        m_username = username;
    }

    /**
     * @return the password
     */
    String getPassword() {
        return m_password;
    }

    /**
     * @param password the password to set
     */
    void setPassword(final String password) {
        m_password = password;
    }

    /**
     * @return the passwordExpectedToBeSet
     */
    boolean isPasswordExpectedToBeSet() {
        return m_passwordExpectedToBeSet;
    }

    /**
     * @param passwordExpectedToBeSet the property
     */
    void setPasswordExpectedToBeSet(final boolean passwordExpectedToBeSet) {
        m_passwordExpectedToBeSet = passwordExpectedToBeSet;
    }

    void verify(final CredentialsProvider credProvider) throws InvalidSettingsException {
        CheckUtils.checkSetting(credProvider.listNames().contains(m_credentialsID),
            "Invalid credentials ID '%s'", m_credentialsID);
        ICredentials iCredentials = credProvider.get(m_credentialsID);
        CheckUtils.checkSetting(Objects.equals(iCredentials.getLogin(), getUsername()),
            "Wrong user name, expected '%s' but got '%s'", getUsername(), iCredentials.getLogin());
        if (isPasswordExpectedToBeSet()) {
            CheckUtils.checkSetting(Objects.equals(iCredentials.getPassword(), getPassword()),
                "Wrong password, expected '%s' but got %s",
                getPassword(), iCredentials.getPassword() != null ? "something different" : "null");
        } else {
            CheckUtils.checkSetting(iCredentials.getPassword() == null,
                    "Password expected to be not set (null) but is %s", iCredentials.getPassword());
        }
    }

    void saveSettings(final NodeSettingsWO s) {
        s.addString("credentialsID", m_credentialsID);
        s.addString("username", m_username);
        s.addString("password", m_password);
        s.addBoolean("passwordExpectedToBeSet", m_passwordExpectedToBeSet);
    }

    CredentialsValidateNodeConfiguration loadSettingsInModel(final NodeSettingsRO s) throws InvalidSettingsException {
        m_credentialsID = s.getString("credentialsID");
        m_username = s.getString("username");
        m_password = s.getString("password");
        m_passwordExpectedToBeSet = s.getBoolean("passwordExpectedToBeSet");
        return this;
    }

    void loadSettingsInDialog(final NodeSettingsRO s) {
        m_credentialsID = s.getString("credentialsID", "credentials-id");
        m_username = s.getString("username", "some-user-name");
        m_password = s.getString("password", "some-password");
        m_passwordExpectedToBeSet = s.getBoolean("passwordExpectedToBeSet", false);
    }
}
