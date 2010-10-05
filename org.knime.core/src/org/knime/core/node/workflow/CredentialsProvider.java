/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *   Jun 8, 2010 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.util.Collection;

import org.knime.core.node.NodeModel;


/** Provides available credential variables for a workflow. Credentials are
 * globally defined on workflows, a <code>CredentialsProvider</code> provides
 * read-only access to these credentials.
 *
 * <p>Objects of this class are available in the {@link NodeModel} by the
 * corresponding get method. Concrete implementations of a node will use a
 * dialog component to list all available credentials identifiers
 * (available in {@link #listNames()}), let the user choose one of these
 * identifiers, and fetch the concrete credentials paramenter using
 * {@link #get(String)}.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class CredentialsProvider {

    private final NodeContainer m_client;
    private final CredentialsStore m_store;

    /** Creates new provider for a given node container and a store to read
     * from.
     * @param client The client that fetches password values (used to track,
     * e.g. usage history)
     * @param store The store to get the credentials from.
     */
    CredentialsProvider(final NodeContainer client,
            final CredentialsStore store) {
        if (client == null || store == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_store = store;
        m_client = client;
    }

    /** Read a credentials variable from the store.
     * @param name The identifier of the credentials parameter of interest.
     * @return The credentials for the identifier.
     * @throws IllegalArgumentException If the name is invalid
     * (no such credentials)
     */
    public ICredentials get(final String name) {
        return m_store.get(name, m_client);
    }

    /** List all credentials variables available in the workflow. The names
     * returned in the collection are valid identifiers for the
     * {@link #get(String)} method.
     * @return the collection of valid credentials identifiers.
     */
    public Collection<String> listNames() {
        return m_store.listNames();
    }

    /** @return the client */
    NodeContainer getClient() {
        return m_client;
    }

    /** @return the store */
    CredentialsStore getStore() {
        return m_store;
    }

    /** Remove history of get invocations associated with this client. */
    void clearClientHistory() {
        m_store.clearClient(m_client);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Credentials provider for \"" + m_client.getNameWithID() + "\" ("
            + listNames().size() + " credentials)";
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return m_client.hashCode() + m_store.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof CredentialsProvider) {
            CredentialsProvider prov = (CredentialsProvider)obj;
            return prov.m_client.equals(m_client)
                && prov.m_store.equals(m_store);
        }
        return false;
    }

}
