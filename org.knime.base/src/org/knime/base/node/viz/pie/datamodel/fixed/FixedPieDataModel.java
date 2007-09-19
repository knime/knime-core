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

package org.knime.base.node.viz.pie.datamodel.fixed;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;

import org.knime.base.node.viz.pie.datamodel.PieDataModel;
import org.knime.base.node.viz.pie.datamodel.PieSectionDataModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;



/**
 * This class represents a pie chart which consists of several
 * {@link PieSectionDataModel}s.
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedPieDataModel extends PieDataModel {

    /**The name of the data file which contains all data in serialized form.*/
    private static final String CFG_DATA_FILE = "dataFile";

    private final List<PieSectionDataModel> m_sections;

    private final PieSectionDataModel m_missingSection;

    /**Constructor for class PieDataModel.
     * @param pieColSpec the column specification of the pie column
     */
    public FixedPieDataModel(final DataColumnSpec pieColSpec) {
        super(false);
        if (pieColSpec == null) {
            throw new NullPointerException("pieColSpec must not be null");
        }
        m_missingSection = createDefaultMissingSection(false);
        m_sections = createSections(pieColSpec, false);
    }

    /**Constructor for class PieDataModel.
     * @param sections the sections
     * @param missingSection the missing section
     * @param supportHiliting if hiliting is supported
     */
    protected FixedPieDataModel(final List<PieSectionDataModel> sections,
            final PieSectionDataModel missingSection,
            final boolean supportHiliting) {
        super(supportHiliting);
        if (sections == null) {
            throw new NullPointerException("sections must not be null");
        }
        m_sections = sections;
        if (missingSection == null) {
            m_missingSection = createDefaultMissingSection(supportHiliting);
        } else {
            m_missingSection = missingSection;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addDataRow(final DataRow row, final Color rowColor,
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
        section.addDataRow(rowColor, row.getKey().getId(), aggrCell);
    }

    /**
     * @param directory the directory to write to
     * @param exec the {@link ExecutionMonitor} to provide progress messages
     * @throws IOException if a file exception occurs
     * @throws CanceledExecutionException if the operation was canceled
     */
    public void save2File(final File directory, final ExecutionMonitor exec)
    throws IOException, CanceledExecutionException {
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
        os.writeObject(getSections());
        if (exec != null) {
            exec.setProgress(0.8, "Start saving missing section...");
            exec.checkCanceled();
        }
        os.writeObject(getMissingSection());
        if (exec != null) {
            exec.setProgress(0.9, "Start saving hiliting flag...");
            exec.checkCanceled();
        }
        os.writeBoolean(supportsHiliting());
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
    public static FixedPieDataModel loadFromFile(final File directory,
            final ExecutionMonitor exec)
            throws IOException, ClassNotFoundException,
            CanceledExecutionException {
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
        return new FixedPieDataModel(sections, missingSection, supportHiliting);
    }

    /**
     * @param value the value to get the section for
     * @return the{@link PieSectionDataModel} which represents the given value
     * or <code>null</code> if no section is found for the given value
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
     * @return the {@link PieSectionDataModel} list
     */
    public List<PieSectionDataModel> getSections()  {
        return Collections.unmodifiableList(m_sections);
    }

    /**
     * @return the missing {@link PieSectionDataModel}
     */
    public PieSectionDataModel getMissingSection()  {
        return m_missingSection;
    }
}
