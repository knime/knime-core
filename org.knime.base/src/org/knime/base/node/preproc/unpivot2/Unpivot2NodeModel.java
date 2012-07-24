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
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.base.node.preproc.unpivot2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.base.data.append.column.AppendedColumnRow;
import org.knime.base.data.filter.column.FilterColumnRow;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteTranslator;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;

/**
 * Unpivoting node model which performs the UNPIVOTing operation based on
 * a number of selected retained and value columns.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
final class Unpivot2NodeModel extends NodeModel {

    private SettingsModelColumnFilter2 m_retainedColumns;

    private SettingsModelColumnFilter2 m_valueColumns;

    private final SettingsModelBoolean m_enableHilite =
        Unpivot2NodeDialogPane.createHiLiteModel();

    private final SettingsModelBoolean m_missingValues =
        Unpivot2NodeDialogPane.createMissingValueModel();

    private HiLiteTranslator m_trans = null;
    private final HiLiteHandler m_hilite = new HiLiteHandler();

    private static final String VALUE_COLUMN_VALUES = "ColumnValues";
    private static final String VALUE_COLUMN_NAMES = "ColumnNames";
    private static final String ROWID_COLUMN = "RowIDs";

    /**
     * Constructor that creates one data in- and one data out-port.
     */
    Unpivot2NodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        final DataTableSpec in = inSpecs[0];
        if (m_retainedColumns == null || m_valueColumns == null) {
            // auto-configure, no previous configuration
            m_retainedColumns = Unpivot2NodeDialogPane.createColumnFilterRetainedColumns();
            m_retainedColumns.loadDefaults(in);
            m_valueColumns = Unpivot2NodeDialogPane.createColumnFilterValueColumns();
            m_valueColumns.loadDefaults(in);
            setWarningMessage("Auto configuration: Using all suitable "
                    + "columns in Value and Retain column lists.");
        }
        return new DataTableSpec[]{createOutSpec(inSpecs[0])};
    }

    private DataTableSpec createOutSpec(final DataTableSpec spec) throws InvalidSettingsException {
        final FilterResult valueFilterResult = m_valueColumns.applyTo(spec);
        String[] valueColumns = valueFilterResult.getIncludes();
        if (valueColumns.length == 0) {
            throw new InvalidSettingsException(
            "No column 'value' defined for unpivoting operation.");
        }
        final String[] unknowns = valueFilterResult.getRemovedFromIncludes();
        if (unknowns.length  > 0) {
            setWarningMessage("Some selected value column(s) are no longer available: "
                    + ConvenienceMethods.getShortStringFrom(Arrays.asList(unknowns), 3));
        }
        String[] retainedColumns = m_retainedColumns.applyTo(spec).getIncludes();
        DataColumnSpec[] outSpecs = new DataColumnSpec[retainedColumns.length + 3];
        for (int i = 0; i < retainedColumns.length; i++) {
            outSpecs[i + 3] = spec.getColumnSpec(retainedColumns[i]);
        }
        DataType type = null;
        for (int i = 0; i < valueColumns.length; i++) {
            DataType ctype = spec.getColumnSpec(valueColumns[i]).getType();
            if (type == null) {
                type = ctype;
            } else {
                type = DataType.getCommonSuperType(type, ctype);
            }
        }
        int idx = 0;
        String colName = ROWID_COLUMN;
        while (spec.containsName(colName)) {
            colName = ROWID_COLUMN + "(" + (idx++) + ")";
        }
        outSpecs[0] = new DataColumnSpecCreator(colName,
                StringCell.TYPE).createSpec();
        idx = 0;
        colName = VALUE_COLUMN_NAMES;
        while (spec.containsName(colName)) {
            colName = VALUE_COLUMN_NAMES + "(" + (idx++) + ")";
        }
        outSpecs[1] = new DataColumnSpecCreator(
                colName, StringCell.TYPE).createSpec();
        idx = 0;
        colName = VALUE_COLUMN_VALUES;
        while (spec.containsName(colName)) {
            colName = VALUE_COLUMN_VALUES + "(" + (idx++) + ")";
        }
        outSpecs[2] = new DataColumnSpecCreator(colName, type).createSpec();
        return new DataTableSpec(outSpecs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec inSpec = inData[0].getSpec();
        String[] retainedColumns = m_retainedColumns.applyTo(inSpec).getIncludes();
        String[] valueColumns = m_valueColumns.applyTo(inSpec).getIncludes();
        int[] valueColumnIndices = new int[valueColumns.length];
        for (int i = 0; i < valueColumnIndices.length; i++) {
            valueColumnIndices[i] = inSpec.findColumnIndex(valueColumns[i]);
        }
        int[] orderColumnIdx = new int[retainedColumns.length];
        for (int i = 0; i < orderColumnIdx.length; i++) {
            orderColumnIdx[i] = inSpec.findColumnIndex(retainedColumns[i]);
        }
        final double newRowCnt = inData[0].getRowCount() * valueColumns.length;
        final boolean enableHilite = m_enableHilite.getBooleanValue();
        LinkedHashMap<RowKey, Set<RowKey>> map =
            new LinkedHashMap<RowKey, Set<RowKey>>();
        DataTableSpec outSpec = createOutSpec(inSpec);
        BufferedDataContainer buf = exec.createDataContainer(outSpec);
        final boolean skipMissings = m_missingValues.getBooleanValue();
        for (DataRow row : inData[0]) {
            LinkedHashSet<RowKey> set = new LinkedHashSet<RowKey>();
            FilterColumnRow crow = new FilterColumnRow(row, orderColumnIdx);
            for (int i = 0; i < valueColumns.length; i++) {
                String colName = valueColumns[i];
                DataCell acell = row.getCell(valueColumnIndices[i]);
                if (acell.isMissing() && skipMissings) {
                    // skip rows containing missing cells (in Value column(s))
                    continue;
                }
                RowKey rowKey = RowKey.createRowKey(buf.size());
                if (enableHilite) {
                    set.add(rowKey);
                }
                DefaultRow drow = new DefaultRow(rowKey,
                        new StringCell(row.getKey().getString()),
                        new StringCell(colName), acell);
                buf.addRowToTable(new AppendedColumnRow(rowKey, drow, crow));
                exec.checkCanceled();
                exec.setProgress(buf.size() / newRowCnt);
            }
            if (enableHilite) {
                map.put(crow.getKey(), set);
            }
        }
        buf.close();
        if (enableHilite) {
            m_trans.setMapper(new DefaultHiLiteMapper(map));
        } else {
            m_trans.setMapper(null);
        }
        return new BufferedDataTable[]{buf.getTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        if (m_trans != null) {
            m_trans.setMapper(null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInHiLiteHandler(final int inIndex,
            final HiLiteHandler hiLiteHdl) {
        if (m_trans == null) {
            m_trans = new HiLiteTranslator(hiLiteHdl);
            m_trans.addToHiLiteHandler(m_hilite);
        } else if (m_trans.getFromHiLiteHandler() != hiLiteHdl) {
            m_trans.removeAllToHiliteHandlers();
            m_trans.setMapper(null);
            m_trans.addToHiLiteHandler(m_hilite);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        return m_hilite;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        if (m_enableHilite.getBooleanValue()) {
            final NodeSettings config = new NodeSettings("hilite_mapping");
            ((DefaultHiLiteMapper) m_trans.getMapper()).save(config);
            config.saveToXML(new GZIPOutputStream(new FileOutputStream(new File(
                    nodeInternDir, "hilite_mapping.xml.gz"))));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        if (m_enableHilite.getBooleanValue()) {
            final NodeSettingsRO config = NodeSettings.loadFromXML(
                    new GZIPInputStream(new FileInputStream(
                    new File(nodeInternDir, "hilite_mapping.xml.gz"))));
            try {
                m_trans.setMapper(DefaultHiLiteMapper.load(config));
            } catch (final InvalidSettingsException ex) {
                throw new IOException(ex.getMessage());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_retainedColumns.loadSettingsFrom(settings);
        m_valueColumns.loadSettingsFrom(settings);
        m_enableHilite.loadSettingsFrom(settings);
        try {
            m_missingValues.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ise) {
            // ignored: new with 2.1.1+
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_retainedColumns.saveSettingsTo(settings);
        m_valueColumns.saveSettingsTo(settings);
        m_enableHilite.saveSettingsTo(settings);
        m_missingValues.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_retainedColumns.validateSettings(settings);
        m_valueColumns.validateSettings(settings);
        m_enableHilite.validateSettings(settings);
    }

}
