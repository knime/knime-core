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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
public abstract class PieDataModel {

    private List<PieSectionDataModel> m_sections;
    private PieSectionDataModel m_missingSection;
    private final boolean m_supportHiliting;

    /**Constructor for class AbstractPieDataModel.
     * @param pieColSpec the column specification of the pie column
     * @param supportHiliting if hiliting is supported
     */
    protected PieDataModel(final DataColumnSpec pieColSpec,
            final boolean supportHiliting) {
        if (pieColSpec == null) {
            throw new NullPointerException("pieColSpec must not be null");
        }
        m_supportHiliting = supportHiliting;
        m_missingSection = createDefaultMissingSection(supportHiliting);
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
        m_sections = sections;
        if (missingSection == null) {
            m_missingSection = createDefaultMissingSection(supportHiliting);
        } else {
            m_missingSection = missingSection;
        }
        m_supportHiliting = supportHiliting;
    }

    /**
     * Creates the default missing section.
     * @param supportHiliting <code>true</code> if hiliting is supported
     * @return the default missing section
     */
    protected static PieSectionDataModel createDefaultMissingSection(
            final boolean supportHiliting) {
        return new PieSectionDataModel(
                FixedPieVizModel.MISSING_VAL_SECTION_CAPTION,
                FixedPieVizModel.MISSING_VAL_SECTION_COLOR, supportHiliting);
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
     * @param sections the new sections of this data model
     */
    protected void setSections(final List<PieSectionDataModel> sections) {
        if (sections == null) {
            throw new NullPointerException("sections must not be null");
        }
        m_sections = sections;
    }

    /**
     * @return the number of all pie sections including empty sections and
     * without an optional missing section
     */
    public int getNoOfSections() {
        return m_sections.size();
    }

    /**
     * Use the {@link #hasMissingSection()} method to check if a missing
     * section is present or not.
     * @return the missingSection or <code>null</code> if no missing section
     * is available
     */
    public PieSectionDataModel getMissingSection() {
        if (!hasMissingSection()) {
            return null;
        }
        return m_missingSection;
    }


    /**
     * @param missingSection the new missing section
     */
    protected void setMissingSection(final PieSectionDataModel missingSection) {
        if (missingSection == null) {
            throw new NullPointerException("missingSection must not be null");
        }
        m_missingSection = missingSection;
    }

    /**
     * @return <code>true</code> if this model contains a missing section
     */
    public boolean hasMissingSection() {
        return m_missingSection.getRowCount() > 0;
    }
    /**
     * Adds the given row values to the histogram.
     * @param id the row key of this row
     * @param rowColor the color of this row
     * @param pieCell the pie value
     * @param aggrCell the optional aggregation value
     */
    public abstract void addDataRow(final DataCell id, final Color rowColor,
            final DataCell pieCell, final DataCell aggrCell);

    /**
     * @param directory the directory to write to
     * @param exec the {@link ExecutionMonitor} to provide progress messages
     * @throws IOException if a file exception occurs
     * @throws CanceledExecutionException if the operation was canceled
     */
    public abstract void save2File(final File directory,
            final ExecutionMonitor exec)
    throws IOException, CanceledExecutionException;
}
