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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Mar 28, 2018 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.algorithms.outlier;

import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObjectSpec;

/**
 * Port Object spec holding the outlier columns and group names
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class NumericOutliersPortObjectSpec extends AbstractSimplePortObjectSpec {

    /** Config key for the group column types. */
    private static final String CFG_GROUP_COL_TYPES = "group-col-types";

    /** Config key of outlier column names. */
    private static final String CFG_OUTLIER_COL_NAMES = "outliers";

    /** Config key of group column names. */
    private static final String CFG_GROUP_COL_NAMES = "groups";

    /** The group column types. */
    private DataType[] m_groupColTypes;

    /** The group column names. */
    private String[] m_groupColNames;

    /** The outlier column names. */
    private String[] m_outlierColNames;

    /**
     * @noreference This class is not intended to be referenced by clients.
     */
    public static final class Serializer extends AbstractSimplePortObjectSpecSerializer<NumericOutliersPortObjectSpec> {
    }

    /** Don't use, framework constructor. */
    public NumericOutliersPortObjectSpec() {

    }

    /**
     * Constructor.
     *
     * @param groupColTypes the group column types
     * @param groupColNames the group column names
     * @param outlierColNames the outlier column names
     *
     */
    NumericOutliersPortObjectSpec(final DataType[] groupColTypes, final String[] groupColNames,
        final String[] outlierColNames) {
        m_groupColTypes = groupColTypes;
        m_groupColNames = groupColNames;
        m_outlierColNames = outlierColNames;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final ModelContentWO model) {
        model.addDataTypeArray(CFG_GROUP_COL_TYPES, m_groupColTypes);
        model.addStringArray(CFG_GROUP_COL_NAMES, m_groupColNames);
        model.addStringArray(CFG_OUTLIER_COL_NAMES, m_outlierColNames);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void load(final ModelContentRO model) throws InvalidSettingsException {
        m_groupColTypes = model.getDataTypeArray(CFG_GROUP_COL_TYPES);
        m_groupColNames = model.getStringArray(CFG_GROUP_COL_NAMES);
        m_outlierColNames = model.getStringArray(CFG_OUTLIER_COL_NAMES);
    }

    /**
     * Returns the group column types.
     *
     * @return the group column types
     */
    public DataType[] getGroupColTypes() {
        return m_groupColTypes;
    }

    /**
     * Returns the group column names.
     *
     * @return the group column names
     */
    public String[] getGroupColNames() {
        return m_groupColNames;
    }

    /**
     * Returns the outlier column names.
     *
     * @return the outlier column names
     */
    public String[] getOutlierColNames() {
        return m_outlierColNames;
    }
}
