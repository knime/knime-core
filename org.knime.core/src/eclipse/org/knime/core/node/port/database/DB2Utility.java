/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   17.06.2014 (thor): created
 */
package org.knime.core.node.port.database;

import org.knime.core.node.port.database.aggregation.DBAggregationFunctionFactory;
import org.knime.core.node.port.database.tablecreator.DBTableCreator;


/**
 * Database utility for IBM DB2.
 *
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 * @since 2.11
 */
public class DB2Utility extends DatabaseUtility {
    private static class DB2StatementManipulator extends StatementManipulator {

        /**
         * Constructor of class {@link DB2StatementManipulator}.
         */
       public DB2StatementManipulator() {
           super(true);
       }

        /**
         * {@inheritDoc}
         */
        @Override
        public String limitRows(final String sql, final long count) {
            return sql + "FETCH FIRST " + count + " ROWS ONLY";
        }


        @Override
        public String randomRows(final String sql, final long count) {
            return sql + " ORDER BY rand() FETCH FIRST " + count + " ROWS ONLY";
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public String[] createTableAsSelect(final String tableName, final String query) {
            return new String[] {"CREATE TABLE  " + tableName + " AS (" + query + ") WITH NO DATA",
                "INSERT INTO " + tableName + " (" + query + ")"};
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String dropTable(final String tableName, final boolean cascade) {
            //db2 does not support the cascade option
            return super.dropTable(tableName, false);
        }
    }

    private static final StatementManipulator MANIPULATOR = new DB2StatementManipulator();

    /**The unique database identifier.
     * @since 2.11*/
    public static final String DATABASE_IDENTIFIER = "db2";

    /**
     * Constructor.
     */
    public DB2Utility() {
        super(DATABASE_IDENTIFIER, MANIPULATOR, (DBAggregationFunctionFactory[]) null);
    }


    @Override
    public boolean supportsRandomSampling() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsCase() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DBTableCreator getTableCreator(final String schema, final String tableName, final boolean isTempTable) {
        return new DB2TableCreator(getStatementManipulator(), schema, tableName, isTempTable);
    }
}
