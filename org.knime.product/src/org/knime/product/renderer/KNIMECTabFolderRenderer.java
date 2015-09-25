/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Fabio Zadrozny - Bug 465711
 *******************************************************************************/
package org.knime.product.renderer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.inject.Inject;

import org.eclipse.e4.ui.css.swt.dom.CTabFolderElement;
import org.eclipse.e4.ui.css.swt.dom.CompositeElement;
import org.eclipse.e4.ui.internal.css.swt.ICTabRendering;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolderRenderer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Pattern;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;

/**
 * This class is a copy of the org.eclipse.e4.ui.workbench.renderers.swt.CTabRendering class with
 * some minor changes to remove gradient and rounding from tabs.
 *
 * @author Tobias Koetter, KNIME.com.
 */
@SuppressWarnings("restriction")
public class KNIMECTabFolderRenderer extends CTabFolderRenderer implements
ICTabRendering {
    private static final String CONTAINS_TOOLBAR = "CTabRendering.containsToolbar"; //$NON-NLS-1$

    // Constants for circle drawing
    final static int LEFT_TOP = 0;
    final static int LEFT_BOTTOM = 1;
    final static int RIGHT_TOP = 2;
    final static int RIGHT_BOTTOM = 3;

    // drop shadow constants
    final static int SIDE_DROP_WIDTH = 3;
    final static int BOTTOM_DROP_WIDTH = 4;

    // keylines
    final static int OUTER_KEYLINE = 1;
    final static int INNER_KEYLINE = 0;
    final static int TOP_KEYLINE = 0;

    // Item Constants
    static final int ITEM_TOP_MARGIN = 2;
    static final int ITEM_BOTTOM_MARGIN = 6;
    static final int ITEM_LEFT_MARGIN = 4;
    static final int ITEM_RIGHT_MARGIN = 4;
    static final int INTERNAL_SPACING = 4;

    static final String E4_TOOLBAR_ACTIVE_IMAGE = "org.eclipse.e4.renderer.toolbar_background_active_image"; //$NON-NLS-1$
    static final String E4_TOOLBAR_INACTIVE_IMAGE = "org.eclipse.e4.renderer.toolbar_background_inactive_image"; //$NON-NLS-1$

    int[] shape;

    Image shadowImage, toolbarActiveImage, toolbarInactiveImage;

    int cornerSize = 14;

    boolean shadowEnabled = true;
    Color shadowColor;
    Color outerKeyline, innerKeyline;
    Color[] activeToolbar;
    int[] activePercents;
    Color[] inactiveToolbar;
    int[] inactivePercents;
    boolean active;

    Color[] selectedTabFillColors;
    int[] selectedTabFillPercents;

    Color[] unselectedTabsColors;
    int[] unselectedTabsPercents;

    Color tabOutlineColor;

    int paddingLeft = 0, paddingRight = 0, paddingTop = 0, paddingBottom = 0;

    private CTabFolderRendererWrapper rendererWrapper;
    private CTabFolderWrapper parentWrapper;

    private Color hotUnselectedTabsColorBackground;

    @Inject
    public KNIMECTabFolderRenderer(final CTabFolder parent) {
        super(parent);
        parentWrapper = new CTabFolderWrapper(parent);
        rendererWrapper = new CTabFolderRendererWrapper(this);
    }

    @Override
    public void setUnselectedHotTabsColorBackground(final Color color) {
        this.hotUnselectedTabsColorBackground = color;
    }

    @Override
    protected Rectangle computeTrim(final int part, final int state, int x, int y,
            int width, int height) {
        boolean onBottom = parent.getTabPosition() == SWT.BOTTOM;
        int borderTop = onBottom ? INNER_KEYLINE + OUTER_KEYLINE : TOP_KEYLINE
                + OUTER_KEYLINE;
        int borderBottom = onBottom ? TOP_KEYLINE + OUTER_KEYLINE
                : INNER_KEYLINE + OUTER_KEYLINE;
        int marginWidth = parent.marginWidth;
        int marginHeight = parent.marginHeight;
        int sideDropWidth = shadowEnabled ? SIDE_DROP_WIDTH : 0;
        switch (part) {
        case PART_BODY:
            if (state == SWT.FILL) {
                x = -1 - paddingLeft;
                int tabHeight = parent.getTabHeight() + 1;
                y = onBottom ? y - paddingTop - marginHeight - borderTop
                        - (cornerSize / 4) : y - paddingTop - marginHeight
                        - tabHeight - borderTop - (cornerSize / 4);
                        width = 2 + paddingLeft + paddingRight;
                        height += paddingTop + paddingBottom;
                        height += tabHeight + (cornerSize / 4) + borderBottom
                                + borderTop;
            } else {
                x = x - marginWidth - OUTER_KEYLINE - INNER_KEYLINE
                        - sideDropWidth - (cornerSize / 2);
                width = width + 2 * OUTER_KEYLINE + 2 * INNER_KEYLINE + 2
                        * marginWidth + 2 * sideDropWidth + cornerSize;
                int tabHeight = parent.getTabHeight() + 1; // TODO: Figure out
                // what
                // to do about the
                // +1
                // TODO: Fix
                if (parent.getMinimized()) {
                    y = onBottom ? y - borderTop - 5 : y - tabHeight
                            - borderTop - 5;
                    height = borderTop + borderBottom + tabHeight;
                } else {
                    // y = tabFolder.onBottom ? y - marginHeight -
                    // highlight_margin
                    // - borderTop: y - marginHeight - highlight_header -
                    // tabHeight
                    // - borderTop;
                    y = onBottom ? y - marginHeight - borderTop
                            - (cornerSize / 4) : y - marginHeight - tabHeight
                            - borderTop - (cornerSize / 4);
                            height = height + borderBottom + borderTop + 2
                                    * marginHeight + tabHeight + cornerSize / 2
                                    + cornerSize / 4
                                    + (shadowEnabled ? BOTTOM_DROP_WIDTH : 0);
                }
            }
            break;
        case PART_HEADER:
            x = x - (INNER_KEYLINE + OUTER_KEYLINE) - sideDropWidth;
            width = width + 2 * (INNER_KEYLINE + OUTER_KEYLINE + sideDropWidth);
            break;
        case PART_BORDER:
            x = x - INNER_KEYLINE - OUTER_KEYLINE - sideDropWidth
            - (cornerSize / 4);
            width = width + 2 * (INNER_KEYLINE + OUTER_KEYLINE + sideDropWidth)
                    + cornerSize / 2;
            height = height + borderTop + borderBottom;
            y = y - borderTop;
            if (onBottom) {
                if (shadowEnabled) {
                    height += 3;
                }
            }

            break;
        default:
            if (0 <= part && part < parent.getItemCount()) {
                x = x - ITEM_LEFT_MARGIN;// - (CORNER_SIZE/2);
                width = width + ITEM_LEFT_MARGIN + ITEM_RIGHT_MARGIN + 1;
                y = y - ITEM_TOP_MARGIN;
                height = height + ITEM_TOP_MARGIN + ITEM_BOTTOM_MARGIN;
            }
            break;
        }
        return new Rectangle(x, y, width, height);
    }

    @Override
    protected Point computeSize(final int part, final int state, final GC gc, int wHint, int hHint) {
        wHint += paddingLeft + paddingRight;
        hHint += paddingTop + paddingBottom;
        if (0 <= part && part < parent.getItemCount()) {
            gc.setAdvanced(true);
            Point result = super.computeSize(part, state, gc, wHint, hHint);
            return result;
        }
        return super.computeSize(part, state, gc, wHint, hHint);
    }

    @Override
    protected void dispose() {
        if (shadowImage != null && !shadowImage.isDisposed()) {
            shadowImage.dispose();
            shadowImage = null;
        }
        super.dispose();
    }

    @Override
    protected void draw(final int part, int state, final Rectangle bounds, final GC gc) {

        switch (part) {
        case PART_BACKGROUND:
            super.draw(part, state, bounds, gc);
            this.drawCustomBackground(gc, bounds, state);
            return;
        case PART_BODY:
            this.drawTabBody(gc, bounds, state);
            return;
        case PART_HEADER:
            this.drawTabHeader(gc, bounds, state);
            return;
        default:
            if (0 <= part && part < parent.getItemCount()) {
                gc.setAdvanced(true);
                if (bounds.width == 0 || bounds.height == 0) {
                    return;
                }
                if ((state & SWT.SELECTED) != 0) {
                    drawSelectedTab(part, gc, bounds, state);
                    state &= ~SWT.BACKGROUND;
                    super.draw(part, state, bounds, gc);
                } else {
                    drawUnselectedTab(part, gc, bounds, state);
                    if ((state & SWT.HOT) == 0 && !active) {
                        gc.setAlpha(0x7f);
                        state &= ~SWT.BACKGROUND;
                        super.draw(part, state, bounds, gc);
                        gc.setAlpha(0xff);
                    } else {
                        state &= ~SWT.BACKGROUND;
                        super.draw(part, state, bounds, gc);
                    }
                }
                return;
            }
        }
        super.draw(part, state, bounds, gc);
    }

    void drawTabHeader(final GC gc, final Rectangle bounds, final int state) {
        // gc.setClipping(bounds.x, bounds.y, bounds.width,
        // parent.getTabHeight() + 1);

        boolean onBottom = parent.getTabPosition() == SWT.BOTTOM;
        int[] points = new int[1024];
        int index = 0;
        int radius = cornerSize / 2;
        int marginWidth = parent.marginWidth;
        int marginHeight = parent.marginHeight;
        int delta = INNER_KEYLINE + OUTER_KEYLINE + 2
                * (shadowEnabled ? SIDE_DROP_WIDTH : 0) + 2 * marginWidth;
        int width = bounds.width - delta;
        int height = bounds.height - INNER_KEYLINE - OUTER_KEYLINE - 2
                * marginHeight - (shadowEnabled ? BOTTOM_DROP_WIDTH : 0);
        int circX = bounds.x + delta / 2 + radius;
        int circY = bounds.y + radius;

        // Fill in background
        Region clipping = new Region();
        gc.getClipping(clipping);
        Region region = new Region();
        region.add(shape);
        region.intersect(clipping);
        gc.setClipping(region);

        int header = shadowEnabled ? onBottom ? 6 : 3 : 1; // TODO: this needs
        // to be added to
        // computeTrim for
        // HEADER
        Rectangle trim = computeTrim(PART_HEADER, state, 0, 0, 0, 0);
        trim.width = bounds.width - trim.width;

        // XXX: The magic numbers need to be cleaned up. See https://bugs.eclipse.org/425777 for details.
        trim.height = (parent.getTabHeight() + (onBottom ? 7 : 4))
                - trim.height;

        trim.x = -trim.x;
        trim.y = onBottom ? bounds.height - parent.getTabHeight() - 1 - header
                : -trim.y;
        draw(PART_BACKGROUND, SWT.NONE, trim, gc);

        gc.setClipping(clipping);
        clipping.dispose();
        region.dispose();

        int[] ltt = drawCircle(circX + 1, circY + 1, radius, LEFT_TOP);
        System.arraycopy(ltt, 0, points, index, ltt.length);
        index += ltt.length;

        int[] lbb = drawCircle(circX + 1, circY + height - (radius * 2) - 2,
                radius, LEFT_BOTTOM);
        System.arraycopy(lbb, 0, points, index, lbb.length);
        index += lbb.length;

        int[] rb = drawCircle(circX + width - (radius * 2) - 2, circY + height
                - (radius * 2) - 2, radius, RIGHT_BOTTOM);
        System.arraycopy(rb, 0, points, index, rb.length);
        index += rb.length;

        int[] rt = drawCircle(circX + width - (radius * 2) - 2, circY + 1,
                radius, RIGHT_TOP);
        System.arraycopy(rt, 0, points, index, rt.length);
        index += rt.length;
        points[index++] = points[0];
        points[index++] = points[1];

        int[] tempPoints = new int[index];
        System.arraycopy(points, 0, tempPoints, 0, index);

        if (outerKeyline == null) {
            outerKeyline = gc.getDevice().getSystemColor(SWT.COLOR_BLACK);
        }
        gc.setForeground(outerKeyline);
        gc.drawPolyline(shape);
    }

    void drawTabBody(final GC gc, final Rectangle bounds, final int state) {
        int[] points = new int[1024];
        int index = 0;
        int radius = cornerSize / 2;
        int marginWidth = parent.marginWidth;
        int marginHeight = parent.marginHeight;
        int delta = INNER_KEYLINE + OUTER_KEYLINE + 2
                * (shadowEnabled ? SIDE_DROP_WIDTH : 0) + 2 * marginWidth;
        int width = bounds.width - delta;
        int height = Math.max(parent.getTabHeight() + INNER_KEYLINE
                + OUTER_KEYLINE + (shadowEnabled ? BOTTOM_DROP_WIDTH : 0),
                bounds.height - INNER_KEYLINE - OUTER_KEYLINE - 2
                * marginHeight
                - (shadowEnabled ? BOTTOM_DROP_WIDTH : 0));

        int circX = bounds.x + delta / 2 + radius;
        int circY = bounds.y + radius;

        // Body
        index = 0;
        int[] ltt = drawCircle(circX, circY, radius, LEFT_TOP);
        System.arraycopy(ltt, 0, points, index, ltt.length);
        index += ltt.length;

        int[] lbb = drawCircle(circX, circY + height - (radius * 2), radius,
                LEFT_BOTTOM);
        System.arraycopy(lbb, 0, points, index, lbb.length);
        index += lbb.length;

        int[] rb = drawCircle(circX + width - (radius * 2), circY + height
                - (radius * 2), radius, RIGHT_BOTTOM);
        System.arraycopy(rb, 0, points, index, rb.length);
        index += rb.length;

        int[] rt = drawCircle(circX + width - (radius * 2), circY, radius,
                RIGHT_TOP);
        System.arraycopy(rt, 0, points, index, rt.length);
        index += rt.length;
        points[index++] = circX;
        points[index++] = circY - radius;

        int[] tempPoints = new int[index];
        System.arraycopy(points, 0, tempPoints, 0, index);
        gc.fillPolygon(tempPoints);

        // Fill in parent background for non-rectangular shape
        Region r = new Region();
        r.add(bounds);
        r.subtract(tempPoints);
        gc.setBackground(parent.getParent().getBackground());
        Display display = parent.getDisplay();
        Region clipping = new Region();
        gc.getClipping(clipping);
        r.intersect(clipping);
        gc.setClipping(r);
        Rectangle mappedBounds = display
                .map(parent, parent.getParent(), bounds);
        parent.getParent().drawBackground(gc, bounds.x, bounds.y, bounds.width,
                bounds.height, mappedBounds.x, mappedBounds.y);

        // Shadow
        if (shadowEnabled) {
            drawShadow(display, bounds, gc);
        }

        gc.setClipping(clipping);
        clipping.dispose();
        r.dispose();

        // Remember for use in header drawing
        shape = tempPoints;
    }

    void drawSelectedTab(final int itemIndex, final GC gc, final Rectangle bounds, final int state) {
        if (parent.getSingle() && parent.getItem(itemIndex).isShowing()) {
            return;
        }

        boolean onBottom = parent.getTabPosition() == SWT.BOTTOM;
        int header = shadowEnabled ? 2 : 0;
        int width = bounds.width;
        int[] points = new int[1024];
        int index = 0;
//        int radius = cornerSize / 2;
//        int circX = bounds.x + radius;
//        int circY = onBottom ? bounds.y + bounds.height + 1 - header - radius
//                : bounds.y - 1 + radius;
        int selectionX1, selectionY1, selectionX2, selectionY2;
        int bottomY = onBottom ? bounds.y - header : bounds.y + bounds.height;
        if (itemIndex == 0
                && bounds.x == -computeTrim(CTabFolderRenderer.PART_HEADER,
                        SWT.NONE, 0, 0, 0, 0).x) {
//            circX -= 1;
//            points[index++] = circX - radius;
            points[index++] = bounds.x;
            points[index++] = bottomY;

            //            points[index++] = selectionX1 = circX - radius;
            points[index++] = selectionX1 = bounds.x;
            points[index++] = selectionY1 = bottomY;
        } else {
            if (active) {
                points[index++] = shadowEnabled ? SIDE_DROP_WIDTH : 0
                        + INNER_KEYLINE + OUTER_KEYLINE;
                points[index++] = bottomY;
            }
            points[index++] = selectionX1 = bounds.x;
            points[index++] = selectionY1 = bottomY;
        }

        points[index++] = bounds.x;
        points[index++] = bounds.y;

        points[index++] = bounds.x + bounds.width;
        points[index++] = bounds.y;

        int startX = bounds.x, endX = bounds.x + bounds.width;
//        int startX = -1, endX = -1;
//        if (!onBottom) {
//            int[] ltt = drawCircle(circX, circY, radius, LEFT_TOP);
//            startX = ltt[6];
//            for (int i = 0; i < ltt.length / 2; i += 2) {
//                int tmp = ltt[i];
//                ltt[i] = ltt[ltt.length - i - 2];
//                ltt[ltt.length - i - 2] = tmp;
//                tmp = ltt[i + 1];
//                ltt[i + 1] = ltt[ltt.length - i - 1];
//                ltt[ltt.length - i - 1] = tmp;
//            }
//            System.arraycopy(ltt, 0, points, index, ltt.length);
//            index += ltt.length;

//            int[] rt = drawCircle(circX + width - (radius * 2), circY, radius,
//                    RIGHT_TOP);
//            endX = rt[rt.length - 4];
//            for (int i = 0; i < rt.length / 2; i += 2) {
//                int tmp = rt[i];
//                rt[i] = rt[rt.length - i - 2];
//                rt[rt.length - i - 2] = tmp;
//                tmp = rt[i + 1];
//                rt[i + 1] = rt[rt.length - i - 1];
//                rt[rt.length - i - 1] = tmp;
//            }
//            System.arraycopy(rt, 0, points, index, rt.length);
//            index += rt.length;
//
            points[index++] = selectionX2 = bounds.width + bounds.x;
            points[index++] = selectionY2 = bounds.y + bounds.height;
//        } else {
//            int[] ltt = drawCircle(circX, circY, radius, LEFT_BOTTOM);
//            startX = ltt[6];
//            System.arraycopy(ltt, 0, points, index, ltt.length);
//            index += ltt.length;
//
//            int[] rt = drawCircle(circX + width - (radius * 2), circY, radius,
//                    RIGHT_BOTTOM);
//            endX = rt[rt.length - 4];
//            System.arraycopy(rt, 0, points, index, rt.length);
//            index += rt.length;
//
//            points[index++] = selectionX2 = bounds.width + circX - radius;
//            points[index++] = selectionY2 = bottomY;
//        }

        if (active) {
            points[index++] = parent.getSize().x
                    - (shadowEnabled ? SIDE_DROP_WIDTH : 0 + INNER_KEYLINE
                            + OUTER_KEYLINE);
            points[index++] = bottomY;
        }
        gc.setClipping(0, onBottom ? bounds.y - header : bounds.y,
                parent.getSize().x
                - (shadowEnabled ? SIDE_DROP_WIDTH : 0 + INNER_KEYLINE
                        + OUTER_KEYLINE), bounds.y + bounds.height);// bounds.height
        // +
        // 4);

        Pattern backgroundPattern = null;
        if (selectedTabFillColors == null) {
            setSelectedTabFill(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
        }
//        if (selectedTabFillColors.length == 1) {
            gc.setBackground(selectedTabFillColors[0]);
            gc.setForeground(selectedTabFillColors[0]);
//        } else if (!onBottom && selectedTabFillColors.length == 2) {
//            // for now we support the 2-colors gradient for selected tab
//            backgroundPattern = new Pattern(gc.getDevice(), 0, 0, 0,
//                    bounds.height + 1, selectedTabFillColors[0],
//                    selectedTabFillColors[1]);
//            gc.setBackgroundPattern(backgroundPattern);
//            gc.setForeground(selectedTabFillColors[1]);
//        }
        int[] tmpPoints = new int[index];
        System.arraycopy(points, 0, tmpPoints, 0, index);
        gc.fillPolygon(tmpPoints);
        gc.drawLine(selectionX1, selectionY1, selectionX2, selectionY2);
        if (tabOutlineColor == null) {
            tabOutlineColor = gc.getDevice().getSystemColor(SWT.COLOR_BLACK);
        }
        gc.setForeground(tabOutlineColor);
        Color gradientLineTop = null;
        Pattern foregroundPattern = null;
        if (!active && !onBottom) {
            RGB blendColor = gc.getDevice()
                    .getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW).getRGB();
            RGB topGradient = blend(blendColor, tabOutlineColor.getRGB(), 40);
            gradientLineTop = new Color(gc.getDevice(), topGradient);
            foregroundPattern = new Pattern(gc.getDevice(), 0, 0, 0,
                    bounds.height + 1, gradientLineTop, gc.getDevice()
                    .getSystemColor(SWT.COLOR_WHITE));
            gc.setForegroundPattern(foregroundPattern);
        }
        gc.drawPolyline(tmpPoints);
        Rectangle rect = null;
        gc.setClipping(rect);

        if (active) {
            if (outerKeyline == null) {
                outerKeyline = gc.getDevice().getSystemColor(SWT.COLOR_RED);
            }
            gc.setForeground(outerKeyline);
            gc.drawPolyline(shape);
        } else {
            if (!onBottom) {
                gc.drawLine(startX, 0, endX, 0);
            }
        }

        if (backgroundPattern != null) {
            backgroundPattern.dispose();
        }
        if (gradientLineTop != null) {
            gradientLineTop.dispose();
        }
        if (foregroundPattern != null) {
            foregroundPattern.dispose();
        }
    }

    void drawUnselectedTab(final int itemIndex, final GC gc, final Rectangle bounds, final int state) {
        if ((state & SWT.HOT) != 0) {
            int header = shadowEnabled ? 2 : 0;
            int width = bounds.width;
            boolean onBottom = parent.getTabPosition() == SWT.BOTTOM;
            int[] points = new int[1024];
            int[] inactive = new int[8];
            int index = 0, inactive_index = 0;
            int radius = cornerSize / 2;
            int circX = bounds.x + radius;
            int circY = onBottom ? bounds.y + bounds.height + 1 - header
                    - radius : bounds.y - 1 + radius;
            int bottomY = onBottom ? bounds.y - header : bounds.y
                    + bounds.height;

            int leftIndex = circX;
            if (itemIndex == 0) {
                if (parent.getSelectionIndex() != 0) {
                    leftIndex -= 1;
                }
                points[index++] = leftIndex - radius;
                points[index++] = bottomY;
            } else {
                points[index++] = bounds.x;
                points[index++] = bottomY;
            }

            if (!active) {
                System.arraycopy(points, 0, inactive, 0, index);
                inactive_index += 2;
            }

            int rightIndex = circX - 1;
            if (!onBottom) {
                int[] ltt = drawCircle(leftIndex, circY, radius, LEFT_TOP);
                for (int i = 0; i < ltt.length / 2; i += 2) {
                    int tmp = ltt[i];
                    ltt[i] = ltt[ltt.length - i - 2];
                    ltt[ltt.length - i - 2] = tmp;
                    tmp = ltt[i + 1];
                    ltt[i + 1] = ltt[ltt.length - i - 1];
                    ltt[ltt.length - i - 1] = tmp;
                }
                System.arraycopy(ltt, 0, points, index, ltt.length);
                index += ltt.length;

                if (!active) {
                    System.arraycopy(ltt, 0, inactive, inactive_index, 2);
                    inactive_index += 2;
                }

                int[] rt = drawCircle(rightIndex + width - (radius * 2), circY,
                        radius, RIGHT_TOP);
                for (int i = 0; i < rt.length / 2; i += 2) {
                    int tmp = rt[i];
                    rt[i] = rt[rt.length - i - 2];
                    rt[rt.length - i - 2] = tmp;
                    tmp = rt[i + 1];
                    rt[i + 1] = rt[rt.length - i - 1];
                    rt[rt.length - i - 1] = tmp;
                }
                System.arraycopy(rt, 0, points, index, rt.length);
                index += rt.length;
                if (!active) {
                    System.arraycopy(rt, rt.length - 4, inactive,
                            inactive_index, 2);
                    inactive[inactive_index] -= 1;
                    inactive_index += 2;
                }
            } else {
                int[] ltt = drawCircle(leftIndex, circY, radius, LEFT_BOTTOM);
                System.arraycopy(ltt, 0, points, index, ltt.length);
                index += ltt.length;

                if (!active) {
                    System.arraycopy(ltt, 0, inactive, inactive_index, 2);
                    inactive_index += 2;
                }

                int[] rt = drawCircle(rightIndex + width - (radius * 2), circY,
                        radius, RIGHT_BOTTOM);
                System.arraycopy(rt, 0, points, index, rt.length);
                index += rt.length;
                if (!active) {
                    System.arraycopy(rt, rt.length - 4, inactive,
                            inactive_index, 2);
                    inactive[inactive_index] -= 1;
                    inactive_index += 2;
                }

            }

            points[index++] = bounds.width + rightIndex - radius;
            points[index++] = bottomY;

            if (!active) {
                System.arraycopy(points, index - 2, inactive, inactive_index, 2);
                inactive[inactive_index] -= 1;
                inactive_index += 2;
            }
            gc.setClipping(points[0], onBottom ? bounds.y - header : bounds.y,
                    parent.getSize().x
                    - (shadowEnabled ? SIDE_DROP_WIDTH : 0
                            + INNER_KEYLINE + OUTER_KEYLINE), bounds.y
                            + bounds.height);

            Color color = hotUnselectedTabsColorBackground;
            if (color == null) {
                // Fallback: if color was not set, use white for highlighting
                // hot tab.
                color = gc.getDevice().getSystemColor(SWT.COLOR_WHITE);
            }
            gc.setBackground(color);
            int[] tmpPoints = new int[index];
            System.arraycopy(points, 0, tmpPoints, 0, index);
            gc.fillPolygon(tmpPoints);
            Color tempBorder = new Color(gc.getDevice(), 182, 188, 204);
            gc.setForeground(tempBorder);
            tempBorder.dispose();
            if (active) {
                gc.drawPolyline(tmpPoints);
            } else {
                gc.drawLine(inactive[0], inactive[1], inactive[2], inactive[3]);
                gc.drawLine(inactive[4], inactive[5], inactive[6], inactive[7]);
            }

            Rectangle rect = null;
            gc.setClipping(rect);

            if (outerKeyline == null)
             {
                outerKeyline = gc.getDevice().getSystemColor(SWT.COLOR_BLACK);
            // gc.setForeground(outerKeyline);
            // gc.drawPolyline(shape);
            }
        }
    }

    static int[] drawCircle(final int xC, final int yC, final int r, final int circlePart) {
        int x = 0, y = r, u = 1, v = 2 * r - 1, e = 0;
        int[] points = new int[1024];
        int[] pointsMirror = new int[1024];
        int loop = 0;
        int loopMirror = 0;
        while (x < y) {
            if (circlePart == RIGHT_BOTTOM) {
                points[loop++] = xC + x;
                points[loop++] = yC + y;
            }
            if (circlePart == RIGHT_TOP) {
                points[loop++] = xC + y;
                points[loop++] = yC - x;
            }
            if (circlePart == LEFT_TOP) {
                points[loop++] = xC - x;
                points[loop++] = yC - y;
            }
            if (circlePart == LEFT_BOTTOM) {
                points[loop++] = xC - y;
                points[loop++] = yC + x;
            }
            x++;
            e += u;
            u += 2;
            if (v < 2 * e) {
                y--;
                e -= v;
                v -= 2;
            }
            if (x > y) {
                break;
            }
            if (circlePart == RIGHT_BOTTOM) {
                pointsMirror[loopMirror++] = xC + y;
                pointsMirror[loopMirror++] = yC + x;
            }
            if (circlePart == RIGHT_TOP) {
                pointsMirror[loopMirror++] = xC + x;
                pointsMirror[loopMirror++] = yC - y;
            }
            if (circlePart == LEFT_TOP) {
                pointsMirror[loopMirror++] = xC - y;
                pointsMirror[loopMirror++] = yC - x;
            }
            if (circlePart == LEFT_BOTTOM) {
                pointsMirror[loopMirror++] = xC - x;
                pointsMirror[loopMirror++] = yC + y;
            }
            // grow?
            if ((loop + 1) > points.length) {
                int length = points.length * 2;
                int[] newPointTable = new int[length];
                int[] newPointTableMirror = new int[length];
                System.arraycopy(points, 0, newPointTable, 0, points.length);
                points = newPointTable;
                System.arraycopy(pointsMirror, 0, newPointTableMirror, 0,
                        pointsMirror.length);
                pointsMirror = newPointTableMirror;
            }
        }
        int[] finalArray = new int[loop + loopMirror];
        System.arraycopy(points, 0, finalArray, 0, loop);
        for (int i = loopMirror - 1, j = loop; i > 0; i = i - 2, j = j + 2) {
            int tempY = pointsMirror[i];
            int tempX = pointsMirror[i - 1];
            finalArray[j] = tempX;
            finalArray[j + 1] = tempY;
        }
        return finalArray;
    }

    static RGB blend(final RGB c1, final RGB c2, final int ratio) {
        int r = blend(c1.red, c2.red, ratio);
        int g = blend(c1.green, c2.green, ratio);
        int b = blend(c1.blue, c2.blue, ratio);
        return new RGB(r, g, b);
    }

    static int blend(final int v1, final int v2, final int ratio) {
        int b = (ratio * v1 + (100 - ratio) * v2) / 100;
        return Math.min(255, b);
    }

    void drawShadow(final Display display, final Rectangle bounds, final GC gc) {
        if (shadowImage == null) {
            createShadow(display);
        }
        int x = bounds.x;
        int y = bounds.y;
        int SIZE = shadowImage.getBounds().width / 3;

        int height = Math.max(bounds.height, SIZE * 2);
        int width = Math.max(bounds.width, SIZE * 2);
        // top left
        gc.drawImage(shadowImage, 0, 0, SIZE, SIZE, 2, 10, SIZE, 20);
        int fillHeight = height - SIZE * 2;
        int fillWidth = width + 5 - SIZE * 2;

        int xFill = 0;
        for (int i = SIZE; i < fillHeight; i += SIZE) {
            xFill = i;
            gc.drawImage(shadowImage, 0, SIZE, SIZE, SIZE, 2, i, SIZE, SIZE);
        }

        // Pad the rest of the shadow
        gc.drawImage(shadowImage, 0, SIZE, SIZE, fillHeight - xFill, 2, xFill
                + SIZE, SIZE, fillHeight - xFill);

        // bl
        gc.drawImage(shadowImage, 0, 40, 20, 20, 2, y + height - SIZE, 20, 20);

        int yFill = 0;
        for (int i = SIZE; i <= fillWidth; i += SIZE) {
            yFill = i;
            gc.drawImage(shadowImage, SIZE, SIZE * 2, SIZE, SIZE, i, y + height
                    - SIZE, SIZE, SIZE);
        }
        // Pad the rest of the shadow
        gc.drawImage(shadowImage, SIZE, SIZE * 2, fillWidth - yFill, SIZE,
                yFill + SIZE, y + height - SIZE, fillWidth - yFill, SIZE);

        // br
        gc.drawImage(shadowImage, SIZE * 2, SIZE * 2, SIZE, SIZE, x + width
                - SIZE - 1, y + height - SIZE, SIZE, SIZE);

        // tr
        gc.drawImage(shadowImage, (SIZE * 2), 0, SIZE, SIZE, x + width - SIZE
                - 1, 10, SIZE, SIZE);

        xFill = 0;
        for (int i = SIZE; i < fillHeight; i += SIZE) {
            xFill = i;
            gc.drawImage(shadowImage, SIZE * 2, SIZE, SIZE, SIZE, x + width
                    - SIZE - 1, i, SIZE, SIZE);
        }

        // Pad the rest of the shadow
        gc.drawImage(shadowImage, SIZE * 2, SIZE, SIZE, fillHeight - xFill, x
                + width - SIZE - 1, xFill + SIZE, SIZE, fillHeight - xFill);
    }

    void createShadow(final Display display) {
        if (shadowImage != null) {
            shadowImage.dispose();
            shadowImage = null;
        }
        ImageData data = new ImageData(60, 60, 32, new PaletteData(0xFF0000,
                0xFF00, 0xFF));
        Image tmpImage = shadowImage = new Image(display, data);
        GC gc = new GC(tmpImage);
        if (shadowColor == null) {
            shadowColor = gc.getDevice().getSystemColor(SWT.COLOR_GRAY);
        }
        gc.setBackground(shadowColor);
        drawTabBody(gc, new Rectangle(0, 0, 60, 60), SWT.None);
        ImageData blured = blur(tmpImage, 5, 25);
        shadowImage = new Image(display, blured);
        tmpImage.dispose();
    }

    public ImageData blur(final Image src, final int radius, final int sigma) {
        float[] kernel = create1DKernel(radius, sigma);

        ImageData imgPixels = src.getImageData();
        int width = imgPixels.width;
        int height = imgPixels.height;

        int[] inPixels = new int[width * height];
        int[] outPixels = new int[width * height];
        int offset = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                RGB rgb = imgPixels.palette.getRGB(imgPixels.getPixel(x, y));
                if (rgb.red == 255 && rgb.green == 255 && rgb.blue == 255) {
                    inPixels[offset] = (rgb.red << 16) | (rgb.green << 8)
                            | rgb.blue;
                } else {
                    inPixels[offset] = (imgPixels.getAlpha(x, y) << 24)
                            | (rgb.red << 16) | (rgb.green << 8) | rgb.blue;
                }
                offset++;
            }
        }

        convolve(kernel, inPixels, outPixels, width, height, true);
        convolve(kernel, outPixels, inPixels, height, width, true);

        ImageData dst = new ImageData(imgPixels.width, imgPixels.height, 24,
                new PaletteData(0xff0000, 0xff00, 0xff));

        dst.setPixels(0, 0, inPixels.length, inPixels, 0);
        offset = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (inPixels[offset] == -1) {
                    dst.setAlpha(x, y, 0);
                } else {
                    int a = (inPixels[offset] >> 24) & 0xff;
                    // if (a < 150) a = 0;
                    dst.setAlpha(x, y, a);
                }
                offset++;
            }
        }
        return dst;
    }

    private void convolve(final float[] kernel, final int[] inPixels, final int[] outPixels,
            final int width, final int height, final boolean alpha) {
        int kernelWidth = kernel.length;
        int kernelMid = kernelWidth / 2;
        for (int y = 0; y < height; y++) {
            int index = y;
            int currentLine = y * width;
            for (int x = 0; x < width; x++) {
                // do point
                float a = 0, r = 0, g = 0, b = 0;
                for (int k = -kernelMid; k <= kernelMid; k++) {
                    float val = kernel[k + kernelMid];
                    int xcoord = x + k;
                    if (xcoord < 0) {
                        xcoord = 0;
                    }
                    if (xcoord >= width) {
                        xcoord = width - 1;
                    }
                    int pixel = inPixels[currentLine + xcoord];
                    // float alp = ((pixel >> 24) & 0xff);
                    a += val * ((pixel >> 24) & 0xff);
                    r += val * (((pixel >> 16) & 0xff));
                    g += val * (((pixel >> 8) & 0xff));
                    b += val * (((pixel) & 0xff));
                }
                int ia = alpha ? clamp((int) (a + 0.5)) : 0xff;
                int ir = clamp((int) (r + 0.5));
                int ig = clamp((int) (g + 0.5));
                int ib = clamp((int) (b + 0.5));
                outPixels[index] = (ia << 24) | (ir << 16) | (ig << 8) | ib;
                index += height;
            }
        }

    }

    private int clamp(final int value) {
        if (value > 255) {
            return 255;
        }
        if (value < 0) {
            return 0;
        }
        return value;
    }

    private float[] create1DKernel(final int radius, final int sigma) {
        // guideline: 3*sigma should be the radius
        int size = radius * 2 + 1;
        float[] kernel = new float[size];
        int radiusSquare = radius * radius;
        float sigmaSquare = 2 * sigma * sigma;
        float piSigma = 2 * (float) Math.PI * sigma;
        float sqrtSigmaPi2 = (float) Math.sqrt(piSigma);
        int start = size / 2;
        int index = 0;
        float total = 0;
        for (int i = -start; i <= start; i++) {
            float d = i * i;
            if (d > radiusSquare) {
                kernel[index] = 0;
            } else {
                kernel[index] = (float) Math.exp(-(d) / sigmaSquare)
                        / sqrtSigmaPi2;
            }
            total += kernel[index];
            index++;
        }
        for (int i = 0; i < size; i++) {
            kernel[i] /= total;
        }
        return kernel;
    }

    public Rectangle getPadding() {
        return new Rectangle(paddingTop, paddingRight, paddingBottom,
                paddingLeft);
    }

    public void setPadding(final int paddingLeft, final int paddingRight, final int paddingTop,
            final int paddingBottom) {
        this.paddingLeft = paddingLeft;
        this.paddingRight = paddingRight;
        this.paddingTop = paddingTop;
        this.paddingBottom = paddingBottom;
        parent.redraw();
    }

    @Override
    public void setCornerRadius(final int radius) {
        cornerSize = radius;
        parent.redraw();
    }

    @Override
    public void setShadowVisible(final boolean visible) {
        this.shadowEnabled = visible;
        parent.redraw();
    }

    @Override
    public void setShadowColor(final Color color) {
        this.shadowColor = color;
        createShadow(parent.getDisplay());
        parent.redraw();
    }

    @Override
    public void setOuterKeyline(final Color color) {
        this.outerKeyline = color;
        // TODO: HACK! Should be set based on pseudo-state.
        if (color != null) {
            setActive(!(color.getRed() == 255 && color.getGreen() == 255 && color
                    .getBlue() == 255));
        }
        parent.redraw();
    }

    @Override
    public void setSelectedTabFill(final Color color) {
        setSelectedTabFill(new Color[] { color }, new int[] { 100 });
    }

    @Override
    public void setSelectedTabFill(final Color[] colors, final int[] percents) {
        selectedTabFillColors = colors;
        selectedTabFillPercents = percents;
        parent.redraw();
    }

    @Override
    public void setUnselectedTabsColor(final Color color) {
        setUnselectedTabsColor(new Color[] { color }, new int[] { 100 });
    }

    @Override
    public void setUnselectedTabsColor(final Color[] colors, final int[] percents) {
        unselectedTabsColors = colors;
        unselectedTabsPercents = percents;
        parent.redraw();
    }

    @Override
    public void setTabOutline(final Color color) {
        this.tabOutlineColor = color;
        parent.redraw();
    }

    @Override
    public void setInnerKeyline(final Color color) {
        this.innerKeyline = color;
        parent.redraw();
    }

    public void setActiveToolbarGradient(final Color[] color, final int[] percents) {
        activeToolbar = color;
        activePercents = percents;
    }

    public void setInactiveToolbarGradient(final Color[] color, final int[] percents) {
        inactiveToolbar = color;
        inactivePercents = percents;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    private void drawCustomBackground(final GC gc, final Rectangle bounds, final int state) {
        boolean selected = (state & SWT.SELECTED) != 0;
        Color defaultBackground = selected ? parent.getSelectionBackground()
                : parent.getBackground();
        boolean vertical = selected ? parentWrapper
                .isSelectionGradientVertical() : parentWrapper
                .isGradientVertical();
                Rectangle partHeaderBounds = computeTrim(PART_HEADER, state, bounds.x,
                        bounds.y, bounds.width, bounds.height);

                drawUnselectedTabBackground(gc, partHeaderBounds, state, vertical,
                        defaultBackground);
                drawTabBackground(gc, partHeaderBounds, state, vertical,
                        defaultBackground);
                drawChildrenBackground(partHeaderBounds);
    }

    private void drawUnselectedTabBackground(final GC gc, final Rectangle partHeaderBounds,
            final int state, final boolean vertical, final Color defaultBackground) {
        if (unselectedTabsColors == null) {
            boolean selected = (state & SWT.SELECTED) != 0;
            unselectedTabsColors = selected ? parentWrapper
                    .getSelectionGradientColors() : parentWrapper
                    .getGradientColors();
                    unselectedTabsPercents = selected ? parentWrapper
                            .getSelectionGradientPercents() :
                                parentWrapper.getGradientPercents();
        }
        if (unselectedTabsColors == null) {
            unselectedTabsColors = new Color[] { gc.getDevice().getSystemColor(
                    SWT.COLOR_WHITE) };
            unselectedTabsPercents = new int[] { 100 };
        }

        rendererWrapper.drawBackground(gc, partHeaderBounds.x,
                partHeaderBounds.y - 1, partHeaderBounds.width,
                partHeaderBounds.height, defaultBackground,
                unselectedTabsColors, unselectedTabsPercents, vertical);
    }

    private void drawTabBackground(final GC gc, final Rectangle partHeaderBounds,
            final int state, final boolean vertical, final Color defaultBackground) {
        Color[] colors = selectedTabFillColors;
        int[] percents = selectedTabFillPercents;

        if (colors != null && colors.length == 2) {
            colors = new Color[] { colors[1], colors[1] };
        }
        if (colors == null) {
            boolean selected = (state & SWT.SELECTED) != 0;
            colors = selected ? parentWrapper.getSelectionGradientColors() :
                parentWrapper.getGradientColors();
            percents = selected ? parentWrapper.getSelectionGradientPercents() :
                parentWrapper.getGradientPercents();
        }
        if (colors == null) {
            colors = new Color[] { gc.getDevice().getSystemColor(SWT.COLOR_WHITE) };
            percents = new int[] { 100 };
        }
        rendererWrapper.drawBackground(gc, partHeaderBounds.x,  partHeaderBounds.height - 1, partHeaderBounds.width,
                parent.getBounds().height, defaultBackground, colors, percents,
                vertical);
    }

    // Workaround for the bug 433276. Remove it when the bug gets fixed
    private void drawChildrenBackground(final Rectangle partHeaderBounds) {
        for (Control control : parent.getChildren()) {
            if (!CompositeElement.hasBackgroundOverriddenByCSS(control)
                    && containsToolbar(control)) {
                drawChildBackground((Composite) control, partHeaderBounds);
            }
        }
    }

    private boolean containsToolbar(final Control control) {
        if (control.getData(CONTAINS_TOOLBAR) != null) {
            return true;
        }

        if (control instanceof Composite) {
            for (Control child : ((Composite) control).getChildren()) {
                if (child instanceof ToolBar) {
                    control.setData(CONTAINS_TOOLBAR, true);
                    return true;
                }
            }
        }
        return false;
    }

    private void drawChildBackground(final Composite composite,
            final Rectangle partHeaderBounds) {
        Rectangle rec = composite.getBounds();
        Color background = null;
        boolean partOfHeader = rec.y >= partHeaderBounds.y
                && rec.y < partHeaderBounds.height;

        if (!partOfHeader && selectedTabFillColors != null) {
            background = selectedTabFillColors.length == 2 ? selectedTabFillColors[1]
                    : selectedTabFillColors[0];
        }

        CTabFolderElement.setBackgroundOverriddenDuringRenderering(composite, background);
    }

    private static class CTabFolderRendererWrapper extends
    ReflectionSupport<CTabFolderRenderer> {
        private Method drawBackgroundMethod;

        public CTabFolderRendererWrapper(final CTabFolderRenderer instance) {
            super(instance);
        }

        public void drawBackground(final GC gc, final int x, final int y, final int width, final int height,
                final Color defaultBackground, final Color[] colors, final int[] percents,
                final boolean vertical) {
            if (drawBackgroundMethod == null) {
                drawBackgroundMethod = getMethod("drawBackground", //$NON-NLS-1$
                        new Class<?>[] { GC.class, int[].class, int.class,
                        int.class, int.class, int.class, Color.class,
                        Image.class, Color[].class, int[].class,
                        boolean.class });
            }
            executeMethod(drawBackgroundMethod, new Object[] { gc, null, x, y,
                    width, height, defaultBackground, null, colors, percents,
                    vertical });
        }
    }

    private static class CTabFolderWrapper extends
    ReflectionSupport<CTabFolder> {
        private Field selectionGradientVerticalField;

        private Field gradientVerticalField;

        private Field selectionGradientColorsField;

        private Field selectionGradientPercentsField;

        private Field gradientColorsField;

        private Field gradientPercentsField;

        public CTabFolderWrapper(final CTabFolder instance) {
            super(instance);
        }

        public boolean isSelectionGradientVertical() {
            if (selectionGradientVerticalField == null) {
                selectionGradientVerticalField = getField("selectionGradientVertical"); //$NON-NLS-1$
            }
            Boolean result = (Boolean) getFieldValue(selectionGradientVerticalField);
            return result != null ? result : true;
        }

        public boolean isGradientVertical() {
            if (gradientVerticalField == null) {
                gradientVerticalField = getField("gradientVertical"); //$NON-NLS-1$
            }
            Boolean result = (Boolean) getFieldValue(gradientVerticalField);
            return result != null ? result : true;
        }

        public Color[] getSelectionGradientColors() {
            if (selectionGradientColorsField == null) {
                selectionGradientColorsField = getField("selectionGradientColorsField"); //$NON-NLS-1$
            }
            return (Color[]) getFieldValue(selectionGradientColorsField);
        }

        public int[] getSelectionGradientPercents() {
            if (selectionGradientPercentsField == null) {
                selectionGradientPercentsField = getField("selectionGradientPercents"); //$NON-NLS-1$
            }
            return (int[]) getFieldValue(selectionGradientPercentsField);
        }

        public Color[] getGradientColors() {
            if (gradientColorsField == null) {
                gradientColorsField = getField("gradientColors"); //$NON-NLS-1$
            }
            return (Color[]) getFieldValue(gradientColorsField);
        }

        public int[] getGradientPercents() {
            if (gradientPercentsField == null) {
                gradientPercentsField = getField("gradientPercents"); //$NON-NLS-1$
            }
            return (int[]) getFieldValue(gradientPercentsField);
        }
    }

    private static class ReflectionSupport<T> {
        private T instance;

        public ReflectionSupport(final T instance) {
            this.instance = instance;
        }

        protected Object getFieldValue(final Field field) {
            Object value = null;
            if (field != null) {
                boolean accessible = field.isAccessible();
                try {
                    field.setAccessible(true);
                    value = field.get(instance);
                } catch (Exception exc) {
                    // do nothing
                } finally {
                    field.setAccessible(accessible);
                }
            }
            return value;
        }

        protected Field getField(final String name) {
            Class<?> cls = instance.getClass();
            while (!cls.equals(Object.class)) {
                try {
                    return cls.getDeclaredField(name);
                } catch (Exception exc) {
                    cls = cls.getSuperclass();
                }
            }
            return null;
        }

        protected Object executeMethod(final Method method, final Object... params) {
            Object value = null;
            if (method != null) {
                boolean accessible = method.isAccessible();
                try {
                    method.setAccessible(true);
                    value = method.invoke(instance, params);
                } catch (Exception exc) {
                    // do nothing
                } finally {
                    method.setAccessible(accessible);
                }
            }
            return value;
        }

        protected Method getMethod(final String name, final Class<?>... params) {
            Class<?> cls = instance.getClass();
            while (!cls.equals(Object.class)) {
                try {
                    return cls.getDeclaredMethod(name, params);
                } catch (Exception exc) {
                    cls = cls.getSuperclass();
                }
            }
            return null;
        }
    }
}
