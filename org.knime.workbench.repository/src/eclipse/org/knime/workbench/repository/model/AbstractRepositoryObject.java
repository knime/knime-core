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
 * -------------------------------------------------------------------
 *
 * History
 *   16.03.2005 (georg): created
 */
package org.knime.workbench.repository.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base implementation of a generic repository object.
 *
 * @author Florian Georg, University of Konstanz
 */
public abstract class AbstractRepositoryObject implements IRepositoryObject,
        Comparable<AbstractRepositoryObject> {
    private IContainerObject m_parent;

    private String m_name;

    private final String m_id;

    private String m_afterID = "";

    private String m_contributingPlugin;

    private Map<String, String> m_additionalInfo;


    /**
     * Creates a new abstract repository object.
     *
     * @param id the (unique) ID, must not be <code>null</code>
     * @param name the name, must not be <code>null</code>
     * @param contributingPlugin the contributing plug-in's ID, must not be <code>null</code>
     */
    protected AbstractRepositoryObject(final String id, final String name, final String contributingPlugin) {
        if (name == null) {
            throw new IllegalArgumentException("Name must not be null");
        }
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
        if (contributingPlugin == null) {
            throw new IllegalArgumentException("Contributing plug-in must not be null");
        }

        m_id = id;
        m_name = name;
        m_contributingPlugin = contributingPlugin;
        m_additionalInfo = new HashMap<String, String>();
    }

    /**
     * Creates a copy of the given repository object.
     *
     * @param copy the object to copy
     */
    protected AbstractRepositoryObject(final AbstractRepositoryObject copy) {
        this.m_parent = copy.m_parent;
        this.m_name = copy.m_name;
        this.m_id = copy.m_id;
        this.m_afterID = copy.m_afterID;
        this.m_contributingPlugin = copy.m_contributingPlugin;
        this.m_additionalInfo = copy.m_additionalInfo;
    }

    /**
     * Default implementation, provides no adapters.
     * {@inheritDoc}
     */
    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") final Class adapter) {
        return null;
    }

    /**
     * Sets the parent. Make sure to remove child references on old parent as
     * well !
     *
     * @param parent The parent
     */
    public void setParent(final IContainerObject parent) {
        if (parent == this) {
            throw new IllegalArgumentException("can't set parent to 'this'");
        }
        if (parent == null) {
            throw new IllegalArgumentException("can't set parent to 'null'");
        }

        m_parent = parent;
    }

    /**
     * @return returns the parent object
     * {@inheritDoc}
     */
    @Override
    public IContainerObject getParent() {
        return m_parent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(final IContainerObject newParent) {
        this.getParent().removeChild(this);
        newParent.addChild(this);
    }

    /**
     * Internal, sets parent to <code>null</code>.
     */
    protected void detach() {
        m_parent = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getID() {
        return m_id;
    }

    /**
     * Returns the human-readable name for this object.
     *
     * @return the name
     */
    @Override
    public String getName() {
        return m_name;
    }

    /**
     * Sets a new name for this object.
     *
     * @param newName the new name, must not be <code>null</code>
     */
    public void setName(final String newName) {
        if (newName == null) {
            throw new IllegalArgumentException("Name must not be null");
        }

        if (m_parent != null) {
            for (IRepositoryObject o : m_parent.getChildren()) {
                if (!(o == this) && (o instanceof AbstractRepositoryObject)
                        && newName.equals(((AbstractRepositoryObject)o).getName())) {
                    throw new IllegalArgumentException("A sibling with name '" + newName + "' already exists");
                }
            }
        }
        m_name = newName;
    }

    /**
     * Returns the ID of the object after which this object should occur.
     *
     * @return the after-ID
     */
    public String getAfterID() {
        return m_afterID;
    }

    /**
     * Sets the ID of the object after which this object should occur.
     *
     * @param id the id, should not be <code>null</code>
     */
    public void setAfterID(final String id) {
        m_afterID = id;
    }

    /**
     * Adds additional infos to the repo object, that is, e.g., additionally displayed in the node repository.
     *
     * @param info
     *
     * @since 3.1
     */
    public void addAdditionalInfo(final String key,final String info) {
        m_additionalInfo.put(key, info);
    }

    /**
     *
     * @param key
     * @return the info stored under the given key, returns <code>null</code> if there is no info for the given key
     *
     * @since 3.1
     */
    public String getAdditionalInfo(final String key) {
        return m_additionalInfo.get(key);
    }

    /**
     * Compares two repository objects lexicographically according to their
     * name.
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final AbstractRepositoryObject o) {
        return m_name.compareTo(o.m_name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_name == null) ? 0 : m_name.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AbstractRepositoryObject other = (AbstractRepositoryObject)obj;
        if (m_name == null) {
            if (other.m_name != null) {
                return false;
            }
        } else if (!m_name.equals(other.m_name)) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContributingPlugin() {
        return m_contributingPlugin;
    }
}
