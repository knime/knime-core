/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
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
 *   08.06.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.pie.node;

import org.knime.base.node.viz.pie.datamodel.PieVizModel;
import org.knime.base.node.viz.pie.impl.PiePlotter;
import org.knime.base.node.viz.pie.impl.PieProperties;
import org.knime.core.node.NodeView;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * The abstract node view which contains the pie chart panel.
 *
 * @author Tobias Koetter, University of Konstanz
 * @param <P> the {@link PieProperties} implementation
 * @param <D> the {@link PieVizModel}implementation
 * @param <T> the {@link PieNodeModel} implementation
 *
 */
public abstract class PieNodeView<P extends PieProperties<D>,
D extends PieVizModel, T extends PieNodeModel<D>> extends NodeView<T> {

    private PiePlotter<P, D> m_plotter;

    /**
     * Creates a new view instance for the histogram node.
     *
     * @param nodeModel the corresponding node model
     */
    @SuppressWarnings("unchecked")
    protected PieNodeView(final T nodeModel) {
        super(nodeModel);

    }

    /**
     * Whenever the model changes an update for the plotter is triggered and new
     * HiLiteHandler are set.
     */
    @Override
    public void modelChanged() {
        final T model = getNodeModel();
        if (model == null) {
            return;
        }
        if (m_plotter != null) {
            m_plotter.reset();
        }
        final D vizModel = model.getVizModel();
        if (vizModel == null) {
            return;
        }
        if (m_plotter == null) {
            m_plotter = getPlotter(vizModel, model.getInHiLiteHandler(0));
            if (vizModel.supportsHiliting()) {
                // add the hilite menu to the menu bar of the node view
                getJMenuBar().add(m_plotter.getHiLiteMenu());
            }
            setComponent(m_plotter);
        }
        m_plotter.setHiLiteHandler(model.getInHiLiteHandler(0));
        m_plotter.setVizModel(vizModel);
        m_plotter.updatePaintModel();
        if (getComponent() == null) {
            setComponent(m_plotter);
        }
    }

    /**
     * @param vizModel the pie visualization model
     * @param handler the hilite handler
     * @return the plotter implementation
     */
    protected abstract PiePlotter<P, D> getPlotter(final D vizModel,
            final HiLiteHandler handler);

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
