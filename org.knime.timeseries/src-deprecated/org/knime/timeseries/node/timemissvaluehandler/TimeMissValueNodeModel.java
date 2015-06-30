/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 */
package org.knime.timeseries.node.timemissvaluehandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.timeseries.node.timemissvaluehandler.TimeMissingValueHandlingColSetting.ConfigType;
import org.knime.timeseries.node.timemissvaluehandler.TimeMissingValueHandlingColSetting.HandlingMethod;
import org.knime.timeseries.node.timemissvaluehandler.tshandler.TSMissVHandler;

/**
 * The node model for the time miss value node.
 *
 * @author Iris Adae, University of Konstanz
 * @author Marcel Hanser, University of Konstanz
 * @deprecated See new missing node that incorporates time series handling in package
 * org.knime.base.node.preproc.pmml.missingval
 */
@Deprecated
public class TimeMissValueNodeModel extends NodeModel {

    private TimeMissingValueHandlingColSetting[] m_colSettings;

    /**
     * constructor.
     */
    protected TimeMissValueNodeModel() {
        super(1, 1);
        m_colSettings = new TimeMissingValueHandlingColSetting[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec spec = inSpecs[0];
        Map<String, TSMissVHandler> nameToMVHandler = checkInputAndCreateHandlerMap(spec);
        Map<String, Integer> nameToInt = findColumns(spec, nameToMVHandler);

        ColumnRearranger c = createColumnRearranger(nameToMVHandler, nameToInt, spec);
        return new DataTableSpec[]{c.createSpec()};

    }

    private Map<String, Integer> findColumns(final DataTableSpec inSpec,
        final Map<String, TSMissVHandler> nameToMVHandler) throws InvalidSettingsException {
        Map<String, Integer> nameToInt = new HashMap<String, Integer>();
        for (Entry<String, TSMissVHandler> entries : nameToMVHandler.entrySet()) {
            int findColumnIndex = inSpec.findColumnIndex(entries.getKey());
            nameToInt.put(entries.getKey(), findColumnIndex);
        }
        return nameToInt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        ExecutionContext createExec = exec.createSubExecutionContext(0.4);

        DataTableSpec spec = inData[0].getDataTableSpec();

        double maxRow = inData[0].getRowCount();
        double currRow = 0;

        Map<String, TSMissVHandler> nameToMVHandler = checkInputAndCreateHandlerMap(spec);
        Map<String, Integer> nameToInt = findColumns(spec, nameToMVHandler);

        if (nameToMVHandler.isEmpty()) {
            return inData;
        }

        for (DataRow row : inData[0]) {
            RowKey key = row.getKey();
            for (String s : nameToInt.keySet()) {
                nameToMVHandler.get(s).incomingValue(key, row.getCell(nameToInt.get(s)));
            }
            createExec.checkCanceled();
            createExec.setProgress(++currRow / maxRow, "Preprocessing... Row " + row.getKey().getString());
        }

        for (String s : nameToMVHandler.keySet()) {
            nameToMVHandler.get(s).close();
        }

        ExecutionContext builtExec = exec.createSubExecutionContext(0.6);
        ColumnRearranger colR = createColumnRearranger(nameToMVHandler, nameToInt, inData[0].getDataTableSpec());

        BufferedDataTable outTable = exec.createColumnRearrangeTable(inData[0], colR, builtExec);
        return new BufferedDataTable[]{outTable};
    }

    private Map<String, TSMissVHandler> checkInputAndCreateHandlerMap(final DataTableSpec spec)
        throws InvalidSettingsException {
        Map<String, TSMissVHandler> nameToMVHandler = new HashMap<String, TSMissVHandler>();

        List<String> columnWhichDoNotExist = new ArrayList<>(0);
        List<String> columnWithUnexpectedType = new ArrayList<>(0);

        // Handle individual configs
        for (TimeMissingValueHandlingColSetting set : m_colSettings) {
            if (!set.isMetaConfig() && !HandlingMethod.DO_NOTHING.equals(set.getMethod())) {
                for (String col : set.getNames()) {
                    DataColumnSpec colSpec = spec.getColumnSpec(col);
                    // sanity checks of the configured columns
                    if (colSpec != null) {
                        if (!TimeMissingValueHandlingNodeDialogPane.isIncompatible(set.getType(), colSpec)) {
                            TSMissVHandler handlerForLabel = set.getMethod().createTSMissVHandler();
                            nameToMVHandler.put(col, handlerForLabel);
                        } else {
                            columnWithUnexpectedType.add(col);
                        }
                    } else {
                        columnWhichDoNotExist.add(col);
                    }
                }
            }
        }
        // fill meta for the remaining columns
        for (TimeMissingValueHandlingColSetting set : m_colSettings) {
            if (set.isMetaConfig() && !HandlingMethod.DO_NOTHING.equals(set.getMethod())) {
                for (DataColumnSpec dcs : spec) {
                    if (!nameToMVHandler.containsKey(dcs.getName())) {
                        if (!TimeMissingValueHandlingNodeDialogPane.isIncompatible(set.getType(), dcs)) {
                            nameToMVHandler.put(dcs.getName(), set.getMethod().createTSMissVHandler());
                        }
                    }
                }
            }
        }

        if (!columnWhichDoNotExist.isEmpty() || !columnWithUnexpectedType.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            if (!columnWhichDoNotExist.isEmpty()) {
                builder.append("Following columns do not exist in input table:\n");
                builder.append(ConvenienceMethods.getShortStringFrom(columnWhichDoNotExist, 5));
            }
            if (!columnWhichDoNotExist.isEmpty() && !columnWithUnexpectedType.isEmpty()) {
                builder.append("\n");
            }
            if (!columnWithUnexpectedType.isEmpty()) {
                builder.append("Following columns have an unexpected type:\n");
                builder.append(ConvenienceMethods.getShortStringFrom(columnWithUnexpectedType, 5));
            }

            setWarningMessage(builder.toString());
        }

        return nameToMVHandler;
    }

    private ColumnRearranger createColumnRearranger(final Map<String, TSMissVHandler> nameToMVHandler,
        final Map<String, Integer> nameToInt, final DataTableSpec spec) {

        ColumnRearranger result = new ColumnRearranger(spec);

        for (String thisCol : nameToMVHandler.keySet()) {
            final TSMissVHandler mvHandler = nameToMVHandler.get(thisCol);
            final int colIndex = spec.findColumnIndex(thisCol);

            DataColumnSpec newColSpec =
                new DataColumnSpecCreator(thisCol, spec.getColumnSpec(thisCol).getType()).createSpec();

            SingleCellFactory c = new SingleCellFactory(newColSpec) {
                @Override
                public DataCell getCell(final DataRow row) {
                    if (row.getCell(colIndex).isMissing()) {
                        return mvHandler.getandRemove(row.getKey());
                    }
                    return row.getCell(colIndex);
                }
            };
            result.replace(c, nameToInt.get(thisCol));
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        TimeMissingValueHandlingColSetting[] def = TimeMissingValueHandlingColSetting.loadMetaColSettings(settings);
        TimeMissingValueHandlingColSetting[] ind =
            TimeMissingValueHandlingColSetting.loadIndividualColSettings(settings);
        m_colSettings = ArrayUtils.addAll(def, ind);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        TimeMissingValueHandlingColSetting.saveMetaColSettings(m_colSettings, settings);
        TimeMissingValueHandlingColSetting.saveIndividualsColSettings(m_colSettings, settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
    }

    /**
     *
     * @param spec a data column spec
     * @return the desired config type numerical, for double, long, int and date/time (otherwise (e.g. Boolean, string,
     *         ...) non_numerical
     */
    public static ConfigType initType(final DataColumnSpec spec) {
        DataType type = spec.getType();
        // we don't care if int or double but no boolean
        if ((!type.isCompatible(BooleanValue.class))
            && (type.isASuperTypeOf(IntCell.TYPE) || type.isASuperTypeOf(DateAndTimeCell.TYPE))) {
            return ConfigType.NUMERICAL;
        } else {
            return ConfigType.NON_NUMERICAL;
        }
    }
}
