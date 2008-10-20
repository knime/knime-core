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
 * --------------------------------------------------------------------- *
 *
 * History
 *   03.05.2007 (gabriel): created
 */
package org.knime.base.node.preproc.pivot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteTranslator;
import org.knime.core.util.Pair;

/**
 * The pivoting node uses on column as grouping (row header) and one as pivoting
 * column (column header) to aggregate a column by its values. One additional
 * column (table content) can be selected the compute an aggregation value
 * for each pair of pivot and group value.
 *
 * @see PivotAggregationMethod
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public class PivotNodeModel extends NodeModel {

    private final SettingsModelString m_group =
        PivotNodeDialogPane.createSettingsGroup();
    private final SettingsModelString m_pivot =
        PivotNodeDialogPane.createSettingsPivot();

    private final SettingsModelString m_agg =
        PivotNodeDialogPane.createSettingsAggregation();
    private final SettingsModelString m_aggMethod =
        PivotNodeDialogPane.createSettingsAggregationMethod();
    private final SettingsModelString m_makeAgg =
        PivotNodeDialogPane.createSettingsMakeAggregation();

    private final SettingsModelBoolean m_hiliting =
        PivotNodeDialogPane.createSettingsEnableHiLite();
    
    private final SettingsModelBoolean m_ignoreMissValues =
        PivotNodeDialogPane.createSettingsMissingValues();

    /**
     * Node returns a new hilite handler instance.
     */
    private final HiLiteTranslator m_hilite = new HiLiteTranslator();

    /**
     * Creates a new pivot model with one in- and out-port.
     */
    public PivotNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        final int group = inSpecs[0].findColumnIndex(m_group.getStringValue());
        if (group < 0) {
            throw new InvalidSettingsException("Group column not found.");
        }
        final int pivot = inSpecs[0].findColumnIndex(m_pivot.getStringValue());
        if (pivot < 0) {
            throw new InvalidSettingsException("Pivot column not found.");
        }
        if (m_makeAgg.getStringValue().equals(
                PivotNodeDialogPane.MAKE_AGGREGATION[1])) {
        final int agg = inSpecs[0].findColumnIndex(m_agg.getStringValue());
            if (agg < 0) {
                throw new InvalidSettingsException(
                        "Aggregation column not found.");
            }
        }
        final DataColumnSpec cspec = inSpecs[0].getColumnSpec(pivot);
        if (!cspec.getDomain().hasValues()) {
            return new DataTableSpec[1];
        } else {
            final Set<DataCell> vals = new LinkedHashSet<DataCell>(
                    cspec.getDomain().getValues());
            if (!m_ignoreMissValues.getBooleanValue()) {
                vals.add(DataType.getMissingCell());
            }
            return new DataTableSpec[]{initSpec(vals)};
        }
    }

    /**
     * Creates a new DataTableSpec using the given possible values, each
     * of them as one double-type column.
     * @param vals possible values
     * @return possible values as DataTableSpec
     */
    private DataTableSpec initSpec(final Set<DataCell> vals) {
        final String[] names = new String[vals.size()];
        final DataType[] types = new DataType[vals.size()];
        final DataType setType;
        if (m_makeAgg.getStringValue().equals(
                PivotNodeDialogPane.MAKE_AGGREGATION[0])) {
            setType = IntCell.TYPE;
        } else {
            setType = DoubleCell.TYPE;
        }
        int idx = 0;
        for (final DataCell val : vals) {
            names[idx] = val.toString();
            types[idx] = setType;
            idx++;
        }
        return new DataTableSpec(names, types);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        final DataTableSpec inspec = inData[0].getDataTableSpec();
        final int group = inspec.findColumnIndex(m_group.getStringValue());
        final int pivot = inspec.findColumnIndex(m_pivot.getStringValue());
        final int aggre = (m_makeAgg.getStringValue().equals(
                PivotNodeDialogPane.MAKE_AGGREGATION[1])
                ? inspec.findColumnIndex(m_agg.getStringValue()) : -1);
        PivotAggregationMethod aggMethod;
        if (aggre < 0) {
            aggMethod = PivotAggregationMethod.COUNT;
        } else {
            aggMethod = PivotAggregationMethod.METHODS.get(
                        m_aggMethod.getStringValue());
        }
        // pair contains group and pivot plus the aggregation value
        final Map<Pair<DataCell, DataCell>, Double[]> map =
            new LinkedHashMap<Pair<DataCell, DataCell>, Double[]>();
        // list of pivot values
        final Set<DataCell> pivotList = new LinkedHashSet<DataCell>();
        DataColumnSpec pivotSpec = inspec.getColumnSpec(pivot);
        if (pivotSpec.getDomain().hasValues()) {
            pivotList.addAll(pivotSpec.getDomain().getValues());
        }
        // list of group values
        final Set<DataCell> groupList = new LinkedHashSet<DataCell>();
        final LinkedHashMap<RowKey, Set<RowKey>> mapping =
            new LinkedHashMap<RowKey, Set<RowKey>>();
        final double nrRows = inData[0].getRowCount();
        int rowCnt = 0;
        ExecutionContext subExec = exec.createSubExecutionContext(0.75);
        boolean containsMissing = false;
        // final all group, pivot pair and aggregate the values of each group
        for (final DataRow row : inData[0]) {
            subExec.checkCanceled();
            subExec.setProgress(++rowCnt / nrRows,
                    "Aggregating row: \"" + row.getKey().getString() + "\" ("
                    + rowCnt + "\\" + (int) nrRows + ")");
            final DataCell groupCell = row.getCell(group);
            groupList.add(groupCell);
            final DataCell pivotCell = row.getCell(pivot);
            // if missing values should be ignored
            if (pivotCell.isMissing()) {
                if (m_ignoreMissValues.getBooleanValue()) {
                    containsMissing = true;
                    continue;
                }
            }
            pivotList.add(pivotCell);
            final Pair<DataCell, DataCell> pair =
                new Pair<DataCell, DataCell>(groupCell, pivotCell);
            Double[] aggValue = map.get(pair);
            if (aggValue == null) {
                aggValue = aggMethod.init();
                map.put(pair, aggValue);
            }
            if (aggre < 0) {
                aggMethod.compute(aggValue, null);
            } else {
                final DataCell value = row.getCell(aggre);
                aggMethod.compute(aggValue, value);
            }
            if (m_hiliting.getBooleanValue()) {
                final RowKey groupKey = new RowKey(groupCell.toString());
                Set<RowKey> set = mapping.get(groupKey);
                if (set == null) {
                    set = new LinkedHashSet<RowKey>();
                    mapping.put(groupKey, set);
                }
                set.add(row.getKey());
            }
        }
        // check pivoted elements for missing values
        if (containsMissing) {
            setWarningMessage("Pivot column \"" + m_pivot.getStringValue() 
                + "\" contains missing values which are ignored.");
        }

        final DataTableSpec outspec = initSpec(pivotList);
        // will contain the final pivoting table
        final BufferedDataContainer buf = exec.createDataContainer(outspec);
        final double nrElements = groupList.size();
        int elementCnt = 0;
        subExec = exec.createSubExecutionContext(0.25);
        for (final DataCell groupCell : groupList) {
            subExec.checkCanceled();
            subExec.setProgress(++elementCnt / nrElements,
                    "Computing aggregation of group \"" + groupCell + "\" ("
                    + elementCnt + "\\" + (int) nrElements + ")");
            // contains the aggregated values
            final DataCell[] aggValues = new DataCell[pivotList.size()];
            int idx = 0; // pivot index
            for (final DataCell pivotCell : pivotList) {
                final Pair<DataCell, DataCell> newPair =
                    new Pair<DataCell, DataCell>(groupCell, pivotCell);
                final Double[] aggValue = map.get(newPair);
                aggValues[idx] = aggMethod.done(aggValue);
                idx++;
            }
            // create new row with the given group id and aggregation values
            buf.addRowToTable(new DefaultRow(groupCell.toString(), aggValues));
        }
        buf.close();
        if (m_hiliting.getBooleanValue()) {
            m_hilite.setMapper(new DefaultHiLiteMapper(mapping));
        }
        return new BufferedDataTable[]{buf.getTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        if (m_hiliting.getBooleanValue()) {
            final NodeSettingsRO config = NodeSettings.loadFromXML(
                    new GZIPInputStream(new FileInputStream(
                    new File(nodeInternDir, "hilite_mapping.xml.gz"))));
            try {
                m_hilite.setMapper(DefaultHiLiteMapper.load(config));
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
        m_group.loadSettingsFrom(settings);
        m_pivot.loadSettingsFrom(settings);
        m_agg.loadSettingsFrom(settings);
        m_aggMethod.loadSettingsFrom(settings);
        m_makeAgg.loadSettingsFrom(settings);
        m_hiliting.loadSettingsFrom(settings);
        m_ignoreMissValues.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_hilite.getFromHiLiteHandler().fireClearHiLiteEvent();
        m_hilite.setMapper(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        if (m_hiliting.getBooleanValue()) {
            final NodeSettings config = new NodeSettings("hilite_mapping");
            ((DefaultHiLiteMapper) m_hilite.getMapper()).save(config);
            config.saveToXML(new GZIPOutputStream(new FileOutputStream(new File(
                    nodeInternDir, "hilite_mapping.xml.gz"))));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_group.saveSettingsTo(settings);
        m_pivot.saveSettingsTo(settings);
        m_agg.saveSettingsTo(settings);
        m_aggMethod.saveSettingsTo(settings);
        m_makeAgg.saveSettingsTo(settings);
        m_hiliting.saveSettingsTo(settings);
        m_ignoreMissValues.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_group.validateSettings(settings);
        m_pivot.validateSettings(settings);
        m_agg.validateSettings(settings);
        m_aggMethod.validateSettings(settings);
        m_makeAgg.validateSettings(settings);
        m_hiliting.validateSettings(settings);
        m_ignoreMissValues.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInHiLiteHandler(final int inIndex,
            final HiLiteHandler hiLiteHdl) {
        m_hilite.removeAllToHiliteHandlers();
        if (hiLiteHdl != null) {
            m_hilite.addToHiLiteHandler(hiLiteHdl);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        assert outIndex == 0;
        return m_hilite.getFromHiLiteHandler();
    }

}
