/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
    
    private static final String HILITE_MAPPING0 = "hilite_mapping0.xml.gz";
    private static final String HILITE_MAPPING1 = "hilite_mapping1.xml.gz";

    private final HiLiteTranslator m_trans0 = new HiLiteTranslator();
    private final HiLiteTranslator m_trans1 = new HiLiteTranslator();
    private final HiLiteManager m_outHiLiteHandler = new HiLiteManager();

    /**Constructor for class SetOperatorNodeModel.
     */
    protected SetOperatorNodeModel() {
        super(2, 1);
        m_setOp = new SettingsModelString(CFG_OP,
                SetOperation.getDefault().getName());
        m_sortInMemory = new SettingsModelBoolean(CFG_SORT_IN_MEMORY, false);
        m_skipMissing = new SettingsModelBoolean(CFG_SKIP_MISSING, true);
        m_outHiLiteHandler.addToHiLiteHandler(m_trans0.getFromHiLiteHandler());
        m_outHiLiteHandler.addToHiLiteHandler(m_trans1.getFromHiLiteHandler());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInHiLiteHandler(final int inIndex, 
            final HiLiteHandler hiLiteHdl) {
        if (inIndex == 0) {
            m_trans0.removeAllToHiliteHandlers();
            m_trans0.addToHiLiteHandler(hiLiteHdl);
        } else if (inIndex == 1) {
            m_trans1.removeAllToHiliteHandlers();
            m_trans1.addToHiLiteHandler(hiLiteHdl);
        }
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
            m_trans0.setMapper(
                    new DefaultHiLiteMapper(table.getHiliteMapping0()));
            m_trans1.setMapper(
                    new DefaultHiLiteMapper(table.getHiliteMapping1()));
        }
        return new BufferedDataTable[] {table.getBufferedTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        if (m_enableHilite.getBooleanValue()) {
            final NodeSettingsRO config0 = NodeSettings.loadFromXML(
                    new FileInputStream(new File(
                            nodeInternDir, HILITE_MAPPING0)));
            final NodeSettingsRO config1 = NodeSettings.loadFromXML(
                    new FileInputStream(
                            new File(nodeInternDir, HILITE_MAPPING1)));
            try {
                m_trans0.setMapper(DefaultHiLiteMapper.load(config0));
                m_trans1.setMapper(DefaultHiLiteMapper.load(config1));
            } catch (final InvalidSettingsException ex) {
                throw new IOException(ex);
            }
        }
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
                (DefaultHiLiteMapper) m_trans0.getMapper();
            if (mapper0 != null) {
                mapper0.save(config0);
            }
            config0.saveToXML(new FileOutputStream(
                            new File(nodeInternDir, HILITE_MAPPING0)));

            final NodeSettings config1 = new NodeSettings("hilite_mapping");
            final DefaultHiLiteMapper mapper1 =
                (DefaultHiLiteMapper) m_trans1.getMapper();
            if (mapper1 != null) {
                mapper1.save(config1);
            }
            config1.saveToXML(new FileOutputStream(
                            new File(nodeInternDir, HILITE_MAPPING1)));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_trans0.setMapper(null);
        m_trans1.setMapper(null);
    }
}

