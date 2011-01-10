/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   01.02.2006 (sieb): created
 */
package org.knime.base.node.viz.plotter2D;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JScrollPane;
import javax.swing.ScrollPaneLayout;
import javax.swing.Scrollable;
import javax.swing.border.Border;

/**
 * <code>ScrollPaneLyout</code> for the scatter plotter. In contrast to the
 * default manager for scroll panes, this manager creates the column header at
 * the bottom of the view (as expected of a function graph). For this reason the
 * layout container method is overwritten.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class Plotter2DScrollPaneLayout extends ScrollPaneLayout {

    /**
     * Overwrites the method of the parent class to arrange the header at the
     * bottom of the view.
     * 
     * @param parent parent container of the component rendered by this layout
     *            manager
     * @see java.awt.LayoutManager#layoutContainer(java.awt.Container)
     */
    @Override
    public void layoutContainer(final Container parent) {

        /*
         * Sync the (now obsolete) policy fields with the JScrollPane.
         */
        JScrollPane scrollPane = (JScrollPane)parent;
        vsbPolicy = scrollPane.getVerticalScrollBarPolicy();
        hsbPolicy = scrollPane.getHorizontalScrollBarPolicy();

        Rectangle availR = scrollPane.getBounds();
        availR.x = 0;
        availR.y = 0;

        Insets insets = parent.getInsets();
        availR.x = insets.left;
        availR.y = insets.top;
        availR.width -= insets.left + insets.right;
        availR.height -= insets.top + insets.bottom;

        // remove the width and height of the horizontal and vertical scrollbar
        // generally from the available rec so that there is enough space
        // if they have to be added. the advantage is that this way there
        // is not influence to each other.(sometimes to add one scroll bar
        // causes to add the other one as well)
        if (vsb != null) {

            availR.width -= vsb.getPreferredSize().width;
            // also expand the scroll panes prefered size
        }
        if (hsb != null) {

            availR.height -= hsb.getPreferredSize().height;
        }
        
        /*
         * Get the scrollPane's orientation.
         */
        boolean leftToRight = scrollPane.getComponentOrientation()
                .isLeftToRight();

        /*
         * If there's a visible column header remove the space it needs from the
         * top of availR. The column header is treated as if it were fixed
         * height, arbitrary width.
         */

        Rectangle colHeadR = new Rectangle(0, 0, 0, 0);

        if ((colHead != null) && (colHead.isVisible())) {
            int colHeadHeight = Math.min(availR.height, colHead
                    .getPreferredSize().height);
            colHeadR.height = colHeadHeight;

            // availR.y += colHeadHeight;
            // we do not adjust the y coordinate but the height of th
            // available Rec
            availR.height -= colHeadHeight;
        }

        /*
         * If there's a visible row header remove the space it needs from the
         * left or right of availR. The row header is treated as if it were
         * fixed width, arbitrary height.
         */

        Rectangle rowHeadR = new Rectangle(0, 0, 0, 0);

        if ((rowHead != null) && (rowHead.isVisible())) {
            int rowHeadWidth = Math.min(availR.width, rowHead
                    .getPreferredSize().width);
            rowHeadR.width = rowHeadWidth;
            availR.width -= rowHeadWidth;
            if (leftToRight) {
                rowHeadR.x = availR.x;
                availR.x += rowHeadWidth;
            } else {
                rowHeadR.x = availR.x + availR.width;
            }
        }

        /*
         * If there's a JScrollPane.viewportBorder, remove the space it occupies
         * for availR.
         */

        Border viewportBorder = scrollPane.getViewportBorder();
        Insets vpbInsets;
        if (viewportBorder != null) {
            vpbInsets = viewportBorder.getBorderInsets(parent);
            availR.x += vpbInsets.left;
            availR.y += vpbInsets.top;
            availR.width -= vpbInsets.left + vpbInsets.right;
            availR.height -= vpbInsets.top + vpbInsets.bottom;
        } else {
            vpbInsets = new Insets(0, 0, 0, 0);
        }

        /*
         * At this point availR is the space available for the viewport and
         * scrollbars. rowHeadR is correct except for its height and y and
         * colHeadR is correct except for its width and x. Once we're through
         * computing the dimensions of these three parts we can go back and set
         * the dimensions of rowHeadR.height, rowHeadR.y, colHeadR.width,
         * colHeadR.x and the bounds for the corners.
         * 
         * We'll decide about putting up scrollbars by comparing the viewport
         * views preferred size with the viewports extent size (generally just
         * its size). Using the preferredSize is reasonable because layout
         * proceeds top down - so we expect the viewport to be laid out next.
         * And we assume that the viewports layout manager will give the view
         * it's preferred size. One exception to this is when the view
         * implements Scrollable and
         * Scrollable.getViewTracksViewport{Width,Height} methods return true.
         * If the view is tracking the viewports width we don't bother with a
         * horizontal scrollbar, similarly if view.getViewTracksViewport(Height)
         * is true we don't bother with a vertical scrollbar.
         */

        Component view = (viewport != null) ? viewport.getView() : null;
        Dimension viewPrefSize = (view != null) ? view.getPreferredSize()
                : new Dimension(0, 0);

        Dimension extentSize = (viewport != null) ? viewport
                .toViewCoordinates(availR.getSize()) : new Dimension(0, 0);

        boolean viewTracksViewportWidth = false;
        boolean viewTracksViewportHeight = false;
        boolean isEmpty = (availR.width < 0 || availR.height < 0);
        Scrollable sv;
        // Don't bother checking the Scrollable methods if there is no room
        // for the viewport, we aren't going to show any scrollbars in this
        // case anyway.
        if (!isEmpty && view instanceof Scrollable) {
            sv = (Scrollable)view;
            viewTracksViewportWidth = sv.getScrollableTracksViewportWidth();
            viewTracksViewportHeight = sv.getScrollableTracksViewportHeight();
        } else {
            sv = null;
        }

        /*
         * If there's a vertical scrollbar and we need one, allocate space for
         * it (we'll make it visible later). A vertical scrollbar is considered
         * to be fixed width, arbitrary height.
         */

        Rectangle vsbR = new Rectangle(0, availR.y - vpbInsets.top, 0, 0);

        boolean vsbNeeded;
        if (isEmpty) {
            vsbNeeded = false;
        } else if (vsbPolicy == VERTICAL_SCROLLBAR_ALWAYS) {
            vsbNeeded = true;
        } else if (vsbPolicy == VERTICAL_SCROLLBAR_NEVER) {
            vsbNeeded = false;
        } else { // vsbPolicy == VERTICAL_SCROLLBAR_AS_NEEDED
            vsbNeeded = !viewTracksViewportHeight
                    && (viewPrefSize.height > extentSize.height);
        }

        if ((vsb != null) && vsbNeeded) {
            adjustForVSB(true, availR, vsbR, vpbInsets, leftToRight);
            extentSize = viewport.toViewCoordinates(availR.getSize());
        }

        /*
         * If there's a horizontal scrollbar and we need one, allocate space for
         * it (we'll make it visible later). A horizontal scrollbar is
         * considered to be fixed height, arbitrary width.
         */

        Rectangle hsbR = new Rectangle(availR.x - vpbInsets.left, 0, 0, 0);
        boolean hsbNeeded;
        if (isEmpty) {
            hsbNeeded = false;
        } else if (hsbPolicy == HORIZONTAL_SCROLLBAR_ALWAYS) {
            hsbNeeded = true;
        } else if (hsbPolicy == HORIZONTAL_SCROLLBAR_NEVER) {
            hsbNeeded = false;
        } else { // hsbPolicy == HORIZONTAL_SCROLLBAR_AS_NEEDED
            hsbNeeded = !viewTracksViewportWidth
                    && (viewPrefSize.width > extentSize.width);
        }

        if ((hsb != null) && hsbNeeded) {
            adjustForHSB(true, availR, hsbR, vpbInsets, colHeadR);

            /*
             * If we added the horizontal scrollbar then we've implicitly
             * reduced the vertical space available to the viewport. As a
             * consequence we may have to add the vertical scrollbar, if that
             * hasn't been done so already. Of course we don't bother with any
             * of this if the vsbPolicy is NEVER.
             */
            if ((vsb != null) && !vsbNeeded
                    && (vsbPolicy != VERTICAL_SCROLLBAR_NEVER)) {

                extentSize = viewport.toViewCoordinates(availR.getSize());
                vsbNeeded = viewPrefSize.height > extentSize.height;

                if (vsbNeeded) {
                    adjustForVSB(true, availR, vsbR, vpbInsets, leftToRight);
                }
            }
        }

        /*
         * Set the size of the viewport first, and then recheck the Scrollable
         * methods. Some components base their return values for the Scrollable
         * methods on the size of the Viewport, so that if we don't ask after
         * resetting the bounds we may have gotten the wrong answer.
         */

        if (viewport != null) {
            viewport.setBounds(availR);

            if (sv != null) {
                extentSize = viewport.toViewCoordinates(availR.getSize());

                boolean oldHSBNeeded = hsbNeeded;
                boolean oldVSBNeeded = vsbNeeded;
                viewTracksViewportWidth = sv.getScrollableTracksViewportWidth();
                viewTracksViewportHeight = sv
                        .getScrollableTracksViewportHeight();
                if (vsb != null && vsbPolicy == VERTICAL_SCROLLBAR_AS_NEEDED) {
                    boolean newVSBNeeded = !viewTracksViewportHeight
                            && (viewPrefSize.height > extentSize.height);
                    if (newVSBNeeded != vsbNeeded) {
                        vsbNeeded = newVSBNeeded;
                        adjustForVSB(vsbNeeded, availR, vsbR, vpbInsets,
                                leftToRight);
                        extentSize = viewport.toViewCoordinates(availR
                                .getSize());
                    }
                }
                if (hsb != null && hsbPolicy == HORIZONTAL_SCROLLBAR_AS_NEEDED) {
                    boolean newHSBbNeeded = !viewTracksViewportWidth
                            && (viewPrefSize.width > extentSize.width);
                    if (newHSBbNeeded != hsbNeeded) {
                        hsbNeeded = newHSBbNeeded;
                        adjustForHSB(hsbNeeded, availR, hsbR, vpbInsets,
                                colHeadR);
                        if ((vsb != null) && !vsbNeeded
                                && (vsbPolicy != VERTICAL_SCROLLBAR_NEVER)) {

                            extentSize = viewport.toViewCoordinates(availR
                                    .getSize());
                            vsbNeeded = viewPrefSize.height > extentSize.height;

                            if (vsbNeeded) {
                                adjustForVSB(true, availR, vsbR, vpbInsets,
                                        leftToRight);
                            }
                        }
                    }
                }
                if (oldHSBNeeded != hsbNeeded || oldVSBNeeded != vsbNeeded) {
                    viewport.setBounds(availR);
                    // You could argue that we should recheck the
                    // Scrollable methods again until they stop changing,
                    // but they might never stop changing, so we stop here
                    // and don't do any additional checks.
                }
            }
        }

        /*
         * We now have the final size of the viewport: availR. Now fixup the
         * header and scrollbar widths/heights.
         */
        vsbR.height = availR.height + vpbInsets.top + vpbInsets.bottom;
        hsbR.width = availR.width + vpbInsets.left + vpbInsets.right;
        rowHeadR.height = availR.height + vpbInsets.top + vpbInsets.bottom;
        rowHeadR.y = availR.y - vpbInsets.top;
        colHeadR.width = availR.width + vpbInsets.left + vpbInsets.right;
        colHeadR.x = availR.x - vpbInsets.left;
        colHeadR.y = availR.height + vpbInsets.bottom;

        /*
         * Set the bounds of the remaining components. The scrollbars are made
         * invisible if they're not needed.
         */

        if (rowHead != null) {
            rowHead.setBounds(rowHeadR);
        }

        if (colHead != null) {
            colHead.setBounds(colHeadR);
        }

        if (vsb != null) {
            if (vsbNeeded) {
                vsb.setVisible(true);
                vsb.setBounds(vsbR);
            } else {
                vsb.setVisible(false);
            }
        }

        if (hsb != null) {
            if (hsbNeeded) {
                hsb.setVisible(true);
                hsb.setBounds(hsbR);
            } else {
                hsb.setVisible(false);
            }
        }

        if (lowerLeft != null) {
            lowerLeft.setBounds(leftToRight ? rowHeadR.x : vsbR.x, hsbR.y,
                    leftToRight ? rowHeadR.width : vsbR.width, hsbR.height);
        }

        if (lowerRight != null) {
            lowerRight.setBounds(leftToRight ? vsbR.x : rowHeadR.x, hsbR.y,
                    leftToRight ? vsbR.width : rowHeadR.width, hsbR.height);
        }

        if (upperLeft != null) {
            upperLeft.setBounds(leftToRight ? rowHeadR.x : vsbR.x, colHeadR.y,
                    leftToRight ? rowHeadR.width : vsbR.width, colHeadR.height);
        }

        if (upperRight != null) {
            upperRight.setBounds(leftToRight ? vsbR.x : rowHeadR.x, colHeadR.y,
                    leftToRight ? vsbR.width : rowHeadR.width, colHeadR.height);
        }
    }

    /**
     * Adjusts the <code>Rectangle</code> <code>available</code> based on if
     * the vertical scrollbar is needed (<code>wantsVSB</code>). The
     * location of the vsb is updated in <code>vsbR</code>, and the viewport
     * border insets (<code>vpbInsets</code>) are used to offset the vsb.
     * This is only called when <code>wantsVSB</code> has changed, eg you
     * shouldn't invoke adjustForVSB(true) twice.
     */
    private void adjustForVSB(final boolean wantsVSB,
            final Rectangle available, final Rectangle vsbR,
            final Insets vpbInsets, final boolean leftToRight) {
        int oldWidth = vsbR.width;
        if (wantsVSB) {
            int vsbWidth = Math.max(0, Math.min(vsb.getPreferredSize().width,
                    available.width));

            // available.width -= vsbWidth;
            vsbR.width = vsbWidth;

            if (leftToRight) {
                vsbR.x = available.x + available.width + vpbInsets.right;
            } else {
                vsbR.x = available.x - vpbInsets.left;
                available.x += vsbWidth;
            }
        } else {
            available.width += oldWidth;
        }
    }

    /**
     * Adjusts the <code>Rectangle</code> <code>available</code> based on if
     * the horizontal scrollbar is needed (<code>wantsHSB</code>). The
     * location of the hsb is updated in <code>hsbR</code>, and the viewport
     * border insets (<code>vpbInsets</code>) are used to offset the hsb.
     * This is only called when <code>wantsHSB</code> has changed, eg you
     * shouldn't invoked adjustForHSB(true) twice.
     */
    private void adjustForHSB(final boolean wantsHSB,
            final Rectangle available, final Rectangle hsbR,
            final Insets vpbInsets, final Rectangle colHeadR) {

        int hsbHeight = Math.max(0, Math.min(available.height, hsb
                .getPreferredSize().height));

        int oldHeight = hsbR.height;
        if (wantsHSB) {

            // available.height -= hsbHeight;
            // also adapt the y coordinate. the scroll bar is now below
            // the column header
            hsbR.y = available.y + available.height + colHeadR.height
                    + vpbInsets.bottom;
            hsbR.height = hsbHeight;
        } else {
            available.height += oldHeight;
        }
    }

    /**
     * The UI resource version of <code>Plotter2DScrollPaneLayout</code>.
     */
    public static class UIResource extends ScrollPaneLayout implements
            javax.swing.plaf.UIResource {
    }
}
