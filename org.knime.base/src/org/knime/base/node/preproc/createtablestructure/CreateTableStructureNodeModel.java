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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Aug 7, 2010 (wiswedel): created
 */
package org.knime.base.node.preproc.createtablestructure;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is the model implementation of CreateTableStructure. Creates empty table
 * (no rows) with a predefined structure (columns)
 *
 * @author Bernd.Wiswedel
 */
public class CreateTableStructureNodeModel extends NodeModel {

    /** Output column type. */
    static enum ColType {
        /** String. */
        String(StringCell.TYPE),
        /** Integer. */
        Integer(IntCell.TYPE),
        /** Double. */
        Double(DoubleCell.TYPE);

        private final DataType m_type;

        private ColType(final DataType type) {
            m_type = type;
        }

        /** @return associated type. */
        public DataType getType() {
            return m_type;
        }
    }

    private final SettingsModelString m_colPrefixModel;

    private final SettingsModelIntegerBounded m_colCountModel;

    private final SettingsModelString m_colTypeModel;

    /**
     * Constructor for the node model.
     */
    protected CreateTableStructureNodeModel() {
        super(0, 1);
        m_colCountModel = createColCountModel();
        m_colPrefixModel = createColPrefixModel();
        m_colTypeModel = createColTypeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataContainer cont = exec.createDataContainer(createSpec());
        cont.close();
        return new BufferedDataTable[]{cont.getTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{createSpec()};
    }

    private DataTableSpec createSpec() throws InvalidSettingsException {
        String prefix = m_colPrefixModel.getStringValue();
        if (prefix == null || prefix.isEmpty()) {
            throw new InvalidSettingsException("Invalid (empty) prefix");
        }
        String typeS = m_colTypeModel.getStringValue();
        DataType type;
        try {
            type = ColType.valueOf(typeS).getType();
        } catch (Exception e) {
            throw new InvalidSettingsException("Invalid type: '" + typeS + "'");
        }
        int count = m_colCountModel.getIntValue();
        DataColumnSpec[] colSpecs = new DataColumnSpec[count];
        for (int i = 0; i < count; i++) {
            colSpecs[i] =
                    new DataColumnSpecCreator(prefix + i, type).createSpec();
        }
        return new DataTableSpec("Table Structure", colSpecs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_colCountModel.saveSettingsTo(settings);
        m_colPrefixModel.saveSettingsTo(settings);
        m_colTypeModel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_colCountModel.loadSettingsFrom(settings);
        m_colPrefixModel.loadSettingsFrom(settings);
        m_colTypeModel.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_colCountModel.validateSettings(settings);
        m_colPrefixModel.validateSettings(settings);
        m_colTypeModel.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    /** @return new settings model for column prefix. */
    static SettingsModelString createColPrefixModel() {
        return new SettingsModelString("colPrefix", "Column ");
    }

    /** @return new settings model for column count. */
    static SettingsModelIntegerBounded createColCountModel() {
        return new SettingsModelIntegerBounded("colCount", 5, 0,
                Integer.MAX_VALUE);
    }

    /** @return new settings model for column type. */
    static SettingsModelString createColTypeModel() {
        return new SettingsModelString("colType", ColType.String.toString());
    }

}
