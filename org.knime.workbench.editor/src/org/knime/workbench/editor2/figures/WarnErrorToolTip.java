/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
            m_tooltip.setIcon(ImageRepository.getImage("icons/error.png"));
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
