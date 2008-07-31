/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *    21.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.pie.impl.fixed;

import javax.swing.JLabel;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.pie.datamodel.fixed.FixedPieVizModel;
import org.knime.base.node.viz.pie.impl.PieProperties;


/**
 * The fixed implementation of the {@link PieProperties} panel.
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedPieProperties
    extends PieProperties<FixedPieVizModel> {

    private static final long serialVersionUID = -4156051632727774736L;

    private final JLabel m_pieCol;

    private final JLabel m_aggrCol;

    /**Constructor for class FixedPieProperties.
     * @param vizModel the visualization model to initialize all swing
     * components
     */
    public FixedPieProperties(final FixedPieVizModel vizModel) {
        super(vizModel);
        m_pieCol = new JLabel(vizModel.getPieColumnName());
        final String aggrColName = vizModel.getAggregationColumnName();
        if (aggrColName == null) {
            m_aggrCol = new JLabel("none selected");
        } else {
            m_aggrCol = new JLabel(aggrColName);
        }
        super.addColumnTab(m_pieCol, m_aggrCol);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSelectAggrMethod(final AggregationMethod aggrMethod) {
        //nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updatePanelInternal(final FixedPieVizModel vizModel) {
        m_pieCol.setText(vizModel.getPieColumnName());
        final String aggrColName = vizModel.getAggregationColumnName();
        if (aggrColName == null) {
            m_aggrCol.setText("none selected");
        } else {
            m_aggrCol.setText(aggrColName);
        }
    }
}
