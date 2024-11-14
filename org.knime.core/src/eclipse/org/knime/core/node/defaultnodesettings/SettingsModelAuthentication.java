/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.defaultnodesettings;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.Config;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ButtonGroupEnumInterface;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.CredentialsProvider;

/**
 * Model representing credentials, either username/password or a selected credentials variable.
 * @author Lara Gorini
 * @since 3.2
 * @see DialogComponentAuthentication
 */
public final class SettingsModelAuthentication extends SettingsModel {

    private static final String EMPTY_USERNAME = "";

    private static final String EMPTY_PASSWORD = "";

    private static final String EMPTY_CREDENTIAL = null;

    private String m_username;

    private String m_password;

    private String m_credentials;

    private AuthenticationType m_type;

    private final String m_configName;

    private final static String secretKey = "c-rH4Tkyk";

    private final static String CREDENTIAL = "credentials";

    private final static String PASSWORD = "password";

    private final static String USERNAME = "username";

    private final static String SELECTED_TYPE = "selectedType";

    /** Whether to use a credentials identifier or plain username/password. */
    public enum AuthenticationType  implements ButtonGroupEnumInterface {
        /** No authentication required. */
        NONE("None", "No authentication is required."),
        /** Authentication with username. */
        USER("Username", "Username based authentication. No password required."),
        /** Authentication with username and password. */
        USER_PWD("Username & password", "Username and password based authentication"),
        /**
         * Authentication with password.
         * @since 4.1
         */
        PWD("Password", "Password based authentication. No username required."),
        /** Authentication with workflow credentials. */
        CREDENTIALS("Credentials", "Workflow credentials"),
        /** Authentication with workflow credentials. */
        KERBEROS("Kerberos", "Kerberos ticket based authentication");

        private String m_toolTip;
        private String m_text;

        private AuthenticationType(final String text, final String toolTip) {
            m_text = text;
            m_toolTip = toolTip;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getText() {
            return m_text;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getActionCommand() {
            return name();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getToolTip() {
            return m_toolTip;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isDefault() {
            //user name and password is the default
            return this.equals(USER_PWD);
        }

        /**
         * @return true if this type is {@link #PWD} or {@link #USER_PWD}. Added as part of AP-15442 so that
         * #addPassword method invocations are only done when needed.
         * @noreference This enum method is not intended to be referenced by clients.
         */
        public boolean requiresPassword() {
            return this == PWD || this == USER_PWD;
        }

        /**
         * @return true if this type is {@link #USER} or {@link #USER_PWD}.
         * @noreference This enum method is not intended to be referenced by clients.
         */
        public boolean requiresUsername() {
            return this == USER || this == USER_PWD;
        }

        /**
         * @param actionCommand the action command
         * @return the {@link AuthenticationType} for the action command
         */
        public static AuthenticationType get(final String actionCommand) {
            return valueOf(actionCommand);
        }
    }

    /**
     * Constructor with no preset user and credential.
     * @param configName The identifier the values are stored with in the {@link org.knime.core.node.NodeSettings} object.
     * @param defaultType the initial authentication type
     */
    public SettingsModelAuthentication(final String configName, final AuthenticationType defaultType) {
        this(configName, defaultType, null, null, null);
    }

    /**
     * @param configName The identifier the values are stored with in the {@link org.knime.core.node.NodeSettings} object.
     * @param defaultType the initial authentication type
     * @param defaultUsername Initial username.
     * @param defaultPassword Initial password.
     * @param defaultCredential Initial credential name.
     */
    public SettingsModelAuthentication(final String configName, final AuthenticationType defaultType,
        final String defaultUsername, final String defaultPassword, final String defaultCredential) {
        CheckUtils.checkArgument(StringUtils.isNotEmpty(configName), "The configName must be a non-empty string");

        m_username = Objects.requireNonNullElse(defaultUsername, EMPTY_USERNAME);
        m_password = Objects.requireNonNullElse(defaultPassword, EMPTY_PASSWORD);

        m_credentials = defaultCredential;
        m_configName = configName;

        m_type = defaultType;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelAuthentication createClone() {
        return new SettingsModelAuthentication(m_configName, m_type, m_username, m_password, m_credentials);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModelTypeID() {
        return "SMID_authentication";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getConfigName() {
        return m_configName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        saveSettingsForModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final Config config = settings.getConfig(m_configName);
        final String type = config.getString(SELECTED_TYPE);
        final String credential = config.getString(CREDENTIAL);
        final String userName = config.getString(USERNAME);
        final AuthenticationType authType = AuthenticationType.get(type);
        final String pwd = authType.requiresPassword() //
                ? config.getPassword(PASSWORD, secretKey) //
                : EMPTY_PASSWORD;
        switch (authType) {
            case CREDENTIALS:
                if (StringUtils.isEmpty(credential)) {
                    throw new InvalidSettingsException("Please select a valid credential");
                }
                break;
            case KERBEROS:
                break;
            case NONE:
                break;
            case USER:
                if (StringUtils.isEmpty(userName)) {
                    throw new InvalidSettingsException("Please enter a valid user name");
                }
                break;
            case USER_PWD:
                if (StringUtils.isEmpty(userName)) {
                    throw new InvalidSettingsException("Please enter a valid user name");
                }
                if (StringUtils.isEmpty(pwd)) {
                    throw new InvalidSettingsException("Please enter a valid password");
                }
                break;
            case PWD:
                if (StringUtils.isEmpty(pwd)) {
                    throw new InvalidSettingsException("Please enter a valid password");
                }
                break;
            default:
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        // no default value, throw an exception instead
        final var config = settings.getConfig(m_configName);
        final var authType = AuthenticationType.valueOf(config.getString(SELECTED_TYPE));
        final var credentials = authType == AuthenticationType.CREDENTIALS //
            ? config.getString(CREDENTIAL) : EMPTY_CREDENTIAL;
        final var username = authType.requiresUsername() //
            ? config.getString(USERNAME) : EMPTY_USERNAME;
        final var password = authType.requiresPassword() //
            ? config.getPassword(PASSWORD, secretKey) : EMPTY_PASSWORD;
        setValues(authType, credentials, username, password);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        // use the current value, if no value is stored in the settings
        final Config config;
        try {
            config = settings.getConfig(m_configName);
            setValues(AuthenticationType.valueOf(config.getString(SELECTED_TYPE, m_type.name())),
                config.getString(CREDENTIAL, m_credentials), config.getString(USERNAME, m_username),
                config.getPassword(PASSWORD, secretKey, m_password));
        } catch (InvalidSettingsException ex) {
            throw new NotConfigurableException(ex.getMessage());
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        Config config = settings.addConfig(m_configName);
        if (useCredential()) {
            config.addString(CREDENTIAL, m_credentials);
        }
        if (m_type.requiresUsername()) {
            config.addString(USERNAME, m_username);
        }
        if (m_type.requiresPassword()) { // only when needed, see AP-15442
            config.addPassword(PASSWORD, secretKey, m_password);
        }
        config.addString(SELECTED_TYPE, m_type.name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + m_configName + "')";
    }

    /**
     * @param type Type of authentication that is selected.
     * @param selectedCredentials Credentials that is selected.
     * @param userName Given username.
     * @param pwd Given password.
     */
    public void setValues(final AuthenticationType type, final String selectedCredentials, final String userName,
        final String pwd) {
        boolean changed = false;
        changed = setType(type) || changed;
        changed = setCredential(selectedCredentials) || changed;
        changed = setUsername(userName) || changed;
        changed = setPassword(pwd) || changed;
        if (changed) {
            notifyChangeListeners();
        }
    }

    private boolean setType(final AuthenticationType type) {
        boolean sameValue = type.name().equals(m_type.name());
        m_type = type;
        return !sameValue;
    }

    private boolean setCredential(final String newValue) {

        boolean sameValue;
        if (newValue == null) {
            sameValue = (m_credentials == null);
        } else {
            sameValue = newValue.equals(m_credentials);
        }
        m_credentials = newValue;
        return !sameValue;
    }

    private boolean setUsername(final String newValue) {
        boolean sameValue;
        if (newValue == null) {
            sameValue = (m_username == null);
        } else {
            sameValue = newValue.equals(m_username);
        }
        m_username = newValue;
        return !sameValue;
    }

    private boolean setPassword(final String pwd) {
        boolean sameValue;
        if (pwd == null) {
            sameValue = (m_password == null);
        } else {
            sameValue = pwd.equals(m_password);
        }
        m_password = pwd;
        return !sameValue;
    }

    /**
     * @return credential name.
     */
    public String getCredential() {
        return m_credentials;
    }

    /**
     * @return username.
     */
    public String getUsername() {
        return m_username;
    }

    /**
     * Helper method that returns the user name from the {@link CredentialsProvider} if {@link #useCredential()}
     * returns <code>true</code> otherwise it returns the value of the user name field.
     * @param cp the {@link CredentialsProvider} to use
     * @return the user name
     * @since 3.4
     */
    public String getUserName(final CredentialsProvider cp) {
        if (useCredential()) {
            final String credential = getCredential();
            if (StringUtils.isBlank(credential)) {
                return null;
            }
            return cp.get(credential).getLogin();
        }
        return getUsername();
    }

    /**
     * @return password.
     */
    public String getPassword() {
        return m_password;
    }

    /**
     * Helper method that returns the password from the {@link CredentialsProvider} if {@link #useCredential()}
     * returns <code>true</code> otherwise it returns the value of the password field.
     * @param cp the {@link CredentialsProvider} to use
     * @return the password
     * @since 3.4
     */
    public String getPassword(final CredentialsProvider cp) {
        if (useCredential()) {
            final String credential = getCredential();
            if (StringUtils.isBlank(credential)) {
                return null;
            }
            return cp.get(getCredential()).getPassword();
        }
        return getPassword();
    }

    /**
     * @return selected type.
     */
    public AuthenticationType getAuthenticationType() {
        return m_type;
    }


    /**
     * @return <code>true</code> if {@link AuthenticationType} {@link AuthenticationType#KERBEROS} is selected
     */
    public boolean useKerberos() {
        return AuthenticationType.KERBEROS == getAuthenticationType();
    }

    /**
     * @return <code>true</code> if {@link AuthenticationType} {@link AuthenticationType#CREDENTIALS} is selected
     */
    public boolean useCredential() {
        return AuthenticationType.CREDENTIALS == getAuthenticationType();
    }

    /**
     * Clears all stored credentials by resetting them to default values, i.e. empty
     * strings and null values. Does not modify the stored {@link AuthenticationType}.
     * <p>
     * Advisable to call when stored credentials do not need to be persisted.
     *
     * @since 5.3
     */
    public void clear() {
        setUsername(EMPTY_USERNAME);
        setPassword(EMPTY_PASSWORD);
        setCredential(EMPTY_CREDENTIAL);
    }
}
