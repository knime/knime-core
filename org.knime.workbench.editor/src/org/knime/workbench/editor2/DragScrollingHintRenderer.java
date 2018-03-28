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
 *
 * @author loki der quaeler
 */
class DragScrollingHintRenderer implements MouseListener, MouseMoveListener, SelectionListener {

    /*
     * This was getting instantiated at the end of WorkflowEditor.createGraphicalViewer thusly:
     *
     *  WorkflowFigure figure = ((WorkflowRootEditPart)rep.getContents()).getFigure();
     *  m_scrollingHintRenderer = new DragScrollingHintRenderer(getGraphicalViewer(), figure);
     */

    private static final float SPATIAL_ZONE_PERCENTAGE = 0.1f;
    private static final float SPATIAL_INSET_PERCENTAGE = 0.165f;

    private static final int MIN_HINT_ZONE_ALPHA = 11;
    private static final int MAX_HINT_ZONE_ALPHA = 136;
    private static final int ALPHA_TWEAK_ANIMATION_SLEEP = 76;

    private static final Dimension CORNER_DIMENSION = new Dimension(7, 7);

    // Makes for better code readability
    private static final int NORTH = 0;
    private static final int WEST = 1;
    private static final int SOUTH = 2;
    private static final int EAST = 3;


    private final FigureCanvas m_backingCanvas;
    private final WorkflowFigure m_rootFigure;

    private final Color m_fillColor;

    private RoundedRectangle[] m_currentlyDisplayedRegions;

    // This will only be consulted from the SWT thread, so i'm content making it neither volatile nor
    //  AtomicBoolean
    private boolean m_weAreInADrag;


    // We cache these at mouse-down time to save calculation time during the drag

    // Similarly, this will only be interacted with on the SWT thread; the indices represent N,W,S,E.
    private int[] m_renderSpatialThresholds;
    // Similarly ...; this is used to store the reduced width, 0, or height, 1, of a highlight region.
    private int[] m_renderDimensions;
    private int[] m_locationInsetOffsets;


    /**
     * This constructs on instance of the renderer.
     *
     * @param viewer we assume this instance is being created by WorkflowGraphicalViewerCreator
     */
    DragScrollingHintRenderer(final GraphicalViewer viewer, final WorkflowFigure figure) {
        m_backingCanvas = (FigureCanvas)viewer.getControl();

        m_backingCanvas.addMouseListener(this);
        m_backingCanvas.addMouseMoveListener(this);

        // See comments on widgetSelected(SelectionEvent)
        m_backingCanvas.getHorizontalBar().addSelectionListener(this);
        m_backingCanvas.getVerticalBar().addSelectionListener(this);

        m_rootFigure = figure;

        m_fillColor = new Color(m_backingCanvas.getDisplay(), 0, 134, 197);
    }

    // This will always be called on the SWT thread
    private RoundedRectangle makeRoundedRectangle(final Rectangle viewportBounds, final int side) {
        final RoundedRectangle roundedRectangle = new RoundedRectangle();

        roundedRectangle.setFill(true);
        roundedRectangle.setBackgroundColor(m_fillColor);
        roundedRectangle.setCornerDimensions(CORNER_DIMENSION);
        roundedRectangle.setAlpha(MIN_HINT_ZONE_ALPHA);

        switch (side) {
            case NORTH:
            case SOUTH:
                roundedRectangle.setPreferredSize((viewportBounds.width - (2 * m_locationInsetOffsets[0])),
                                                  m_renderDimensions[1]);
                break;
            default:
                roundedRectangle.setPreferredSize(m_renderDimensions[0],
                                                  (viewportBounds.height - (2 * m_locationInsetOffsets[1])));
                break;
        }

        m_rootFigure.add(roundedRectangle);
        m_backingCanvas.getDisplay().timerExec(ALPHA_TWEAK_ANIMATION_SLEEP, new AlphaTweaker(side, 5));

        return roundedRectangle;
    }

    // This will always be called on the SWT thread
    private void updateLocation(final Rectangle viewportBounds, final int side) {
        final int x;
        final int y;

        switch (side) {
            case NORTH:
                x = viewportBounds.x + m_locationInsetOffsets[0];
                y = viewportBounds.y;
                break;
            case SOUTH:
                x = viewportBounds.x + m_locationInsetOffsets[0];
                y = viewportBounds.y + viewportBounds.height - m_renderDimensions[1];
                break;
            case WEST:
                x = viewportBounds.x;
                y = viewportBounds.y + m_locationInsetOffsets[1];
                break;
            default:
                x = viewportBounds.x + viewportBounds.width - m_renderDimensions[0];
                y = viewportBounds.y + m_locationInsetOffsets[1];
                break;
        }

        m_currentlyDisplayedRegions[side].setLocation(new Point(x, y));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseMove(final MouseEvent me) {
        if (m_weAreInADrag) {
            final Rectangle bounds = m_backingCanvas.getViewport().getBounds();
            final boolean[] renders = new boolean[4];

            renders[NORTH] = (me.y <= (m_renderSpatialThresholds[NORTH] + bounds.y));
            renders[WEST] = (me.x <= (m_renderSpatialThresholds[WEST] + bounds.x));
            renders[SOUTH] = (me.y >= (m_renderSpatialThresholds[SOUTH] + bounds.y));
            renders[EAST] = (me.x >= (m_renderSpatialThresholds[EAST] + bounds.x));

            for (int i = 0; i < 4; i++) {
                if (renders[i]) {
                    if (m_currentlyDisplayedRegions[i] == null) {
                        m_currentlyDisplayedRegions[i] = makeRoundedRectangle(bounds, i);
                    }

                    updateLocation(bounds, i);
                } else if (m_currentlyDisplayedRegions[i] != null) {
                    m_rootFigure.remove(m_currentlyDisplayedRegions[i]);

                    m_currentlyDisplayedRegions[i] = null;
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
        m_weAreInADrag = true;

        final Rectangle viewportBounds = m_backingCanvas.getViewport().getBounds();

        m_renderSpatialThresholds = new int[4];
        m_renderSpatialThresholds[NORTH] = (int)(SPATIAL_ZONE_PERCENTAGE * viewportBounds.height);
        m_renderSpatialThresholds[WEST] = (int)(SPATIAL_ZONE_PERCENTAGE * viewportBounds.width);
        m_renderSpatialThresholds[SOUTH] = viewportBounds.height - m_renderSpatialThresholds[0];
        m_renderSpatialThresholds[EAST] = viewportBounds.width - m_renderSpatialThresholds[1];

        m_renderDimensions = new int[2];
        m_renderDimensions[0] = m_renderSpatialThresholds[WEST];
        m_renderDimensions[1] = m_renderSpatialThresholds[NORTH];

        m_locationInsetOffsets = new int[2];
        m_locationInsetOffsets[0] = (int)(SPATIAL_INSET_PERCENTAGE * viewportBounds.width);
        m_locationInsetOffsets[1] = (int)(SPATIAL_INSET_PERCENTAGE * viewportBounds.height);

        m_currentlyDisplayedRegions = new RoundedRectangle[4];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseUp(final MouseEvent me) {
        m_weAreInADrag = false;

        m_renderSpatialThresholds = null;
        m_renderDimensions = null;
        m_locationInsetOffsets = null;

        for (int i = 0; i < 4; i++) {
            if (m_currentlyDisplayedRegions[i] != null) {
                m_rootFigure.remove(m_currentlyDisplayedRegions[i]);
            }
        }
        m_currentlyDisplayedRegions = null;
    }

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
        if (m_weAreInADrag) {
            final Rectangle bounds = m_backingCanvas.getViewport().getBounds();

            for (int i = 0; i < 4; i++) {
                if (m_currentlyDisplayedRegions[i] != null) {
                    updateLocation(bounds, i);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void widgetDefaultSelected(final SelectionEvent se) {
        widgetSelected(se);
    }


    class AlphaTweaker implements Runnable {

        private final int m_regionIndex;
        private final int m_alphaDelta;
        private boolean m_increasing;
        private int m_alpha;

        AlphaTweaker(final int index, final int delta) {
            m_regionIndex = index;

            m_alphaDelta = delta;

            m_increasing = true;

            m_alpha = MIN_HINT_ZONE_ALPHA;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            final RoundedRectangle region = m_currentlyDisplayedRegions[m_regionIndex];

            if (region != null) {
                region.setAlpha(m_alpha);

                if (m_increasing) {
                    m_alpha += m_alphaDelta;

                    if (m_alpha > MAX_HINT_ZONE_ALPHA) {
                        m_increasing = false;
                        m_alpha = MAX_HINT_ZONE_ALPHA;
                    }
                } else {
                    m_alpha -= m_alphaDelta;

                    if (m_alpha < MIN_HINT_ZONE_ALPHA) {
                        m_increasing = true;
                        m_alpha = MIN_HINT_ZONE_ALPHA;
                    }
                }

                m_backingCanvas.getDisplay().timerExec(ALPHA_TWEAK_ANIMATION_SLEEP, this);
            }
        }

    }

}
