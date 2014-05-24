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
 */
package org.knime.workbench.repository.model;

import org.knime.core.node.workflow.WorkflowManager;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class MetaNodeTemplate extends AbstractNodeTemplate {

    private final WorkflowManager m_manager;

    private String m_description;

    /**
     * Creates a new metanode template.
     *
     * @param id the (unique) id of the node template
     * @param name the name
     * @param categoryPath the absolute path of the category in which this template should be placed
     * @param contributingPlugin the contributing plug-in's ID
     * @param manager the metanode's workflow manager
     */
    public MetaNodeTemplate(final String id, final String name,
            final String categoryPath, final String contributingPlugin, final WorkflowManager manager) {
        super(id, name, contributingPlugin);
        m_manager = manager;
        setAfterID("");
        setCategoryPath(categoryPath);
    }

    /**
     * Creates a copy of the given object.
     *
     * @param copy the object to copy
     */
    protected MetaNodeTemplate(final MetaNodeTemplate copy) {
        super(copy);
        this.m_manager = copy.m_manager;
        this.m_description = copy.m_description;
    }

    /**
     * Returns the metanode's workflow manager.
     *
     * @return a workflow manager
     */
    public WorkflowManager getManager() {
        return m_manager;
    }

    @Override
    public String getCategoryPath() {
        if (super.getCategoryPath() != null) {
            return super.getCategoryPath();
        }
        return "/meta";
    }

    /**
     * Returns a description for this metanode template.
     *
     * @return a description, never <code>null</code>
     */
    public String getDescription() {
        if (m_description != null) {
            return m_description;
        }
        return m_manager.getName() + ": " + (m_manager.getCustomDescription() != null ? m_manager
                .getCustomDescription() : "");
    }

    /**
     * Sets a description for this metanode template.
     *
     * @param description a description
     */
    public void setDescription(final String description) {
        /*
         * If we have a description in the extension but no custom description
         * in the meta node -> set description also as custom description If we
         * have a custom description -> add the description found in the
         * extension.
         */
        m_description = description;
        if ((m_manager != null) && (m_manager.getCustomDescription() == null)) {
            m_manager.setCustomDescription(m_description);
        } else if ((m_manager != null) && (m_manager.getCustomDescription() != null)) {
            m_manager.setCustomDescription(m_manager.getCustomDescription()
                    + " " + m_description);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IRepositoryObject deepCopy() {
        return new MetaNodeTemplate(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result =
                prime
                        * result
                        + ((m_description == null) ? 0 : m_description
                                .hashCode());
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
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MetaNodeTemplate other = (MetaNodeTemplate)obj;
        if (m_description == null) {
            if (other.m_description != null) {
                return false;
            }
        } else if (!m_description.equals(other.m_description)) {
            return false;
        }
        return true;
    }

    /**
    *
    * {@inheritDoc}
    */
   @Override
   public String toString() {
       return m_description;
   }
}
