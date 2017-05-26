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
 */

package org.knime.base.data.aggregation.numerical;

import java.util.List;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.base.data.aggregation.general.AbstractMedianOperator;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;


/**
 * Computes the median of a list of numbers.
 * @author Tobias Koetter, University of Konstanz
 */
public class MedianOperator extends AbstractMedianOperator {

    private static final DataType TYPE = DoubleCell.TYPE;

    private static EvenListMedianMethod createCustomMeanMedianMethod() {
        return new EvenListMedianMethod() {

            @Override
            public DataCell extractMedian(final List<DataCell> cells, final int lowerCandidateIdx,
                final int upperCandidateIdx) {
                final double double1 = ((DoubleValue)cells.get(lowerCandidateIdx)).getDoubleValue();
                final double double2 = ((DoubleValue)cells.get(upperCandidateIdx)).getDoubleValue();

                return DoubleCellFactory.create((double1 + double2) / 2);
            }
        };
    }

    /**Constructor for class MedianOperator.
     * @param operatorData the operator data
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    protected MedianOperator(final OperatorData operatorData,
            final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        super(createDefaultMedianMethodDescriptions(createCustomMeanMedianMethod()), operatorData,
            globalSettings, opColSettings);
    }

    /**Constructor for class MedianOperator.
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    public MedianOperator(final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        super("Median", false, DoubleValue.class, createCustomMeanMedianMethod(),
            globalSettings, AggregationOperator.setInclMissingFlag(opColSettings, false));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataType getDataType(final DataType origType) {
        return TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(
            final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        final MedianOperator operator = new MedianOperator(getOperatorData(), globalSettings,
            opColSettings);
        operator.setMedianMethod(getMedianMethod());
        return operator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataCell getResultInternal(final DataCell median) {
        return convertToDoubleCellIfNecessary(median);
    }

    /** Converts argument to DoubleCell if it does not fully support the
     * DataValue interfaces supported by DoubleCell.TYPE .
     * @param cell Cell to convert (or not)
     * @return The argument or a new DoubleCell.
     */
    private DataCell convertToDoubleCellIfNecessary(final DataCell cell) {
        if (cell.isMissing()) {
            return DataType.getMissingCell();
        }
        if (TYPE.isASuperTypeOf(cell.getType())) {
            return cell;
        }
        return new DoubleCell(((DoubleValue)cell).getDoubleValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Calculates the median of a list of numbers. "
                + "Missing cells are skipped.";
    }

}
