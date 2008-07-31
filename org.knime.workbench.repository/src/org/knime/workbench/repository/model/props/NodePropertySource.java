/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
package org.knime.workbench.repository.model.props;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import org.knime.workbench.repository.model.NodeTemplate;

/**
 * Property Source for a "NodeTemplate" object.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NodePropertySource implements IPropertySource {
    private static final PropertyDescriptor NAME_DESC = new PropertyDescriptor(
            "name", "Name");

    private static final PropertyDescriptor DESC_DESC = new PropertyDescriptor(
            "description", "Description");

    private static final PropertyDescriptor ID_DESC = new PropertyDescriptor(
            "id", "ID");

    private static final PropertyDescriptor TYPE_DESC = new PropertyDescriptor(
            "type", "Type");

    private static final PropertyDescriptor FACT_DESC = new PropertyDescriptor(
            "factory", "Factory class");

    private static final PropertyDescriptor ICON_DESC = new PropertyDescriptor(
            "icon.small", "Small icon");


    private static final PropertyDescriptor CAT_DESC = new PropertyDescriptor(
            "category", "Category path");

    private static final String CATEGORY_INFO = "Info";

    private static final String CATEGORY_ICONS = "Icons";

    private static final String CATEGORY_PATH = "Path";

    private NodeTemplate m_node;

    static {
        NAME_DESC.setCategory(CATEGORY_INFO);
        DESC_DESC.setCategory(CATEGORY_INFO);
        FACT_DESC.setCategory(CATEGORY_INFO);
        TYPE_DESC.setCategory(CATEGORY_INFO);

        ICON_DESC.setCategory(CATEGORY_ICONS);
        ID_DESC.setCategory(CATEGORY_PATH);
    }

    /**
     * Constructs a new property source for the.
     * 
     * @param model The node that is wrapped by this property source
     */
    public NodePropertySource(final NodeTemplate model) {
        m_node = model;
    }

    /**
     * {@inheritDoc}
     */
    public Object getEditableValue() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public IPropertyDescriptor[] getPropertyDescriptors() {
        return new IPropertyDescriptor[] {ID_DESC, NAME_DESC, TYPE_DESC,
                DESC_DESC, FACT_DESC, CAT_DESC, ICON_DESC};
    }

    /**
     * {@inheritDoc}
     */
    public Object getPropertyValue(final Object id) {
        if ("name".equals(id)) {
            return m_node.getName();
        }
        if ("id".equals(id)) {
            return m_node.getID();
        }
        if ("type".equals(id)) {
            return m_node.getType();
        }
        if ("factory".equals(id)) {
            return m_node.getFactory();
        }
        if ("category".equals(id)) {
            return m_node.getCategoryPath();
        }

        // this should not happen
        assert false : "Unknown property id: " + id;
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPropertySet(final Object id) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void resetPropertyValue(final Object id) {

    }

    /**
     * {@inheritDoc}
     */
    public void setPropertyValue(final Object id, final Object value) {
        // TODO Auto-generated method stub
    }
}
