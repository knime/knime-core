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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Jul 2, 2010 (ohl): created
 */
package org.knime.workbench.editor2.figures;

import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Locator;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 *
 * @author ohl, University of Konstanz
 */
public class NodeContainerLocator implements Locator {

    private final NodeContainerFigure m_container;

    /**
     * Distance (vertically) between the contained figures in the node figure
     */
    public final static int GAP = 1;

    /**
     * Places the components in the node figure. (I.e. the Node Name, the icon
     * (symbol figure), the custom name and the status or progress indicator.
     * Ports have their own locators.
     *
     * @param container the node to layout
     */
    NodeContainerLocator(final NodeContainerFigure container) {
        m_container = container;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void relocate(final IFigure fig) {
        // lets assume the figure above got layouted already
        Dimension pref = fig.getPreferredSize();
        // place the figure one pixel below the above figure
        int y = 0;
        // as components change (status bar gets replaced by progress)
        // we need to find the component above us
        List<IFigure> children = m_container.getChildren();
        IFigure above = m_container;
        for (IFigure f : children) {
            if (f == fig) {
                break;
            }
            above = f;
        }
        Rectangle r = above.getBounds().getCopy();
        if (above == m_container) {
            // we are the first component in the container
            y = r.y + GAP;
        } else {
            y = r.y + r.height + GAP;
        }
        // center it
        Rectangle contBounds = m_container.getBounds().getCopy();
        if (m_container.getBounds().width > pref.width) {
            int wDiff = contBounds.width - pref.width;
            int x = contBounds.x + (wDiff / 2);
            Rectangle bounds = new Rectangle(x, y, pref.width, pref.height);
            fig.setBounds(bounds);
        } else {
            // container is too narrow
            Rectangle bounds =
                    new Rectangle(contBounds.x, y, contBounds.width,
                            pref.height);
            fig.setBounds(bounds);
        }
    }
}
