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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.io.database.sampling;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.knime.base.node.io.database.DBNodeModel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabasePortObject;
import org.knime.core.node.port.database.DatabasePortObjectSpec;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.port.database.DatabaseReaderConnection;
import org.knime.core.node.port.database.StatementManipulator;
import org.knime.core.node.util.ButtonGroupEnumInterface;

/**
 * The node model of the database sampling node.
 *
 * @author Lara Gorini
 */
final class DBSamplingNodeModel extends DBNodeModel {

    /** The method to use, RELATIVE or ABSOLUTE. */
    private SettingsModelString m_countMethod = createCountModel();

    /** Count of samples to choose (if absolute sampling). */
    private SettingsModelIntegerBounded m_absoluteCount = createAbsoluteModel();

    /** Fraction to use (if relative sampling). */
    private SettingsModelDoubleBounded m_relativeFraction = createRelativeModel();

    /** The sampling method to use, FIRST or RANDOM. */
    private SettingsModelString m_samplingMethod = createSamplingModel();

    /** TRUE if stratified sampling is selected, FALSE otherwise */
    private SettingsModelBoolean m_stratified = createStratifiedModel();

    /** Name of column for stratified sampling */
    private SettingsModelString m_classColumnName = createColumnModel();

    /**
     * Creates a new database reader.
     */
    DBSamplingNodeModel() {
        super(new PortType[]{DatabasePortObject.TYPE}, new PortType[]{DatabasePortObject.TYPE});
    }

    static SettingsModelString createCountModel() {
        return new SettingsModelString("method", CountMethod.ABSOLUTE.getText());
    }

    static SettingsModelIntegerBounded createAbsoluteModel() {
        return new SettingsModelIntegerBounded("count", 100, 0, Integer.MAX_VALUE);
    }

    static SettingsModelDoubleBounded createRelativeModel() {
        final SettingsModelDoubleBounded model = new SettingsModelDoubleBounded("fraction", 10, 0, 100);
        model.setEnabled(false);
        return model;
    }

    static SettingsModelString createSamplingModel() {
        return new SettingsModelString("samplingMethod", SamplingMethod.FIRST.getText());
    }

    static SettingsModelBoolean createStratifiedModel() {
        return new SettingsModelBoolean("stratifiedSampling", false);
    }

    static SettingsModelString createColumnModel() {
        final SettingsModelString model = new SettingsModelString("class_column", null);
        model.setEnabled(false);
        return model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_countMethod.saveSettingsTo(settings);
        m_absoluteCount.saveSettingsTo(settings);
        m_relativeFraction.saveSettingsTo(settings);
        m_samplingMethod.saveSettingsTo(settings);
        m_stratified.saveSettingsTo(settings);
        m_classColumnName.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        m_countMethod.validateSettings(settings);
        m_absoluteCount.validateSettings(settings);
        m_relativeFraction.validateSettings(settings);
        m_samplingMethod.validateSettings(settings);
        m_stratified.validateSettings(settings);
        m_classColumnName.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_countMethod.loadSettingsFrom(settings);
        m_absoluteCount.loadSettingsFrom(settings);
        m_relativeFraction.loadSettingsFrom(settings);
        m_samplingMethod.loadSettingsFrom(settings);
        m_stratified.loadSettingsFrom(settings);
        m_classColumnName.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final PortObject[] execute(final PortObject[] inData, final ExecutionContext exec)
        throws CanceledExecutionException, Exception {
        final DatabasePortObject dbObject = (DatabasePortObject)inData[0];
        final DatabasePortObject outObject = new DatabasePortObject(createDbOutSpec(dbObject.getSpec(), exec, false));
        return new PortObject[]{outObject};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (m_stratified.getBooleanValue()) {
            final DatabasePortObjectSpec dbSpec = (DatabasePortObjectSpec)inSpecs[0];
            final DataTableSpec tableSpec = dbSpec.getDataTableSpec();
            if (!tableSpec.containsName(m_classColumnName.getStringValue())) {
                throw new InvalidSettingsException("Please choose a suitable column for stratified sampling.");
            }
        }
        return inSpecs;
    }

    /**
     * @param inSpec Spec of the input database object
     * @param exec The {@link ExecutionMonitor}
     * @param checkRetrieveMetadata true if the retrieveMetadataInConfigure settings should be respected,
     *            <code>false</code> if the metadata should be retrieved in any case (for execute)
     * @return Spec of the output database object
     * @throws InvalidSettingsException if the current settings are invalid
     * @throws CanceledExecutionException if execution is canceled
     */
    private DatabasePortObjectSpec createDbOutSpec(final DatabasePortObjectSpec inSpec, final ExecutionMonitor exec,
        final boolean checkRetrieveMetadata) throws InvalidSettingsException, CanceledExecutionException {
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
            throw new InvalidSettingsException("Failure during query generation. Error: " + e1.getMessage(), e1);
        }
    }

    private String createQuery(final Connection connection, final String query, final StatementManipulator manipulator,
        final DataTableSpec dataTableSpec, final ExecutionMonitor exec)
            throws SQLException, InvalidSettingsException, CanceledExecutionException {
        final SamplingMethod samplingMethod = SamplingMethod.getMethod(m_samplingMethod.getStringValue());
        final boolean useRandomSampling = SamplingMethod.RANDOM.equals(samplingMethod);
        final CountMethod countMethod = CountMethod.getMethod(m_countMethod.getStringValue());
        final boolean absolute = CountMethod.ABSOLUTE.equals(countMethod);
        if (m_stratified.getBooleanValue()) {
            return createStratifiedQuery(connection, query, manipulator, useRandomSampling, absolute, exec);
        } else {
            final StringBuilder resultQuery = new StringBuilder();
            exec.setProgress(0.2, "Calculating number of rows.");
            long totalCount = 0;
            final String countQuery = "SELECT COUNT(*) FROM (" + query + ") samplingTable_" + Math.abs(hashCode());
            try (ResultSet valueSet = connection.createStatement().executeQuery(countQuery);) {
                exec.checkCanceled();
                while (valueSet.next()) {
                    final long countVal = valueSet.getLong(1);
                    if (!valueSet.wasNull()) {
                        totalCount = countVal;
                    }
                }
            }

            exec.setProgress(0.7, "Calculating limits.");
            exec.checkCanceled();
            long valueToLimit;
            if (absolute) {
                valueToLimit = m_absoluteCount.getIntValue();
            } else {
                valueToLimit = Math.round(m_relativeFraction.getDoubleValue() / 100 * totalCount);
            }

            if (valueToLimit >= totalCount) {
                setWarningMessage("Input query does not contain more rows than requested. Node returns input query.");
                return query;
            }

            exec.setProgress(0.9, "Creating query.");
            exec.checkCanceled();
            resultQuery.append(manipulator.getSamplingStatement(query, valueToLimit, useRandomSampling));
            return resultQuery.toString();
        }
    }

    private String createStratifiedQuery(final Connection connection, final String query,
        final StatementManipulator manipulator, final boolean useRandomSampling, final boolean absolute,
        final ExecutionMonitor exec) throws SQLException, InvalidSettingsException, CanceledExecutionException {

        exec.setProgress(0.2, "Stratified sampling: calculating number of rows per class.");
        String tableName = "samplingTable_" + Math.abs(hashCode());
        final String classNamesAndCountQuery =
            "SELECT " + tableName + "." + manipulator.quoteIdentifier(m_classColumnName.getStringValue())
                + ", COUNT(*) FROM (" + query + ") " + tableName + " GROUP BY " + tableName + "."
                + manipulator.quoteIdentifier(m_classColumnName.getStringValue());
        long totalCount = 0;
        final Map<Object, Long> classNamesAndCount = new LinkedHashMap<>();
        try (ResultSet valueSet = connection.createStatement().executeQuery(classNamesAndCountQuery)) {
            exec.checkCanceled();
            while (valueSet.next()) {
                final Object classVal = valueSet.getObject(1);
                final long countVal = valueSet.getLong(2);
                if (!valueSet.wasNull()) {
                    classNamesAndCount.put(classVal, countVal);
                    totalCount += countVal;
                }
            }
        }

        List<Object> classNames = new ArrayList<>(classNamesAndCount.keySet());
        classNames.sort(new Comparator<Object>() {

            @Override
            public int compare(final Object o1, final Object o2) {
                return o1.toString().compareTo(o2.toString());
            }

        });


        exec.setProgress(0.7, "Stratified sampling: calculating limits.");
        long sum = 0;
        final double fraction;
        final long absoluteCount;
        if (absolute) {
            fraction = m_absoluteCount.getIntValue() / (double)totalCount;
            absoluteCount = m_absoluteCount.getIntValue();
        } else {
            fraction = m_relativeFraction.getDoubleValue() / 100.0;
            absoluteCount = (long)(fraction * totalCount);
        }

        if (absoluteCount >= totalCount) {
            setWarningMessage("Input query does not contain more rows than requested. Node returns input query.");
            return query;
        }

        final Map<Object, Long> classNamesAndLimits = new LinkedHashMap<>(classNamesAndCount);
        for (Object className : classNames) {
            exec.checkCanceled();
            final long classCount = classNamesAndCount.get(className);
            final long valueToLimit = Math.min(Math.round(fraction * classCount), absoluteCount - sum);
            sum += valueToLimit;
            classNamesAndLimits.put(className, new Long(valueToLimit));
        }

        if (sum < absoluteCount) {
            long addLimit = absoluteCount - sum;
            for (Object className : classNames) {
                exec.checkCanceled();
                long classLimit = classNamesAndLimits.get(className);
                long classCount = classNamesAndCount.get(className);
                long diff = Math.min(addLimit, classCount - classLimit);
                if (diff <= 0) {
                    continue;
                }
                long newLimit = classLimit + diff;
                classNamesAndLimits.put(className, new Long(newLimit));
                addLimit -= diff;
                if (addLimit <= 0) {
                    break;
                }
            }
        }

        final StringBuilder resultQuery = new StringBuilder();

        exec.setProgress(0.9, "Stratified sampling: creating query.");
        resultQuery.append("SELECT * FROM (");
        int counter = 0;
        for (Map.Entry<Object, Long> classes : classNamesAndLimits.entrySet()) {
            exec.checkCanceled();
            final Object className = classes.getKey();
            final long valueToLimit = classes.getValue().longValue();
            final String sql = "SELECT * FROM (" + query + ") " + tableName + " WHERE " + tableName + "."
                + manipulator.quoteIdentifier(m_classColumnName.getStringValue()) + "='" + className + "'";

            resultQuery.append(manipulator.getSamplingStatement(sql, valueToLimit, useRandomSampling));

            if (counter < classNamesAndCount.size() - 1) {
                resultQuery.append(") A \n UNION ALL SELECT * FROM (");
            } else {
                resultQuery.append(") A");
            }
            counter++;
        }
        return resultQuery.toString();
    }

    enum CountMethod implements ButtonGroupEnumInterface {

        ABSOLUTE("Absolute", "Selecting an absolute number of samples."),

        RELATIVE("Relative[%]", "Selecting a percentaged part of samples.");

        private String m_label;

        private String m_desc;

        private CountMethod(final String label, final String desc) {
            m_label = label;
            m_desc = desc;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getText() {
            return m_label;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getActionCommand() {
            return name();
        }

        public static CountMethod getMethod(final String actionCommand) {
            return valueOf(actionCommand);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getToolTip() {
            return m_desc;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isDefault() {
            return ABSOLUTE.equals(this);
        }
    }

    enum SamplingMethod implements ButtonGroupEnumInterface {

        FIRST("Take from top", "Selecting samples from the beginning."),

        RANDOM("Draw randomly", "Selecting samples randomly.");

        private String m_label;

        private String m_desc;

        private SamplingMethod(final String label, final String desc) {
            m_label = label;
            m_desc = desc;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getText() {
            return m_label;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getActionCommand() {
            return name();
        }

        public static SamplingMethod getMethod(final String actionCommand) {
            return valueOf(actionCommand);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getToolTip() {
            return m_desc;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isDefault() {
            return FIRST.equals(this);
        }
    }

}
