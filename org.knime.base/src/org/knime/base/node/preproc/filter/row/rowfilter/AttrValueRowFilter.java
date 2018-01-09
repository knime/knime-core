/*
 * ------------------------------------------------------------------------
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
 *   04.03.2008 (ohl): created
 */
package org.knime.base.node.preproc.filter.row.rowfilter;

import java.util.Stack;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.ConvenienceMethods;

/**
 * Super class for all row filters that test an attribute value (like
 * {@link StringCompareRowFilter}, {@link MissingValueRowFilter}, and
 * {@link RangeRowFilter}).
 *
 * @author ohl, University of Konstanz
 */
public abstract class AttrValueRowFilter extends RowFilter {

    private static final String CFGKEY_INCLUDE = "include";

    private static final String CFGKEY_COLNAME = "ColumnName";

    private static final String CFGKEY_DEEP_FILTERING = "deepFiltering";

    private boolean m_include;

    private boolean m_deepFiltering;

    private String m_colName;

    private int m_colIdx;

    /**
     * Don't use an instance created by this constructor until calling
     * loadSettings and configure.
     */
    AttrValueRowFilter() {
        this("", false);
    }

    /**
     * The super class stores the class name and the include flag for all
     * filters checking the attribute value in some way.
     *
     * @param colName the name of the column to test
     * @param include flag indicating whether to include or exclude matching
     *            rows
     */
    protected AttrValueRowFilter(final String colName, final boolean include) {
        this(colName, include, false);
    }

    /**
     * The super class stores the class name and the include flag for all
     * filters checking the attribute value in some way.
     *
     * @param colName the name of the column to test
     * @param include flag indicating whether to include or exclude matching
     *            rows
     * @param deepFiltering flag indicating whether to perform a deep search for collection columns
     * @since 2.10
     */
    protected AttrValueRowFilter(final String colName, final boolean include, final boolean deepFiltering) {
        if (colName == null) {
            throw new NullPointerException("Column name can't be null");
        }
        m_colName = colName;
        m_colIdx = -1;
        m_include = include;
        m_deepFiltering = deepFiltering;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec configure(final DataTableSpec inSpec)
            throws InvalidSettingsException {

        if (!inSpec.containsName(m_colName)) {
            throw new InvalidSettingsException("Column range filter: "
                    + "Input table doesn't contain specified column name");
        }

        m_colIdx = inSpec.findColumnIndex(m_colName);

        return inSpec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO cfg)
            throws InvalidSettingsException {
        m_colName = cfg.getString(CFGKEY_COLNAME);
        m_colIdx = -1;

        if ((m_colName == null) || (m_colName.length() < 1)) {
            throw new InvalidSettingsException("String compare filter: "
                    + "NodeSettings object contains no column name");
        }
        m_include = cfg.getBoolean(CFGKEY_INCLUDE);

        if (cfg.containsKey(CFGKEY_DEEP_FILTERING)) {
            //introduced in KNIME 2.9+
            m_deepFiltering = cfg.getBoolean(CFGKEY_DEEP_FILTERING);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettings(final NodeSettingsWO cfg) {
        cfg.addString(CFGKEY_COLNAME, m_colName);
        cfg.addBoolean(CFGKEY_INCLUDE, m_include);
        cfg.addBoolean(CFGKEY_DEEP_FILTERING, m_deepFiltering);
    }

    /**
     * @return the colName
     */
    public String getColName() {
        return m_colName;
    }

    /**
     * @return the index of the column with the column name. Not valid until
     *         configure is called.
     */
    protected int getColIdx() {
        return m_colIdx;
    }

    /**
     * @return the include
     */
    public boolean getInclude() {
        return m_include;
    }

    /**
     * @return the deep filtering flag
     * @since 2.10
     */
    public boolean getDeepFiltering() {
        return m_deepFiltering;
    }

    /**
     * {@inheritDoc}
     * Attribute value based filters (based on the value of a cell) must implement this and should use the
     * {@link #performDeepFiltering(CollectionDataValue)} if the cell is a collection. In order for the deep
     * filtering to work they MUST overwrite the default implementation of {@link #matches(DataCell)}!
     */
    @Override
    public boolean matches(final DataRow row, final long rowIndex) throws EndOfTableException, IncludeFromNowOn {
        return matches(row, ConvenienceMethods.checkTableSize(rowIndex));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matches(final DataRow row, final int rowIndex) throws EndOfTableException, IncludeFromNowOn {
        return matches(row, (long) rowIndex);
    }

    /**
     * @param theCell {@link CollectionDataValue} to process
     * @return <code>true</code> if one of the elements of the given {@link CollectionDataValue} matches the
     * filter criterion otherwise it returns <code>false</code>
     * @since 2.10
     */
    protected boolean performDeepFiltering(final CollectionDataValue theCell) {
        //use a LIFO queue to perform a depth first search through all elements of the collections
        final Stack<CollectionDataValue> collCells = new Stack<CollectionDataValue>();
        collCells.push(theCell);
        while (!collCells.isEmpty()) {
            final CollectionDataValue collCell = collCells.pop();
            for (final DataCell cell : collCell) {
                if (cell instanceof CollectionDataValue) {
                    collCells.push((CollectionDataValue)cell);
                } else if (matches(cell)) {
                    //if we find a match we can return otherwise we have to keep on comparing till the end
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Implementations should overwrite this for {@link #performDeepFiltering(CollectionDataValue)} to work. Also see
     * {@link #matches(DataRow, int)}.
     *
     * @param theCell the {@link DataCell} to check
     * @return <code>true</code> if the cell matches the filter criterion
     * @since 2.10
     */
    protected boolean matches(final DataCell theCell) {
        return false;
    }
}
