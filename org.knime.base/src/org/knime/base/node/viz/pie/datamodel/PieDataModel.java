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
 *    13.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.pie.datamodel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.def.IntCell;



/**
 * This class represents a pie chart which consists of several
 * {@link PieSectionDataModel}s.
 * @author Tobias Koetter, University of Konstanz
 */
public class PieDataModel {

    private final List<PieSectionDataModel> m_sections;

    private PieSectionDataModel m_missingSection;

    private final boolean m_supportHiliting;

    /**Constructor for class PieDataModel.
     * @param pieColSpec the column specification of the pie column
     * @param supportHiliting if hiliting is supported
     */
    public PieDataModel(final DataColumnSpec pieColSpec,
            final boolean supportHiliting) {
        if (pieColSpec == null) {
            throw new NullPointerException("pieColSpec must not be null");
        }
        m_supportHiliting = supportHiliting;
        final DataColumnDomain domain = pieColSpec.getDomain();
        if (domain == null) {
            throw new IllegalArgumentException(
                    "Pie column domain must not be null");
        }
        if (pieColSpec.getType().isCompatible(NominalValue.class)) {
            final Set<DataCell> values = domain.getValues();
            if (values == null || values.size() < 1) {
                throw new IllegalArgumentException(
                        "Pie column domain containes no values");
            }

            m_sections = new ArrayList<PieSectionDataModel>(values.size());
            final int noOfVals = values.size();
            int idx = 0;
            for (final DataCell value : values) {
                final Color color = generateColor(idx++, noOfVals);
                final PieSectionDataModel section =
                    new PieSectionDataModel(value.toString(),
                        color, m_supportHiliting);
                m_sections.add(section);

            }
        } else if (pieColSpec.getType().isCompatible(IntValue.class)) {
              if (domain.getLowerBound() == null
                      || domain.getUpperBound() == null) {
                  throw new IllegalArgumentException(
                          "Pie column domain contains no bounds");
              }
              final int lower = ((IntCell)domain.getLowerBound()).getIntValue();
              final int upper = ((IntCell)domain.getUpperBound()).getIntValue();
              final int range = upper - lower;
              m_sections = new ArrayList<PieSectionDataModel>(range);
              for (int i = lower; i <= upper; i++) {
                  final Color color = generateColor(i, range);
                  final PieSectionDataModel section =
                      new PieSectionDataModel(Integer.toString(i),
                          color, m_supportHiliting);
                  m_sections.add(section);
              }
          } else {
              throw new IllegalArgumentException("Invalid pie column");
          }
    }

    /**Constructor for class PieDataModel.
     * @param sections the sections
     * @param missingSection the missing section
     * @param supportHiliting if hiliting is supported
     */
    protected PieDataModel(final List<PieSectionDataModel> sections,
            final PieSectionDataModel missingSection,
            final boolean supportHiliting) {
        if (sections == null) {
            throw new NullPointerException("sections must not be null");
        }
//        if (missingSection == null) {
//            throw new NullPointerException("missingSection must not be null");
//        }
        m_sections = sections;
        m_missingSection = missingSection;
        m_supportHiliting = supportHiliting;
    }

    private static Color generateColor(final int idx, final int size) {
        // use Color, half saturated, half bright for base color
        return Color.getColor(null, Color.HSBtoRGB((float)idx / (float)size,
                1.0f, 1.0f));
    }

    /**
     * Adds the given row values to the histogram.
     * @param id the row key of this row
     * @param rowColor the color of this row
     * @param pieCell the pie value
     * @param aggrCell the optional aggregation value
     */
    public void addDataRow(final DataCell id, final Color rowColor,
            final DataCell pieCell, final DataCell aggrCell) {
        if (pieCell == null) {
            throw new NullPointerException("X value must not be null.");
        }
        final PieSectionDataModel section = getSection(pieCell);
        if (section == null) {
            throw new IllegalArgumentException("No section found for: "
                    + pieCell.toString());
        }
        section.addDataRow(rowColor, id, aggrCell);
    }

    /**
     * @param value the value to look for
     * @return the section which represent the given value or <code>null</code>
     * if none exists
     */
    public PieSectionDataModel getSection(final DataCell value) {
        for (final PieSectionDataModel section : m_sections) {
            if (section.getName().equals(value.toString())) {
                return section;
            }
        }
        return null;
    }

    /**
     * @return <code>true</code> if hiliting is supported
     */
    public boolean supportsHiliting() {
        return m_supportHiliting;
    }

    /**
     * @return the sections
     */
    public List<PieSectionDataModel> getSections() {
        return Collections.unmodifiableList(m_sections);
    }

    /**
     * @return the number of pie sections
     */
    public int getNoOfSections() {
        return m_sections.size();
    }

    /**
     * @return the missingSection
     */
    public PieSectionDataModel getMissingSection() {
        return m_missingSection;
    }

    /**
     * @return <code>true</code> if this model contains a missing section
     */
    public boolean hasMissingSection() {
        return m_missingSection != null;
    }

    /**
     * @param aggrMethod the {@link AggregationMethod} to use
     * @param includeMissing <code>true</code> if the missing value section
     * should be used as well
     * @return the total aggregation value of all pie section
     */
    protected double getAggregationValue(final AggregationMethod aggrMethod,
            final boolean includeMissing) {
        double sum = 0;
        for (final PieSectionDataModel section : m_sections) {
            sum += section.getAggregationValue(aggrMethod);
        }
        if (includeMissing && hasMissingSection()) {
            sum += m_missingSection.getAggregationValue(aggrMethod);
        }
        return sum;
    }
}
