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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Dec 19, 2007 (ohl): created
 */
package org.knime.base.node.preproc.cellsplitbypos;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is the model implementation of CellSplitterByPosition.
 * 
 * @author ohl, University of Konstanz
 */
public class CellSplitterByPosNodeModel extends NodeModel {

    private final SettingsModelString m_splitPoints =
            createSplitPointSettingsModel();

    private final SettingsModelString m_newColNames =
            createColNameSettingsModel();

    private final SettingsModelString m_selColumn =
            createColSelectSettingsModel();

    /*
     * static factory methods for the SettingsModels used here, and in the
     * NodeDialog.
     */
    /**
     * @return the settings model used to store the new col names in.
     */
    static SettingsModelString createColNameSettingsModel() {
        return new SettingsModelString("NewColNames", null);
    }

    /**
     * @return the settings model used to store the split points in.
     */
    static SettingsModelString createSplitPointSettingsModel() {
        return new SettingsModelString("SplitPoints", null);
    }

    /**
     * @return the settings model used to store the target column name in.
     */
    static SettingsModelString createColSelectSettingsModel() {
        return new SettingsModelString("SelColumn", null);
    }

    /** The constructor */
    CellSplitterByPosNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        BufferedDataTable outTable =
                exec.createColumnRearrangeTable(inData[0],
                        createColumnRearranger(inData[0].getDataTableSpec()),
                        exec);

        return new BufferedDataTable[]{outTable};

    }

    /**
     * Creates the ColumnRearranger for the rearranger table. Also used to
     * compute the output table spec. Call only when valid user settings are
     * available!
     * 
     * @param inTableSpec the spec of the table to split the column from
     * @return the col rearranger
     */
    private ColumnRearranger createColumnRearranger(
            final DataTableSpec inTableSpec) throws InvalidSettingsException {
        // user settings must be set and valid
        assert m_splitPoints.getStringValue() != null;
        assert m_newColNames.getStringValue() != null;
        assert m_selColumn.getStringValue() != null;

        ColumnRearranger c = new ColumnRearranger(inTableSpec);
        c.append(new CellSplitterByPosCellFactory(inTableSpec, m_selColumn
                .getStringValue(), convertSplitPointsToInts(m_splitPoints
                .getStringValue()), convertNewColumnNames(m_newColNames
                .getStringValue(), inTableSpec)));
        return c;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

        if ((m_newColNames.getStringValue() == null)
                || (m_splitPoints.getStringValue() == null)
                || (m_selColumn.getStringValue() == null)) {
            throw new InvalidSettingsException("Please configure node.");
        }

        // selected column should exist in input table
        if (!inSpecs[0].containsName(m_selColumn.getStringValue())) {
            throw new InvalidSettingsException("Selected column '"
                    + m_selColumn.getStringValue()
                    + "' doesn't exist in input table");
        }
        // and it should be of type string
        if (!inSpecs[0].getColumnSpec(m_selColumn.getStringValue()).getType()
                .isCompatible(StringValue.class)) {
            throw new InvalidSettingsException("Selected column '"
                    + m_selColumn.getStringValue() + "' is not of type String");
        }

        // make sure the specified columns are not already in the input table
        String[] newCols = m_splitPoints.getStringValue().split(",");
        for (String c : newCols) {
            if (inSpecs[0].containsName(c)) {
                throw new InvalidSettingsException(
                        "Specified new column name '" + c
                                + "' already exists in input table");
            }
        }

        // now, user settings look good: we are good to go. Create out spec
        DataTableSpec outSpec = createColumnRearranger(inSpecs[0]).createSpec();
        return new DataTableSpec[]{outSpec};
    }

    /**
     * Converts the string argument that must contain comma separated numbers
     * larger than 0, into an array of int containing these numbers. It ensures
     * that the specified numbers are strictly increasing.
     * 
     * @param splitPoints the comma separated numbers (must be larger than 0)
     * @return these numbers in an array
     * @throws InvalidSettingsException if the string contains invalid numbers
     *             (i.e. not integers, or smaller than 1) or the specified
     *             numbers are not strictly increasing
     */
    private int[] convertSplitPointsToInts(final String splitPoints)
            throws InvalidSettingsException {
        String[] points = splitPoints.split(",");
        int[] result = new int[points.length];

        for (int i = 0; i < points.length; i++) {
            String point = points[i];
            try {
                int p = Integer.parseInt(point);
                if (p < 1) {
                    throw new InvalidSettingsException("Only split points "
                            + "larger than 0 are valid");
                }
                result[i] = p;

            } catch (NumberFormatException nfe) {
                throw new InvalidSettingsException("Split points must be "
                        + "integer numbers larger than 0. ('" + point
                        + "' is not)");
            }
        }

        int prev = 0;
        for (int p : result) {
            if (p <= prev) {
                throw new InvalidSettingsException("Please specify strictly "
                        + "increasing split point numbers");
            }
            prev = p;
        }

        return result;
    }

    /**
     * Converts the string argument that must contain comma separated column
     * names into an array of String containing these names. It ensures that the
     * specified names are not empty and unique - and if a spec is provided it
     * also complains if the new names are contained in this spec.
     * 
     * @param colNames the comma separated new column names
     * @param inSpec the spec to test the names against (can be null)
     * @return these names in an array
     * @throws InvalidSettingsException if the string contains invalid names
     *             (i.e. empty or not unique) or the specified names are
     *             contained in the spec (if provided).
     */
    private String[] convertNewColumnNames(final String colNames,
            final DataTableSpec inSpec) throws InvalidSettingsException {

        String[] names = colNames.split(",");

        for (int c = 1; c < names.length; c++) {
            if (names[c] == null) {
                throw new InvalidSettingsException("<null> is not a valid"
                        + " column name.");
            }
            if (names[c].length() == 0) {
                throw new InvalidSettingsException("Empty column names are not"
                        + "valid.");
            }
            if ((inSpec != null) && inSpec.containsName(names[c])) {
                throw new InvalidSettingsException("Column name '" + names[c]
                        + "' is already contained in the input table");
            }

            // make sure we haven't seen that name before
            for (int i = 0; i < c; i++) {
                if (names[i].equals(names[c])) {
                    throw new InvalidSettingsException("Column name '"
                            + names[i] + "' appears twice in the list of"
                            + " new column names");
                }
            }
        }

        return names;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {

        m_splitPoints.saveSettingsTo(settings);
        m_newColNames.saveSettingsTo(settings);
        m_selColumn.saveSettingsTo(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {

        m_splitPoints.loadSettingsFrom(settings);
        m_newColNames.loadSettingsFrom(settings);
        m_selColumn.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {

        SettingsModelString tmpSplits =
                m_splitPoints.createCloneWithValidatedValue(settings);
        SettingsModelString tmpColNames =
                m_newColNames.createCloneWithValidatedValue(settings);

        // null or empty string is invalid for split points or colNames
        if ((tmpSplits.getStringValue() == null)
                || tmpSplits.getStringValue().equals("")) {
            throw new InvalidSettingsException("Please specify split points");
        }
        if ((tmpColNames.getStringValue() == null)
                || tmpColNames.getStringValue().equals("")) {
            throw new InvalidSettingsException(
                    "Please specify new column names");
        }

        int[] points = convertSplitPointsToInts(tmpSplits.getStringValue());
        String[] colNames =
                convertNewColumnNames(tmpColNames.getStringValue(), null);

        // make sure enough (and not too many) col names are specified
        if (points.length + 1 != colNames.length) {
            throw new InvalidSettingsException(
                    "Number of new column names is not as "
                            + "required for the specified splits "
                            + "(no. of new columns: " + (points.length + 1)
                            + ")");
        }

        m_splitPoints.validateSettings(settings);
        m_newColNames.validateSettings(settings);
        m_selColumn.validateSettings(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

}
