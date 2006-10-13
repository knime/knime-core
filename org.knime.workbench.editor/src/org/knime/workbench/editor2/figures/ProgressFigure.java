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
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;

/**
 * This figure creates the progress bar within a node container figure.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class ProgressFigure extends RectangleFigure {

    /** absolute width of this figure. * */
    public static final int WIDTH = 32;

    /** absolute height of this figure. * */
    public static final int HEIGHT = 12;

    private static final int UNKNOW_PROGRESS_BAR_WIDTH = 6;

    private static final Font PROGRESS_FONT;

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

    /**
     * The progress message.
     */
    private String m_progressMessage;

    private boolean m_unknownProgress = false;

    private int m_unknownProgressBarRenderingPosition;

    private int m_unknownProgressBarDirection;

    private boolean m_executing;

    /**
     * The progress in percent.
     */
    private Label m_progressLabel;

    /**
     * Creates a new node figure.
     */
    public ProgressFigure() {

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

        // progress message (Label)
        m_progressMessage = "Progress message.";
        setToolTip(new Label(m_progressMessage));

        // progress label within the progress bar
        m_progressLabel = new Label("70 %");
        new ProgressPart();
        m_progressLabel
                .setBackgroundColor(m_progressLabel.getBackgroundColor());
        // add(new ProgressPart());
        // add(m_progressLabel);

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

        super.paintFigure(graphics);

        // paint the specified bar length for the progress
        graphics.setForegroundColor(ColorConstants.darkBlue);
        int localLineWidth = HEIGHT / 2;
        graphics.setLineStyle(Graphics.LINE_SOLID);
        graphics.setLineWidth(localLineWidth);

        Rectangle r = getBounds();
        int x = r.x + localLineWidth / 2;
        int y = r.y + localLineWidth / 2;
        int w = r.width - Math.max(1, localLineWidth);
        int h = r.height - Math.max(1, localLineWidth);

        if (m_executing) {
            if (!m_unknownProgress) {

                graphics.drawRectangle(x + 1, y + 1, 17, h - 2);
                graphics.setFont(PROGRESS_FONT);
                String progressString = "75 %";

                graphics.setXORMode(true);
                graphics.setForegroundColor(ColorConstants.white);
                graphics.drawString(progressString, x + w / 2
                        - (int)(progressString.length() * 3), y - 4);
            } else {

                graphics.setForegroundColor(ColorConstants.darkBlue);

                int xPos = x + 1 + m_unknownProgressBarRenderingPosition;

                // calculate the rendering direction
                if (m_unknownProgressBarRenderingPosition + 8
                        + UNKNOW_PROGRESS_BAR_WIDTH >= WIDTH) {
                    m_unknownProgressBarDirection = -1;
                } else if (m_unknownProgressBarRenderingPosition <= 0) {
                    m_unknownProgressBarDirection = 1;
                }

                graphics.drawRectangle(xPos, y + 1, UNKNOW_PROGRESS_BAR_WIDTH,
                        h - 2);

                m_unknownProgressBarRenderingPosition += m_unknownProgressBarDirection;

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

        final Display display = Display.getCurrent();

        final Runnable repaintRun = new Runnable() {

            public void run() {
                repaint();
            };
        };

        new Thread() {

            public void run() {

                while (m_unknownProgress) {

                    synchronized (this) {
                        try {
                            wait(100);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    display.syncExec(repaintRun);
                }
            }

        }.start();
    }

    /**
     * Represents a bit of progress in the progress bar.
     * 
     * @author Chrsitoph Sieb, University of Konstanz
     */
    private class ProgressPart extends Figure {
        private static final int PARTWIDTH = 10;

        /**
         * Creates a progress part with a default background color.
         */
        ProgressPart() {
            setBackgroundColor(ColorConstants.blue);
        }

        /**
         * @see org.eclipse.draw2d.IFigure#getPreferredSize(int, int)
         */
        @Override
        public Dimension getPreferredSize(final int wHint, final int hHint) {

            // the width is fixed
            int prefWidth = PARTWIDTH;

            // the height stays of that of the progress bar
            int prefHeight = HEIGHT;

            return new Dimension(prefWidth, prefHeight);
        }
    }

    /**
     * Sets the mode of this progress bar. The modes are executing or queued
     * 
     * @param executing if true the mode is executing otherwise the progress
     *            should be displayed as queued.
     */
    public void setMode(final boolean executing) {
        m_executing = executing;
    }
}
