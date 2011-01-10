/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Jul 24, 2010 (wiswedel): created
 */
package org.knime.base.node.preproc.columntogrid;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Configuration to the node, contains grid count column number and included
 * columns.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class ColumnToGridConfiguration {

    private static final int DEF_COL_COUNT = 4;

    private String[] m_includes;
    private int m_colCount;

    /** Creates new config, auto-guessing defaults if possible (prefers
     * unknown type columns over string columns).
     * @param spec The input spec.
     * @throws InvalidSettingsException If no config possible (only numbers)
     */
    public ColumnToGridConfiguration(final DataTableSpec spec)
        throws InvalidSettingsException {
        m_colCount = DEF_COL_COUNT;
        m_includes = autoGuessIncludeColumns(spec);
        if (m_includes == null) {
            throw new InvalidSettingsException(
                    "No reasonable auto-configuration possible");
        }
    }

    /** Default constructor, inits field empty. */
    public ColumnToGridConfiguration() {
        // nothing to do
    }

    /** Saves current settings to argument.
     * @param settings To save to.
     */
    void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_includes != null) {
            settings.addStringArray("includes", m_includes);
            settings.addInt("grid_col_count", m_colCount);
        }
    }

    /** Loads settings in model.
     * @param settings To load from.
     * @throws InvalidSettingsException If invalid.
     */
    void loadSettings(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        m_includes = settings.getStringArray("includes");
        if (m_includes == null || m_includes.length == 0) {
            throw new InvalidSettingsException("No column(s) selected");
        }
        m_colCount = settings.getInt("grid_col_count");
        if (m_colCount <= 0) {
            throw new InvalidSettingsException(
                    "Invalid grid col count: " + m_colCount);
        }
    }

    /** Loads settings in dialog, inits default if invalid.
     * @param settings To load from.
     * @param spec The input spec.
     */
    void loadSettings(final NodeSettingsRO settings, final DataTableSpec spec) {
        String[] includes = autoGuessIncludeColumns(spec);
        m_includes = settings.getStringArray("includes", includes);
        m_colCount = settings.getInt("grid_col_count", DEF_COL_COUNT);
        if (m_colCount <= 0) {
            m_colCount = DEF_COL_COUNT;
        }
    }

    /** @return the includes */
    String[] getIncludes() {
        if (m_includes == null) {
            return new String[0];
        }
        return m_includes;
    }
    /** @param includes the includes to set */
    void setIncludes(final String[] includes) {
        m_includes = includes;
    }
    /** @return the colCount */
    int getColCount() {
        return m_colCount;
    }
    /** @param colCount the colCount to set */
    void setColCount(final int colCount) {
        m_colCount = colCount;
    }

    /** Auto-guessing: choose first column that is not string, int, double
     * as include; if no such column exists use first string column, otherwise
     * use non (null returned).
     * @param spec Input spec.
     * @return A meaningful default or null.
     */
    static String[] autoGuessIncludeColumns(final DataTableSpec spec) {
        String firstStringCol = null;
        String firstUnknownCol = null;
        for (DataColumnSpec col : spec) {
            DataType type = col.getType();
            if (type.equals(StringCell.TYPE)) {
                if (firstStringCol == null) {
                    firstStringCol = col.getName();
                }
            } else if (type.equals(DoubleCell.TYPE)) {
                // ignore
            } else if (type.equals(IntCell.TYPE)) {
                // ignore
            } else {
                if (firstUnknownCol == null) {
                    firstUnknownCol = col.getName();
                }
            }
        }
        String sel = null;
        if (firstUnknownCol != null) {
            sel = firstUnknownCol;
        } else if (firstStringCol != null) {
            sel = firstStringCol;
        } else {
            return null;
        }
        return new String[] {sel};
    }

}
