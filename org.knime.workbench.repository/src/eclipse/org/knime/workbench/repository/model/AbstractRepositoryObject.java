/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *   16.03.2005 (georg): created
 */
package org.knime.workbench.repository.model;

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

    private boolean m_isExpertNode;


    protected AbstractRepositoryObject(final String id, final String name) {
        m_id = id;
        m_name = name;
    }

    protected AbstractRepositoryObject(final AbstractRepositoryObject copy) {
        this.m_parent = copy.m_parent;
        this.m_name = copy.m_name;
        this.m_id = copy.m_id;
        this.m_afterID = copy.m_afterID;
        this.m_isExpertNode = copy.m_isExpertNode;
    }

    /**
     * Default implementation, provides no adapters.
     * {@inheritDoc}
     */
    @Override
    public Object getAdapter(final Class adapter) {
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
     * Moves this object to another parent.
     * {@inheritDoc}
     */
    @Override
    public void move(final IContainerObject newParent) {
        this.getParent().removeChild(this);
        newParent.addChild(this);
    }

    /**
     * internal, sets parent to null.
     *
     */
    protected void detach() {
        m_parent = null;
    }

    /**
     * @return Returns the id.
     */
    @Override
    public String getID() {
        return m_id;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return m_name;
    }

    public void setName(final String newName) {
        m_name = newName;
    }

    /**
     * @return Returns the afterID.
     */
    public String getAfterID() {
        return m_afterID;
    }

    /**
     * @param id the id to set
     */
    public void setAfterID(final String id) {
        m_afterID = id;
    }

    /**
     * @return the isExpertNode
     */
    public boolean isExpertNode() {
        return m_isExpertNode;
    }

    /**
     * @param isExpertNode the isExpertNode to set
     */
    public void setExpertNode(final boolean isExpertNode) {
        m_isExpertNode = isExpertNode;
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
}
