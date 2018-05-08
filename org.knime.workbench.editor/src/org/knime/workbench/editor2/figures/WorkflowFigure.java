/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   29.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.FreeformLayeredPane;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;

/**
 * The root figure, containing all diagram elements inside the workflow.
 *
 * TODO a grid in the background (?GridLayer - where?)
 *
 * @author Florian Georg, University of Konstanz
 */
public class WorkflowFigure extends FreeformLayeredPane {

    private static final Color MSG_BG = new Color(null, 255, 249, 0);

    private ProgressToolTipHelper m_progressToolTipHelper;

    private Image m_jobManagerFigure;

    /* Error (index 0) and warning (index 1) messages. */
    private Label[] m_messages = new Label[2];

    /* Rectangle underneath the message figure */
    private RectangleFigure[] m_messageRects = new RectangleFigure[2];

    /**
     * New workflow root figure.
     */
    public WorkflowFigure() {
        // not opaque, so that we can directly select on the "background" layer
        this.setOpaque(false);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void paint(final Graphics graphics) {
        super.paint(graphics);
        paintChildren(graphics);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void paintFigure(final Graphics graphics) {
        super.paintFigure(graphics);
        if (m_jobManagerFigure != null) {
            org.eclipse.swt.graphics.Rectangle imgBox = m_jobManagerFigure.getBounds();
            Rectangle bounds2 = getBounds();
            graphics.drawImage(m_jobManagerFigure, 0, 0, imgBox.width,
                               imgBox.height, bounds2.width - imgBox.width, 5, imgBox.width, imgBox.height + 5);
        }
        int y_offset = 0;
        int y_0 = 10;
        for (int i = 0; i < m_messages.length; i++) {
            if (m_messages[i] != null) {
                Rectangle b = m_messages[i].getBounds();
                b.setY(y_0 + y_offset);
                Rectangle r = new Rectangle(0, y_offset, getBounds().width, m_messages[i].getBounds().height + 20);
                //setConstraint(m_messageRects[i], r);
                m_messageRects[i].getBounds().setBounds(r);//setting the bounds without repainting it
                y_offset += b.y + b.height + 10;
            }
        }
    }

    /**
     * Sets a warning message displayed at the top of the editor (above an error message if there is any).
     *
     *
     * @param msg the message to display or <code>null</code> to remove it
     */
    public void setWarningMessage(final String msg) {
        setMessage(msg, 0, SharedImages.Warning);
    }

    /**
     * Sets an error message displayed at the top of the editor (underneath a warning message if there is any).
     *
     * @param msg the message to display or <code>null</code> to remove it
     */
    public void setErrorMessage(final String msg) {
        setMessage(msg, 1, SharedImages.Error);
    }

    private void setMessage(final String msg, final int index, final SharedImages icon) {
        if ((msg == null && m_messages[index] == null)
            || (msg != null && m_messages[index] != null && msg.equals(m_messages[index].getText()))) {
            //nothing has changed
            return;
        }

        if (m_messages[index] != null) {
            remove(m_messages[index]);
            remove(m_messageRects[index]);
            m_messages[index] = null;
            m_messageRects[index] = null;
        }

        if (msg != null) {
            m_messageRects[index] = new RectangleFigure();
            m_messageRects[index].setOpaque(true);
            m_messageRects[index].setBackgroundColor(MSG_BG);
            m_messageRects[index].setForegroundColor(MSG_BG);
            add(m_messageRects[index]);

            m_messages[index] = new Label(msg);
            m_messages[index].setOpaque(true);
            m_messages[index].setBackgroundColor(MSG_BG);
            m_messages[index].setIcon(ImageRepository.getUnscaledIconImage(icon));
            Rectangle msgBounds = new Rectangle(m_messages[index].getBounds());
            msgBounds.x += 10;
            msgBounds.y += 10;
            m_messages[index].setBounds(msgBounds);
            add(m_messages[index], new Rectangle(msgBounds.x, msgBounds.y, getBounds().width - 20, 120));
        }
        repaint();
    }
    /**
     * @param jobManagerFigure the jobManagerFigure to set
     */
    public void setJobManagerFigure(final Image jobManagerFigure) {
        m_jobManagerFigure = jobManagerFigure;
        repaint();
    }

    public ProgressToolTipHelper getProgressToolTipHelper() {
        return m_progressToolTipHelper;
    }


    public void setProgressToolTipHelper(final ProgressToolTipHelper progressToolTipHelper) {
        m_progressToolTipHelper = progressToolTipHelper;
    }


}
