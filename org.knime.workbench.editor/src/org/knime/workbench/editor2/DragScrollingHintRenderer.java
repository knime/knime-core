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
 *   Mar 9, 2018 (loki): created
 */
package org.knime.workbench.editor2;

import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.RoundedRectangle;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.knime.workbench.editor2.figures.WorkflowFigure;

/**
 * This class exists to provide a render hint to the workflow editor user when they are dragging so that
 *      they are made aware that the canvas will auto-scroll.
 */
class DragScrollingHintRenderer implements MouseListener, MouseMoveListener, SelectionListener {

    static private final float SPATIAL_ZONE_PERCENTAGE = 0.1f;
    static private final float SPATIAL_INSET_PERCENTAGE = 0.165f;

    static private final int MIN_HINT_ZONE_ALPHA = 11;
    static private final int MAX_HINT_ZONE_ALPHA = 136;
    static private final int ALPHA_TWEAK_ANIMATION_SLEEP = 76;

    static private final Dimension CORNER_DIMENSION = new Dimension(7, 7);

    // Makes for better code readability
    static private final int NORTH = 0;
    static private final int WEST = 1;
    static private final int SOUTH = 2;
    static private final int EAST = 3;


    final private FigureCanvas backingCanvas;
    final private WorkflowFigure rootFigure;

    final private Color fillColor;

    private RoundedRectangle[] currentlyDisplayedRegions;

    // This will only be consulted from the SWT thread, so i'm content making it neither volatile nor
    //  AtomicBoolean
    private boolean weAreInADrag;


    // We cache these at mouse-down time to save calculation time during the drag

    // Similarly, this will only be interacted with on the SWT thread; the indices represent N,W,S,E.
    private int[] renderSpatialThresholds;
    // Similarly ...; this is used to store the reduced width, 0, or height, 1, of a highlight region.
    private int[] renderDimensions;
    private int[] locationInsetOffsets;


    /**
     * @param viewer we assume this instance is being created by WorkflowGraphicalViewerCreator
     */
    DragScrollingHintRenderer(final GraphicalViewer viewer, final WorkflowFigure figure) {
        this.backingCanvas = (FigureCanvas)viewer.getControl();

        this.backingCanvas.addMouseListener(this);
        this.backingCanvas.addMouseMoveListener(this);

        // See comments on widgetSelected(SelectionEvent)
        this.backingCanvas.getHorizontalBar().addSelectionListener(this);
        this.backingCanvas.getVerticalBar().addSelectionListener(this);

        this.rootFigure = figure;

        this.fillColor = new Color(this.backingCanvas.getDisplay(), 0, 134, 197);
    }

    // This will always be called on the SWT thread
    private RoundedRectangle makeRoundedRectangle(final Rectangle viewportBounds, final int side) {
        final RoundedRectangle rhett = new RoundedRectangle();

        rhett.setFill(true);
        rhett.setBackgroundColor(this.fillColor);
        rhett.setCornerDimensions(CORNER_DIMENSION);
        rhett.setAlpha(MIN_HINT_ZONE_ALPHA);

        switch (side) {
            case NORTH:
            case SOUTH:
                rhett.setPreferredSize((viewportBounds.width - (2 * this.locationInsetOffsets[0])),
                                       this.renderDimensions[1]);
                break;
            default:
                rhett.setPreferredSize(this.renderDimensions[0],
                                       (viewportBounds.height - (2 * this.locationInsetOffsets[1])));
                break;
        }

        this.rootFigure.add(rhett);
        this.backingCanvas.getDisplay().timerExec(ALPHA_TWEAK_ANIMATION_SLEEP, new AlphaTweaker(side, 5));

        return rhett;
    }

    // This will always be called on the SWT thread
    private void updateLocation (final Rectangle viewportBounds, final int side) {
        final int x;
        final int y;

        switch (side) {
            case NORTH:
                x = viewportBounds.x + this.locationInsetOffsets[0];
                y = viewportBounds.y;
                break;
            case SOUTH:
                x = viewportBounds.x + this.locationInsetOffsets[0];
                y = viewportBounds.y + viewportBounds.height - this.renderDimensions[1];
                break;
            case WEST:
                x = viewportBounds.x;
                y = viewportBounds.y + this.locationInsetOffsets[1];
                break;
            default:
                x = viewportBounds.x + viewportBounds.width - this.renderDimensions[0];
                y = viewportBounds.y + this.locationInsetOffsets[1];
                break;
        }

        this.currentlyDisplayedRegions[side].setLocation(new Point(x, y));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseMove(final MouseEvent me) {
        if (this.weAreInADrag) {
            final Rectangle bounds = this.backingCanvas.getViewport().getBounds();
            final boolean[] renders = new boolean[4];

            renders[NORTH] = (me.y <= (this.renderSpatialThresholds[NORTH] + bounds.y));
            renders[WEST] = (me.x <= (this.renderSpatialThresholds[WEST] + bounds.x));
            renders[SOUTH] = (me.y >= (this.renderSpatialThresholds[SOUTH] + bounds.y));
            renders[EAST] = (me.x >= (this.renderSpatialThresholds[EAST] + bounds.x));

            for (int i = 0; i < 4; i++) {
                if (renders[i]) {
                    if (this.currentlyDisplayedRegions[i] == null) {
                        this.currentlyDisplayedRegions[i] = this.makeRoundedRectangle(bounds, i);
                    }

                    this.updateLocation(bounds, i);
                }
                else if (this.currentlyDisplayedRegions[i] != null) {
                    this.rootFigure.remove(this.currentlyDisplayedRegions[i]);

                    this.currentlyDisplayedRegions[i] = null;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseDoubleClick(final MouseEvent me) { } // NOPMD

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseDown(final MouseEvent me) {
        final Rectangle viewportBounds;

        this.weAreInADrag = true;

        viewportBounds = this.backingCanvas.getViewport().getBounds();

        this.renderSpatialThresholds = new int[4];
        this.renderSpatialThresholds[NORTH] = (int)(SPATIAL_ZONE_PERCENTAGE * viewportBounds.height);
        this.renderSpatialThresholds[WEST] = (int)(SPATIAL_ZONE_PERCENTAGE * viewportBounds.width);
        this.renderSpatialThresholds[SOUTH] = viewportBounds.height - this.renderSpatialThresholds[0];
        this.renderSpatialThresholds[EAST] = viewportBounds.width - this.renderSpatialThresholds[1];

        this.renderDimensions = new int[2];
        this.renderDimensions[0] = this.renderSpatialThresholds[WEST];
        this.renderDimensions[1] = this.renderSpatialThresholds[NORTH];

        this.locationInsetOffsets = new int[2];
        this.locationInsetOffsets[0] = (int)(SPATIAL_INSET_PERCENTAGE * viewportBounds.width);
        this.locationInsetOffsets[1] = (int)(SPATIAL_INSET_PERCENTAGE * viewportBounds.height);

        this.currentlyDisplayedRegions = new RoundedRectangle[4];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseUp(final MouseEvent me) {
        this.weAreInADrag = false;

        this.renderSpatialThresholds = null;
        this.renderDimensions = null;
        this.locationInsetOffsets = null;

        for (int i = 0; i < 4; i++) {
            if (this.currentlyDisplayedRegions[i] != null) {
                this.rootFigure.remove(this.currentlyDisplayedRegions[i]);
            }
        }
        this.currentlyDisplayedRegions = null;
    }

//static private final NodeLogger LOGGER = NodeLogger.getLogger(DragScrollingHintRenderer.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void widgetSelected(final SelectionEvent se) {
        /*
         * We need to be a scroll listener as potentially the drag could not change but a scrolling (and
         *  thereby location change of the viewport) will occur.
         *
         * I've written something that should work, but it's hard to tell both because of SWT's
         *  handling of scrolling notifications, and more vitally since i do not see auto-scrolling
         *  behaviour on Mac.
         * I am suspicious that i see no notifications here when the scrollbars themselves auto-appear
         *  and change range, but i suppose that is not technically scrolling behaviour.
         */
//LOGGER.warn("widget selected: " + se);
        if (this.weAreInADrag) {
            final Rectangle bounds = this.backingCanvas.getViewport().getBounds();

            for (int i = 0; i < 4; i++) {
                if (this.currentlyDisplayedRegions[i] != null) {
                    this.updateLocation(bounds, i);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void widgetDefaultSelected(final SelectionEvent se) {
        this.widgetSelected(se);
    }


    private class AlphaTweaker implements Runnable {

        final protected int regionIndex;
        final protected int alphaDelta;
        protected boolean increasing;
        protected int alpha;

        private AlphaTweaker (final int index, final int delta) {
            this.regionIndex = index;

            this.alphaDelta = delta;

            this.increasing = true;

            this.alpha = MIN_HINT_ZONE_ALPHA;
        }

        @Override
        public void run() {
            final DragScrollingHintRenderer outer = DragScrollingHintRenderer.this;
            final RoundedRectangle region = outer.currentlyDisplayedRegions[this.regionIndex];

            if (region != null) {
                region.setAlpha(this.alpha);

                if (this.increasing) {
                    this.alpha += this.alphaDelta;

                    if (this.alpha > MAX_HINT_ZONE_ALPHA) {
                        this.increasing = false;
                        this.alpha = MAX_HINT_ZONE_ALPHA;
                    }
                }
                else {
                    this.alpha -= this.alphaDelta;

                    if (this.alpha < MIN_HINT_ZONE_ALPHA) {
                        this.increasing = true;
                        this.alpha = MIN_HINT_ZONE_ALPHA;
                    }
                }

                outer.backingCanvas.getDisplay().timerExec(ALPHA_TWEAK_ANIMATION_SLEEP, this);
            }
        }

    }

}
