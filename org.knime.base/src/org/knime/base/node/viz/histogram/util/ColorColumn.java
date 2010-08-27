/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *    01.01.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.util;

import java.awt.Color;
import java.io.Serializable;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.ConfigWO;

/**
 * Holds the color for a given column name.
 * @author Tobias Koetter, University of Konstanz
 */
public class ColorColumn implements Serializable {

    private static final long serialVersionUID = 761641948686587L;

    private static final String CFG_COLOR_RGB = "colorRGB";

    private static final String CFG_COL_NAME = "colName";

    private final Color m_color;

    private final String m_columnName;

    /**Constructor for class ColorColumn.
     * @param color the color
     * @param colName the name of the column
     */
    public ColorColumn(final Color color, final String colName) {
        if (color == null) {
            throw new IllegalArgumentException("Color not defined");
        }
        if (colName == null) {
            throw new IllegalArgumentException("No column name defined.");
        }
        m_color = color;
        m_columnName = colName;
    }

    /**
     * @return the color of the column
     */
    public Color getColor() {
        return m_color;
    }

    /**
     * @return the name of the column
     */
    public String getColumnName() {
        return m_columnName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_color == null) ? 0 : m_color.hashCode());
        result = prime * result
            + ((m_columnName == null) ? 0 : m_columnName.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ColorColumn other = (ColorColumn)obj;
        if (m_color == null) {
            if (other.m_color != null) {
                return false;
            }
        } else if (!m_color.equals(other.m_color)) {
            return false;
        }
        if (m_columnName == null) {
            if (other.m_columnName != null) {
                return false;
            }
        } else if (!m_columnName.equals(other.m_columnName)) {
            return false;
        }
        return true;
    }

    /**
     * @param config the config object to use
     * @param exec the {@link ExecutionMonitor} to provide progress messages
     * @throws CanceledExecutionException if the operation is canceled
     */
    public void save2File(final ConfigWO config,
            final ExecutionMonitor exec) throws CanceledExecutionException {
        config.addString(CFG_COL_NAME, getColumnName());
        config.addInt(CFG_COLOR_RGB, getColor().getRGB());
        exec.checkCanceled();
    }

    /**
     * @param config the config object to use
     * @param exec the {@link ExecutionMonitor} to provide progress messages
     * @return the {@link ColorColumn}
     * @throws CanceledExecutionException if the operation is canceled
     * @throws InvalidSettingsException if the config object is invalid
     */
    public static ColorColumn loadFromFile(final Config config,
            final ExecutionMonitor exec) throws CanceledExecutionException,
            InvalidSettingsException {
        final String name = config.getString(CFG_COL_NAME);
        final Color color = new Color(config.getInt(CFG_COLOR_RGB));
        exec.checkCanceled();
        return new ColorColumn(color, name);
    }
}
