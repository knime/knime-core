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
 *    22.11.2007 (Tobias Koetter): created
 */

package org.knime.base.node.preproc.setoperator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteManager;
import org.knime.core.node.property.hilite.HiLiteTranslator;


/**
 * This class is the node implementation of the set operation node.
 * @author Tobias Koetter, University of Konstanz
 */
public class SetOperatorNodeModel extends NodeModel {

    /**Config key of the first set column.*/
    protected static final String CFG_COL1 = "col1";
    /**Config key of the second set column.*/
    protected static final String CFG_COL2 = "col2";
    /**Config key of the set operation.*/
    protected static final String CFG_OP = "operation";
    /**Config key for sort in memory.*/
    protected static final String CFG_SORT_IN_MEMORY = "sortInMemory";
    /**Config key for ignore missing option.*/
    protected static final String CFG_SKIP_MISSING = "skipMissing";
    /**Configuration key for the enable hilite option.*/
    protected static final String CFG_ENABLE_HILITE = "enableHilite";

    private final SettingsModelColumnName m_col1 =
        new SettingsModelColumnName(SetOperatorNodeModel.CFG_COL1, null);
    private final SettingsModelColumnName m_col2 =
        new SettingsModelColumnName(SetOperatorNodeModel.CFG_COL2, null);

    private final SettingsModelString m_setOp;

    private final SettingsModelBoolean m_enableHilite =
        new SettingsModelBoolean(CFG_ENABLE_HILITE, false);

    private final SettingsModelBoolean m_sortInMemory;

    private final SettingsModelBoolean m_skipMissing;

    private final HiLiteTranslator m_hilite0 = new HiLiteTranslator();
    private final HiLiteTranslator m_hilite1 = new HiLiteTranslator();
    private final HiLiteManager m_outHiLiteHandler = new HiLiteManager();

    /**Constructor for class SetOperatorNodeModel.
     */
    protected SetOperatorNodeModel() {
        super(2, 1);
        m_setOp = new SettingsModelString(CFG_OP,
                SetOperation.getDefault().getName());
        m_sortInMemory = new SettingsModelBoolean(CFG_SORT_IN_MEMORY, false);
        m_skipMissing = new SettingsModelBoolean(CFG_SKIP_MISSING, true);
        m_outHiLiteHandler.addToHiLiteHandler(m_hilite0.getFromHiLiteHandler());
        m_outHiLiteHandler.addToHiLiteHandler(m_hilite1.getFromHiLiteHandler());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        return m_outHiLiteHandler.getFromHiLiteHandler();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_col1.loadSettingsFrom(settings);
        m_col2.loadSettingsFrom(settings);
        m_setOp.loadSettingsFrom(settings);
        m_sortInMemory.loadSettingsFrom(settings);
        m_skipMissing.loadSettingsFrom(settings);
        m_enableHilite.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_col1.saveSettingsTo(settings);
        m_col2.saveSettingsTo(settings);
        m_setOp.saveSettingsTo(settings);
        m_sortInMemory.saveSettingsTo(settings);
        m_skipMissing.saveSettingsTo(settings);
        m_enableHilite.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_col1.validateSettings(settings);
        m_col2.validateSettings(settings);
        m_setOp.validateSettings(settings);
        m_skipMissing.validateSettings(settings);
        m_enableHilite.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (inSpecs.length < 2) {
            throw new IllegalArgumentException("Two input tables expected");
        }
        final SetOperation op =
            SetOperation.getOperation4Name(m_setOp.getStringValue());
        final DataColumnSpec col1Spec;
        if (m_col1.useRowID()) {
            col1Spec = SetOperationTable.createRowIDSpec("RowID1");
        } else {
            final String col1 = m_col1.getStringValue();
            if (col1 == null) {
                throw new InvalidSettingsException("No set 1 column defined");
            }
            col1Spec = inSpecs[0].getColumnSpec(col1);
            if (col1Spec == null) {
                throw new InvalidSettingsException(
                        "No column spec found for set 1 column: "
                        + m_col1.getStringValue());
            }
        }
        final DataColumnSpec col2Spec;
        if (m_col2.useRowID()) {
            col2Spec = SetOperationTable.createRowIDSpec("RowID2");
        } else {
            final String col2 = m_col2.getStringValue();
            if (col2 == null) {
                throw new InvalidSettingsException("No set 2 column defined");
            }
            col2Spec = inSpecs[1].getColumnSpec(col2);
            if (col2Spec == null) {
                throw new InvalidSettingsException(
                        "No column spec found for set 2 column: "
                        + m_col2.getStringValue());
            }
        }
        if (!col1Spec.getType().equals(col2Spec.getType())) {
            setWarningMessage("Different column types found. "
                    + "Result column will be of type String.");
        } else if (m_col1.useRowID() || m_col2.useRowID()) {
            setWarningMessage("Using row id. "
                    + "Result column will be of type String.");
        }
        final DataTableSpec spec =
            SetOperationTable.createResultTableSpec(op, col1Spec, col2Spec);
        return new DataTableSpec[]{spec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        if (inData.length < 2) {
            throw new IllegalArgumentException("Two input tables expected");
        }

        final SetOperation op =
            SetOperation.getOperation4Name(m_setOp.getStringValue());
        final SetOperationTable table = new SetOperationTable(exec,
                m_col1.useRowID(), m_col1.getColumnName(), inData[0],
                m_col2.useRowID(), m_col2.getColumnName(), inData[1],
                op, m_enableHilite.getBooleanValue(),
                m_skipMissing.getBooleanValue(),
                m_sortInMemory.getBooleanValue());
        if (m_enableHilite.getBooleanValue()) {
            initializeHiliteHandler(
                    new DefaultHiLiteMapper(table.getHiliteMapping0()),
                    new DefaultHiLiteMapper(table.getHiliteMapping1()));
        }
        return new BufferedDataTable[] {table.getBufferedTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
    throws IOException {
        if (m_enableHilite.getBooleanValue()) {
            final NodeSettingsRO config0 = NodeSettings.loadFromXML(
                    new FileInputStream(new File("hilite_mapping0.xml.gz")));
            final NodeSettingsRO config1 = NodeSettings.loadFromXML(
                    new FileInputStream(new File("hilite_mapping1.xml.gz")));
            try {
                initializeHiliteHandler(DefaultHiLiteMapper.load(config0),
                            DefaultHiLiteMapper.load(config1));
            } catch (final InvalidSettingsException ex) {
                throw new IOException(ex.getMessage());
            }
        }
    }

    private void initializeHiliteHandler(
            final DefaultHiLiteMapper defaultHiLiteMapper,
            final DefaultHiLiteMapper defaultHiLiteMapper2) {
        m_hilite0.addToHiLiteHandler(getInHiLiteHandler(0));
        m_hilite0.setMapper(defaultHiLiteMapper);
        m_hilite1.addToHiLiteHandler(getInHiLiteHandler(1));
        m_hilite1.setMapper(defaultHiLiteMapper2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
    throws IOException {
        if (m_enableHilite.getBooleanValue()) {
            final NodeSettings config0 = new NodeSettings("hilite_mapping");
            final DefaultHiLiteMapper mapper0 =
                (DefaultHiLiteMapper) m_hilite0.getMapper();
            if (mapper0 != null) {
                mapper0.save(config0);
            }
            config0.saveToXML(
                    new FileOutputStream(new File("hilite_mapping0.xml.gz")));

            final NodeSettings config1 = new NodeSettings("hilite_mapping");
            final DefaultHiLiteMapper mapper1 =
                (DefaultHiLiteMapper) m_hilite1.getMapper();
            if (mapper1 != null) {
                mapper1.save(config1);
            }
            config1.saveToXML(
                    new FileOutputStream(new File("hilite_mapping1.xml.gz")));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_hilite0.removeAllToHiliteHandlers();
        m_hilite0.getFromHiLiteHandler().fireClearHiLiteEvent();
        m_hilite0.setMapper(null);
        m_hilite1.removeAllToHiliteHandlers();
        m_hilite1.getFromHiLiteHandler().fireClearHiLiteEvent();
        m_hilite1.setMapper(null);
    }
}

