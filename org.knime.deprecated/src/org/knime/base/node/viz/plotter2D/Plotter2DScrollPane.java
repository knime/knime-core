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
 *   01.02.2006 (sieb): created
 */
package org.knime.base.node.viz.plotter2D;

import java.awt.Component;
import java.awt.LayoutManager;

import javax.swing.JScrollPane;
import javax.swing.ScrollPaneLayout;

/**
 * Overrides the default <code>JScrollPane</code> to force the application of
 * the own <code>Plotter2DScrollPaneLayout</code> overriding the default
 * <code>ScrollPaneLayout</code>.
 * 
 * @see org.knime.base.node.viz.plotter2D.Plotter2DScrollPaneLayout
 * @author Christoph Sieb, University of Konstanz
 */
public class Plotter2DScrollPane extends JScrollPane {

    /**
     * The constructor for a scatter plotter scroll pane.
     * 
     * @param view the underlying view of the scroll pane. In case of the
     *            scatterplotter this is the drawing pane rendering the dots
     */
    public Plotter2DScrollPane(final Component view) {
        super(view);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLayout(final LayoutManager layout) {

        LayoutManager managerToSet = null;
        if (!(layout instanceof Plotter2DScrollPaneLayout)) {
            managerToSet = new Plotter2DScrollPaneLayout();
        } else {
            managerToSet = layout;
        }

        if (managerToSet instanceof ScrollPaneLayout) {
            super.setLayout(managerToSet);
            ((ScrollPaneLayout)managerToSet).syncWithScrollPane(this);
        } else if (managerToSet == null) {
            super.setLayout(managerToSet);
        } else {
            String s = "layout of JScrollPane must be a ScrollPaneLayout";
            throw new ClassCastException(s);
        }
    }

}
