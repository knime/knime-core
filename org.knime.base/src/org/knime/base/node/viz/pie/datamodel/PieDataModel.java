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

package org.knime.base.node.viz.pie.datamodel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.def.IntCell;

/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
public abstract class PieDataModel {


    private final boolean m_supportHiliting;

    /**Constructor for class AbstractPieDataModel.
     * @param supportHiliting if hiliting is supported
     */
    protected PieDataModel(final boolean supportHiliting) {
        m_supportHiliting = supportHiliting;
    }

    /**
     * @param pieColSpec the column specification of the pie column
     * @param supportsHiliting <code>true</code> if hiliting is supported
     * @return the {@link List} of {@link PieSectionDataModel} for the
     * given column specification
     */
    protected static List<PieSectionDataModel> createSections(
            final DataColumnSpec pieColSpec, final boolean supportsHiliting) {
        final DataColumnDomain domain = pieColSpec.getDomain();
        if (domain == null) {
            throw new IllegalArgumentException(
                    "Pie column domain must not be null");
        }
        final ArrayList<PieSectionDataModel> sections;
        if (pieColSpec.getType().isCompatible(NominalValue.class)) {
            final Set<DataCell> values = domain.getValues();
            if (values == null || values.size() < 1) {
                throw new IllegalArgumentException(
                        "Pie column domain containes no values");
            }

            sections = new ArrayList<PieSectionDataModel>(values.size());
            final int noOfVals = values.size();
            int idx = 0;
            for (final DataCell value : values) {
                final Color color = generateColor(idx++, noOfVals);
                final PieSectionDataModel section =
                    new PieSectionDataModel(value.toString(),
                        color, supportsHiliting);
                sections.add(section);

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
              sections = new ArrayList<PieSectionDataModel>(range);
              for (int i = lower; i <= upper; i++) {
                  final Color color = generateColor(i, range);
                  final PieSectionDataModel section =
                      new PieSectionDataModel(Integer.toString(i),
                          color, supportsHiliting);
                  sections.add(section);
              }
          } else {
              throw new IllegalArgumentException("Invalid pie column");
          }
        return sections;
    }

    /**
     * Creates the default missing section.
     * @param supportHiliting <code>true</code> if hiliting is supported
     * @return the default missing section
     */
    protected static PieSectionDataModel createDefaultMissingSection(
            final boolean supportHiliting) {
        return new PieSectionDataModel(
                PieVizModel.MISSING_VAL_SECTION_CAPTION,
                PieVizModel.MISSING_VAL_SECTION_COLOR, supportHiliting);
    }

    /**
     * @param idx the current index
     * @param size the total number of elements
     * @return the color for the current index
     */
    protected static Color generateColor(final int idx, final int size) {
        // use Color, half saturated, half bright for base color
        return Color.getColor(null, Color.HSBtoRGB((float)idx / (float)size,
                1.0f, 1.0f));
    }


    /**
     * @param value the value to look for
     * @return the section which represent the given value or <code>null</code>
     * if none exists
     */
    public abstract PieSectionDataModel getSection(final DataCell value);

    /**
     * @return <code>true</code> if hiliting is supported
     */
    public boolean supportsHiliting() {
        return m_supportHiliting;
    }

    /**
     * @return the sections
     */
    public abstract List<PieSectionDataModel> getSections();

    /**
     * @return the missing section
     */
    public abstract PieSectionDataModel getMissingSection();

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
            throw new NullPointerException(
                    "Pie section value must not be null.");
        }
        final PieSectionDataModel section;
        if (pieCell.isMissing()) {
            section = getMissingSection();
        } else {
            section = getSection(pieCell);
            if (section == null) {
                throw new IllegalArgumentException("No section found for: "
                        + pieCell.toString());
            }
        }
        section.addDataRow(rowColor, id, aggrCell);
    }
}
