/*
 * ------------------------------------------------------------------
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
 *   07.07.2005 (ohl): created
 */
package org.knime.base.node.preproc.filter.row;

import java.awt.Component;
import java.awt.Container;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.knime.base.node.preproc.filter.row.rowfilter.RowFilter;
import org.knime.core.node.InvalidSettingsException;

/**
 * They provide a en/disable method which affects all added components. They add
 * horizontal and vertical struts so that the size of the panel doesn't change,
 * even if everything is disabled.
 * 
 * @author Peter Ohl, University of Konstanz
 */
public abstract class RowFilterPanel extends JPanel {

    private JPanel m_panel;

    /**
     * A rowfilter panel.
     * 
     * @param width minimum width of the panel
     * @param height minimum height of the panel
     */
    public RowFilterPanel(final int width, final int height) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        m_panel = new JPanel();

        super.add(Box.createHorizontalStrut(width));

        Box panelBox = Box.createVerticalBox();
        panelBox.add(m_panel);
        panelBox.add(Box.createVerticalGlue());

        Box inner = Box.createHorizontalBox();
        inner.add(Box.createVerticalStrut(height));
        inner.add(m_panel);
        inner.add(Box.createHorizontalGlue());

        super.add(inner);

        Box inVisText = Box.createHorizontalBox();
        inVisText.add(Box.createHorizontalGlue());
        inVisText.add(Box.createHorizontalGlue());
        super.add(inVisText);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component add(final Component comp) {
        return m_panel.add(comp);
    }

    /**
     * Makes all components contained in the panel in/visible, depending on the
     * value passed in.
     * 
     * @param visible if set <code>true</code> all components in the panel
     *            will be made visible, if <code>false</code> they will be
     *            invisible
     */
    public void setComponentsVisible(final boolean visible) {
        Component[] compos = m_panel.getComponents();
        for (int c = 0; c < compos.length; c++) {
            setVisibleRec(visible, compos[c]);
        }
    }

    /*
     * sets the passed component and all its children visible
     */
    private void setVisibleRec(final boolean visible, final Component comp) {
        comp.setVisible(visible);
        if (comp instanceof Container) {
            Component[] compos = ((Container)comp).getComponents();
            for (int c = 0; c < compos.length; c++) {
                setVisibleRec(visible, compos[c]);
            }
        }
    }

    /**
     * Adjusts the settings/values of its components to reflect the
     * settings/properties of the filter passed in.
     * 
     * @param filter containing specs for filter properties
     * @throws InvalidSettingsException if the filter passed is not the one
     *             represented by this panel
     */
    public abstract void loadSettingsFromFilter(final RowFilter filter)
            throws InvalidSettingsException;

    /**
     * @param include flag telling whether to create an include filter or one
     *            that excludes the specified rows.
     * @return a filter object from the current settings of the panel
     * @throws InvalidSettingsException if settings were invalid and no filter
     *             could be created.
     */
    public abstract RowFilter createFilter(final boolean include)
            throws InvalidSettingsException;
}
