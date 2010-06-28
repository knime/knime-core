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
 */
package org.knime.core.node.workflow;

import java.util.WeakHashMap;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/** The credentials implementation. Fields are mutable (in comparison to the
 * interface).
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class Credentials implements ICredentials {

    private final String m_name;
    private String m_login;
    private String m_password;

    /** A set of nodes that read this credentials object in either their
     * configure or execute methods. Used to determine which of the credentials
     * need to be prompted on workflow load.
     *
     * <p>This values in this map are null (only using weak reference
     * property of this map implementation). */
    private WeakHashMap<NodeContainer, Object> m_clientNodes;

    /** Create new credentials for a given name. The name must be unique in
     * the context of a workflow's credentials store.
     * @param name Name of credentials (identifier)
     */
    Credentials(final String name) {
        this(name, null, null);
    }

    /** Create new credentials for a given name, initializing defaults.
     *  The name must be unique in the context of a workflow's credentials
     *  store.
     * @param name Name of credentials (identifier)
     * @param login The login name.
     * @param password the password.
     */
    public Credentials(final String name, final String login, 
        final String password) {
        if (name == null) {
            throw new NullPointerException("Argument must not be null");
        }
        if (name.length() == 0) {
            throw new IllegalArgumentException("Name must not be empty");
        }
        m_name = name;
        m_login = login;
        m_password = password;
    }

    /** {@inheritDoc} */
    @Override
    public String getLogin() {
        return m_login;
    }

    /**
     * @param login the login to set
     */
    void setLogin(final String login) {
        m_login = login;
    }

    /** {@inheritDoc} */
    @Override
    public String getPassword() {
        return m_password;
    }

    /**
     * @param password the password to set
     */
    void setPassword(final String password) {
        m_password = password;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return m_name;
    }
    
    /**
     * @return true, if this and the given {@link Credentials} object's
     *         name and login matches, otherwise false 
     * {@inheritDoc} 
     */
    @Override
    public boolean equals(final Object obj) {
	if (obj == this) {
	    return true;
	}
	if (!(obj instanceof Credentials)) {
	    return false;
	}
	Credentials cred = (Credentials) obj;
	if (!m_name.equals(cred.m_name)) {
	    return false;
	}
	if (!m_login.equals(cred.m_login)) {
	    return false;
	}
	return true;
    }

    /** Remove the client from the history list (if registered).
     * @param nc The client to remove. */
    void removeClient(final NodeContainer nc) {
        m_clientNodes.remove(nc);
    }

    /** Add a client to the set of interested clients.
     * @param nc The client.
     */
    void addClient(final NodeContainer nc) {
        m_clientNodes.put(nc, null);
    }

    private static final String CFG_NAME = "name";
    private static final String CFG_LOGIN = "login";

    /** Saves name and login info to argument.
     * @param settings To save to.
     */
    void save(final NodeSettingsWO settings) {
        settings.addString(CFG_NAME, getName());
        settings.addString(CFG_LOGIN, getLogin());
    }

    /** Load credentials from argument.
     * @param settings To load from.
     * @return a new credentials object
     * @throws InvalidSettingsException If that fails for any reason.
     */
    static Credentials load(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        String name = settings.getString(CFG_NAME);
        String login = settings.getString(CFG_LOGIN);
        try {
            Credentials result = new Credentials(name);
            result.setLogin(login);
            return result;
        } catch (Exception e) {
            throw new InvalidSettingsException("Can't create credentials for "
                    + "name \"" + name + "\": " + e.getMessage(), e);
        }
    }

}
