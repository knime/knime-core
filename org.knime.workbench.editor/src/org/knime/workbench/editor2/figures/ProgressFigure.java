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
 * History
 *   31.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.figures;

import java.util.Vector;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.DelegatingLayout;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseMotionListener;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.workflow.NodeProgress;

/**
 * This figure creates the progress bar within a node container figure.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class ProgressFigure extends RectangleFigure implements
        MouseMotionListener {

    // private static final NodeLogger LOGGER =
    // NodeLogger.getLogger(ProgressFigure.class);

    /** absolute width of this figure. * */
    public static final int WIDTH = 32;

    /** absolute height of this figure. * */
    public static final int HEIGHT = 12;

    private static final int UNKNOW_PROGRESS_BAR_WIDTH = 10;

    private static final Font PROGRESS_FONT;

    private static final Font QUEUED_FONT;

    private static final Font QUEUED_FONT_SMALL;

    private static final Color PROGRESS_BAR_BACKGROUND_COLOR = new Color(null,
            220, 220, 220);

    private static final Color PROGRESS_BAR_COLOR = ColorConstants.darkBlue;

    private static final boolean ON = true;

    private static final boolean RENDER = true;

    private static final boolean INVOKE_DISPLAY = true;

    private static final UnknownProgressTimer unknownProgressTimer;

    // private static final Color PROGRESS_BAR_COLOR = new Color(null, 240, 200,
    // 10);

    static {
        Display current = Display.getCurrent();
        Font systemFont = current.getSystemFont();
        FontData[] systemFontData = systemFont.getFontData();
        String name = "Arial"; // fallback
        int height = 8;
        if (systemFontData.length >= 1) {
            name = systemFontData[0].getName();
            // height = systemFontData[0].getHeight();
        }
        PROGRESS_FONT = new Font(current, name, height, SWT.NORMAL);
        QUEUED_FONT = new Font(current, name, 7, SWT.NORMAL);
        QUEUED_FONT_SMALL = new Font(current, name, 6, SWT.NORMAL);

        // instantiate and start the unkown progress timer
        unknownProgressTimer = new UnknownProgressTimer();
        unknownProgressTimer.start();
    }

    private boolean m_unknownProgress = false;

    private int m_unknownProgressBarRenderingPosition;

    private int m_unknownProgressBarDirection;

    public static enum ProgressMode { EXECUTING, QUEUED, PAUSED };
    private ProgressMode m_progressMode;

    private int m_currentWorked;

    private String m_currentProgressMessage = "";

    private String m_stateMessage;

    private static Display m_currentDisplay;

    private MouseEvent m_mouseEvent;

    private ProgressToolTipHelper m_toolTipHelper;

    private final Runnable m_repaintObject = new Runnable() {
        public void run() {

            repaint();
        }
    };

    /**
     * Creates a new node figure.
     */
    public ProgressFigure() {

        setBounds(new Rectangle(0, 0, WIDTH, HEIGHT));

        if (m_currentDisplay != null) {
            m_currentDisplay = Display.getCurrent();
        }

        setOpaque(true);
        setFill(true);
        setOutline(true);

        m_unknownProgressBarRenderingPosition = 0;

        // no border
        // setBorder(SimpleEtchedBorder.singleton);

        // add sub-figures
        // ToolbarLayout layout = new ToolbarLayout(false);
        // layout.setSpacing(1);
        // layout.setStretchMinorAxis(true);
        // layout.setVertical(true);
        // delegating layout, children provide a Locator as constraint
        DelegatingLayout layout = new DelegatingLayout();
        setLayoutManager(layout);
        // FlowLayout layout = new FlowLayout(true);
        // layout.setMajorAlignment(FlowLayout.ALIGN_LEFTTOP);
        setOutline(false);

        addMouseMotionListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getMinimumSize(final int whint, final int hhint) {
        return getPreferredSize(whint, hhint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize(WIDTH, HEIGHT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getPreferredSize(final int wHint, final int hHint) {

        return new Dimension(WIDTH, HEIGHT);
    }

    /**
     * We need to set the color before invoking super.
     *
     * @see org.eclipse.draw2d.Shape#fillShape(org.eclipse.draw2d.Graphics)
     */
    @Override
    protected void fillShape(final Graphics graphics) {
        graphics.setBackgroundColor(getBackgroundColor());
        super.fillShape(graphics);
    }

    private void drawSmoothRect(final Graphics graphics, final int x, final int y, final int w,
            final int h) {

        graphics.drawLine(x + 1, y, x + w - 2, y);
        graphics.drawLine(x + 1, y + h - 1, x + w - 2, y + h - 1);
        graphics.drawLine(x, y + 1, x, y + h - 2);
        graphics.drawLine(x + w - 1, y + 1, x + w - 1, y + h - 2);
    }

    // private void fillSmoothRect(final Graphics graphics, int x, int y, int w,
    // int h) {
    // graphics.fillRectangle(x + 1, y, w - 2, h);
    // graphics.drawLine(x, y + 1, x, y + h - 2);
    // graphics.drawLine(x + w - 1, y + 1, x + w - 1, y + h - 2);
    //
    // }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintFigure(final Graphics graphics) {

        if (RENDER) {
            // super.paintFigure(graphics);

            // paint the specified bar length for the progress
            graphics.setForegroundColor(ColorConstants.black);
            graphics.setBackgroundColor(PROGRESS_BAR_BACKGROUND_COLOR);

            Rectangle r = getBounds();
            int x = r.x;
            int y = r.y;
            int w = r.width;
            int h = r.height;

            graphics.fillRectangle(0, 0, 1000, 200);

            graphics.fillRectangle(x + 1, y + 1, w - 2, h - 2);
            drawSmoothRect(graphics, x, y, w, h);

            graphics.setForegroundColor(PROGRESS_BAR_COLOR);
            graphics.setBackgroundColor(PROGRESS_BAR_COLOR);

            switch (m_progressMode) {
            case EXECUTING:
                if (!m_unknownProgress) {

                    // calculate the progress bar width from the percentage
                    // current worked value
                    int barWidth = (int) Math.round((WIDTH - 2)
                            / 100.0D * m_currentWorked);

                    graphics.fillRectangle(x + 1, y + 1, barWidth, h - 2);

                    // graphics.fillRectangle(x, y, barWidth, h);
                    graphics.setFont(PROGRESS_FONT);

                    // create the percentage string
                    String progressString = m_currentWorked + "%";

                    graphics.setXORMode(true);
                    graphics.setForegroundColor(ColorConstants.white);
                    graphics.drawString(progressString, x + w / 2
                            - (progressString.length() * 4), y - 1);
                } else {

                    graphics.setForegroundColor(ColorConstants.darkBlue);

                    // calculate the rendering direction
                    if (m_unknownProgressBarRenderingPosition
                            + UNKNOW_PROGRESS_BAR_WIDTH >= WIDTH - 2) {
                        m_unknownProgressBarDirection = -1;
                    } else if (m_unknownProgressBarRenderingPosition <= 0) {
                        m_unknownProgressBarDirection = 1;
                    }

                    graphics.fillRectangle(x + 1
                            + m_unknownProgressBarRenderingPosition, y + 1,
                            UNKNOW_PROGRESS_BAR_WIDTH, h - 2);

                    m_unknownProgressBarRenderingPosition += m_unknownProgressBarDirection;

                }
                break;
            case QUEUED:
            case PAUSED:
                // draw "Queued"/"Paused"
                String queuedString = m_progressMode.equals(ProgressMode.QUEUED) ? "queued" : "paused";
                graphics.setFont(QUEUED_FONT);
                Dimension dim = FigureUtilities.getStringExtents(queuedString,
                        QUEUED_FONT);

                // if the string is to big for the progress bar
                // reduce the font size
                if (dim.width > 30) {
                    graphics.setFont(QUEUED_FONT_SMALL);
                }
                graphics.drawString(queuedString, x + 1, y);
                break;
            }
        }
    }

    /**
     * Stops the rendering of an unknown progress.
     */
    public void stopUnknownProgress() {
        m_unknownProgress = false;
        unknownProgressTimer.removeFigure(this);
    }

    /**
     * Activates this progress bar to render an unknown progress.
     */
    public void activateUnknownProgress() {

        // if switched off, return
        if (!ON) {
            return;
        }

        // check if there is still a progress to render
        if (m_currentWorked >= 0) {
            m_unknownProgress = false;
            repaint();
            return;
        }

        m_unknownProgress = true;

        // reset the worked value
        m_currentWorked = -1;

        if (m_currentDisplay == null) {
            return;
        }

        unknownProgressTimer.addFigure(this);

        // new Thread("Unknown Progress Timer") {
        //
        // public void run() {
        //
        // while (m_unknownProgress) {
        //
        // try {
        // Thread.sleep(500);
        // } catch (Exception e) {
        // e.printStackTrace();
        // }
        //
        // if (INVOKE_DISPLAY) {
        // m_currentDisplay.syncExec(m_repaintObject);
        // }
        // }
        // }
        //
        // }.start();
    }

    /**
     * Sets the mode of this progress bar.
     *
     * @param executing
     */
    public void setProgressMode(final ProgressMode ps) {
        m_progressMode = ps;
    }

    /**
     * Get the mode of this progress bar.
     *
     * @return progress mode.
     */
    public ProgressMode getProgressMode() {
        return m_progressMode;
    }

    /**
     * Updates UI after progress has changed.
     * @param pe the new progress to display
     */
    public synchronized void progressChanged(final NodeProgress pe) {

        if (!ON) {
            return;
        }

        int newWorked = m_currentWorked;
        if (pe.hasProgress()) {
            double progress = pe.getProgress().doubleValue();
            newWorked = (int) Math.round(Math.max(0, Math.min(progress * 100,
                    100)));
        }

        // if something changed, change the values and repaint
        boolean changed = false;
        if (newWorked > m_currentWorked) {

            // switch to known progress
            // this causes another rendering type and stops the thread
            // for unknown redering triggering started in
            // activateUnknownProgress
            m_unknownProgress = false;
            unknownProgressTimer.removeFigure(this);

            m_currentWorked = newWorked;

            // to ensure that a 100 % bar is not shown the current work
            // can be at most 99%
            if (m_currentWorked > 99) {
                m_currentWorked = 99;
            }
            changed = true;
        }

        String message = pe.getMessage();
        // LOGGER.debug("Event message: " + message);
        // LOGGER.debug("current progress message: " +
        // m_currentProgressMessage);
        if (!m_currentProgressMessage.equals(message)) {

            String meString = m_currentProgressMessage;
            m_currentProgressMessage = message == null ? "" : m_stateMessage
                    + " - " + message;

            // LOGGER.debug("current progress message after if: " +
            // m_currentProgressMessage);

            if (!m_currentProgressMessage.equals(meString)
                    && m_mouseEvent != null) {

                // LOGGER.debug("Show message: " + m_currentProgressMessage);

                if (m_currentDisplay != null 
                        // bugfix: 1392
                        && !m_currentDisplay.isDisposed()) {
                    m_currentDisplay.syncExec(new Runnable() {
                        public void run() {
                            if (m_mouseEvent != null) {
                                getToolTipHelper().displayToolTipNear(
                                        ProgressFigure.this,
                                        new Label(m_currentProgressMessage),
                                        m_mouseEvent.x, m_mouseEvent.y);
                            }
                        }
                    });
                }
            }

        }

        if (m_currentDisplay == null) {
            return;
        }

        if (changed) {

            if (INVOKE_DISPLAY) {
                m_currentDisplay.syncExec(m_repaintObject);
            }
        }
    }

    /**
     * Sets the state message. This message is something like: Executing or
     * Waiting or Queued, etc. The message is always appended before the
     * progress message.
     *
     * @param stateMessage
     *            the state message to show
     */
    public void setStateMessage(final String stateMessage) {
        m_stateMessage = stateMessage;

        m_currentProgressMessage = m_stateMessage;
    }

    /**
     * Resets the work amount and message text.
     */
    public void reset() {
        m_currentProgressMessage = "";
        m_currentWorked = -1;
        m_unknownProgress = true;
        unknownProgressTimer.removeFigure(this);
        m_mouseEvent = null;

        if (getToolTipHelper() != null) {
            getToolTipHelper().hideTip();
        }

    }

    /**
     * To set the current display. Null display is not set.
     *
     * @param currentDisplay
     *            the dipsplay to set
     */
    public void setCurrentDisplay(final Display currentDisplay) {

        if (currentDisplay == null) {
            return;
        }

        m_currentDisplay = currentDisplay;
    }

    public void mouseDragged(final MouseEvent me) {
        // TODO Auto-generated method stub

    }

    public void mouseEntered(final MouseEvent me) {

        m_mouseEvent = me;
        // if there is a usefull progress message and there is a tooltip
        // position indicating that a tooltip should be shown, set the
        // tooltip
        if (m_currentProgressMessage != null
                && !m_currentProgressMessage.equals("") && m_mouseEvent != null) {

            IFigure tip = new Label(m_currentProgressMessage);

            // org.eclipse.swt.graphics.Point absolute;
            // absolute =
            // control.toDisplay(new org.eclipse.swt.graphics.Point(
            // m_mouseEvent.x, m_mouseEvent.y));
            getToolTipHelper().displayToolTipNear(ProgressFigure.this, tip,
                    m_mouseEvent.x, m_mouseEvent.y);

        }
    }

    public void mouseExited(final MouseEvent me) {

        m_mouseEvent = null;

        if (m_toolTipHelper != null) {
            // hides the tooltip
            m_toolTipHelper.hideTip();
        }
    }

    private ProgressToolTipHelper getToolTipHelper() {

        // if (m_toolTipHelper == null) {

        IFigure parent = getParent();
        if (parent != null) {

            WorkflowFigure workflowFigure = (WorkflowFigure) getParent()
                    .getParent();
            if (workflowFigure != null) {

                m_toolTipHelper = workflowFigure.getProgressToolTipHelper();
                // new ProgressToolTipHelper(m_currentDisplay
                // .getActiveShell());
            }
        }

        // }

        return m_toolTipHelper;
    }

    public void mouseHover(final MouseEvent me) {
        // TODO Auto-generated method stub

    }

    public void mouseMoved(final MouseEvent me) {
        // TODO Auto-generated method stub

    }

    /**
     * Implements a thread that updates all figures passed to it. The thread for
     * updating the unknown progress figures (cycling figures) is intended to
     * run just one time. The advantage is that the expensive rendering on the
     * display thread is only invoked once for all figures to render.
     *
     * @author Christoph Sieb, University of Konstanz
     */
    private static class UnknownProgressTimer extends Thread {

        private final Vector<ProgressFigure> m_figuresToPaint = new Vector<ProgressFigure>();

        /**
         * Creats an unknown progress timer for cycling progress bar rendering.
         */
        public UnknownProgressTimer() {

            super("Unknown Progress Timer");
        }

        @Override
        public void run() {

            while (true) {

                // if the queue is empty wait for the next
                // figure to render
                if (m_figuresToPaint.size() == 0) {
                    synchronized (this) {
                        try {
                            wait();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                // the figures are rendered all 500 ms
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (INVOKE_DISPLAY) {
                    if (m_currentDisplay != null && !m_currentDisplay.isDisposed()) {
                        m_currentDisplay.syncExec(new Runnable() {
                            public void run() {
                                synchronized (m_figuresToPaint) {
                                    for (ProgressFigure figure : m_figuresToPaint) {
                                        figure.repaint();
                                    }
                                }
                            }
                        });
                    }
                }
            }
        }

        /**
         * Add a progress figure that should be rendered regularly. (Intended
         * for cycling progress bars)
         *
         * @param figure
         *            The figure to render regularly
         */
        public void addFigure(final ProgressFigure figure) {

            synchronized (m_figuresToPaint) {
                m_figuresToPaint.add(figure);
            }

            synchronized (this) {
                this.notify();
            }

        }

        /**
         * Remove a figure that is no longer intended to be rendered regulary.
         *
         * @param figure
         *            the figure to remove
         */
        public void removeFigure(final ProgressFigure figure) {

            synchronized (m_figuresToPaint) {
                m_figuresToPaint.remove(figure);
            }

        }
    }

}
