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
package org.knime.base.node.io.database.binning.apply;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.dmg.pmml.DerivedFieldDocument.DerivedField;
import org.knime.base.node.io.database.DBNodeModel;
import org.knime.base.node.io.database.binning.DBAutoBinner;
import org.knime.base.node.io.database.binning.DBBinnerMaps;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabasePortObject;
import org.knime.core.node.port.database.DatabasePortObjectSpec;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.port.database.StatementManipulator;
import org.knime.core.node.port.database.reader.DBReader;
import org.knime.core.node.port.pmml.PMMLPortObject;

/**
 * Node model to apply PMML Models of Database Auto-Binner and Database Numeric-Binner
 *
 * @author Lara Gorini
 */
final class DBApplyBinnerNodeModel extends DBNodeModel {

    /** Creates a new binner. */
    DBApplyBinnerNodeModel() {
        super(new PortType[]{PMMLPortObject.TYPE, DatabasePortObject.TYPE}, new PortType[]{DatabasePortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final PortObject[] execute(final PortObject[] inData, final ExecutionContext exec)
        throws CanceledExecutionException, Exception {

        PMMLPortObject pmmlPortObject = (PMMLPortObject)inData[0];
        DatabasePortObject databasePortObject = (DatabasePortObject)inData[1];

        DatabaseQueryConnectionSettings connectionSettings =
            databasePortObject.getConnectionSettings(getCredentialsProvider());

        return new PortObject[]{
            createDatabasePortObject(databasePortObject.getSpec(), connectionSettings, pmmlPortObject)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        //        final PMMLPortObjectSpec pmmlPortObjectSpec = (PMMLPortObjectSpec)inSpecs[0];
        //        final DatabasePortObjectSpec dbSpec = (DatabasePortObjectSpec)inSpecs[1];
        //TODO new spec?
        return new PortObjectSpec[]{null};
    }

    private String createQuery(final String query, final StatementManipulator statementManipulator,
        final DataTableSpec dataTableSpec, final PMMLPortObject pmmlPortObject) throws InvalidSettingsException {
        DBBinnerMaps maps = DBAutoBinner.intoBinnerMaps(pmmlPortObject, dataTableSpec);

        DerivedField[] derivedFields = pmmlPortObject.getDerivedFields();
        final DataTableSpec pmmlInputSpec = pmmlPortObject.getSpec().getDataTableSpec();
        String[] includeCols = new String[derivedFields.length];
        for (int i = 0; i < pmmlPortObject.getDerivedFields().length; i++) {
            String fieldName = derivedFields[i].getDiscretize().getField();
            final DataColumnSpec colSpec = dataTableSpec.getColumnSpec(fieldName);
            if (colSpec == null) {
                throw new InvalidSettingsException("Column '" + fieldName + "' not found in input table");
            }

            DataColumnSpec pmmlInputColSpec = pmmlInputSpec.getColumnSpec(fieldName);
            assert (pmmlInputColSpec != null) : "Column '" + fieldName
                + "' from derived fields not found in PMML model spec";

            DataType knimeType = pmmlInputColSpec.getType();
            if (!colSpec.getType().isCompatible(knimeType.getPreferredValueClass())) {
                throw new InvalidSettingsException(
                    "Date type of column '" + fieldName + "' is not compatible with PMML model: expected '" + knimeType
                        + "' but is '" + colSpec.getType() + "'");
            }
            includeCols[i] = fieldName;
        }
        String[] allColumns = dataTableSpec.getColumnNames();
        String[] excludeCols = filter(includeCols, allColumns);
        String result = statementManipulator.getBinnerStatement(query, includeCols, excludeCols, maps.getBoundariesMap(),
            maps.getBoundariesOpenMap(), maps.getNamingMap(), maps.getAppendMap());
        return result;
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
        DBReader reader = querySettings.getUtility().getReader(querySettings);

        try {
            DatabasePortObjectSpec databasePortObjectSpec = new DatabasePortObjectSpec(
                reader.getDataTableSpec(getCredentialsProvider()), connectionSettings.createConnectionModel());
            DatabasePortObject databasePortObject = new DatabasePortObject(databasePortObjectSpec);
            return databasePortObject;
        } catch (SQLException e) {
            throw new InvalidSettingsException("Failure during query generation. Error: " + e.getMessage());
        }
    }

    private String[] filter(final String[] includeCols, final String[] allColumns) {
        Set<String> excludeCols = new HashSet<>(Arrays.asList(allColumns));
        Stream.of(includeCols).forEach(col -> excludeCols.remove(col));
        return excludeCols.toArray(new String[excludeCols.size()]);
    }

}
