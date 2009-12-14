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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
