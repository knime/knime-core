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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
 * This class represents a pie chart which consists of several
 * {@link PieSectionDataModel}s.
 * @author Tobias Koetter, University of Konstanz
 */
public class PieDataModel {

    /**The name of the data file which contains all data in serialized form.*/
    private static final String CFG_DATA_FILE = "dataFile";

    private final List<PieSectionDataModel> m_sections;

    private final PieSectionDataModel m_missingSection;

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
//        if (missingSection == null) {
//            throw new NullPointerException("missingSection must not be null");
//        }
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
     */
    private static PieSectionDataModel createDefaultMissingSection(
            final boolean supportHiliting) {
        return new PieSectionDataModel(
                PieVizModel.MISSING_VAL_SECTION_CAPTION,
                PieVizModel.MISSING_VAL_SECTION_COLOR, supportHiliting);
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
            throw new NullPointerException(
                    "Pie section value must not be null.");
        }
        final PieSectionDataModel section;
        if (pieCell.isMissing()) {
            section = m_missingSection;
        } else {
            section = getSection(pieCell);
            if (section == null) {
                throw new IllegalArgumentException("No section found for: "
                        + pieCell.toString());
            }
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
     * @return <code>true</code> if this model contains a missing section
     */
    public boolean hasMissingSection() {
        return m_missingSection.getRowCount() > 0;
    }

    /**
     * @param directory the directory to write to
     * @param exec the {@link ExecutionMonitor} to provide progress messages
     * @throws IOException if a file exception occurs
     * @throws CanceledExecutionException if the operation was canceled
     */
    public void save2File(final File directory,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        if (exec != null) {
            exec.setProgress(0.0, "Start saving histogram data model to file");
        }
        final File dataFile = new File(directory, CFG_DATA_FILE);
        final FileOutputStream dataOS = new FileOutputStream(dataFile);
        final ObjectOutputStream os = new ObjectOutputStream(dataOS);
        if (exec != null) {
            exec.setProgress(0.3, "Start saving sections...");
            exec.checkCanceled();
        }
        os.writeObject(m_sections);
        if (exec != null) {
            exec.setProgress(0.8, "Start saving missing section...");
            exec.checkCanceled();
        }
        os.writeObject(m_missingSection);
        if (exec != null) {
            exec.setProgress(0.9, "Start saving hiliting flag...");
            exec.checkCanceled();
        }
        os.writeBoolean(m_supportHiliting);
        os.flush();
        os.close();
        dataOS.flush();
        dataOS.close();
        if (exec != null) {
            exec.setProgress(1.0, "Pie data model saved");
        }
    }

    /**
     * @param directory the directory to write to
     * @param exec the {@link ExecutionMonitor} to provide progress messages
     * @return the data model
     * wasn't valid
     * @throws IOException if a file exception occurs
     * @throws ClassNotFoundException if a class couldn't be deserialized
     * @throws CanceledExecutionException if the operation was canceled
     */
    @SuppressWarnings("unchecked")
    public static PieDataModel loadFromFile(final File directory,
            final ExecutionMonitor exec) throws IOException,
            ClassNotFoundException, CanceledExecutionException {
        if (exec != null) {
            exec.setProgress(0.0, "Start reading data from file");
        }
        final File dataFile = new File(directory, CFG_DATA_FILE);
        final FileInputStream dataIS = new FileInputStream(dataFile);
        final ObjectInputStream os = new ObjectInputStream(dataIS);
        if (exec != null) {
            exec.setProgress(0.3, "Loading sections...");
            exec.checkCanceled();
        }
        final List<PieSectionDataModel> sections =
            (List<PieSectionDataModel>)os.readObject();

        if (exec != null) {
            exec.setProgress(0.8, "Loading missing section...");
            exec.checkCanceled();
        }
        final PieSectionDataModel missingSection = (
                PieSectionDataModel)os.readObject();

        if (exec != null) {
            exec.setProgress(0.0, "Loading hiliting flag...");
            exec.checkCanceled();
        }
        final boolean supportHiliting = os.readBoolean();
        if (exec != null) {
            exec.setProgress(1.0, "Pie data mdoel loaded ");
        }
        //close the streams
        os.close();
        dataIS.close();
        return new PieDataModel(sections, missingSection, supportHiliting);
    }
}
