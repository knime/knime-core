/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   Jan 26, 2006 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota.view;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.JToolTip;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolTipUI;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
class MultiLineToolTipUI extends BasicToolTipUI {
    private static MultiLineToolTipUI sharedInstance = new MultiLineToolTipUI();

    private CellRendererPane m_rendererPane;

    private static JTextArea textArea;

    /**
     * @see javax.swing.plaf.ComponentUI#createUI(javax.swing.JComponent)
     */
    public static ComponentUI createUI(final JComponent c) {
        return sharedInstance;
    }

    /**
     * Creates new instance of MultiLineToolTipUI.
     */
    public MultiLineToolTipUI() {
        super();
    }

    /**
     * @see javax.swing.plaf.ComponentUI#installUI(javax.swing.JComponent)
     */
    @Override
    public void installUI(final JComponent c) {
        super.installUI(c);
        m_rendererPane = new CellRendererPane();
        c.add(m_rendererPane);
    }

    /**
     * @see javax.swing.plaf.ComponentUI#uninstallUI(javax.swing.JComponent)
     */
    @Override
    public void uninstallUI(final JComponent c) {
        super.uninstallUI(c);
        c.remove(m_rendererPane);
        m_rendererPane = null;
    }

    /**
     * @see javax.swing.plaf.ComponentUI#paint(Graphics, JComponent)
     */
    @Override
    public void paint(final Graphics g, final JComponent c) {
        Dimension size = c.getSize();
        textArea.setBackground(c.getBackground());
        m_rendererPane.paintComponent(g, textArea, c, 1, 1, size.width - 1,
                size.height - 1, true);
    }

    /**
     * @see javax.swing.plaf.ComponentUI#getPreferredSize(JComponent)
     */
    @Override
    public Dimension getPreferredSize(final JComponent c) {
        String tipText = ((JToolTip)c).getTipText();
        if (tipText == null) {
            return new Dimension(0, 0);
        }
        textArea = new JTextArea(tipText);
        m_rendererPane.removeAll();
        m_rendererPane.add(textArea);
        textArea.setWrapStyleWord(true);
        int width = ((JMultiLineToolTip)c).getFixedWidth();
        int columns = ((JMultiLineToolTip)c).getColumns();

        if (columns > 0) {
            textArea.setColumns(columns);
            textArea.setSize(0, 0);
            textArea.setLineWrap(true);
            textArea.setSize(textArea.getPreferredSize());
        } else if (width > 0) {
            textArea.setLineWrap(true);
            Dimension d = textArea.getPreferredSize();
            d.width = width;
            d.height++;
            textArea.setSize(d);
        } else {
            textArea.setLineWrap(false);
        }

        Dimension dim = textArea.getPreferredSize();

        dim.height += 1;
        dim.width += 1;
        return dim;
    }

    /**
     * @see javax.swing.plaf.ComponentUI#getMinimumSize(javax.swing.JComponent)
     */
    @Override
    public Dimension getMinimumSize(final JComponent c) {
        return getPreferredSize(c);
    }

    /**
     * @see javax.swing.plaf.ComponentUI#getMaximumSize(javax.swing.JComponent)
     */
    @Override
    public Dimension getMaximumSize(final JComponent c) {
        return getPreferredSize(c);
    }
}
