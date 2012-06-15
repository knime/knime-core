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

import org.eclipse.swt.graphics.Image;

/**
 * Abstract base class of "leaf" objects (that is, objects without children).
 *
 * @author Florian Georg, University of Konstanz
 */
public abstract class AbstractNodeTemplate extends AbstractRepositoryObject
        implements ISimpleObject {

    private Image m_icon;

    private String m_categoryPath;

    private String m_pluginID;

    protected AbstractNodeTemplate(final String id, final String name) {
        super(id, name);
    }

    protected AbstractNodeTemplate(final AbstractNodeTemplate copy) {
        super(copy);
        this.m_icon = copy.m_icon;
        this.m_categoryPath = copy.m_categoryPath;
        this.m_pluginID = copy.m_pluginID;
    }


    /**
     * @return Returns the pluginID.
     */
    public String getPluginID() {
        return m_pluginID;
    }

    /**
     * @param pluginID The pluginID to set.
     */
    public void setPluginID(final String pluginID) {
        m_pluginID = pluginID;
    }

    /**
     * @return Returns the icon.
     */
    public Image getIcon() {
        return m_icon;
    }

    /**
     * @param icon The icon to set.
     */
    public void setIcon(final Image icon) {
        m_icon = icon;
    }

    /**
     * @return Returns the categoryPath.
     */
    public String getCategoryPath() {
        return m_categoryPath;
    }

    /**
     * @param categoryPath The categoryPath to set.
     */
    public void setCategoryPath(final String categoryPath) {
        m_categoryPath = categoryPath;
    }
}
