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
package org.knime.base.node.io.database.binning.auto;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.knime.base.node.io.database.DBNodeModel;
import org.knime.base.node.io.database.binning.DBAutoBinner;
import org.knime.base.node.io.database.binning.DBBinnerMaps;
import org.knime.base.node.preproc.autobinner.pmml.PMMLPreprocDiscretize;
import org.knime.base.node.preproc.autobinner3.AutoBinnerLearnSettings;
import org.knime.base.node.preproc.autobinner3.AutoBinnerLearnSettings.EqualityMethod;
import org.knime.base.node.preproc.autobinner3.AutoBinnerLearnSettings.Method;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabasePortObject;
import org.knime.core.node.port.database.DatabasePortObjectSpec;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.port.database.DatabaseReaderConnection;
import org.knime.core.node.port.database.StatementManipulator;
import org.knime.core.node.port.pmml.PMMLPortObject;

/**
 *
 * Node Model of Database Auto-Binner
 *
 * @author Lara Gorini
 */
public final class DBAutoBinnerNodeModel extends DBNodeModel {

    private final AutoBinnerLearnSettings m_settings;

    /** Creates a new binner. */
    DBAutoBinnerNodeModel() {
        super(new PortType[]{DatabasePortObject.TYPE}, new PortType[]{DatabasePortObject.TYPE, PMMLPortObject.TYPE});
        m_settings = new AutoBinnerLearnSettings();
        //ensure that the fixed number method is selected since this is the only supported method
        m_settings.setMethod(Method.fixedNumber);
        m_settings.setEqualityMethod(EqualityMethod.width);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        AutoBinnerLearnSettings s = new AutoBinnerLearnSettings();
        s.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettings(settings);
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
        final DatabasePortObjectSpec inSpec = dbObject.getSpec();
        DatabaseQueryConnectionSettings connectionSettings = inSpec.getConnectionSettings(getCredentialsProvider());

        PMMLPortObject pmmlPortObject = createPMMLPortObject(inSpec, connectionSettings, exec);
        DatabasePortObject databasePortObject = createDatabasePortObject(inSpec, connectionSettings, pmmlPortObject);

        return new PortObject[]{databasePortObject, pmmlPortObject};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DatabasePortObjectSpec dbSpec = (DatabasePortObjectSpec)inSpecs[0];
        DatabaseQueryConnectionSettings connectionSettings = dbSpec.getConnectionSettings(getCredentialsProvider());
        boolean suppCase = connectionSettings.getUtility().supportsCase();
        if (!suppCase) {
            if (m_settings.getFilterConfiguration().applyTo(dbSpec.getDataTableSpec()).getIncludes().length > 1) {
                throw new InvalidSettingsException(
                    "Database does not support \"CASE\". Please choose only one column.");
            }
        }
        if (connectionSettings.getRetrieveMetadataInConfigure()){
            PMMLPortObject pmmlPortObject = createPMMLPortObject(dbSpec, connectionSettings, new ExecutionMonitor());
            DatabasePortObject databasePortObject = createDatabasePortObject(dbSpec, connectionSettings, pmmlPortObject);
            return new PortObjectSpec[]{databasePortObject.getSpec(), pmmlPortObject.getSpec()};
        }
        return new PortObjectSpec[]{null, null};
    }

    private DatabasePortObject createDatabasePortObject(final DatabasePortObjectSpec inSpec,
        DatabaseQueryConnectionSettings connectionSettings, final PMMLPortObject pmmlPortObject)
            throws InvalidSettingsException {

        final StatementManipulator statementManipulator = connectionSettings.getUtility().getStatementManipulator();

        String newQuery =
            createQuery(connectionSettings.getQuery(), statementManipulator, inSpec.getDataTableSpec(), pmmlPortObject);
        connectionSettings = createDBQueryConnection(inSpec, newQuery);
        DatabaseQueryConnectionSettings querySettings =
            new DatabaseQueryConnectionSettings(connectionSettings, newQuery);
        DatabaseReaderConnection conn = new DatabaseReaderConnection(querySettings);

        DatabasePortObjectSpec databasePortObjectSpec = null;
        try {
            databasePortObjectSpec = new DatabasePortObjectSpec(conn.getDataTableSpec(getCredentialsProvider()),
                connectionSettings.createConnectionModel());
        } catch (SQLException e) {
            throw new InvalidSettingsException("Failure during query generation. Error: " + e.getMessage());
        }
        DatabasePortObject databasePortObject = new DatabasePortObject(databasePortObjectSpec);
        return databasePortObject;
    }

    private PMMLPortObject createPMMLPortObject(final DatabasePortObjectSpec inSpec,
        final DatabaseQueryConnectionSettings connectionSettings, final ExecutionMonitor exec)
            throws InvalidSettingsException {
        Connection connection = null;
        try {
            connection = connectionSettings.createConnection(getCredentialsProvider());
        } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException | SQLException | IOException e) {
            throw new InvalidSettingsException("Failure during query generation. Error: " + e.getMessage());
        }
        final StatementManipulator statementManipulator = connectionSettings.getUtility().getStatementManipulator();
        DataTableSpec dataTableSpec = inSpec.getDataTableSpec();
        DBAutoBinner autoBinner = new DBAutoBinner(m_settings, dataTableSpec);
        PMMLPreprocDiscretize pMMLPrepocDiscretize;
        try {
            pMMLPrepocDiscretize = autoBinner.createPMMLPrepocDiscretize(connection,
                connectionSettings.getQuery(), statementManipulator, dataTableSpec);
        PMMLPortObject pmmlPortObject = DBAutoBinner.translate(pMMLPrepocDiscretize, dataTableSpec);
        return pmmlPortObject;
        } catch (SQLException e) {
            throw new InvalidSettingsException("Could not retrieve boundaries from database. Exception: " + e.getMessage(), e);
        }
    }

    private String createQuery(final String query, final StatementManipulator statementManipulator,
        final DataTableSpec dataTableSpec, final PMMLPortObject pmmlPortObject) {
        DBBinnerMaps maps = DBAutoBinner.intoBinnerMaps(pmmlPortObject, dataTableSpec);
        String[] binningCols = m_settings.getFilterConfiguration().applyTo(dataTableSpec).getIncludes();
        String[] allColumns = dataTableSpec.getColumnNames();
        String[] additionalCols = DBAutoBinner.filter(binningCols, allColumns);
        String result = statementManipulator.getBinnerStatement(query, binningCols, additionalCols, maps.getBoundariesMap(),
            maps.getBoundariesOpenMap(), maps.getNamingMap(), maps.getAppendMap());
        return result;
    }

}
