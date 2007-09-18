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
 *    18.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.pie.datamodel.interactive;

import java.util.List;

import org.knime.base.node.viz.pie.datamodel.PieDataModel;
import org.knime.base.node.viz.pie.datamodel.PieSectionDataModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;


/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractivePieDataModel extends PieDataModel {

    private final List<PieSectionDataModel> m_sections;

    private final PieSectionDataModel m_missingSection;

    /**Constructor for class InteractivePieDataModel.
     * @param pieColSpec the column specification of the pie column
     */
    public InteractivePieDataModel(final DataColumnSpec pieColSpec) {
        super(true);
        m_missingSection = createDefaultMissingSection(false);
        m_sections = createSections(pieColSpec, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PieSectionDataModel getMissingSection() {
        return m_missingSection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PieSectionDataModel getSection(final DataCell value) {
        for (final PieSectionDataModel section : m_sections) {
            if (section.getName().equals(value.toString())) {
                return section;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PieSectionDataModel> getSections() {
        return m_sections;
    }
}
