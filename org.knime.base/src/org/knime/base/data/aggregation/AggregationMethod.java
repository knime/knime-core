/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * -------------------------------------------------------------------
 */

package org.knime.base.data.aggregation;

import java.util.Collection;
import java.util.Comparator;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataValue;
import org.knime.core.node.port.database.aggregation.AggregationFunction;


/**
 * Interface that implements the main methods of an aggregation method.
 * However the main work is done by the {@link AggregationOperator} that can
 * be created using the
 * {@link #createOperator(GlobalSettings, OperatorColumnSettings)} method.
 * A new {@link AggregationOperator} should be created per column.
 * AggregationMethods are sorted first by the supported data type and then
 * by the label.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public interface AggregationMethod extends AggregationFunction, Comparable<AggregationMethod> {

    /**
     * Comparator that sorts {@link AggregationMethod}s based on their label that is displayed to the
     * user in ascending order.
     * @see #getLabel()
     * @since 2.10
     */
    public static final Comparator<AggregationMethod> ASC_NAME_COMPARATOR = new Comparator<AggregationMethod>() {

        /**{@inheritDoc}*/
        @Override
        public int compare(final AggregationMethod o1, final AggregationMethod o2) {
            if (o1 == o2) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            }
            if (o2 == null) {
                return 1;
            }
            return o1.getLabel().compareTo(o2.getLabel());
        }
    };

    /**
     * @param colName the unique name of the column
     * @param origSpec the original {@link DataColumnSpec}
     * @return the new {@link DataColumnSpecCreator} for the aggregated column
     */
    DataColumnSpec createColumnSpec(String colName, DataColumnSpec origSpec);

    /**
     * @return the label of the aggregation method which is
     * used in the column name
     */
    String getColumnLabel();

    /**
     * @param origColSpec the {@link DataColumnSpec} of the column to
     * check for compatibility
     * @return <code>true</code> if the aggregation method is compatible
     */
    boolean isCompatible(DataColumnSpec origColSpec);

    /**
     * Creates a new instance of this operator and returns it.
     * A new instance must be created for each column.
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     * @return a new instance of this operator
     */
    AggregationOperator createOperator(GlobalSettings globalSettings,
            OperatorColumnSettings opColSettings);

    /**
     * @return <code>true</code> if the operator supports the alteration of
     * the missing cell option
     */
    public boolean supportsMissingValueOption();

    /**
     * @return <code>true</code> if missing cells are considered during
     * aggregation
     */
    public boolean inclMissingCells();

    /**
     * @return the supported {@link DataValue} class
     */
    public Class<? extends DataValue> getSupportedType();


    /**
     * @return the user friendly label of the supported {@link DataValue} class
     */
    String getSupportedTypeLabel();

    /**
     * This method should return the name of all column names this operator requires
     * since the {@link DataRow} that is passed to the
     * {@link AggregationOperator#compute(DataRow, int...)} method contains only the
     * group, aggregation and additional column names for performance reasons.
     * Thus if the operator requires additional columns during the computation
     * besides the aggregation column itself it must return the names of all
     * these additional columns here. This is most likely the case for aggregation
     * operators that have optional setting {@link #hasOptionalSettings()} which
     * allow the user to select an additional column from the input table.
     *
     * @return {@link Collection} of column names that are required by this
     * aggregation operator in addition to the column that should be aggregated
     * @since 2.7
     */
    public Collection<String> getAdditionalColumnNames();
}
