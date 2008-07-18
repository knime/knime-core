/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.pie.datamodel.PieDataModel;
import org.knime.base.node.viz.pie.datamodel.PieSectionDataModel;
import org.knime.base.node.viz.pie.datamodel.PieVizModel;
import org.knime.base.node.viz.pie.util.PieColumnFilter;
import org.knime.base.node.viz.pie.util.TooManySectionsException;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;


/**
 * This is the interactive implementation of the {@link PieVizModel}.
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractivePieVizModel extends PieVizModel {

    private final InteractivePieDataModel m_model;

    private DataColumnSpec m_pieColSpec;

    private DataColumnSpec m_aggrColSpec;

    private final List<PieSectionDataModel> m_sections =
        new ArrayList<PieSectionDataModel>();

    private PieSectionDataModel m_missingSection;


    /**Constructor for class InteractivePieVizModel.
     * @param model the data model
     * @param pieColumn the name of the pie column
     * @param aggrCol the name of the aggregation column
     * @throws TooManySectionsException if more sections are created than
     * supported
     */
    public InteractivePieVizModel(final InteractivePieDataModel model,
            final String pieColumn, final String aggrCol)
    throws TooManySectionsException {
        super(model.supportsHiliting(), model.detailsAvailable());
        m_model = model;
        m_aggrColSpec = getColSpec(aggrCol);
        setPieColumn(pieColumn);
    }

    /**
     * @param pieColName the name of the pie column
     * @return <code>true</code> if the name has changed
     * @throws TooManySectionsException if more sections are created than
     * supported
     */
    public boolean setPieColumn(final String pieColName)
        throws TooManySectionsException {
        if (pieColName == null) {
            throw new NullPointerException("pieCol must not be null");
        }
        if (m_pieColSpec != null
                && m_pieColSpec.getName().equals(pieColName)) {
            return false;
        }
        m_pieColSpec = getColSpec(pieColName);
        createSectionsWithData();
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPieColumnName() {
        return m_pieColSpec.getName();
    }

    /**
     * @param aggrColName the optional name of the aggregation column
     * @return <code>true</code> if the name has changed
     * @throws TooManySectionsException if more sections are created than
     * supported
     */
    public boolean setAggrColumn(final String aggrColName)
        throws TooManySectionsException {
        if (aggrColName == null && m_aggrColSpec == null) {
            return false;
        }
        if (m_aggrColSpec != null
                && m_aggrColSpec.getName().equals(aggrColName)) {
            return false;
        }
        if (aggrColName == null) {
            m_aggrColSpec = null;
            setAggregationMethod(AggregationMethod.COUNT);
        } else {
            m_aggrColSpec = getColSpec(aggrColName);
        }
        createSectionsWithData();
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAggregationColumnName() {
        if (m_aggrColSpec == null) {
            return null;
        }
        return m_aggrColSpec.getName();
    }

    /**
     * @param value the value to get the section for
     * @return the{@link PieSectionDataModel} which represents the given value
     * or <code>null</code> if no section is found for the given value
     */
    private PieSectionDataModel getSection(final DataCell value) {
        for (final PieSectionDataModel section : m_sections) {
            if (section.getName().equals(value.toString())) {
                return section;
            }
        }
        return null;
    }

    /**
     * Creates the sections for the selected pie column and adds all rows to
     * the appropriate section.
     * @throws TooManySectionsException if more sections are created than
     * supported
     */
    private void createSectionsWithData() throws TooManySectionsException {
        m_sections.clear();
        m_missingSection = PieDataModel.createDefaultMissingSection(
                m_model.supportsHiliting());
        addRows2Sections();
        final boolean numeric =
            m_pieColSpec.getType().isCompatible(DoubleValue.class);
        PieDataModel.sortSections(getSections(), numeric, true);
        //set the section color only if the colorized column is selected
        //as aggregation column
        if (m_pieColSpec.getColorHandler() != null) {
            PieDataModel.setSectionColor(m_sections);
        }
    }

    /**
     * Adds all rows to the available sections.
     * @throws TooManySectionsException if more sections are created than
     * supported
     */
    private void addRows2Sections() throws TooManySectionsException {
        final int pieColIdx = m_model.getColIndex(m_pieColSpec.getName());
        final int aggrColIdx;
        if (m_aggrColSpec == null) {
            aggrColIdx = -1;
        } else {
            aggrColIdx = m_model.getColIndex(m_aggrColSpec.getName());
        }
        for (final DataRow row : m_model) {
            final DataCell pieCell = row.getCell(pieColIdx);
            final DataCell aggrCell;
            if (aggrColIdx < 0) {
                aggrCell = null;
            } else {
                aggrCell = row.getCell(aggrColIdx);
            }
            final Color rowColor = m_model.getRowColor(row);
            PieSectionDataModel section;
            if (pieCell.isMissing()) {
                section = getMissingSection();
            } else {
                section = getSection(pieCell);
                if (section == null) {
                    if (m_sections.size()
                            >= PieColumnFilter.MAX_NO_OF_SECTIONS) {
                        throw new TooManySectionsException(
                                "Selected pie column contains more than "
                                + PieColumnFilter.MAX_NO_OF_SECTIONS
                                + " unique values.");
                    }
//                  throw new IllegalArgumentException("No section found for: "
//                            + pieCell.toString());
                    section = new PieSectionDataModel(pieCell.toString(),
                                Color.BLACK, m_model.supportsHiliting());
                    m_sections.add(section);
                }
            }
            section.addDataRow(rowColor, row.getKey(), aggrCell);
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

    /**
     * @return the {@link DataTableSpec} of the input data
     */
    public DataTableSpec getTableSpec() {
        return m_model.getDataTableSpec();
    }
}
