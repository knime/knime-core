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
package org.knime.base.node.io.database.binning.numeric;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.knime.base.node.io.database.DBNodeModel;
import org.knime.base.node.io.database.binning.DBAutoBinner;
import org.knime.base.node.io.database.binning.DBBinnerMaps;
import org.knime.base.node.preproc.pmml.binner.BinnerColumnFactory.Bin;
import org.knime.base.node.preproc.pmml.binner.NumericBin;
import org.knime.base.node.preproc.pmml.binner.PMMLBinningTranslator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabasePortObject;
import org.knime.core.node.port.database.DatabasePortObjectSpec;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.port.database.StatementManipulator;
import org.knime.core.node.port.database.reader.DBReader;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;

/**
 * Node Model of Database Numeric-Binner
 *
 * @author Lara Gorini
 */
final class DBNumericBinnerNodeModel extends DBNodeModel {

    /** Key for binned columns. */
    static final String NUMERIC_COLUMNS = "binned_columns";

    /** Key if new column is appended. */
    static final String IS_APPENDED = "_is_appended";

    /** Selected columns for binning. */
    private final Map<String, Bin[]> m_columnToBins = new HashMap<>();

    private final Map<String, String> m_columnToAppended = new HashMap<>();

    /** Creates a new binner. */
    DBNumericBinnerNodeModel() {
        super(new PortType[]{DatabasePortObject.TYPE}, new PortType[]{DatabasePortObject.TYPE, PMMLPortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_columnToBins.clear();
        m_columnToAppended.clear();
        String[] columns = settings.getStringArray(NUMERIC_COLUMNS, new String[0]);
        for (int i = 0; i < columns.length; i++) {
            NodeSettingsRO column = settings.getNodeSettings(columns[i].toString());
            Set<String> bins = column.keySet();
            NumericBin[] binnings = new NumericBin[bins.size()];
            int s = 0;
            for (String binKey : bins) {
                NodeSettingsRO bin = column.getNodeSettings(binKey);
                binnings[s] = new NumericBin(bin);
                s++;
            }
            m_columnToBins.put(columns[i], binnings);
            String appended = settings.getString(columns[i].toString() + IS_APPENDED, null);
            m_columnToAppended.put(columns[i], appended);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        for (String columnKey : m_columnToBins.keySet()) {
            NodeSettingsWO column = settings.addNodeSettings(columnKey);
            if (m_columnToAppended.get(columnKey) != null) {
                settings.addString(columnKey + IS_APPENDED, m_columnToAppended.get(columnKey));
            } else {
                settings.addString(columnKey + IS_APPENDED, null);
            }
            Bin[] bins = m_columnToBins.get(columnKey);
            for (int b = 0; b < bins.length; b++) {
                NodeSettingsWO bin = column.addNodeSettings(bins[b].getBinName() + "_" + b);
                bins[b].saveToSettings(bin);
            }
        }
        settings.addStringArray(NUMERIC_COLUMNS, m_columnToAppended.keySet().toArray(new String[0]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        StringBuffer sb = new StringBuffer();
        String[] columns = settings.getStringArray(NUMERIC_COLUMNS, new String[0]);
        if (columns == null) {
            sb.append("Numeric column array can't be 'null'\n");
        } else {
            for (int i = 0; i < columns.length; i++) {
                // appended or replaced
                settings.getString(columns[i].toString() + IS_APPENDED, null);
                double old = Double.NEGATIVE_INFINITY;
                if (columns[i] == null) {
                    sb.append("Column can't be 'null': " + i + "\n");
                    continue;
                }
                NodeSettingsRO set = settings.getNodeSettings(columns[i].toString());
                for (String binKey : set.keySet()) {
                    NodeSettingsRO bin = set.getNodeSettings(binKey);
                    NumericBin theBin = null;
                    try {
                        theBin = new NumericBin(bin);
                    } catch (InvalidSettingsException ise) {
                        sb.append(columns[i] + ": " + ise.getMessage() + "\n");
                        continue;
                    }
                    String binName = theBin.getBinName();
                    double l = theBin.getLeftValue();
                    if (l != old) {
                        sb.append(columns[i] + ": " + binName + " check interval: " + "left=" + l + ",oldright=" + old
                            + "\n");
                    }
                    double r = theBin.getRightValue();
                    boolean lOpen = theBin.isLeftOpen();
                    boolean rOpen = theBin.isRightOpen();

                    if (r < l) {
                        sb.append(
                            columns[i] + ": " + binName + " check interval: " + "left=" + l + ",right=" + r + "\n");
                    } else {
                        if (r == l && !(!lOpen && !rOpen)) {
                            sb.append(
                                columns[i] + ": " + binName + " check borders: " + "left=" + l + ",right=" + r + "\n");
                        }
                    }
                    old = r;
                }
                if (old != Double.POSITIVE_INFINITY) {
                    sb.append(columns[i] + ": check last right interval value=" + old + "\n");
                }
            }
        }

        if (sb.length() > 0) {
            throw new InvalidSettingsException(sb.toString());
        }
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
        checkDuplicateBinNames();

        final DatabasePortObject inDatabasePortObject = (DatabasePortObject)inData[0];
        final DatabasePortObjectSpec inDatabasePortObjectSpec = inDatabasePortObject.getSpec();
        DatabaseQueryConnectionSettings connectionSettings =
            inDatabasePortObject.getConnectionSettings(getCredentialsProvider());
        DataTableSpec outDataTableSpec =
            DBAutoBinner.createNewDataTableSpec(inDatabasePortObjectSpec.getDataTableSpec(), m_columnToAppended);
        PMMLPortObject outPMMLPortObject =
            createPMMLPortObject(inDatabasePortObjectSpec.getDataTableSpec(), outDataTableSpec);
        DBBinnerMaps binnerMaps =
            DBAutoBinner.intoBinnerMaps(outPMMLPortObject, inDatabasePortObjectSpec.getDataTableSpec());
        DatabasePortObjectSpec outDatabasePortObjectSpec =
            createDatabasePortObjectSpec(connectionSettings, inDatabasePortObjectSpec, binnerMaps);
        return new PortObject[]{new DatabasePortObject(outDatabasePortObjectSpec), outPMMLPortObject};
    }

    private DatabasePortObjectSpec createDatabasePortObjectSpec(DatabaseQueryConnectionSettings connectionSettings,
        final DatabasePortObjectSpec inDatabasePortObjectSpec, final DBBinnerMaps binnerMaps)
            throws InvalidSettingsException, SQLException, BadPaddingException, IllegalBlockSizeException,
            InvalidKeyException, IOException {

        final StatementManipulator statementManipulator = connectionSettings.getUtility().getStatementManipulator();
        final Connection connection = connectionSettings.createConnection(getCredentialsProvider());
        final String newQuery = createQuery(connection, connectionSettings.getQuery(), statementManipulator,
            inDatabasePortObjectSpec.getDataTableSpec(), binnerMaps);
        connectionSettings = createDBQueryConnection(inDatabasePortObjectSpec, newQuery);
        final DatabaseQueryConnectionSettings querySettings =
            new DatabaseQueryConnectionSettings(connectionSettings, newQuery);
        DBReader reader = querySettings.getUtility().getReader(querySettings);
        DataTableSpec outDataTableSpec = reader.getDataTableSpec(getCredentialsProvider());
        DatabasePortObjectSpec outDatabasePortObjectSpec =
            new DatabasePortObjectSpec(outDataTableSpec, connectionSettings.createConnectionModel());
        return outDatabasePortObjectSpec;
    }

    private PMMLPortObject createPMMLPortObject(final DataTableSpec inDataTableSpec,
        final DataTableSpec outDataTableSpec) {
        PMMLPortObjectSpec initPMMLSpec = new PMMLPortObjectSpecCreator(outDataTableSpec).createSpec();
        PMMLPortObject initPMMLPortObject = new PMMLPortObject(initPMMLSpec, null, outDataTableSpec);
        PMMLBinningTranslator pmmlBinningTranslator =
            new PMMLBinningTranslator(m_columnToBins, m_columnToAppended, new DerivedFieldMapper(initPMMLPortObject));
        PMMLPortObject outPMMLPortObject = new PMMLPortObject(initPMMLSpec, initPMMLPortObject, inDataTableSpec);
        outPMMLPortObject.addGlobalTransformations(pmmlBinningTranslator.exportToTransDict());
        return outPMMLPortObject;
    }

    private void checkDuplicateBinNames() {
        for (Bin[] bins : m_columnToBins.values()) {
            Set<String> binNames = new HashSet<>();
            Optional<Bin> duplicate = Stream.of(bins).filter(b -> !binNames.add(b.getBinName())).findFirst();
            if (duplicate.isPresent()) {
                setWarningMessage(
                    "Bin name \"" + duplicate.get().getBinName() + "\" is used for different intervals.");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DatabasePortObjectSpec inDatabasePortObjectSpec = (DatabasePortObjectSpec)inSpecs[0];
        DatabaseQueryConnectionSettings connectionSettings =
            inDatabasePortObjectSpec.getConnectionSettings(getCredentialsProvider());
        boolean suppCase = connectionSettings.getUtility().supportsCase();
        if (!suppCase) {
            if (m_columnToBins.size() > 1) {
                throw new InvalidSettingsException(
                    "Database does not support \"CASE\". Please choose only one column.");
            }
        }
        if (m_columnToBins.isEmpty()) {
            setWarningMessage("No columns selected for binning");
        } else {
            checkDuplicateBinNames();
        }

        DataTableSpec outDataTableSpec =
            DBAutoBinner.createNewDataTableSpec(inDatabasePortObjectSpec.getDataTableSpec(), m_columnToAppended);
        PMMLPortObject outPMMLPortObject =
            createPMMLPortObject(inDatabasePortObjectSpec.getDataTableSpec(), outDataTableSpec);
        DBBinnerMaps binnerMaps =
            DBAutoBinner.intoBinnerMaps(outPMMLPortObject, inDatabasePortObjectSpec.getDataTableSpec());
        DatabasePortObjectSpec outDatabasePortObjectSpec = null;
        try {
            outDatabasePortObjectSpec =
                createDatabasePortObjectSpec(connectionSettings, inDatabasePortObjectSpec, binnerMaps);
        } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException | SQLException | IOException e) {
            // TODO Auto-generated catch block
        }
        return new PortObjectSpec[]{outDatabasePortObjectSpec, outPMMLPortObject.getSpec()};
    }

    private String createQuery(final Connection connection, final String query,
        final StatementManipulator statementManipulator, final DataTableSpec dataTableSpec,
        final DBBinnerMaps binnerMaps) throws SQLException {
        String[] includeCols = m_columnToBins.keySet().toArray(new String[m_columnToBins.keySet().size()]);
        String[] allColumns = dataTableSpec.getColumnNames();
        String[] excludeCols = DBAutoBinner.filter(includeCols, allColumns);
        String result =
            statementManipulator.getBinnerStatement(query, includeCols, excludeCols, binnerMaps.getBoundariesMap(),
                binnerMaps.getBoundariesOpenMap(), binnerMaps.getNamingMap(), binnerMaps.getAppendMap());
        return result;
    }

}
