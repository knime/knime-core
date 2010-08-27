/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *    18.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.pie.datamodel;

import org.knime.base.node.viz.aggregation.util.AggrValModelComparator;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * The abstract pie data model which provides method to hold the data which
 * should be displayed as a pie chart.
 * @author Tobias Koetter, University of Konstanz
 */
public abstract class PieDataModel {

    private final boolean m_supportHiliting;

    private final boolean m_detailsAvailable;

    /**Constructor for class AbstractPieDataModel.
     * @param supportHiliting if hiliting is supported
     * @param detailsAvailable <code>true</code> if details are available
     */
    protected PieDataModel(final boolean supportHiliting,
            final boolean detailsAvailable) {
        m_supportHiliting = supportHiliting;
        m_detailsAvailable = detailsAvailable;
    }

    /**
     * Creates the default missing section.
     * @param supportHiliting <code>true</code> if hiliting is supported
     * @return the default missing section
     */
    public static PieSectionDataModel createDefaultMissingSection(
            final boolean supportHiliting) {
        return new PieSectionDataModel(
                PieVizModel.MISSING_VAL_SECTION_CAPTION,
                PieVizModel.MISSING_VAL_SECTION_COLOR, supportHiliting);
    }

    /**
     * @param sections the sections to set the color
     */
    public static void setSectionColor(
            final List<PieSectionDataModel> sections) {
        if (sections == null) {
            throw new NullPointerException("sections must not be null");
        }
        boolean useSectionColor = true;
        for (final PieSectionDataModel section : sections) {
            if (section.getNoOfElements() > 1) {
                useSectionColor = false;
            }
        }
        if (useSectionColor) {
            for (final PieSectionDataModel section : sections) {
                if (!section.isEmpty()) {
                    final Collection<PieSubSectionDataModel> elements =
                        section.getElements();
                    if (elements.size() > 1) {
                        throw new IllegalArgumentException(
                                "Section should have only one element");
                    }
                    if (elements.size() == 1) {
                        section.setColor(elements.iterator().next().getColor());
                    }
                }
            }
        }
    }

    /**
     * @param sections the sections to sort
     * @param numerical if the pie column is numerical
     * @param ascending <code>true</code> if the section should be ordered
     * in ascending order
     */
    public static void sortSections(final List<PieSectionDataModel> sections,
            final boolean numerical, final boolean ascending) {
        final AggrValModelComparator comparator =
            new AggrValModelComparator(numerical, ascending);
        Collections.sort(sections, comparator);
    }

    /**
     * @return <code>true</code> if hiliting is supported
     */
    public boolean supportsHiliting() {
        return m_supportHiliting;
    }

    /**
     * @return <code>true</code> if details are available
     */
    public boolean detailsAvailable() {
        return m_detailsAvailable;
    }
}
