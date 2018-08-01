/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Sep 15, 2018 (loki): created
 */
package org.knime.workbench.editor2;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.core.LayoutExemptingLayout;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.knime.workbench.editor2.figures.WorkflowFigure;

/**
 * Our subclass of <code>ScrollingGraphicalViewer</code> which facilitates the pinning of info, warning and error
 * messages to the top of the viewport, regardless of where the user scrolls to on the canvas, or how the user resizes
 * the canvas' editor pane.
 *
 * @author loki der quaeler
 */
public class ViewportPinningGraphicalViewer extends ScrollingGraphicalViewer {
    private static final int MESSAGE_BACKGROUND_OPACITY = 171;
    private static final Color WARN_ERROR_MESSAGE_BACKGROUND = new Color(null, 255, 249, 0, MESSAGE_BACKGROUND_OPACITY);
    private static final Color INFO_MESSAGE_BACKGROUND = new Color(null, 200, 200, 255, MESSAGE_BACKGROUND_OPACITY);

    private static final int MESSAGE_INSET = 10;

    private static NodeLogger LOGGER = NodeLogger.getLogger(ViewportPinningGraphicalViewer.class);

    private enum MessageAttributes {

        INFO(0, INFO_MESSAGE_BACKGROUND, SharedImages.Info),
        WARNING(1, WARN_ERROR_MESSAGE_BACKGROUND, SharedImages.Warning),
        ERROR(2, WARN_ERROR_MESSAGE_BACKGROUND, SharedImages.Error);

        private final int m_index;
        private final Color m_fillColor;
        private final SharedImages m_icon;

        MessageAttributes(final int index, final Color c, final SharedImages icon) {
            m_index = index;
            m_fillColor = c;
            m_icon = icon;
        }

        /**
         * @return the internal arrays' index for the message type
         */
        public int getIndex() {
            return m_index;
        }

        /**
         * @return the fill color associated with this message type
         */
        public Color getFillColor() {
            return m_fillColor;
        }

        /**
         * @return the icon associated with this message type
         */
        public SharedImages getIcon() {
            return m_icon;
        }
    }


    private final AtomicBoolean m_haveInitializedViewport = new AtomicBoolean(false);
    private final AtomicInteger m_currentMessageViewHeight = new AtomicInteger(0);

    /* Message figures indexed per MessageIndex */
    private final Label[] m_messages = new Label[MessageAttributes.values().length];

    /* Background rectangles for the message figures; indexed per MessageIndex */
    private final Composite[] m_fillRectangles = new Composite[MessageAttributes.values().length];

    private Composite m_parent;

    /**
     * Sets an info message (with an info icon and light purple background) at the top of the editor (above an error
     * message and above a warning message, if either or both exist.)
     *
     * @param msg the message to display or <code>null</code> to remove it
     */
    public void setInfoMessage(final String msg) {
        Display.getDefault().asyncExec(() -> {
            if (msg != null) {
                setMessage(msg, MessageAttributes.INFO);
            } else {
                removeMessageFromView(MessageAttributes.INFO);
            }
        });
    }

    /**
     * Sets a warning message displayed at the top of the editor (above an error message if there is any, and below an
     * info message if there is any.)
     *
     * @param msg the message to display or <code>null</code> to remove it
     */
    public void setWarningMessage(final String msg) {
        Display.getDefault().asyncExec(() -> {
            if (msg != null) {
                setMessage(msg, MessageAttributes.WARNING);
            } else {
                removeMessageFromView(MessageAttributes.WARNING);
            }
        });
    }

    /**
     * Sets an error message displayed at the top of the editor (underneath a warning message and underneath an info
     * message, if either or both exist.)
     *
     * @param msg the message to display or <code>null</code> to remove it
     */
    public void setErrorMessage(final String msg) {
        Display.getDefault().asyncExec(() -> {
            if (msg != null) {
                setMessage(msg, MessageAttributes.ERROR);
            } else {
                removeMessageFromView(MessageAttributes.ERROR);
            }
        });
    }

    /**
     * A less computationally / redraw intensive method to clear messages than call each set-message-type with null.
     */
    public void clearAllMessages() {
        Display.getDefault().asyncExec(() -> {
            for (int i = 0; i < m_messages.length; i++) {
                removeMessageFromView(i, false);
            }

            m_currentMessageViewHeight.set(0);
            updateTopWhitespaceBuffer();

            repaint();
        });
    }

    /**
     * @see org.eclipse.gef.EditPartViewer#setControl(Control)
     */
    @Override
    public void setControl(final Control control) {
        if (control != null) {
            m_parent = control.getParent();
            m_parent.setLayout(new LayoutExemptingLayout());
        }

        super.setControl(control);
    }

    private void removeMessageFromView(final MessageAttributes attributes) {
        removeMessageFromView(attributes.getIndex(), true);

        layoutMessages(true);
    }

    private void removeMessageFromView(final int index, final boolean triggerRepaint) {
        if (m_messages[index] != null) {
            m_messages[index] = null;

            m_fillRectangles[index].dispose();
            m_fillRectangles[index] = null;

            if (triggerRepaint) {
                repaint();
            }
        }
    }

    private void repaint() {
        getFigureCanvas().redraw();
    }

    private void updateTopWhitespaceBuffer() {
        final int yOffset = m_currentMessageViewHeight.get();
        final WorkflowFigure workflowFigure = ((WorkflowRootEditPart)getRootEditPart().getContents()).getFigure();
        workflowFigure.placeTentStakeToAllowForWhitespaceBuffer(yOffset);

        final FigureCanvas fc = getFigureCanvas();
        if (fc.getViewport().getViewLocation().y == 0) {
            // If the view is already sitting at the 0-height position, then scroll the view back to
            //      tent-stake so that the messages are not covering any of the canvas elements.
            // We asyncExec again to give *something* a pause, for invoking this immediately rarely seems to work :-/
            Display.getDefault().asyncExec(() -> {
                fc.scrollTo(0, -yOffset);
            });
        }
    }

    private void setMessage(final String msg, final MessageAttributes attributes) {
        assert(msg != null);

        final int index = attributes.getIndex();

        if ((m_messages[index] != null) && msg.equals(m_messages[index].getText())) {
            //nothing has changed
            return;
        }

        final Composite messageRectangle = new Composite(m_parent, SWT.NONE);
        messageRectangle.setBackground(attributes.getFillColor());
        messageRectangle.addPaintListener(new MessagePainter(index));
        messageRectangle.setVisible(false);
        messageRectangle.moveBelow(null);
        LayoutExemptingLayout.exemptControlFromLayout(messageRectangle);
        m_fillRectangles[index] = messageRectangle;

        final Label message = new Label(msg);
        message.setOpaque(false);
        message.setIcon(ImageRepository.getUnscaledIconImage(attributes.getIcon()));
        message.setLabelAlignment(PositionConstants.LEFT);
        m_messages[index] = message;

        layoutMessages(true);
    }

    private void layoutMessages(final boolean requireTopWhitespaceReplacement) {
        final Viewport v = getViewport();

        if (v != null) {
            final Rectangle bounds = v.getBounds();
            int yOffset = 0;

            for (int i = 0; i < m_messages.length; i++) {
                if (m_messages[i] != null) {
                    final Dimension preferredMessageSize = m_messages[i].getPreferredSize();
                    final Rectangle messageBounds = new Rectangle(MESSAGE_INSET, (yOffset + MESSAGE_INSET),
                        (bounds.width - (2 * MESSAGE_INSET)), preferredMessageSize.height);
                    final int rectangleHeight = (messageBounds.height + (2 * MESSAGE_INSET));

                    m_messages[i].setBounds(messageBounds);
                    m_fillRectangles[i].setLocation(0, yOffset);
                    m_fillRectangles[i].setSize(bounds.width, rectangleHeight);
                    m_fillRectangles[i].moveAbove(null);
                    m_fillRectangles[i].setVisible(true);

                    yOffset += rectangleHeight;
                }
            }

            m_currentMessageViewHeight.set(yOffset);

            if (requireTopWhitespaceReplacement) {
                updateTopWhitespaceBuffer();
            }
        } else {
            LOGGER.warn("Could not get viewport to layout messages.");
        }
    }

    private Viewport getViewport() {
        final FigureCanvas fc = getFigureCanvas();

        if (fc != null) {
            final Viewport v = fc.getViewport();

            if (v != null) {
                if (!m_haveInitializedViewport.getAndSet(true)) {
                    v.addFigureListener((figure) -> {
                        // this is invoked when the size of the viewport changes
                        layoutMessages(false);
                    });
                }

                return fc.getViewport();
            }
        } else {
            LOGGER.error("Could not get viewer's figure canvas.");
        }

        return null;
    }


    private class MessagePainter implements PaintListener {
        private final int m_arrayIndex;

        private MessagePainter(final int index) {
            m_arrayIndex = index;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void paintControl(final PaintEvent pe) {
            final Point location = m_fillRectangles[m_arrayIndex].getLocation();

            final GC gc = pe.gc;
            gc.setAdvanced(true);
            gc.setAntialias(SWT.ON);
            gc.setTextAntialias(SWT.ON);

            final SWTGraphics g = new SWTGraphics(gc);

            try {
                g.translate(0, -location.y);

                m_messages[m_arrayIndex].paint(g);
            } finally {
                g.dispose();
            }
        }
    }
}
