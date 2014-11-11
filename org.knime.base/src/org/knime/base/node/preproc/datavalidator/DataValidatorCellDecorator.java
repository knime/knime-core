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
 *   09.09.2014 (Marcel Hanser): created
 */
package org.knime.base.node.preproc.datavalidator;

import static org.knime.base.node.preproc.datavalidator.DataValidatorColConflicts.containsMissingValue;
import static org.knime.base.node.preproc.datavalidator.DataValidatorColConflicts.convertionFailed;
import static org.knime.base.node.preproc.datavalidator.DataValidatorColConflicts.outOfDomain;
import static org.knime.core.node.util.CheckUtils.checkArgument;
import static org.knime.core.node.util.CheckUtils.checkNotNull;

import java.util.Set;

import org.knime.base.node.preproc.datavalidator.DataValidatorColConfiguration.ConvertionType;
import org.knime.base.node.preproc.datavalidator.DataValidatorColConfiguration.DataTypeHandling;
import org.knime.base.node.preproc.datavalidator.DataValidatorColConfiguration.DomainHandling;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.RowKey;

/**
 * Uses the decorator pattern for data cell validation. I.e. missing value or domain checks can be dynamically added and
 * the functionality is nice encapsulated. See the factory methods on this class to receive instances.
 *
 * @author Marcel Hanser
 */
abstract class DataValidatorCellDecorator {
    private DataValidatorCellDecorator m_innerDecorator;

    /**
     * Use the factory methods to get an instance.
     *
     * @param innerDecorator
     */
    private DataValidatorCellDecorator(final DataValidatorCellDecorator innerDecorator) {
        m_innerDecorator = innerDecorator;
    }

    /**
     * First invokes the {@link #handleCell(RowKey, DataCell)} method on the inner decorator and afterwards performs
     * {@link #doHandleCell(RowKey, DataCell)} on the result.
     *
     * @param rowKey the row this cell is contained in
     * @param cell the data cell
     * @return the (decorated) cell
     */
    final DataCell handleCell(final RowKey rowKey, final DataCell cell) {
        DataCell decoratedCell = cell;
        if (m_innerDecorator != null) {
            decoratedCell = m_innerDecorator.handleCell(rowKey, cell);
        }

        return doHandleCell(rowKey, decoratedCell);
    }

    /**
     * Performs the actual validation, which may add conflicts or return a converted cell.
     *
     * @param rowKey the row key this cell is contained in
     * @param decoratedCell the cell which might be changed in inner decorators
     * @return the decorated data cell
     */
    abstract DataCell doHandleCell(final RowKey rowKey, final DataCell decoratedCell);

    /**
     * @return the data column spec associated with this decorator
     */
    protected DataColumnSpec getDataColumnSpec() {
        checkArgument(m_innerDecorator != null, "Should not be null");
        return m_innerDecorator.getDataColumnSpec();
    }

    /**
     * @return the name of the input column spec
     */
    protected String getInputColumnName() {
        checkArgument(m_innerDecorator != null, "Should not be null");
        return m_innerDecorator.getInputColumnName();
    }

    /**
     * The basic decorator which returns the name and the input spec.
     *
     * @param inputColumnName the input column name
     * @param inputSpec the input column spec
     * @return the decorator
     */
    static DataValidatorCellDecorator forColumn(final String inputColumnName, final DataColumnSpec inputSpec) {
        return new DataValidatorCellDecorator(null) {

            @Override
            protected DataCell doHandleCell(final RowKey rowKey, final DataCell decoratedCell) {
                return decoratedCell;
            }

            @Override
            protected DataColumnSpec getDataColumnSpec() {
                return inputSpec;
            }

            @Override
            protected String getInputColumnName() {
                return inputColumnName;
            }
        };
    }

    /**
     * Adds a conflict if a missing value is contained.
     *
     * @param inner the inner decorator
     * @param conflicts the conflicts collection
     * @return a decorator
     */
    static DataValidatorCellDecorator missingHandlingCellDecorator(final DataValidatorCellDecorator inner,
        final DataValidatorColConflicts conflicts) {
        return new DataValidatorCellDecorator(inner) {

            @Override
            protected DataCell doHandleCell(final RowKey rowKey, final DataCell decoratedCell) {
                if (decoratedCell.isMissing()) {
                    conflicts.addConflict(containsMissingValue(getInputColumnName(), rowKey));
                }
                return decoratedCell;
            }
        };
    }

    /**
     * Tries to convert the given cell to the given type. Missing values are directly returned
     *
     * @param inner the inner decorator
     * @param handling data type handling
     * @param type to convert into
     * @param referenceSpec the reference specification
     * @param conflicts the conflicts collection
     * @return a decorator
     */
    static DataValidatorCellDecorator convertionCellDecorator(final DataValidatorCellDecorator inner,
        final DataTypeHandling handling, final ConvertionType type, final DataColumnSpec referenceSpec,
        final DataValidatorColConflicts conflicts) {
        checkNotNull(handling);
        checkNotNull(type);
        checkNotNull(referenceSpec);
        return new DataValidatorCellDecorator(inner) {

            @Override
            protected DataCell doHandleCell(final RowKey rowKey, final DataCell decoratedCell) {
                try {
                    if (decoratedCell.isMissing()) {
                        return decoratedCell;
                    }
                    return type.convertCell(decoratedCell);
                } catch (RuntimeException e) {
                    switch (handling) {
                        case CONVERT_FAIL:
                            conflicts.addConflict(convertionFailed(getInputColumnName(), rowKey, type.getTargetType()));
                            // also return the missing cell
                            return DataType.getMissingCell();
                        default:
                            throw new IllegalArgumentException("Invalid handling!" + handling);
                    }
                }
            }

            @Override
            protected DataColumnSpec getDataColumnSpec() {
                return referenceSpec;
            }
        };
    }

    /**
     * Checks that the given cell is within the given domain.
     *
     * @param inner the inner decorator
     * @param comparator data value comparator for this spec
     * @param domainHandling the domain handling
     * @param min minimum value
     * @param max maximum value
     * @param referenceSpec the reference spec
     * @param conflicts the conflicts collection
     * @return a decorator
     */
    static DataValidatorCellDecorator domainHandlingCellDecorator(final DataValidatorCellDecorator inner,
        final DataValueComparator comparator, final DomainHandling domainHandling, final DataCell min,
        final DataCell max, final DataColumnSpec referenceSpec, final DataValidatorColConflicts conflicts) {
        checkNotNull(domainHandling);
        checkNotNull(comparator);
        checkNotNull(referenceSpec);
        return new DataValidatorCellDecorator(inner) {

            @Override
            protected DataCell doHandleCell(final RowKey rowKey, final DataCell decoratedCell) {
                if (decoratedCell.isMissing()) {
                    return decoratedCell;
                }
                if (min != null && comparator.compare(min, decoratedCell) > 0) {
                    switch (domainHandling) {
                        case FAIL:
                            conflicts.addConflict(outOfDomain(getInputColumnName(), rowKey,
                                "less than the defined minimal value"));
                            // also return the missing cell
                        case MISSING_VALUE:
                            return DataType.getMissingCell();
                        default:
                            throw new IllegalArgumentException("Invalid handling!" + domainHandling);
                    }
                }
                if (max != null && comparator.compare(max, decoratedCell) < 0) {
                    switch (domainHandling) {
                        case FAIL:
                            conflicts.addConflict(outOfDomain(getInputColumnName(), rowKey,
                                "greater than the defined maximal value"));
                            // also return the missing cell
                        case MISSING_VALUE:
                            return DataType.getMissingCell();
                        default:
                            throw new IllegalArgumentException("Invalid handling!" + domainHandling);
                    }
                }
                return decoratedCell;
            }

            @Override
            protected DataColumnSpec getDataColumnSpec() {
                return referenceSpec;
            }
        };
    }

    /**
     * Checks that the given cell is within the given domain.
     *
     * @param inner the inner decorator
     * @param domainHandling the domain handling
     * @param values possible values
     * @param referenceSpec the reference spec
     * @param conflicts the conflicts collection
     * @return a decorator
     */
    static DataValidatorCellDecorator domainHandlingCellDecorator(final DataValidatorCellDecorator inner,
        final DomainHandling domainHandling, final Set<DataCell> values, final DataColumnSpec referenceSpec,
        final DataValidatorColConflicts conflicts) {
        checkNotNull(domainHandling);
        checkNotNull(values);
        checkNotNull(referenceSpec);
        return new DataValidatorCellDecorator(inner) {

            @Override
            protected DataCell doHandleCell(final RowKey rowKey, final DataCell decoratedCell) {
                if (decoratedCell.isMissing()) {
                    return decoratedCell;
                }
                if (!values.contains(decoratedCell)) {
                    switch (domainHandling) {
                        case FAIL:
                            conflicts.addConflict(outOfDomain(getInputColumnName(), rowKey,
                                "is not contained in the possible values set"));
                            // also return the missing cell
                        case MISSING_VALUE:
                            return DataType.getMissingCell();
                        default:
                            throw new IllegalArgumentException("Invalid handling!" + domainHandling);
                    }
                }
                return decoratedCell;
            }

            @Override
            protected DataColumnSpec getDataColumnSpec() {
                return referenceSpec;
            }
        };
    }
}
