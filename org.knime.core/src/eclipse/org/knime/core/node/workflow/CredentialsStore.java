/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jun 7, 2010 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;


/**
 * Container for credentials defined on a workflow. Elements in this store
 * are used by individual node implementations to get password information.
 *
 * <p>This store is modified by the workbench GUI (not by nodes). Any
 * modification should be synchronized (all methods are synchronized) but
 * batch modification should be done when synchronizing on this object.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class CredentialsStore implements Observer {

    private final WorkflowManager m_manager;
    private final Map<String, Credentials> m_credentials;

    /** Create new credential store for a workflow.
     * @param manager The workflow keeping this store to persist credentials.
     */
    @SuppressWarnings("unchecked")
    CredentialsStore(final WorkflowManager manager) {
        this(manager, Collections.EMPTY_LIST);
    }

    /** Create new credential store for a workflow.
     * @param manager The workflow keeping this store to persist credentials.
     * @param creds The list of initial credentials.
     */
    CredentialsStore(final WorkflowManager manager,
            final List<Credentials> creds) {
        if (manager == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_manager = manager;
        m_credentials = new LinkedHashMap<String, Credentials>();
        for (Credentials c : creds) {
            addNoNotify(c);
        }
    }

    /**
     * Read out credentials under a given name. The name is the (global)
     * identifier under which this credentials are store (see
     * {@link Credentials#getName()}).
     * @param name The name to lookup
     * @param client The client accessing the credential (used to keep lookup
     * history and (possibly in future versions) to implement access
     * restrictions
     * @return The credentials for this name (either in this store or any
     * parent store).
     * @throws IllegalArgumentException If the identifier is unknown
     */
    public synchronized Credentials get(final String name,
            final NodeContainer client) {
        Credentials c = get(name);
        c.addClient(client);
        return c;
    }

    /**
     * Read out credentials under a given name. The name is the (global)
     * identifier under which this credentials are store (see
     * {@link Credentials#getName()}).
     * @param name The name to lookup
     * @return The credentials for this name (either in this store or the
     * parent store).
     * @throws IllegalArgumentException If the identifier is unknown
     */
    public Credentials get(final String name) {
        return getAsOptional(name).orElseThrow(() -> new IllegalArgumentException(
            "No credentials stored to name \"" + name + "\""));
    }

    /**
     * Implementation of {@link #get(String)} returning an optional.
     * @param name The name to lookup
     * @return The credentials for this name as {@link Optional} (either in this store or the parent store).
     */
    synchronized Optional<Credentials> getAsOptional(final String name) {
        Credentials c = m_credentials.get(name);
        WorkflowManager parent = m_manager.getParent();
        while (c == null && parent != null) {
            CredentialsStore parentStore = parent.getCredentialsStore();
            c = parentStore.m_credentials.get(name);
            parent = parent.getParent();
        }
        return Optional.ofNullable(c);
    }

    /**
     * Checks, if a {@link CredentialsStore} is contained in this store
     * or the parent store under the given name.
     * @param name credential's name to check
     * @return true, if a credentials exists, otherwise false
     */
    public synchronized boolean contains(final String name) {
        return listNames().contains(name);
    }

    /**
     * Update the {@link Credentials} with the names from the given
     * credentials list. Only the login and password are updated.
     * @param credentialsList the list of credentials to change
     * @return true, if there were changes in any of the fields
     * @throws IllegalArgumentException If the identifier is unknown
     */
    synchronized boolean update(final Credentials... credentialsList) {
        for (Credentials credentials : credentialsList) {
            Credentials c = get(credentials.getName());
            c.setPassword(credentials.getPassword());
            c.setLogin(credentials.getLogin());
        }
        // this could be done smarter, e.g. only notify when things change
        return credentialsList.length > 0;
    }

    /** Get iterable for credentials. Used internally (load/save). Caller
     * must not modify the list! Does not include parent store items.
     * @return The credentials in this store.
     * @noreference This method is not intended to be referenced by clients.
     */
    public Iterable<Credentials> getCredentials() {
        return Collections.unmodifiableCollection(m_credentials.values());
    }

    /** Clear any access history of the given client on any of the credentials.
     * @param nc The client to clear.
     */
    public synchronized void clearClient(final NodeContainer nc) {
        for (Credentials c : m_credentials.values()) {
            c.removeClient(nc);
        }
        WorkflowManager parent = m_manager.getParent();
        while (parent != null) {
            CredentialsStore parentStore = parent.getCredentialsStore();
            for (Credentials c : parentStore.m_credentials.values()) {
                c.removeClient(nc);
            }
            parent = parent.getParent();
        }
    }

    /** Add a new credentials object to this store. Its name must be unique
     * across all credentials in this store.
     * @param cred The new credentials to add.
     * @throws NullPointerException If the argument is null
     * @throws IllegalArgumentException If the the argument's name is already
     * in use
     */
    public void add(final Credentials cred) {
        addNoNotify(cred);
        m_manager.setDirty();
    }

    /** Add credentials, don't notify Observers.
     * @param cred To add.
     */
    private synchronized void addNoNotify(final Credentials cred) {
        if (m_credentials.containsKey(cred.getName())) {
            throw new IllegalArgumentException("Identifier \""
                    + cred.getName() + "\" for credentials already in use");
        }
        cred.addObserver(this);
        m_credentials.put(cred.getName(), cred);
    }

    /** Remove a credentials variable from this store.
     * @param name Name of variable to remove.
     * @throws NullPointerException If the argument is null
     * @throws IllegalArgumentException If the variable is unknown.
     */
    public void remove(final String name) {
        synchronized (this) {
            Credentials c = m_credentials.remove(name);
            if (c == null) {
                throw new IllegalArgumentException("No credentials stored to "
                        + "name \"" + name + "\"");
            }
            c.deleteObserver(this);
        }
        m_manager.setDirty();
    }

    /** Get a list with identifiers of the available credential variables.
     * Each element in the returned list is a valid argument for the
     * {@link #get(String, NodeContainer)} method.
     * @return A collection of valid identifiers.
     */
    public synchronized Collection<String> listNames() {
        ArrayList<String> result = new ArrayList<String>();
        result.addAll(m_credentials.keySet());
        WorkflowManager parent = m_manager.getParent();
        while (parent != null) {
            CredentialsStore parentStore = parent.getCredentialsStore();
            for (String s : parentStore.m_credentials.keySet()) {
                if (!result.contains(s)) {
                    result.add(s);
                }
            }
            parent = parent.getParent();
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Credentials store for workflow + \"" + m_manager.getNameWithID()
            + "\" (" + listNames() + " credentials)";

    }

    /** {@inheritDoc} */
    @Override
    public void update(final Observable o, final Object arg) {
        m_manager.setDirty();
    }

    /** Adds the credentials defined in the argument to this store. Used when running 'external' workflows that need
     * to inherit credentials from the calling node/workflow.
     * @param v Credentials flow variable.
     * @throws IllegalArgumentException If flow variable doesn't represent credentials.
     * @since 3.3
     */
    public void addFromFlowVariable(final FlowVariable v) {
        CheckUtils.checkArgument(v.getType().equals(FlowVariable.Type.CREDENTIALS),
            "Not a crendentials flow variable: %s", v);
        CredentialsFlowVariableValue credentialsValue = v.getCredentialsValue();
        add(new Credentials(credentialsValue.getName(), credentialsValue.getLogin(), credentialsValue.getPassword()));
    }

    /** Framework private method to update or add a credentials object. Used by the Credentials quickform node
     * to hijack the store and add/fix. Subject to change in the next feature release.
     *
     * @param p provider set at node.
     * @param credIdentifier non-null credentials key
     * @param userName login/user name
     * @param password password
     * @noreference This method is not intended to be referenced by clients.
     */
    public static void update(final CredentialsProvider p, final String credIdentifier,
        final String userName, final String password) {
        CredentialsStore store = p.getStore();
        synchronized (store) {
            Credentials credentials = store.m_credentials.get(CheckUtils.checkArgumentNotNull(credIdentifier));
            if (credentials == null) {
                credentials = new Credentials(credIdentifier, userName, password);
                store.add(credentials);
            } else {
                credentials.setLogin(userName);
                credentials.setPassword(password);
            }
            store.m_manager.setDirty();
        }
    }

    /** Factory to create a flow variable wrapping the credentials information. Used by the framework, no API.
     * @noreference This method is not intended to be referenced by clients. */
    public static FlowVariable newCredentialsFlowVariable(final String name, final String login, final String password,
        final boolean saveWeaklyEncrypted, final boolean useServerLogin) throws InvalidSettingsException {
        return new FlowVariable(name, new CredentialsFlowVariableValue(name, login, password));
    }

    static final class CredentialsFlowVariableValue implements ICredentials {

        private final String m_name;
        private final String m_login;
        private final String m_password;

        /**
         * @param credentials
         * @throws InvalidSettingsException
         */
        CredentialsFlowVariableValue(final String name, final String login, final String password) {
            m_name = CheckUtils.checkArgumentNotNull(name);
            m_login = CheckUtils.checkArgumentNotNull(login);
            m_password = password;
        }


        /** {@inheritDoc} */
        @Override
        public String getName() {
            return m_name;
        }

        /** {@inheritDoc} */
        @Override
        public String getLogin() {
            return m_login;
        }

        /** {@inheritDoc} */
        @Override
        public String getPassword() {
            return m_password;
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return new HashCodeBuilder().append(m_name).append(m_login).append(m_password).hashCode();
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(final Object obj) {
            if (obj == null) { return false; }
            if (obj == this) { return true; }
            if (obj.getClass() != getClass()) {
              return false;
            }
            CredentialsFlowVariableValue other = (CredentialsFlowVariableValue)obj;
            return new EqualsBuilder().append(m_name, other.m_name)
                          .append(m_login, other.m_login)
                          .append(m_password, other.m_password)
                          .isEquals();
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return String.format("%s [login: %s, passwort set: %b]", m_name, m_login, m_password != null);
        }

        void save(final NodeSettingsWO settings) {
            settings.addString("name", getName());
            settings.addString("login", getLogin());
        }

        static CredentialsFlowVariableValue load(final NodeSettingsRO settings) throws InvalidSettingsException {
            String name = settings.getString("name");
            String login = settings.getString("login");
            return new CredentialsFlowVariableValue(name, login, null);
        }
    }

    /** Implemented by {@linkplain NodeModel} classes that define credential variables. It allows the implementation
     * to retrieve an up-to-date password from the workflow load routine (e.g. a pop-up window or some commandline
     * flag/prompt).
     *
     * <p>Clients are usually not required to define credential-defining nodes so this interface is not considered to
     * be API and may change in the future, specifically the dependency to {@link WorkflowLoadHelper}.
     *
     * @since 3.1
     */
    public interface CredentialsNode {

        /** Pushes a flow variable wrapping the passed credentials.
         * @param name Non-null identifier
         * @param login Non-null login
         * @param password Possibly null password.
         */
        public default void pushCredentialsFlowVariable(final String name, final String login, final String password) {
            CheckUtils.checkState(this instanceof NodeModel, "Interface %s not implemented by a NodeModel instance");
            Node.invokePushFlowVariable((NodeModel)this,
                new FlowVariable(name, new CredentialsFlowVariableValue(name, login, password)));
        }

        /** Called when node is loaded from disc. Implementations will prompt the password by means of the workflow
         * load helper and then {@link #pushCredentialsFlowVariable(String, String, String)} it.
         * @param loadHelper Non-null helper.
         * @param credProvider The credentials provider set on the corresponding node, not null. Read-only!!
         * @param isExecuted Whether node is executed - executed nodes do not prompt for password via the callback
         * @param isInactive TODO
         */
        public void doAfterLoadFromDisc(final WorkflowLoadHelper loadHelper,
            final CredentialsProvider credProvider, boolean isExecuted, boolean isInactive);

        /**
         * Called when the workflow credentials change on a workflow. Credential QF nodes in the workflow can then
         * inherit the modified password from the workflow. Used to address AP-5974.
         * <p>
         * Method is only called on non-executed nodes; a configure call follows after all nodes in the workflow have
         * been updated.
         *
         * @param workflowCredentials read-only list of workflow credentials.
         * @since 3.3
         */
        public default void onWorkfowCredentialsChanged(final Collection<Credentials> workflowCredentials) {
        }
    }
}
