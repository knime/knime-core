/*
 * ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.base.node.preproc.columnTrans;

import java.io.File;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class Many2OneColNodeModel extends NodeModel {

    /**
     * Possible methods to let a column match.
     *
     * @author Fabian Dill, University of Konstanz
     */
    public enum IncludeMethod {
        /** 1 matches, 0 not. */
        Binary,
        /** Maximum value of row matches. */
        Maximum,
        /** Minimum value of row matches. */
        Minimum,
        /** Regualr Expression pattern matches. */
        RegExpPattern
    }


    // our logger instance
//    private static final NodeLogger LOGGER = NodeLogger
//            .getLogger(Many2OneColNodeModel.class);

    /**The port were the  model expects the in data.*/
    public static final int DATA_IN_PORT = 0;

    /**The port which the model uses to return the data.*/
    public static final int DATA_OUT_PORT = 0;

    /**The name of the settings tag which holds the recognition values the user
     *  has entered in the dialog as <code>String</code>.*/
    public static final String RECOGNICTION_REGEX = "many2OneRecoValBox";

    /**The name of the settings tag which holds the names of the columns the
     * user has selected in the dialog as <code>String[]</code>.*/
    public static final String SELECTED_COLS = "many2OneCols2Condense";

    /**The name of the settings tag which holds the indices in the select field
     * of the columns the user has selected in the dialog as
     * <code>int[]</code>.*/
    public static final String SELECTED_COLS_IDX = "many2OneCols2CondenseIdx";

    /**The name of the settings tag which holds the new name of the condensed
     * column the user has entered in the dialog as <code>String</code>.*/
    public static final String CONDENSED_COL_NAME = "many2OneCondenseColName";

    /**The name of the settings tag which holds <code>true</code> if the user
     * wants have the selected condensing columns removed as
     * <code>boolean</code>.*/
    public static final String KEEP_COLS = "many2OneRemoveCols";

    /**The name of the settings tag which holds <code>true</code> if the user
     * wants that the node handles multiple occurrences as
     *  <code>boolean</code>.*/
    public static final String ALLOW_MULTIPLE = "many2OneAllowMultiple";

    /**
     * Config key for the method to determine whether the value of a column is
     * selected or not.
     */
    public static final String INCLUDE_METHOD = "includeMethod";


    private SettingsModelString m_appendedColumnName = new SettingsModelString(
            CONDENSED_COL_NAME, "Condensed Column");

    private SettingsModelFilterString m_includedColumns
        = new SettingsModelFilterString(SELECTED_COLS);

    private SettingsModelString m_includeMethod = new SettingsModelString(
            INCLUDE_METHOD, IncludeMethod.Binary.name());

    private SettingsModelString m_pattern = new SettingsModelString(
            RECOGNICTION_REGEX, "[^0]*");

    private SettingsModelBoolean m_keepColumns = new SettingsModelBoolean(
            KEEP_COLS, true);


    /**Constructor for class Many2OneColNodeModel.
     */
    protected Many2OneColNodeModel() {
//      we have one data in and out port and one model in port
        super(1, 1);
        // register the listener for the include method and pattern models
        m_includeMethod.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                m_pattern.setEnabled(
                        m_includeMethod.getStringValue().equals(
                                IncludeMethod.RegExpPattern.name()));
            }
        });
        // initially disable/enable
        m_pattern.setEnabled(m_includeMethod.getStringValue()
                .equals(Many2OneColNodeModel.IncludeMethod.
                        RegExpPattern.name()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {
        return new BufferedDataTable[] {exec.createColumnRearrangeTable(
                inData[0],
                createRearranger(inData[0].getDataTableSpec()), exec)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {

    }


    private ColumnRearranger createRearranger(final DataTableSpec spec) {
        ColumnRearranger rearranger = new ColumnRearranger(spec);
        CellFactory cellFactory;
        IncludeMethod method = IncludeMethod.valueOf(m_includeMethod
                .getStringValue());
        int[] includedColIndices = new int[m_includedColumns.
                                           getIncludeList().size()];
        int index = 0;
        for (String colName : m_includedColumns.getIncludeList()) {
            includedColIndices[index++] = spec.findColumnIndex(colName);
        }
        if (method.equals(IncludeMethod.RegExpPattern)) {
            cellFactory = new RegExpCellFactory(spec,
                    m_appendedColumnName.getStringValue(),
                    includedColIndices, m_pattern.getStringValue());
        } else if (method.equals(IncludeMethod.Minimum)) {
            cellFactory = new MinMaxCellFactory(spec,
                    m_appendedColumnName.getStringValue(),
                    includedColIndices, false);
        } else if (method.equals(IncludeMethod.Binary)) {
            cellFactory = new BinaryCellFactory(spec,
                    m_appendedColumnName.getStringValue(),
                    includedColIndices);
        } else {
            cellFactory = new MinMaxCellFactory(spec,
                    m_appendedColumnName.getStringValue(),
                    includedColIndices, true);
        }
        if (!m_keepColumns.getBooleanValue()) {
            rearranger.remove(includedColIndices);
        }
        rearranger.append(cellFactory);
        return rearranger;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_includedColumns.getIncludeList().size() <= 0) {
            setWarningMessage("No column selected. Node will have no effect!");
        }
        // if it is not a reg exp it must be double compatible
        if (!m_includeMethod.getStringValue().equals(
                IncludeMethod.RegExpPattern.name())) {
            for (String colName : m_includedColumns.getIncludeList()) {
                if (!inSpecs[0].getColumnSpec(colName).getType().isCompatible(
                        DoubleValue.class)) {
                    throw new InvalidSettingsException(
                            "For selected include method '"
                            + m_includeMethod.getStringValue()
                            + "' only double compatible values are allowed."
                            + " Column '" + colName + "' is not.");
                }
            }
        }
        return new DataTableSpec[] {createRearranger(inSpecs[0]).createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_appendedColumnName.saveSettingsTo(settings);
        m_includedColumns.saveSettingsTo(settings);
        m_includeMethod.saveSettingsTo(settings);
        m_pattern.saveSettingsTo(settings);
        m_keepColumns.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_appendedColumnName.loadSettingsFrom(settings);
        m_includedColumns.loadSettingsFrom(settings);
        m_includeMethod.loadSettingsFrom(settings);
        m_pattern.loadSettingsFrom(settings);
        m_keepColumns.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_appendedColumnName.validateSettings(settings);
        m_includedColumns.validateSettings(settings);
        m_includeMethod.validateSettings(settings);
        m_pattern.validateSettings(settings);
        m_keepColumns.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) {
        //nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) {
        //nothing to do
    }

}
