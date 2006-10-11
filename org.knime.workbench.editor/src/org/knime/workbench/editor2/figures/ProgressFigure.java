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
        graphics.setBackgroundColor(getBackgroundColor());
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
        graphics.drawRectangle(x + 1, y + 1, 17, h - 2);
        graphics.setFont(PROGRESS_FONT);
        String progressString = "75 %";

        graphics.setXORMode(true);
        graphics.setForegroundColor(ColorConstants.white);
        graphics.drawString(progressString, x + w / 2 - (int)(progressString.length()
                * 3), y - 4);
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
}
