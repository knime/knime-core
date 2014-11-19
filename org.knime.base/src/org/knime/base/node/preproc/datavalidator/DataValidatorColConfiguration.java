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
package org.knime.base.node.preproc.datavalidator;

import static org.knime.base.node.preproc.datavalidator.ConfigSerializationUtils.addEnum;
import static org.knime.base.node.preproc.datavalidator.ConfigSerializationUtils.getEnum;
import static org.knime.base.node.preproc.datavalidator.DataValidatorColConflicts.invalidType;
import static org.knime.core.node.util.CheckUtils.checkNotNull;
import static org.knime.core.node.util.CheckUtils.checkSettingNotNull;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Configuration object for a couple of columns.
 *
 * @author Marcel Hanser, University of Konstanz
 */
final class DataValidatorColConfiguration {

    /** NodeSettings key: write column name (only for individual columns). */
    private static final String CFG_COL_NAMES = "column_names";

    private static final String CFG_CASE_INSENSITIVE = "case_insensitive_name_matching";

    private static final String CFG_DATA_TYPE_HANDLING = "datatype_handling";

    private static final String CFG_DOMAIN_HANDLING_POSSIBLE_DOMAIN = "domain_handling_possible_values";

    private static final String CFG_DOMAIN_HANDLING_MIN_MAX = "domain_handling_min_max";

    private static final String CFG_COLUMN_MISSING_HANDLING = "colum_missing_handling";

    private static final String CFG_REJECT_ON_MISSING_VALUE = "reject_on_missing_value";

    /** String array with names of the column or null if meta column. */
    private String[] m_names;

    private DataTypeHandling m_dataTypeHandling = DataTypeHandling.FAIL;

    private ColumnExistenceHandling m_columnExistenceHandling = ColumnExistenceHandling.FAIL;

    private DomainHandling m_domainHandlingPossbileValues = DomainHandling.NONE;

    private DomainHandling m_domainHandlingMinMax = DomainHandling.NONE;

    private boolean m_caseInsensitiveNameMatching = false;

    private boolean m_rejectOnMissingValue = false;

    /**
     * Creates a new configuration with default settings.
     */
    DataValidatorColConfiguration() {
        m_names = new String[0];
    }

    /**
     * Constructor for a list of columns.
     *
     * @param specs list of column specs
     */
    DataValidatorColConfiguration(final List<DataColumnSpec> specs) {
        m_names = new String[specs.size()];
        for (int i = 0; i < m_names.length; i++) {
            m_names[i] = specs.get(i).getName();
        }
    }

    /**
     * Constructor for list of names.
     *
     * @param names the names
     */
    DataValidatorColConfiguration(final String[] names) {
        m_names = checkNotNull(names);
    }

    /**
     * @return the names
     */
    String[] getNames() {
        return m_names;
    }

    /**
     * @param names the names to set
     */
    void setNames(final String[] names) {
        m_names = names;
    }

    /**
     * @return the dataTypeHandling
     */
    DataTypeHandling getDataTypeHandling() {
        return m_dataTypeHandling;
    }

    /**
     * @param dataTypeHandling the dataTypeHandling to set
     */
    void setDataTypeHandling(final DataTypeHandling dataTypeHandling) {
        m_dataTypeHandling = dataTypeHandling;
    }

    /**
     * @return the caseInsensitiveNameMatching
     */
    boolean isCaseInsensitiveNameMatching() {
        return m_caseInsensitiveNameMatching;
    }

    /**
     * @param caseInsensitiveNameMatching the caseInsensitiveNameMatching to set
     */
    void setCaseInsensitiveNameMatching(final boolean caseInsensitiveNameMatching) {
        m_caseInsensitiveNameMatching = caseInsensitiveNameMatching;
    }

    /**
     * @return the domainHandlingPossbileValues
     */
    DomainHandling getDomainHandlingPossbileValues() {
        return m_domainHandlingPossbileValues;
    }

    /**
     * @param domainHandlingPossbileValues the domainHandlingPossbileValues to set
     */
    void setDomainHandlingPossbileValues(final DomainHandling domainHandlingPossbileValues) {
        m_domainHandlingPossbileValues = domainHandlingPossbileValues;
    }

    /**
     * @return the domainHandlingMinMax
     */
    DomainHandling getDomainHandlingMinMax() {
        return m_domainHandlingMinMax;
    }

    /**
     * @param domainHandlingMinMax the domainHandlingMinMax to set
     */
    void setDomainHandlingMinMax(final DomainHandling domainHandlingMinMax) {
        m_domainHandlingMinMax = domainHandlingMinMax;
    }

    /**
     * @return the columnExistenceHandling
     */
    ColumnExistenceHandling getColumnExistenceHandling() {
        return m_columnExistenceHandling;
    }

    /**
     * @param columnExistenceHandling the columnExistenceHandling to set
     */
    void setColumnExistenceHandling(final ColumnExistenceHandling columnExistenceHandling) {
        m_columnExistenceHandling = columnExistenceHandling;
    }

    /**
     * Checks this configuration against an input table spec.
     *
     * @param referenceColSpec the reference spec
     * @param inputColSpec the input spec
     * @param conflicts conflicts collection
     * @return <code>true</code> if a decorator for this column should be created.
     */
    boolean applyColConfiguration(final DataColumnSpec referenceColSpec, final DataColumnSpec inputColSpec,
        final DataValidatorColConflicts conflicts) {
        boolean nameShouldBeChanged = !inputColSpec.getName().equals(referenceColSpec.getName());
        switch (m_dataTypeHandling) {
            case NONE:
                return nameShouldBeChanged || includesDomainCheck(referenceColSpec) || m_rejectOnMissingValue;
            case FAIL:
                if (!referenceColSpec.getType().isASuperTypeOf(inputColSpec.getType())) {
                    conflicts.addConflict(invalidType(inputColSpec.getName(), referenceColSpec.getType(),
                        inputColSpec.getType()));
                }
                return nameShouldBeChanged || includesDomainCheck(referenceColSpec) || m_rejectOnMissingValue;
            case CONVERT_FAIL:
                return nameShouldBeChanged || isNotCompatible(referenceColSpec, inputColSpec)
                    || includesDomainCheck(referenceColSpec) || m_rejectOnMissingValue;
            default:
                throw new IllegalArgumentException("Unkown type...");
        }
    }

    /**
     * @return the failOnMissingValue
     */
    boolean isRejectOnMissingValue() {
        return m_rejectOnMissingValue;
    }

    /**
     * @param failOnMissingValue the failOnMissingValue to set
     */
    void setRejectOnMissingValue(final boolean failOnMissingValue) {
        m_rejectOnMissingValue = failOnMissingValue;
    }

    private boolean includesDomainCheck(final DataColumnSpec referenceColSpec) {
        boolean includeDomainCheck =
            !DomainHandling.NONE.equals(m_domainHandlingMinMax) && isCheckDomainMinMax(referenceColSpec);
        includeDomainCheck |=
            !DomainHandling.NONE.equals(m_domainHandlingPossbileValues) && checkDomainPossibleValues(referenceColSpec);
        return includeDomainCheck;
    }

    /**
     * @param specs
     */
    private boolean isCheckDomainMinMax(final DataColumnSpec spec) {
        return spec.getDomain().hasLowerBound() || spec.getDomain().hasUpperBound();
    }

    /**
     * @param specs
     */
    private boolean checkDomainPossibleValues(final DataColumnSpec spec) {
        return spec.getDomain().hasValues();
    }

    /**
     * Loads settings from a NodeSettings object, used in {@link org.knime.core.node.NodeModel}.
     *
     * @param settings the (sub-) config to load from
     * @return a new {@link DataValidatorColConfiguration} containing the contents of the settings object
     * @throws InvalidSettingsException if any setting is missing
     */
    static DataValidatorColConfiguration load(final NodeSettingsRO settings) throws InvalidSettingsException {

        DataValidatorColConfiguration dataValidatorColConfiguration =
            new DataValidatorColConfiguration(checkSettingNotNull(settings.getStringArray(CFG_COL_NAMES),
                "No names specified."));
        dataValidatorColConfiguration.setCaseInsensitiveNameMatching(settings.getBoolean(CFG_CASE_INSENSITIVE));
        dataValidatorColConfiguration.setDataTypeHandling(getEnum(settings, CFG_DATA_TYPE_HANDLING,
            DataTypeHandling.FAIL));
        dataValidatorColConfiguration.setDomainHandlingPossbileValues(getEnum(settings,
            CFG_DOMAIN_HANDLING_POSSIBLE_DOMAIN, DomainHandling.NONE));
        dataValidatorColConfiguration.setDomainHandlingMinMax(getEnum(settings, CFG_DOMAIN_HANDLING_MIN_MAX,
            DomainHandling.NONE));
        dataValidatorColConfiguration.setColumnExistenceHandling(getEnum(settings, CFG_COLUMN_MISSING_HANDLING,
            ColumnExistenceHandling.FAIL));

        dataValidatorColConfiguration.setRejectOnMissingValue(settings.getBoolean(CFG_REJECT_ON_MISSING_VALUE));
        return dataValidatorColConfiguration;
    }

    /**
     * Save settings to config object.
     *
     * @param settings to save to
     */
    void save(final NodeSettingsWO settings) {
        settings.addStringArray(CFG_COL_NAMES, m_names);
        settings.addBoolean(CFG_CASE_INSENSITIVE, m_caseInsensitiveNameMatching);
        settings.addBoolean(CFG_REJECT_ON_MISSING_VALUE, m_rejectOnMissingValue);
        addEnum(settings, CFG_DATA_TYPE_HANDLING, m_dataTypeHandling);
        addEnum(settings, CFG_DOMAIN_HANDLING_POSSIBLE_DOMAIN, m_domainHandlingPossbileValues);
        addEnum(settings, CFG_DOMAIN_HANDLING_MIN_MAX, m_domainHandlingMinMax);
        addEnum(settings, CFG_COLUMN_MISSING_HANDLING, m_columnExistenceHandling);
    }

    /**
     * Creates a cell validation decorator for the given parameters and this configuration.
     *
     * @param refColumnSpec the reference column spec
     * @param originalColumnSpec the original column spec
     * @param conflicts conflicts container
     * @return the validation decorator
     */
    DataValidatorCellDecorator createCellValidator(final DataColumnSpec refColumnSpec,
        final DataColumnSpec originalColumnSpec, final DataValidatorColConflicts conflicts) {

        DataColumnSpecCreator renamedColumnSpec = new DataColumnSpecCreator(originalColumnSpec);
        renamedColumnSpec.setName(refColumnSpec.getName());

        DataValidatorCellDecorator decorator =
            DataValidatorCellDecorator.forColumn(originalColumnSpec.getName(), renamedColumnSpec.createSpec());

        if (m_rejectOnMissingValue) {
            decorator = DataValidatorCellDecorator.missingHandlingCellDecorator(decorator, conflicts);
        }

        boolean rejectButDomainCheck =
        // there is a failing caused by incompatible domains and
            isNotCompatible(refColumnSpec, originalColumnSpec)
            // but it should fail due to
                && DataTypeHandling.FAIL.equals(m_dataTypeHandling)
                // and some domain checks, they can therefore not work
                && (!DomainHandling.NONE.equals(m_domainHandlingMinMax) || !DomainHandling.NONE
                    .equals(m_domainHandlingPossbileValues));

        if (!rejectButDomainCheck
            && !EnumSet.of(DataTypeHandling.NONE, DataTypeHandling.FAIL).contains(m_dataTypeHandling)
            && isNotCompatible(refColumnSpec, originalColumnSpec)) {
            decorator =
                DataValidatorCellDecorator.convertionCellDecorator(decorator, m_dataTypeHandling,
                    getConvertionType(refColumnSpec), refColumnSpec, conflicts);
        }

        if (!rejectButDomainCheck && !DomainHandling.NONE.equals(m_domainHandlingMinMax)
            && (refColumnSpec.getDomain().hasLowerBound() || refColumnSpec.getDomain().hasUpperBound())) {
            decorator =
                DataValidatorCellDecorator.domainHandlingCellDecorator(decorator, refColumnSpec.getType()
                    .getComparator(), m_domainHandlingMinMax, refColumnSpec.getDomain().getLowerBound(), refColumnSpec
                    .getDomain().getUpperBound(), refColumnSpec, conflicts);
        }

        if (!rejectButDomainCheck && !DomainHandling.NONE.equals(m_domainHandlingPossbileValues)
            && refColumnSpec.getDomain().hasValues()) {
            decorator =
                DataValidatorCellDecorator.domainHandlingCellDecorator(decorator, m_domainHandlingPossbileValues,
                    refColumnSpec.getDomain().getValues(), refColumnSpec, conflicts);
        }
        return decorator;
    }

    /**
     * @param refColumnSpec
     * @param originalColumnSpec
     * @return
     */
    private boolean isNotCompatible(final DataColumnSpec refColumnSpec, final DataColumnSpec originalColumnSpec) {
        return !refColumnSpec.getType().isASuperTypeOf(originalColumnSpec.getType());
    }

    /**
     * @param refColumnSpec
     * @return
     */
    private ConvertionType getConvertionType(final DataColumnSpec refColumnSpec) {
        DataType type = refColumnSpec.getType();

        // NOTE: The sequence here is important as we go from the most smallest general type to the most general one
        if (BooleanCell.TYPE.isASuperTypeOf(type)) {
            return ConvertionType.BOOLEAN;
        }
        if (IntCell.TYPE.isASuperTypeOf(type)) {
            return ConvertionType.INT;
        }
        if (LongCell.TYPE.isASuperTypeOf(type)) {
            return ConvertionType.LONG;
        }
        if (DoubleCell.TYPE.isASuperTypeOf(type)) {
            return ConvertionType.DOUBLE;
        }
        if (StringCell.TYPE.isASuperTypeOf(type)) {
            return ConvertionType.STRING;
        }
        throw new IllegalArgumentException("Type cannot be converted, " + type + " only "
            + Arrays.toString(ConvertionType.values()) + " are supported types.");

    }

    /**
     * Defines the data type handling.
     *
     * @author Marcel Hanser
     */
    enum DataTypeHandling {
        /**
         * No handling.
         */
        NONE(""),
        /**
         * Fails on different types.
         */
        FAIL("Reject if different"),
        /**
         * Trys to convert and fails if not possible.
         */
        CONVERT_FAIL("Try to convert (reject if not compatible)");
        private final String m_description;

        private DataTypeHandling(final String description) {
            m_description = description;
        }

        @Override
        public String toString() {
            return m_description;
        }
    }

    /**
     * Defines the domain check handling.
     *
     * @author Marcel Hanser
     */
    enum DomainHandling {
        /**
         * No handling.
         */
        NONE(""),
        /**
         * Fails if a cell is out of domain.
         */
        FAIL("Reject if out of domain"),
        /**
         * Replaces a cell with a missing value.
         */
        MISSING_VALUE("Replace with missing Values");
        private final String m_description;

        private DomainHandling(final String description) {
            m_description = description;
        }

        @Override
        public String toString() {
            return m_description;
        }
    }

    /**
     * Defines the data type handling.
     *
     * @author Marcel Hanser
     */
    enum ColumnExistenceHandling {
        /**
         * No handling.
         */
        NONE(""),
        /**
         * Fails on different types.
         */
        FAIL("Reject if column is missing"),
        /**
         * Trys to convert and fails if not possible.
         */
        FILL_WITH_MISSINGS("Insert column filled with missing values");
        private final String m_description;

        private ColumnExistenceHandling(final String description) {
            m_description = description;
        }

        @Override
        public String toString() {
            return m_description;
        }
    }

    /**
     * Defines the convertion possibilities of this node.
     *
     * @author Marcel Hanser
     */
    enum ConvertionType {
        /**
         * Boolean type.
         */
        BOOLEAN(BooleanCell.TYPE) {

            @Override
            public DataCell convertCell(final DataCell decoratedCell) {
                return Boolean.valueOf(decoratedCell.toString()) ? BooleanCell.TRUE : BooleanCell.FALSE;
            }
        },
        /**
         * Double type.
         */
        DOUBLE(DoubleCell.TYPE) {
            @Override
            public DataCell convertCell(final DataCell decoratedCell) {
                return new DoubleCell(Double.valueOf(decoratedCell.toString().replaceAll(",", ".")));
            }
        },
        /**
         * Integer type.
         */
        INT(IntCell.TYPE) {
            @Override
            public DataCell convertCell(final DataCell decoratedCell) {
                return new IntCell(Integer.valueOf(decoratedCell.toString()));
            }
        },
        /**
         * Long type.
         */
        LONG(LongCell.TYPE) {
            @Override
            public DataCell convertCell(final DataCell decoratedCell) {
                return new LongCell(Long.valueOf(decoratedCell.toString()));
            }
        },
        /**
         * String type.
         */
        STRING(StringCell.TYPE) {
            @Override
            public DataCell convertCell(final DataCell decoratedCell) {
                return new StringCell(decoratedCell.toString());
            }
        };

        private final DataType m_dataType;

        /**
         * @param dataType
         */
        private ConvertionType(final DataType dataType) {
            m_dataType = dataType;
        }

        @Override
        public String toString() {
            return name().substring(0, 1).toUpperCase() + name().substring(1).toLowerCase();
        }

        /**
         * @param decoratedCell the cell to convert
         * @return the converted cell
         */
        public abstract DataCell convertCell(final DataCell decoratedCell);

        /**
         * @return the target data type.
         */
        public DataType getTargetType() {
            return m_dataType;
        }
    }
}
