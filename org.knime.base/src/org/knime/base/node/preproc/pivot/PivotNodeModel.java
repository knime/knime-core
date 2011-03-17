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
package org.knime.base.node.preproc.pivot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
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
import org.knime.core.data.DoubleValue;
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
    private final HiLiteTranslator m_translator = new HiLiteTranslator();

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
            if (!inSpecs[0].getColumnSpec(agg).getType().isCompatible(
                    DoubleValue.class)) {
                throw new InvalidSettingsException(
                        "Selected aggregation column '"
                        + m_agg.getStringValue() + "' not of type double.");
            }
        }
        final DataColumnSpec cspec = inSpecs[0].getColumnSpec(pivot);
        if (cspec.getDomain().hasValues()) {
            if (m_ignoreMissValues.getBooleanValue()) {
                final Set<DataCell> vals = new LinkedHashSet<DataCell>(
                        cspec.getDomain().getValues());
                Set<String> pivotList = new LinkedHashSet<String>();
                for (DataCell domValue : vals) {
                    pivotList.add(domValue.toString());
                }
                return new DataTableSpec[]{initSpec(pivotList)};
            } else {
                return new DataTableSpec[1];
            }
        } else {
            return new DataTableSpec[1];
        }
    }

    /**
     * Creates a new DataTableSpec using the given possible values, each
     * of them as one double-type column.
     * @param vals possible values
     * @return possible values as DataTableSpec
     */
    private DataTableSpec initSpec(final Set<String> vals) {
        final DataType setType;
        if (m_makeAgg.getStringValue().equals(
                PivotNodeDialogPane.MAKE_AGGREGATION[0])) {
            setType = IntCell.TYPE;
        } else {
            setType = DoubleCell.TYPE;
        }
        DataType[] types = new DataType[vals.size()];
        Arrays.fill(types, setType);
        String[] names = vals.toArray(new String[vals.size()]);
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
        final Map<Pair<String, String>, Double[]> map =
            new LinkedHashMap<Pair<String, String>, Double[]>();
        // list of pivot values
        final Set<String> pivotList = new LinkedHashSet<String>();
        final DataColumnSpec pivotSpec = inspec.getColumnSpec(pivot);
        if (pivotSpec.getDomain().hasValues()) {
            for (DataCell domValue : pivotSpec.getDomain().getValues()) {
                pivotList.add(domValue.toString());
            }
        }
        // list of group values
        final Set<String> groupList = new LinkedHashSet<String>();
        final LinkedHashMap<RowKey, Set<RowKey>> mapping =
            new LinkedHashMap<RowKey, Set<RowKey>>();
        final double nrRows = inData[0].getRowCount();
        int rowCnt = 0;
        ExecutionContext subExec = exec.createSubExecutionContext(0.75);
        // final all group, pivot pair and aggregate the values of each group
        for (final DataRow row : inData[0]) {
            subExec.checkCanceled();
            subExec.setProgress(++rowCnt / nrRows,
                    "Aggregating row: \"" + row.getKey().getString() + "\" ("
                    + rowCnt + "\\" + (int) nrRows + ")");
            final String groupString = row.getCell(group).toString();
            groupList.add(groupString);
            final DataCell pivotCell = row.getCell(pivot);
            // if missing values should be ignored
            if (pivotCell.isMissing()) {
                if (m_ignoreMissValues.getBooleanValue()) {
                    continue;
                }
            }
            final String pivotString = pivotCell.toString();
            pivotList.add(pivotString);
            final Pair<String, String> pair =
                new Pair<String, String>(groupString, pivotString);
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
                final RowKey groupKey = new RowKey(groupString);
                Set<RowKey> set = mapping.get(groupKey);
                if (set == null) {
                    set = new LinkedHashSet<RowKey>();
                    mapping.put(groupKey, set);
                }
                set.add(row.getKey());
            }
        }

        final DataTableSpec outspec = initSpec(pivotList);
        // will contain the final pivoting table
        final BufferedDataContainer buf = exec.createDataContainer(outspec);
        final double nrElements = groupList.size();
        int elementCnt = 0;
        subExec = exec.createSubExecutionContext(0.25);
        for (final String groupString : groupList) {
            subExec.checkCanceled();
            subExec.setProgress(++elementCnt / nrElements,
                    "Computing aggregation of group \"" + groupString + "\" ("
                    + elementCnt + "\\" + (int) nrElements + ")");
            // contains the aggregated values
            final DataCell[] aggValues = new DataCell[pivotList.size()];
            int idx = 0; // pivot index
            for (final String pivotString : pivotList) {
                final Pair<String, String> newPair =
                    new Pair<String, String>(groupString, pivotString);
                final Double[] aggValue = map.get(newPair);
                aggValues[idx] = aggMethod.done(aggValue);
                idx++;
            }
            // create new row with the given group id and aggregation values
            buf.addRowToTable(new DefaultRow(groupString, aggValues));
        }
        buf.close();
        if (m_hiliting.getBooleanValue()) {
            m_translator.setMapper(new DefaultHiLiteMapper(mapping));
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
                m_translator.setMapper(DefaultHiLiteMapper.load(config));
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
        try {
            m_ignoreMissValues.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ise) {
            // new with 1.3.5
            m_ignoreMissValues.setBooleanValue(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_translator.setMapper(null);
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
            ((DefaultHiLiteMapper) m_translator.getMapper()).save(config);
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
        // m_ignoreMissValues.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInHiLiteHandler(final int inIndex,
            final HiLiteHandler hiLiteHdl) {
        m_translator.removeAllToHiliteHandlers();
        m_translator.addToHiLiteHandler(hiLiteHdl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        return m_translator.getFromHiLiteHandler();
    }

}
