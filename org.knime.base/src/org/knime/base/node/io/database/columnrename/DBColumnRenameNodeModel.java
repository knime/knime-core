/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Apr 30, 2015 (budiyanto): created
 */
package org.knime.base.node.io.database.columnrename;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.knime.base.node.io.database.DBNodeModel;
import org.knime.base.node.preproc.rename.RenameColumnSetting;
import org.knime.base.node.preproc.rename.RenameConfiguration;
import org.knime.core.data.DataTableSpec;
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
import org.knime.core.node.util.ConvenienceMethods;

/**
 *
 * @author Budi Yanto, KNIME.com
 */
public class DBColumnRenameNodeModel extends DBNodeModel{

    /**
     * Config identifier for the NodeSettings object contained in the NodeSettings which contains the settings.
     */
    public static final String CFG_SUB_CONFIG = "all_columns";

    /** contains settings for each individual column. */
    private RenameConfiguration m_config;

    /**
     * Constructor for the node model.
     */
    protected DBColumnRenameNodeModel() {

        super(new PortType[]{DatabasePortObject.TYPE},
            new PortType[]{DatabasePortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DatabasePortObjectSpec dbSpec = (DatabasePortObjectSpec) inSpecs[0];
        final DatabasePortObjectSpec outSpec = createDBOutSpec(dbSpec);

        return new PortObjectSpec[]{outSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        final DatabasePortObject dbObject = (DatabasePortObject)inObjects[0];
        final DatabasePortObject outObject = new DatabasePortObject(
                createDBOutSpec(dbObject.getSpec()));

        return new PortObject[]{outObject};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_config != null) {
            final NodeSettingsWO subSettings = settings.addNodeSettings(CFG_SUB_CONFIG);
            m_config.save(subSettings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new RenameConfiguration(settings.getNodeSettings(CFG_SUB_CONFIG));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config = new RenameConfiguration(settings.getNodeSettings(CFG_SUB_CONFIG));
    }

    private String createQuery(final String selectQuery, final DataTableSpec inSpec,
                final StatementManipulator manipulator) {
        final StringBuilder buf = new StringBuilder();
        final String tableName = "table_" + System.identityHashCode(this);

        final StringBuilder columnBuf = new StringBuilder();
        Collection<RenameColumnSetting> columnSettings = m_config.getRenameColumnSettings();
        if (columnSettings.isEmpty()) {
            setWarningMessage("No column is renamed.");
            columnBuf.append("*"); // selects all columns
        } else {
            final List<RenameColumnSetting> renameSettings =
                        new ArrayList<>(columnSettings);
            final int totalColumns = inSpec.getNumColumns();
            for (int i = 0; i < totalColumns; i++) {
                final String colName = inSpec.getColumnSpec(i).getName();
                RenameColumnSetting colSettings = findAndRemoveSettings(
                                                    colName, renameSettings);
                final String oldName = manipulator.quoteIdentifier(colName);
                if (colSettings == null){
                    columnBuf.append(oldName);
                }else{
                    final String newName = colSettings.getNewColumnName();
                    if (newName == null){
                        columnBuf.append(oldName);
                    }else{
                        columnBuf.append(oldName + " AS " + manipulator.quoteIdentifier(newName));
                    }
                }
                if (i + 1 < totalColumns) {
                    columnBuf.append(",");
                }
            }
        }
        buf.append("SELECT " + columnBuf.toString() + " FROM (" + selectQuery + ") "
                + manipulator.quoteIdentifier(tableName));
        return buf.toString();
    }

    /**
     * Traverses the array renameSettings and finds the settings object for a given column.
     *
     * @param colName The column name.
     * @param renameSettings
     * @return The settings to the column (if any), otherwise null.
     */
    private RenameColumnSetting findAndRemoveSettings(final String colName,
        final List<RenameColumnSetting> renameSettings) {
        Iterator<RenameColumnSetting> listIterator = renameSettings.iterator();
        while (listIterator.hasNext()) {
            RenameColumnSetting set = listIterator.next();
            if (set.getName().equals(colName)) {
                listIterator.remove();
                return set;
            }
        }
        return null;
    }

    private DatabasePortObjectSpec createDBOutSpec(final DatabasePortObjectSpec inSpec)
            throws InvalidSettingsException{
        if (m_config == null) {
            throw new InvalidSettingsException("No configuration available");
        }
        final DataTableSpec tableSpec = inSpec.getDataTableSpec();
        final DataTableSpec outSpec = m_config.getNewSpec(tableSpec);
        final List<String> missingColumnNames = m_config.getMissingColumnNames();
        if (missingColumnNames != null && !missingColumnNames.isEmpty()) {
            setWarningMessage("The following columns are configured but no longer exist: "
                + ConvenienceMethods.getShortStringFrom(missingColumnNames, 5));
        }
        DatabaseQueryConnectionSettings conn = inSpec.getConnectionSettings(getCredentialsProvider());
        final StatementManipulator statementManipulator = conn.getUtility().getStatementManipulator();
        String newQuery = createQuery(conn.getQuery(), tableSpec, statementManipulator);
        conn = createDBQueryConnection(inSpec, newQuery);

        return new DatabasePortObjectSpec(outSpec, conn.createConnectionModel());
    }

}
