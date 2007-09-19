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

import java.awt.Color;
import java.util.List;

import org.knime.base.node.viz.pie.datamodel.PieDataModel;
import org.knime.base.node.viz.pie.datamodel.PieSectionDataModel;
import org.knime.base.node.viz.pie.datamodel.PieVizModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;


/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractivePieVizModel extends PieVizModel {

    private final InteractivePieDataModel m_model;

    private DataColumnSpec m_pieColSpec;

    private DataColumnSpec m_aggrColSpec;

    private List<PieSectionDataModel> m_sections;

    private PieSectionDataModel m_missingSection;

    /**Constructor for class InteractivePieVizModel.
     * @param model the data model
     * @param pieColumn the name of the pie column
     * @param aggrCol the name of the aggregation column
     */
    public InteractivePieVizModel(final InteractivePieDataModel model,
            final String pieColumn, final String aggrCol) {
        super(model);
        m_model = model;
        m_aggrColSpec = getColSpec(aggrCol);
        setPieColumn(pieColumn);
    }

    /**
     * @param pieColName the name of the pie column
     * @return <code>true</code> if the name has changed
     */
    public boolean setPieColumn(final String pieColName) {
        if (pieColName == null) {
            throw new NullPointerException("pieCol must not be null");
        }
        if (m_pieColSpec != null
                && m_pieColSpec.getName().equals(pieColName)) {
            return false;
        }
        m_pieColSpec = getColSpec(pieColName);
        createSections();
        addRows2Sections();
        return true;
    }

    /**
     * @param aggrColName the optional name of the aggregation column
     * @return <code>true</code> if the name has changed
     */
    public boolean setAggrColumn(final String aggrColName) {
        if (aggrColName == null && m_aggrColSpec == null) {
            return false;
        }
        if (m_aggrColSpec != null
                && m_aggrColSpec.getName().equals(aggrColName)) {
            return false;
        }
        m_aggrColSpec = getColSpec(aggrColName);
        createSections();
        addRows2Sections();
        return true;
    }

    /**
     * @param value the value to get the section for
     * @return the{@link PieSectionDataModel} which represents the given value
     * or <code>null</code> if no section is found for the given value
     */
    public PieSectionDataModel getSection(final DataCell value) {
        if (m_sections == null) {
            throw new IllegalStateException("No sections available. "
                    + "Viz model may not have been initialized.");
        }
        for (final PieSectionDataModel section : m_sections) {
            if (section.getName().equals(value.toString())) {
                return section;
            }
        }
        return null;
    }

    /**
     * Creates the sections for the selected pie column.
     */
    private void createSections() {
        m_sections = PieDataModel.createSections(m_pieColSpec,
                m_model.supportsHiliting());
        m_missingSection = PieDataModel.createDefaultMissingSection(
                m_model.supportsHiliting());
    }

    /**
     * Adds all rows to the available sections.
     */
    private void addRows2Sections() {
        final int pieColIdx = m_model.getColIndex(m_pieColSpec.getName());
        final int aggrColIdx;
        if (m_aggrColSpec == null) {
            aggrColIdx = -1;
        } else {
            aggrColIdx = m_model.getColIndex(m_aggrColSpec.getName());
        }
        for (final DataRow row : m_model.getDataRows()) {
            final DataCell pieCell = row.getCell(pieColIdx);
            final DataCell aggrCell;
            if (aggrColIdx < 0) {
                aggrCell = null;
            } else {
                aggrCell = row.getCell(aggrColIdx);
            }
            final Color rowColor = m_model.getRowColor(row);
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
    }

    /**
     * @param colName the column name to get the spec for
     * @return the {@link DataColumnSpec}
     */
    public DataColumnSpec getColSpec(final String colName) {
        final DataColumnSpec spec = m_model.getColSpec(colName);
        return spec;
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
