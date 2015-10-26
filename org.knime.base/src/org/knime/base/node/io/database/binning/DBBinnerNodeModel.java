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
package org.knime.base.node.io.database.binning;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.knime.base.node.io.database.DBNodeModel;
import org.knime.base.node.preproc.autobinner3.AutoBinner;
import org.knime.base.node.preproc.autobinner3.AutoBinnerLearnSettings.BinNaming;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelOptionalString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabasePortObject;
import org.knime.core.node.port.database.DatabasePortObjectSpec;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.port.database.DatabaseReaderConnection;
import org.knime.core.node.port.database.StatementManipulator;
import org.knime.core.util.Pair;

final class DBBinnerNodeModel extends DBNodeModel {

    SettingsModelColumnFilter2 m_column = getColumnModel();

    SettingsModelIntegerBounded m_numberBins = getNumberBinsModel();

    SettingsModelString m_naming = getNamingModel();

    SettingsModelOptionalString m_appendColumn = getAppendColumnModel();

    /**
     * @return
     */
    public static SettingsModelColumnFilter2 getColumnModel() {
        return new SettingsModelColumnFilter2("column",
            new Class[]{IntValue.class, LongValue.class, DoubleValue.class});
    }

    /**
     * @return
     */
    public static SettingsModelIntegerBounded getNumberBinsModel() {
        return new SettingsModelIntegerBounded("number_of_bins", 5, 1, Integer.MAX_VALUE);
    }

    /**
     * @return
     */
    public static SettingsModelString getNamingModel() {
        return new SettingsModelString("bin_naming", BinNaming.numbered.name());
    }

    /**
     * @return
     */
    public static SettingsModelOptionalString getAppendColumnModel() {
        return new SettingsModelOptionalString("append_column", "column [Binned]", true);
    }

    /** Creates a new binner. */
    DBBinnerNodeModel() {
        super(new PortType[]{DatabasePortObject.TYPE}, new PortType[]{DatabasePortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_column.saveSettingsTo(settings);
        m_numberBins.saveSettingsTo(settings);
        m_naming.saveSettingsTo(settings);
        m_appendColumn.saveSettingsTo(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_column.validateSettings(settings);
        m_numberBins.validateSettings(settings);
        m_naming.validateSettings(settings);
        m_appendColumn.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_column.loadSettingsFrom(settings);
        m_numberBins.loadSettingsFrom(settings);
        m_naming.loadSettingsFrom(settings);
        m_appendColumn.loadSettingsFrom(settings);
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
    protected final PortObject[] execute(final PortObject[] inData, final ExecutionContext exec)
        throws CanceledExecutionException, Exception {
        exec.setMessage("Retrieving metadata from database");
        final DatabasePortObject dbObject = (DatabasePortObject)inData[0];
        final DatabasePortObject outObject = new DatabasePortObject(createDbOutSpec(dbObject.getSpec(), exec));
        return new PortObject[]{outObject};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DatabasePortObjectSpec dbSpec = (DatabasePortObjectSpec)inSpecs[0];
        boolean suppCase;
        try {
            suppCase = dbSpec.getConnectionSettings(getCredentialsProvider()).getUtility().supportsCase();
        } catch (InvalidSettingsException e) {
            throw new InvalidSettingsException(e.getMessage());
        }
        if (!suppCase) {
            if (m_column.applyTo(dbSpec.getDataTableSpec()).getIncludes().length > 1) {
                throw new InvalidSettingsException(
                    "Database does not support \"CASE\". Please choose only one column.");
            }
        }
        return new PortObjectSpec[]{dbSpec};
    }

    /**
     * @param inSpec Spec of the input database object
     * @param exec The {@link ExecutionMonitor}
     * @return Spec of the output database object
     * @throws InvalidSettingsException if the current settings are invalid
     * @throws CanceledExecutionException if execution is canceled
     */
    private DatabasePortObjectSpec createDbOutSpec(final DatabasePortObjectSpec inSpec, final ExecutionMonitor exec)
        throws InvalidSettingsException, CanceledExecutionException {
        DatabaseQueryConnectionSettings connectionSettings = inSpec.getConnectionSettings(getCredentialsProvider());
        final StatementManipulator statementManipulator = connectionSettings.getUtility().getStatementManipulator();
        try {
            Connection connection = connectionSettings.createConnection(getCredentialsProvider());
            String newQuery = createQuery(connection, connectionSettings.getQuery(), statementManipulator,
                inSpec.getDataTableSpec(), exec);
            connectionSettings = createDBQueryConnection(inSpec, newQuery);
            DatabaseQueryConnectionSettings querySettings =
                new DatabaseQueryConnectionSettings(connectionSettings, newQuery);
            DatabaseReaderConnection conn = new DatabaseReaderConnection(querySettings);
            DataTableSpec tableSpec;
            exec.setMessage("Retrieving result specification.");
            tableSpec = conn.getDataTableSpec(getCredentialsProvider());
            return new DatabasePortObjectSpec(tableSpec, connectionSettings.createConnectionModel());
        } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException | SQLException
                | IOException e1) {
            throw new InvalidSettingsException("Failure during query generation. Error: " + e1.getMessage());
        }
    }

    /**
     * @param connection
     * @param query
     * @param statementManipulator
     * @param dataTableSpec
     * @param exec
     * @return
     * @throws InvalidSettingsException
     */
    private String createQuery(final Connection connection, final String query,
        final StatementManipulator statementManipulator, final DataTableSpec dataTableSpec, final ExecutionMonitor exec)
            throws SQLException, InvalidSettingsException {

        String[] includeCols = m_column.applyTo(dataTableSpec).getIncludes();
        String[] allColumns = dataTableSpec.getColumnNames();
        String[] excludeCols = filter(includeCols, allColumns);

        double max = 0;
        double min = 0;
        StringBuilder minMaxQuery = new StringBuilder();
        minMaxQuery.append("SELECT");
        for (int i = 0; i < includeCols.length; i++) {
            minMaxQuery.append(" MAX(" + statementManipulator.quoteIdentifier(includeCols[i]) + ") "
                + statementManipulator.quoteIdentifier("max_" + includeCols[i]) + ",");
            minMaxQuery.append(" MIN(" + statementManipulator.quoteIdentifier(includeCols[i]) + ") "
                + statementManipulator.quoteIdentifier("min_" + includeCols[i]));
            if (i < includeCols.length - 1) {
                minMaxQuery.append(",");
            }
        }
        minMaxQuery.append(" FROM (" + query + ") T");
        HashMap<String, Pair<Double, Double>> maxAndMin = new LinkedHashMap<>();
        try (ResultSet valueSet = connection.createStatement().executeQuery(minMaxQuery.toString());) {
            while (valueSet.next()) {
                for (int i = 0; i < includeCols.length; i++) {
                    max = valueSet.getDouble("max_" + includeCols[i]);
                    min = valueSet.getDouble("min_" + includeCols[i]);
                    maxAndMin.put(includeCols[i], new Pair<Double, Double>(min, max));
                }
            }
        } catch (SQLException e) {
            //TODO
        }

        int number = m_numberBins.getIntValue();

        Map<String, Double[][]> limitsMap = new LinkedHashMap<>();
        Map<String, Boolean[][]> includeMap = new LinkedHashMap<>();
        Map<String, String[]> namingMap = new LinkedHashMap<>();
        Map<String, String> appendMap = new LinkedHashMap<>();

        boolean appendColumn = m_appendColumn.isActive() && !m_appendColumn.getStringValue().equals("");

        for (Entry<String, Pair<Double, Double>> entry : maxAndMin.entrySet()) {

            Double[][] limits = new Double[number][2];
            Boolean[][] include = new Boolean[number][2];

            double[] edges = AutoBinner.calculateBounds(number, entry.getValue().getFirst(), entry.getValue().getSecond());
            BinNaming name = BinNaming.valueOf(m_naming.getStringValue());
            String[] naming = nameBins(edges, name);

            for (int i = 0; i < number; i++) {
                limits[i][0] = edges[i];
                limits[i][1] = edges[i + 1];

                include[i][0] = false;
                include[i][1] = true;
            }

            limits[0][0] = Double.NEGATIVE_INFINITY;
            limits[number - 1][1] = Double.POSITIVE_INFINITY;
            include[0][0] = true;

            String columnName = entry.getKey();
            limitsMap.put(columnName, limits);
            includeMap.put(columnName, include);
            namingMap.put(columnName, naming);
            String append = null;
            if(appendColumn) {
                append = m_appendColumn.getStringValue();
            }
            appendMap.put(columnName, append);
        }

        String result = statementManipulator.getBinnerStatement(query, includeCols, excludeCols, limitsMap, includeMap,
            namingMap, appendMap);

        return result;
    }

    private String[] nameBins(final double[] edges, final BinNaming binNaming) {
        String[] binNames = new String[edges.length - 1];
        switch (binNaming) {
            case edges:
                binNames[0] = "'[" + format(edges[0]) + "," + format(edges[1]) + "]'";
                for (int i = 1; i < binNames.length; i++) {
                    binNames[i] = "'(" + format(edges[i]) + "," + format(edges[i + 1]) + "]'";
                }
                break;
            case numbered:
                for (int i = 0; i < binNames.length; i++) {
                    binNames[i] = "'Bin " + (i + 1) + "'";
                }
                break;
            case midpoints:
                binNames[0] = "'"+format((edges[1] - edges[0]) / 2 + edges[0])+"'";
                for (int i = 1; i < binNames.length; i++) {
                    binNames[i] = "'" + format((edges[i + 1] - edges[i]) / 2 + edges[i]) + "'";
                }
                break;
            default:
                for (int i = 0; i < binNames.length; i++) {
                    binNames[i] = "'Bin " + (i + 1) + "'";
                }
                break;
        }

        return binNames;
    }

    private String format(final double d) {
        DecimalFormat smallFormat = new DecimalFormat("0.00E0", new DecimalFormatSymbols(Locale.US));
        NumberFormat defaultFormat = NumberFormat.getNumberInstance(Locale.US);

        if (d == 0.0) {
            return "0";
        }
        if (Double.isInfinite(d) || Double.isNaN(d)) {
            return Double.toString(d);
        }
        NumberFormat format;
        double abs = Math.abs(d);
        if (abs < 0.0001) {
            format = smallFormat;
        } else {
            format = defaultFormat;
        }
        synchronized (format) {
            return format.format(d);

        }
    }

    /**
     * @param includeCols
     * @param allColumns
     * @return
     */
    private String[] filter(final String[] includeCols, final String[] allColumns) {
        List<String> excludeColsList = new LinkedList<>();
        for (int i = 0; i < allColumns.length; i++) {
            for (int j = 0; j < includeCols.length; j++) {
                if (allColumns[i].equals(includeCols[j])) {
                    if (excludeColsList.contains(allColumns[i])) {
                        excludeColsList.remove(allColumns[i]);
                        break;
                    } else {
                        break;
                    }
                } else {
                    if (!excludeColsList.contains(allColumns[i])) {
                        excludeColsList.add(allColumns[i]);
                    } else {
                        break;
                    }
                }
            }
        }
        String[] excludeCols = excludeColsList.toArray(new String[excludeColsList.size()]);
        return excludeCols;
    }

}
