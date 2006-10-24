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
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   31.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.DelegatingLayout;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.NodeProgressEvent;
import org.knime.core.node.NodeProgressListener;

/**
 * This figure creates the progress bar within a node container figure.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class ProgressFigure extends RectangleFigure implements
        NodeProgressListener {

    /** absolute width of this figure. * */
    public static final int WIDTH = 32;

    /** absolute height of this figure. * */
    public static final int HEIGHT = 12;

    private static final int UNKNOW_PROGRESS_BAR_WIDTH = 10;

    private static final Font PROGRESS_FONT;

    private static final Color PROGRESS_BAR_BACKGROUND_COLOR =
            new Color(null, 220, 220, 220);

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
    }

    private boolean m_unknownProgress = false;

    private int m_unknownProgressBarRenderingPosition;

    private int m_unknownProgressBarDirection;

    private boolean m_executing;

    private int m_currentWorked;

    private String m_currentProgressMessage = "";

    private String m_stateMessage;

    private Display m_currentDisplay;

    /**
     * Creates a new node figure.
     */
    public ProgressFigure() {

        m_currentDisplay = Display.getCurrent();

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
    }

    /**
     * 
     * @see org.eclipse.draw2d.IFigure#getMinimumSize(int, int)
     */
    @Override
    public Dimension getMinimumSize(final int whint, final int hhint) {
        return getPreferredSize(whint, hhint);
    }

    /**
     * @see org.eclipse.draw2d.Figure#getMaximumSize()
     */
    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize(WIDTH, HEIGHT);
    }

    /**
     * @see org.eclipse.draw2d.IFigure#getPreferredSize(int, int)
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

    /**
     * @see org.eclipse.draw2d.Figure#paintFigure(org.eclipse.draw2d.Graphics)
     */
    @Override
    public void paintFigure(final Graphics graphics) {

        // super.paintFigure(graphics);

        // paint the specified bar length for the progress
        graphics.setForegroundColor(ColorConstants.black);
        graphics.setBackgroundColor(PROGRESS_BAR_BACKGROUND_COLOR);

        Rectangle r = getBounds();
        int x = r.x;
        int y = r.y;
        int w = r.width;
        int h = r.height;

        // defines a polygon within the bounds of this figure with round
        // edges
        int firstLeftX = x;
        int secondLeftX = x + 1;
        int firstRightX = x + w - 2;
        int secondRightX = x + w - 1;
        int firstUpperY = y;
        int secondUpperY = y + 1;
        int firstLowerY = y + h - 2;
        int secondLowerY = y + h - 1;

        int[] pointList =
                {secondLeftX, firstUpperY, firstRightX, firstUpperY,
                        secondRightX, secondUpperY, secondRightX, firstLowerY,
                        firstRightX, secondLowerY, secondLeftX, secondLowerY,
                        firstLeftX, firstLowerY, firstLeftX, secondUpperY};

        graphics.fillPolygon(pointList);
        graphics.drawPolygon(pointList);

        graphics.setForegroundColor(ColorConstants.darkBlue);
        graphics.setBackgroundColor(ColorConstants.darkBlue);

        if (m_executing) {
            if (!m_unknownProgress) {

                // calculate the progress bar width from the percentage
                // current worked value
                int barWidth =
                        (int)Math.round((double)WIDTH / (double)100
                                * (double)m_currentWorked);

                graphics.fillRectangle(x, y, barWidth, h);
                graphics.setFont(PROGRESS_FONT);

                // create the percentage string
                String progressString = m_currentWorked + "%";

                graphics.setXORMode(true);
                graphics.setForegroundColor(ColorConstants.white);
                graphics.drawString(progressString, x + w / 2
                        - (int)(progressString.length() * 4), y - 1);
            } else {

                graphics.setForegroundColor(ColorConstants.darkBlue);

                // calculate the rendering direction
                if (m_unknownProgressBarRenderingPosition
                        + UNKNOW_PROGRESS_BAR_WIDTH > WIDTH + 1) {
                    m_unknownProgressBarDirection = -1;
                } else if (m_unknownProgressBarRenderingPosition <= 0) {
                    m_unknownProgressBarDirection = 1;
                }

                // defines a polygon bar with round edges
                firstLeftX =
                        firstLeftX + m_unknownProgressBarRenderingPosition - 1;
                secondLeftX = firstLeftX + 1;
                firstRightX = firstLeftX + UNKNOW_PROGRESS_BAR_WIDTH;
                secondRightX = firstRightX + 1;
                firstUpperY = firstUpperY + 1;
                secondUpperY = firstUpperY + 1;
                firstLowerY = firstUpperY + h - 4;
                secondLowerY = firstLowerY + 2;

                // NOTE: the - 1 is a workaround due to a problem in 
                // the rendering routine of fillPolygon
                // if not used the lower right corner is not smooth
                int[] movingBar =
                        {secondLeftX, firstUpperY, firstRightX, firstUpperY,
                                secondRightX, secondUpperY, secondRightX,
                                firstLowerY, firstRightX - 1, secondLowerY,
                                secondLeftX, secondLowerY, firstLeftX,
                                firstLowerY, firstLeftX, secondUpperY};

                graphics.fillPolygon(movingBar);
                // graphics.fillRectangle(xPos, y, UNKNOW_PROGRESS_BAR_WIDTH,
                // h);

                m_unknownProgressBarRenderingPosition +=
                        m_unknownProgressBarDirection;

            }
        }
    }

    /**
     * Stops the rendering of an unknown progress.
     */
    public void stopUnknownProgress() {
        m_unknownProgress = false;
    }

    /**
     * Activates this progress bar to render an unknown progress.
     */
    public void activateUnknownProgress() {
        m_unknownProgress = true;

        // reset the worked value
        m_currentWorked = 0;

        // m_currentDisplay = Display.getCurrent();

        final Runnable repaintRun = new Runnable() {

            public void run() {

                repaint();

            };
        };

        new Thread("Unknown Progress Timer") {

            public void run() {

                while (m_unknownProgress) {

                    synchronized (this) {
                        try {
                            wait(100);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    m_currentDisplay.syncExec(repaintRun);
                }
            }

        }.start();
    }

    /**
     * Sets the mode of this progress bar. The modes are executing or queued
     * 
     * @param executing if true the mode is executing otherwise the progress
     *            should be displayed as queued.
     */
    public void setExecuting(final boolean executing) {
        m_executing = executing;
    }

    /**
     * Get the mode of this progress bar. The modes are executing or queued
     * 
     * @return executing if true the mode is executing otherwise the progress
     *         should be displayed as queued.
     */
    public boolean isExecuting() {
        return m_executing;
    }

    /**
     * Updates UI after progress has changed.
     * 
     * @see org.knime.core.node.NodeProgressListener
     *      #progressChanged(NodeProgressEvent)
     */
    public synchronized void progressChanged(final NodeProgressEvent pe) {

        String message = pe.getMessage();

        int newWorked = m_currentWorked;
        if (pe.hasProgress()) {
            double progress = pe.getProgress().doubleValue();
            newWorked =
                    (int)Math.round(Math.max(0, Math.min(progress * 100, 100)));
        }

        // if something changed, change the values and repaint
        boolean changed = false;
        if (newWorked > m_currentWorked) {

            // switch to known progress
            // this causes another rendering type and stops the thread
            // for unknown redering triggering started in
            // activateUnknownProgress
            m_unknownProgress = false;

            m_currentWorked = newWorked;
            changed = true;
        }

        if (!m_currentProgressMessage.equals(message)) {

            m_currentProgressMessage =
                    message == null ? "" : m_stateMessage + " - " + message;
            // set the message to the tooltip
            setToolTip(new Label(m_currentProgressMessage));

        }

        if (changed) {
            assert m_currentDisplay != null;
            try {
                m_currentDisplay.syncExec(new Runnable() {
                    public void run() {

                        try {
                            repaint();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sets the state message. This message is something like: Executing or
     * Waiting or Queued, etc. The message is always appended before the
     * progress message.
     * 
     * @param stateMessage the state message to show
     */
    public void setStateMessage(final String stateMessage) {
        m_stateMessage = stateMessage;

        m_currentProgressMessage = m_stateMessage;

        // set the message to the tooltip
        setToolTip(new Label(m_currentProgressMessage));
    }

    /**
     * Resets the work amount and message text.
     */
    public void reset() {
        m_currentProgressMessage = "";
        m_currentWorked = 0;
        m_unknownProgress = true;
    }
}
