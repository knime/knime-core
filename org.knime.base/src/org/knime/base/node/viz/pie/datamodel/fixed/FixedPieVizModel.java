/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
