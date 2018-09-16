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
 *   Sep 24, 2018 (loki): created
 */
package org.knime.workbench.core;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Scrollable;

/**
 * This layout performs the same spatial layout as <code>org.eclipse.swt.layout.FillLayout</code> but allowing for
 * components to register themselves as items to avoid being laid out (and thereby also, having other components in the
 * layout from taking into account their spatial consumption;) this is convenient in performing absolute positioning of
 * components.
 *
 * Portions of this code are taken from <code>FillLayout</code>.
 *
 * @author loki der quaeler
 * @see org.eclipse.swt.layout.FillLayout
 */
public class LayoutExemptingLayout extends Layout {
    private static final String EXEMPTING_KEY = "LdEqL!";

    /**
     * <code>Control</code> instances which wish to be exempt from the layout done by their parent container (whose
     * layout is an instance of this class) should be passed as the parameter value to this method.
     *
     * @param c a <code>Control</code> whose spatial location will be untouched during the layout governed by instances
     *            of this layout class.
     */
    public static void exemptControlFromLayout(final Control c) {
        c.setData(EXEMPTING_KEY, new Object());
    }

    private static boolean controlIsLayoutExempt(final Control c) {
        return (c.getData(EXEMPTING_KEY) != null);
    }

    private static int nonExemptChildrenCount(final Control[] children) {
        int nonExemptCount = 0;

        for (Control child : children) {
            if (! controlIsLayoutExempt(child)) {
                nonExemptCount++;
            }
        }

        return nonExemptCount;
    }

    private static Point computeChildSize(final Control control, final int wHint, final int hHint,
        final boolean flushCache) {
        FillData data = (FillData)control.getLayoutData();
        if (data == null) {
            data = new FillData();
            control.setLayoutData(data);
        }

        if ((wHint == SWT.DEFAULT) && (hHint == SWT.DEFAULT)) {
            return data.computeSize(control, wHint, hHint, flushCache);
        } else {
            // "TEMPORARY CODE" (as commented in FillLayout)
            int trimX;
            int trimY;
            if (control instanceof Scrollable) {
                Rectangle rect = ((Scrollable)control).computeTrim(0, 0, 0, 0);
                trimX = rect.width;
                trimY = rect.height;
            } else {
                trimX = trimY = control.getBorderWidth() * 2;
            }

            final int w = (wHint == SWT.DEFAULT) ? wHint : Math.max(0, wHint - trimX);
            final int h = (hHint == SWT.DEFAULT) ? hHint : Math.max(0, hHint - trimY);
            return data.computeSize(control, w, h, flushCache);
        }
    }


    /**
     * type specifies how controls will be positioned within the layout.
     *
     * The default value is HORIZONTAL.
     *
     * Possible values are:
     * <ul>
     * <li>SWT.HORIZONTAL: Position the controls horizontally from left to right</li>
     * <li>SWT.VERTICAL: Position the controls vertically from top to bottom</li>
     * </ul>
     */
    public int m_type = SWT.HORIZONTAL;

    /**
     * marginWidth specifies the number of pixels of horizontal margin that will be placed along the left and right
     * edges of the layout.
     *
     * The default value is 0.
     */
    public int m_marginWidth = 0;

    /**
     * marginHeight specifies the number of pixels of vertical margin that will be placed along the top and bottom
     * edges of the layout.
     *
     * The default value is 0.
     */
    public int m_marginHeight = 0;

    /**
     * spacing specifies the number of pixels between the edge of one cell and the edge of its neighbouring cell.
     *
     * The default value is 0.
     */
    public int m_spacing = 0;

    /**
     * Constructs a new instance of this class.
     */
    public LayoutExemptingLayout() { }

    /**
     * Constructs a new instance of this class given the type.
     *
     * @param type the type of fill layout
     *
     * @since 2.0
     */
    public LayoutExemptingLayout(final int type) {
        m_type = type;
    }

    @Override
    protected boolean flushCache(final Control control) {
        final Object data = control.getLayoutData();
        if (data != null) {
            ((FillData)data).flushCache();
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Point computeSize(final Composite composite, final int wHint, final int hHint, final boolean flushCache) {
        final Control[] children = composite.getChildren();
        final int count = children.length;
        final int nonExemptCount = nonExemptChildrenCount(children);
        int maxWidth = 0;
        int maxHeight = 0;

        for (int i = 0; i < count; i++) {
            final Control child = children[i];

            if (! controlIsLayoutExempt(child)) {
                int w = wHint;
                int h = hHint;

                if ((m_type == SWT.HORIZONTAL) && (wHint != SWT.DEFAULT)) {
                    w = Math.max(0, (wHint - ((nonExemptCount - 1) * m_spacing)) / nonExemptCount);
                }
                if ((m_type == SWT.VERTICAL) && (hHint != SWT.DEFAULT)) {
                    h = Math.max(0, (hHint - ((nonExemptCount - 1) * m_spacing)) / nonExemptCount);
                }

                final Point size = computeChildSize(child, w, h, flushCache);
                maxWidth = Math.max(maxWidth, size.x);
                maxHeight = Math.max(maxHeight, size.y);
            }
        }
        int width = 0;
        int height = 0;
        if (m_type == SWT.HORIZONTAL) {
            width = nonExemptCount * maxWidth;
            if (nonExemptCount != 0) {
                width += (nonExemptCount - 1) * m_spacing;
            }
            height = maxHeight;
        } else {
            width = maxWidth;
            height = nonExemptCount * maxHeight;
            if (nonExemptCount != 0) {
                height += (nonExemptCount - 1) * m_spacing;
            }
        }
        width += m_marginWidth * 2;
        height += m_marginHeight * 2;
        if (wHint != SWT.DEFAULT) {
            width = wHint;
        }
        if (hHint != SWT.DEFAULT) {
            height = hHint;
        }

        return new Point(width, height);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void layout(final Composite composite, final boolean flushCache) {
        final Rectangle rect = composite.getClientArea();
        final Control[] children = composite.getChildren();
        final int count = children.length;
        if (count == 0) {
            return;
        }

        int width = rect.width - m_marginWidth * 2;
        int height = rect.height - m_marginHeight * 2;

        final int nonExemptCount = nonExemptChildrenCount(children);

        if (m_type == SWT.HORIZONTAL) {
            width -= (nonExemptCount - 1) * m_spacing;

            int x = rect.x + m_marginWidth;
            final int extra = width % nonExemptCount;

            final int y = rect.y + m_marginHeight;
            final int cellWidth = width / nonExemptCount;
            for (int i = 0; i < count; i++) {
                final Control child = children[i];

                if (! controlIsLayoutExempt(child)) {
                    int childWidth = cellWidth;
                    if (i == 0) {
                        childWidth += extra / 2;
                    } else {
                        if (i == count - 1) {
                            childWidth += (extra + 1) / 2;
                        }
                    }
                    child.setBounds(x, y, childWidth, height);
                    x += childWidth + m_spacing;
                }
            }
        } else {
            height -= (nonExemptCount - 1) * m_spacing;

            final int x = rect.x + nonExemptCount;
            final int cellHeight = height / nonExemptCount;

            int y = rect.y + nonExemptCount;
            final int extra = height % nonExemptCount;
            for (int i = 0; i < count; i++) {
                final Control child = children[i];

                if (! controlIsLayoutExempt(child)) {
                    int childHeight = cellHeight;
                    if (i == 0) {
                        childHeight += extra / 2;
                    } else {
                        if (i == count - 1) {
                            childHeight += (extra + 1) / 2;
                        }
                    }
                    child.setBounds(x, y, width, childHeight);
                    y += childHeight + m_spacing;
                }
            }
        }
    }

    /**
     * Returns a string containing a concise, human-readable description of the receiver.
     *
     * @return a string representation of the layout
     */
    @Override
    public String toString() {
        String string = "LayoutExemptingLayout {";

        string += "type=" + ((m_type == SWT.VERTICAL) ? "SWT.VERTICAL" : "SWT.HORIZONTAL") + " ";
        if (m_marginWidth != 0) {
            string += "marginWidth=" + m_marginWidth + " ";
        }
        if (m_marginHeight != 0) {
            string += "marginHeight=" + m_marginHeight + " ";
        }
        if (m_spacing != 0) {
            string += "spacing=" + m_spacing + " ";
        }
        string = string.trim();
        string += "}";

        return string;
    }


    private static class FillData {

        private int m_defaultWidth = -1;
        private int m_defaultHeight = -1;

        private int m_currentWidthHint;
        private int m_currentHeightHint;
        private int m_currentWidth = -1;
        private int m_currentHeight = -1;

        private Point computeSize(final Control control, final int wHint, final int hHint, final boolean flushCache) {
            if (flushCache) {
                flushCache();
            }
            if ((wHint == SWT.DEFAULT) && (hHint == SWT.DEFAULT)) {
                if ((m_defaultWidth == -1) || (m_defaultHeight == -1)) {
                    final Point size = control.computeSize(wHint, hHint, flushCache);
                    m_defaultWidth = size.x;
                    m_defaultHeight = size.y;
                }
                return new Point(m_defaultWidth, m_defaultHeight);
            }
            if ((m_currentWidth == -1) || (m_currentHeight == -1) || (wHint != m_currentWidthHint)
                || (hHint != m_currentHeightHint)) {
                final Point size = control.computeSize(wHint, hHint, flushCache);
                m_currentWidthHint = wHint;
                m_currentHeightHint = hHint;
                m_currentWidth = size.x;
                m_currentHeight = size.y;
            }
            return new Point(m_currentWidth, m_currentHeight);
        }

        private void flushCache() {
            m_defaultWidth = m_defaultHeight = -1;
            m_currentWidth = m_currentHeight = -1;
        }
    }

}
