/*
 * -------------------------------------------------------------------
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

    private final String m_pieCol;

    private final String m_aggrCol;

    private final List<PieSectionDataModel> m_sections;

    private final PieSectionDataModel m_missingSection;

    /**Constructor for class PieVizModel.
     * @param pieCol the pie column name
     * @param aggrCol the optional aggregation column name
     * @param sections the sections
     * @param missingSection the optional missing section
     * @param supportHiliting <code>true</code> if hiliting should be supported
     * @param containsColorHandler <code>true</code> if a color handler is set
     */
    public FixedPieVizModel(final String pieCol, final String aggrCol,
            final List<PieSectionDataModel> sections,
            final PieSectionDataModel missingSection,
            final boolean supportHiliting, final boolean containsColorHandler) {
        super(supportHiliting, containsColorHandler);
        if (sections == null) {
            throw new NullPointerException("sections must not be null");
        }
        m_pieCol = pieCol;
        m_aggrCol = aggrCol;
        m_sections = sections;
        m_missingSection = missingSection;
//        calculateContainsSubsections();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPieColumnName() {
        return m_pieCol;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAggregationColumnName() {
        return m_aggrCol;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<PieSectionDataModel> getSections() {
        return m_sections;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PieSectionDataModel getMissingSection() {
        return m_missingSection;
    }
}
