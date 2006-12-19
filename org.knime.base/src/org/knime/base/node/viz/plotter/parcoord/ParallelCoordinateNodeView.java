/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   27.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.parcoord;

import org.knime.base.node.viz.plotter.node.DefaultVisualizationNodeView;
import org.knime.core.node.NodeModel;

/**
 * Extends the 
 * {@link org.knime.base.node.viz.plotter.node.DefaultVisualizationNodeView} 
 * since it provides an additional menu to show, fade or hide unhilited lines 
 * (rows).
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class ParallelCoordinateNodeView extends DefaultVisualizationNodeView {

    /**
     * Adds  a show/hide menu to the menu bar.
     * 
     * @param model the node model
     * @param plotter the plotter
     */
    public ParallelCoordinateNodeView(final NodeModel model, 
            final ParallelCoordinatesPlotter plotter) {
        super(model, plotter);
        getJMenuBar().add(plotter.getShowHideMenu());
    }
}
