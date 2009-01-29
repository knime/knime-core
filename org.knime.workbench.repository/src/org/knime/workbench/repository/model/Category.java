/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   16.03.2005 (georg): created
 */
package org.knime.workbench.repository.model;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.IPropertySource;

import org.knime.workbench.repository.model.props.CategoryPropertySource;

/**
 * Implementation of a repository category. (usually contributed by extension
 * point)
 * 
 * @author Florian Georg, University of Konstanz
 */
public class Category extends AbstractContainerObject {
    private String m_description;

    private String m_path;

    private Image m_icon;

    private ImageDescriptor m_iconDescriptor;
    
    private String m_pluginID;

    /**
     * Creates a new repository category with the given level-id.
     * 
     * @param id The id
     */
    public Category(final String id) {
        setID(id);
    }

    /**
     * @return Returns the description.
     */
    public String getDescription() {
        return m_description;
    }

    /**
     * @param description The description to set.
     */
    public void setDescription(final String description) {
        m_description = description;
    }
    
    /**
     * 
     * @param pluginID the id of the declaring plug-in.
     */
    public void setPluginID(final String pluginID) {
        m_pluginID = pluginID;
    }
    
    /**
     * 
     * @return the id of the declaring plugin
     */
    public String getPluginID() {
        return m_pluginID;
    }

    /**
     * @return Returns the path.
     */
    public String getPath() {
        return m_path;
    }

    /**
     * @param path The path to set.
     */
    public void setPath(final String path) {
        m_path = path;
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
     * {@inheritDoc}
     */
    @Override
    public Object getAdapter(final Class adapter) {
        if (adapter == IPropertySource.class) {
            return new CategoryPropertySource(this);
        }

        return super.getAdapter(adapter);
    }

    /**
     * Sets the image descriptor for the icon.
     * 
     * @param imageDescriptor The descriptor
     */
    public void setIconDescriptor(final ImageDescriptor imageDescriptor) {
        m_iconDescriptor = imageDescriptor;
    }

    /**
     * @return Returns the iconDescriptor.
     */
    public ImageDescriptor getIconDescriptor() {
        return m_iconDescriptor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Id: " + getID() + " Name: " + getName() + " After-id: "
                + getAfterID();
    }
}
