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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import org.knime.core.node.NodeLogger;



/**
 * This class represents a pie chart which consists of several
 * {@link PieSectionDataModel}s.
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedPieDataModel extends PieDataModel {
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(FixedPieDataModel.class);
    /**The name of the data file which contains all data in serialized form.*/
    private static final String CFG_DATA_FILE = "dataFile";

    private final String m_pieCol;

    private final String m_aggrCol;

    private final List<PieSectionDataModel> m_sections;

    private final PieSectionDataModel m_missingSection;

    /**Constructor for class PieDataModel.
     * @param pieColSpec the {@link DataColumnSpec} of the pie column
     * @param aggrColSpec the optional {@link DataColumnSpec} of the
     * aggregation column
     * @param containsColorHandler <code>true</code> if a color handler is set
     */
    public FixedPieDataModel(final DataColumnSpec pieColSpec,
            final DataColumnSpec aggrColSpec,
            final boolean containsColorHandler) {
        super(false, containsColorHandler);
        if (pieColSpec == null) {
            throw new NullPointerException("pieCol, aggrCol must not be null");
        }
        if (aggrColSpec == null) {
            m_aggrCol = null;
        } else {
            m_aggrCol = aggrColSpec.getName();
        }
        m_pieCol = pieColSpec.getName();
        m_missingSection = createDefaultMissingSection(false);
        m_sections = createSections(pieColSpec, false);
    }

    /**Constructor for class PieDataModel.
     * @param pieCol the name of the pie column
     * @param aggrCol the name of the aggregation column
     * @param sections the sections
     * @param missingSection the missing section
     * @param supportHiliting if hiliting is supported
     */
    protected FixedPieDataModel(final String pieCol, final String aggrCol,
            final List<PieSectionDataModel> sections,
            final PieSectionDataModel missingSection,
            final boolean supportHiliting, final boolean showDetails) {
        super(supportHiliting, showDetails);
        if (pieCol == null) {
            throw new NullPointerException("pieCol must not be null");
        }
        m_pieCol = pieCol;
        if (aggrCol == null) {
            throw new NullPointerException("aggrCol must not be null");
        }
        m_aggrCol = aggrCol;
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
        os.writeObject(m_pieCol);
        os.writeObject(m_aggrCol);
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
        os.writeBoolean(detailsAvailable());
        os.flush();
        os.close();
        dataOS.flush();
        dataOS.close();
        if (exec != null) {
            exec.setProgress(1.0, "Pie data model saved");
        }
    }

    /**
     * @return the name of the pie column
     */
    public String getPieColName() {
        return m_pieCol;
    }

    /**
     * @return the name of the pie column
     */
    public String getAggrColName() {
        return m_aggrCol;
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
        final String pieCol = (String)os.readObject();
        final String aggrCol = (String)os.readObject();
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
        final boolean detailsAvailable = os.readBoolean();
        if (exec != null) {
            exec.setProgress(1.0, "Pie data mdoel loaded ");
        }
        //close the streams
        os.close();
        dataIS.close();
        return new FixedPieDataModel(pieCol, aggrCol, sections,
                missingSection, supportHiliting, detailsAvailable);
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

    /**
     * @return a real copy of all sections
     */
    @SuppressWarnings("unchecked")
    public List<PieSectionDataModel> getClonedSections()  {
        LOGGER.debug("Entering getClonedSections() of class "
                + "FixedPieDataModel.");
        final long startTime = System.currentTimeMillis();
        List<PieSectionDataModel> binClones = null;
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ObjectOutputStream(baos).writeObject(m_sections);
            final ByteArrayInputStream bais =
                new ByteArrayInputStream(baos.toByteArray());
            binClones = (List<PieSectionDataModel>)
            new ObjectInputStream(bais).readObject();
        } catch (final Exception e) {
            final String msg = "Exception while cloning pie sections: "
                + e.getMessage();
              LOGGER.debug(msg);
              throw new IllegalStateException(msg);
        }

        final long endTime = System.currentTimeMillis();
        final long durationTime = endTime - startTime;
        LOGGER.debug("Time for cloning. " + durationTime + " ms");
        LOGGER.debug("Exiting getClonedSections() of class "
                + "FixedPieDataModel.");
        return binClones;
    }

    /**
     * @return a real copy of the missing section
     */
    public PieSectionDataModel getClonedMissingSection() {
        LOGGER.debug("Entering getClonedMissingSection() of class "
                + "FixedPieDataModel.");
        final long startTime = System.currentTimeMillis();
        PieSectionDataModel missingClone = null;
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ObjectOutputStream(baos).writeObject(m_missingSection);
            final ByteArrayInputStream bais =
                new ByteArrayInputStream(baos.toByteArray());
            missingClone =
                (PieSectionDataModel)new ObjectInputStream(bais).readObject();
        } catch (final Exception e) {
            final String msg = "Exception while cloning pie sections: "
                + e.getMessage();
              LOGGER.debug(msg);
              throw new IllegalStateException(msg);
        }

        final long endTime = System.currentTimeMillis();
        final long durationTime = endTime - startTime;
        LOGGER.debug("Time for cloning. " + durationTime + " ms");
        LOGGER.debug("Exiting getClonedMissingSection() of class "
                + "FixedPieDataModel.");
        return missingClone;
    }
}
