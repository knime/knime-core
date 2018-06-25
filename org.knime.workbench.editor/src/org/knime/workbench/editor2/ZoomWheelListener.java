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
 *   Mar 15, 2018 (loki): created
 */
package org.knime.workbench.editor2;

import java.util.Arrays;

import org.eclipse.core.runtime.Platform;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.NodeLogger;

/**
 * This class came into existence to address AP-5062 which asks for the functionality of CTRL+MouseWheel should change
 * the zoom level on the workflow editor.
 *
 * There will be one of these listeners per WorkflowEditor instance (since there is a 1-1 between an instance of such
 * and a ZoomManager instance.)
 */
final class ZoomWheelListener implements MouseWheelListener {

    private static final double SCROLL_ZOOM_MULTIPLIER = 0.02;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ZoomWheelListener.class);

    private final int m_platformStateMaskModifier;

    private final ZoomManager m_zoomManager;

    private final FigureCanvas m_figureCanvas;

    /**
     * Default constructor.
     *
     * @param zm
     * @param fc the canvas from which we want wheel event notifications
     */
    ZoomWheelListener(final ZoomManager zm, final FigureCanvas fc) {
        if (Platform.OS_MACOSX.equals(Platform.getOS())) {
            m_platformStateMaskModifier = SWT.COMMAND;
        } else {
            m_platformStateMaskModifier = SWT.CTRL;
        }

        m_zoomManager = zm;

        m_figureCanvas = fc;
        m_figureCanvas.addMouseWheelListener(this);
    }

    private int getIndexOrNearestForZoomLevel(final double zoom) {
        int rhett = Arrays.binarySearch(m_zoomManager.getZoomLevels(), zoom);

        if (rhett < 0) {
            rhett += 1;
            rhett *= -1;
        }

        return rhett;
    }

    /**
     * Should be called as part of workflow disposition, before the parent figure canvas has been disposed.
     */
    public void dispose() {
        Display.getCurrent().asyncExec(() -> {
            try {
                ZoomWheelListener outer = ZoomWheelListener.this;
                if (outer.m_figureCanvas.isDisposed()) {
                    // this otherwise causes a "widget disposed" while removing the mouse listener
                    return;
                }
                outer.m_figureCanvas.removeMouseWheelListener(this);
            } catch (Exception e) {
                // canvas has likely already gone.
                LOGGER.debug("We encountered an exception disposing of the zoom wheel listener.", e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseScrolled(final MouseEvent me) {
        if ((me.stateMask & m_platformStateMaskModifier) == m_platformStateMaskModifier) {
            final int scrollEventChange = me.count;
            final double newZoom;

            if ((me.stateMask & SWT.ALT) == SWT.ALT) {
                newZoom = m_zoomManager.getZoom() + (SCROLL_ZOOM_MULTIPLIER * scrollEventChange);
            } else {
                final double currentZoom = m_zoomManager.getZoom();
                int index = getIndexOrNearestForZoomLevel(currentZoom);

                if (scrollEventChange < 0) {
                    index--;
                } else {
                    index++;
                }

                double[] zoomLevels = m_zoomManager.getZoomLevels();
                if (index < 0) {
                    index = 0;
                } else if (index >= zoomLevels.length) {
                    index = zoomLevels.length - 1;
                }

                newZoom = zoomLevels[index];
            }

            m_zoomManager.setZoom(newZoom);
        }
    }

}
