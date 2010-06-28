/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
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
import java.util.LinkedHashMap;
import java.util.Map;


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
public final class CredentialsStore {

    private final WorkflowManager m_manager;
    private final Map<String, Credentials> m_credentials;

    /** Create new credential store for a workflow.
     * @param manager The workflow keeping this store to persist credentials.
     */
    CredentialsStore(final WorkflowManager manager) {
        if (manager == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_manager = manager;
        m_credentials = new LinkedHashMap<String, Credentials>();
    }

    /**
     * Read out credentials under a given name. The name is the (global)
     * identifier under which this credentials are store (see
     * {@link Credentials#getName()}).
     * @param name The name to lookup
     * @param client The client accessing the credential (used to keep lookup
     * history and (possibly in future versions) to implement access
     * restrictions
     * @return The credentials for this name
     * @throws IllegalArgumentException If the identifier is unknown
     */
    public synchronized Credentials get(final String name,
            final NodeContainer client) {
        Credentials c = m_credentials.get(name);
        if (c == null) {
            throw new IllegalArgumentException("No credentials stored to "
                    + "name \"" + name + "\"");
        }
        c.addClient(client);
        return c;
    }

    /** Get iterable for credentials. Used internally (load/save).
     * @return The credentials in this store.
     */
    public Iterable<Credentials> getCredentials() {
        return m_credentials.values();
    }

    /** Clear any access history of the given client on any of the credentials.
     * @param nc The client to clear.
     */
    public synchronized void clearClient(final NodeContainer nc) {
        for (Credentials c : m_credentials.values()) {
            c.removeClient(nc);
        }
    }

    /** Add a new credentials object to this store. Its name must be unique
     * across all credentials in this store.
     * @param cred The new credentials to add.#
     * @throws NullPointerException If the argument is null
     * @throws IllegalArgumentException If the the argument's name is already
     * in use
     */
    public synchronized void add(final Credentials cred) {
        if (m_credentials.containsKey(cred.getName())) {
            throw new IllegalArgumentException("Identifier \""
                    + cred.getName() + "\" for credentials already in use");
        }
        m_credentials.put(cred.getName(), cred);
    }

    /** Remove a credentials variable from this store.
     * @param name Name of variable to remove.
     * @throws NullPointerException If the argument is null
     * @throws IllegalArgumentException If the variable is unknown.
     */
    public synchronized void remove(final String name) {
        Credentials c = m_credentials.remove(name);
        if (c == null) {
            throw new IllegalArgumentException("No credentials stored to "
                    + "name \"" + name + "\"");
        }
    }
    
    /** Replace the credential variable given by the credential name. 
     * @param cred to be replaced in this store
     * @return true, if the previous value is replace, otherwise false
     */
    public synchronized boolean replace(final Credentials cred) {
        return (m_credentials.put(cred.getName(), cred) == null);
    }

    /** Get a list with identifiers of the available credential variables.#
     * Each element in the returned list is a valid argument for the
     * {@link #get(String, NodeContainer)} and {@link #remove(String)} method.
     * @return A collection of valid identifiers.
     */
    public synchronized Collection<String> listNames() {
        return new ArrayList<String>(m_credentials.keySet());
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Credentials store for workflow + \"" + m_manager.getNameWithID()
            + "\" (" + listNames() + " credentials)";

    }

}
