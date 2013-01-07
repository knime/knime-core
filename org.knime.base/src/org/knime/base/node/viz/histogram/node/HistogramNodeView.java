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
 *   08.06.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.histogram.node;

import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel;
import org.knime.base.node.viz.histogram.impl.interactive.InteractiveHistogramPlotter;
import org.knime.base.node.viz.histogram.impl.interactive.InteractiveHistogramProperties;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeView;

/**
 * The node view which contains the histogram plotter panel.
 *
 * @author Tobias Koetter, University of Konstanz
 *
 */
public class HistogramNodeView extends NodeView<HistogramNodeModel> {

    private InteractiveHistogramPlotter m_plotter;

    /**
     * Creates a new view instance for the histogram node.
     *
     * @param nodeModel the corresponding node model
     */
    HistogramNodeView(final HistogramNodeModel nodeModel) {
        super(nodeModel);
    }

    /**
     * Whenever the model changes an update for the plotter is triggered and new
     * HiLiteHandler are set.
     */
    @Override
    public void modelChanged() {
        final AbstractHistogramNodeModel model = getNodeModel();
        if (model == null) {
            return;
        }
        if (m_plotter != null) {
            m_plotter.reset();
        }
        final DataTableSpec tableSpec = model.getTableSpec();
        final AbstractHistogramVizModel vizModel =
            model.getHistogramVizModel();
        if (vizModel == null) {
            return;
        }
        if (m_plotter == null) {
            final InteractiveHistogramProperties props =
                new InteractiveHistogramProperties(tableSpec, vizModel);
            m_plotter = new InteractiveHistogramPlotter(props,
                    model.getInHiLiteHandler(0));
            // add the hilite menu to the menu bar of the node view
            getJMenuBar().add(m_plotter.getHiLiteMenu());
        }
        m_plotter.setHiLiteHandler(model.getInHiLiteHandler(0));
        m_plotter.setHistogramVizModel(tableSpec, vizModel);
        m_plotter.updatePaintModel();
        if (getComponent() != m_plotter) {
            setComponent(m_plotter);
        }
        if (m_plotter != null) {
            m_plotter.fitToScreen();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        return;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        return;
    }
}
