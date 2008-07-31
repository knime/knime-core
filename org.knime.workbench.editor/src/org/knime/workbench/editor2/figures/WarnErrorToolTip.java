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
 *   16.03.2005 georg : renewed
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.jface.resource.JFaceResources;

import org.knime.workbench.editor2.ImageRepository;

/**
 * Figure for displaying tool tips on warning/error signs.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class WarnErrorToolTip extends Figure {
    /**
     * The constant to define a warning tool tip.
     */
    public static final int WARNING = 1;

    /**
     * The constant to define an error tool tip.
     */
    public static final int ERROR = 2;

    private static final Border TOOL_TIP_BORDER = new MarginBorder(0, 2, 0, 2);

    private String m_text;

    private Label m_tooltip;

    /**
     * Creates a new ToolTip.
     * 
     * @param text The text to display
     * @param type The type of the tool tip to create (warning/error)
     */
    public WarnErrorToolTip(final String text, final int type) {

        this.setLayoutManager(new ToolbarLayout(true));

        m_tooltip = new Label("???");

        if (type == WARNING) {
            m_tooltip.setIcon(ImageRepository.getImage("icons/warning.gif"));
        } else {
            m_tooltip.setIcon(ImageRepository.getImage("icons/error.jpg"));
        }
        m_tooltip.setBorder(TOOL_TIP_BORDER);
        m_tooltip.setFont(JFaceResources.getDefaultFont());
        this.add(m_tooltip);

        this.setText(text);

    }

    /**
     * Sets the text to be shown as a tooltip.
     * 
     * @param text The text to show
     */
    public void setText(final String text) {
        m_text = text;

        m_tooltip.setText(m_text);
        m_tooltip.setSize(m_tooltip.getPreferredSize().expand(10, 10));
        this.setSize(m_tooltip.getSize().expand(5, 7));
    }
}
