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
 *   09.02.2005 (georg): created
 */
package org.knime.workbench.ui.wrapper;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Panel;

import javax.swing.JPanel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * Wrapper Composite that uses the SWT_AWT bridge to embed an AWT Panel into a
 * SWT composite.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class Panel2CompositeWrapper extends Composite {
    private Frame m_awtFrame;

    private JPanel m_awtPanel;

    /**
     * Creates a new wrapper.
     * 
     * @param parent The parent composite
     * @param panel The AWT panel to wrap
     * @param style Style bits, ignored so far
     */
    public Panel2CompositeWrapper(final Composite parent,
            final JPanel panel, final int style) {
        super(parent, style | SWT.EMBEDDED);
        final GridLayout gridLayout = new GridLayout();
        gridLayout.verticalSpacing = 0;
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        gridLayout.horizontalSpacing = 0;
        setLayout(gridLayout);

        m_awtFrame = SWT_AWT.new_Frame(this);
        /* Use another panel to enable cursor switching (for instance
         * in an embedded JSplitPane where the mouse cursor changes when you
         * adjust the pane sizes. This is the workaround for bug #594 as
         * suggested by 
         * https://bugs.eclipse.org/bugs/show_bug.cgi?id=58308
         */
        Panel wrap = new Panel(new BorderLayout());
        // use panel as root
        m_awtPanel = panel;
        wrap.add(m_awtPanel);
        m_awtFrame.add(wrap);
        
        // Pack the frame
        m_awtFrame.pack();
        m_awtFrame.setVisible(true);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        super.dispose();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkSubclass() {
    }

    /**
     * @return The wrapped panel, as it can be used within legacy AWT/Swing code
     */
    public JPanel getAwtPanel() {
        return m_awtPanel;
    }
}
