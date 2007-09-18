/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *    12.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.pie.datamodel.fixed;

import java.util.List;

import org.knime.base.node.viz.pie.datamodel.PieSectionDataModel;
import org.knime.base.node.viz.pie.datamodel.PieVizModel;




/**
 * The pie chart visualization model which extends the {@link PieVizModel}
 * class.
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedPieVizModel extends PieVizModel {

    private final FixedPieDataModel m_model;

    /**Constructor for class PieVizModel.
     * @param model the data model
     */
    public FixedPieVizModel(final FixedPieDataModel model) {
        super(model);
        m_model = model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<PieSectionDataModel> getSections() {
        return m_model.getSections();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PieSectionDataModel getMissingSection() {
        return m_model.getMissingSection();
    }
}
