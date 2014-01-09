/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 */
package org.knime.base.node.image.readpng;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/** Configuration object to the Read PNG node.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class ReadPNGFromURLConfig {

    private String m_urlColName;

    private boolean m_failOnInvalid = true;

    private String m_newColumnName;

    /** @return the urlColName */
    public String getUrlColName() {
        return m_urlColName;
    }

    /** @param urlColName the urlColName to set */
    public void setUrlColName(final String urlColName) {
        m_urlColName = urlColName;
    }

    /** @return the failOnInvalid */
    public boolean isFailOnInvalid() {
        return m_failOnInvalid;
    }

    /** @param failOnInvalid the failOnInvalid to set */
    public void setFailOnInvalid(final boolean failOnInvalid) {
        m_failOnInvalid = failOnInvalid;
    }

    /** @return the newColumnName */
    public String getNewColumnName() {
        return m_newColumnName;
    }

    /** @param newColumnName the newColumnName to set */
    public void setNewColumnName(final String newColumnName) {
        m_newColumnName = newColumnName;
    }

    /** Save current configuration.
     * @param settings To save to.
     */
    void save(final NodeSettingsWO settings) {
        settings.addString("urlColumn", m_urlColName);
        settings.addBoolean("failIfInvalid", m_failOnInvalid);
        settings.addString("newColumnName", m_newColumnName);
    }

    /** Load config in node model.
     * @param settings To load from.
     * @throws InvalidSettingsException If invalid.
     */
    void loadInModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_urlColName = settings.getString("urlColumn");
        m_failOnInvalid = settings.getBoolean("failIfInvalid");
        m_newColumnName = settings.getString("newColumnName");
    }

    /** Load config in dialog.
     * @param settings To load from
     * @param in Current input spec
     * @throws NotConfigurableException If no configuration possible, e.g.
     */
    void loadInDialog(final NodeSettingsRO settings, final DataTableSpec in)
    throws NotConfigurableException {
        m_urlColName = settings.getString("urlColumn", null);
        DataColumnSpec col = in.getColumnSpec(m_urlColName);
        if (col == null || !col.getType().isCompatible(StringValue.class)) {
            try {
                guessDefaults(in);
            } catch (InvalidSettingsException e) {
                throw new NotConfigurableException(
                        "No valid input column available");
            }
        }
        // guessDefaults inits default values, so we can use them as fallback
        m_failOnInvalid = settings.getBoolean("failIfInvalid", m_failOnInvalid);
        m_newColumnName = settings.getString("newColumnName", m_newColumnName);
    }

    /** Guesses meaningful default values, e.g. the URL column is a string,
     * whose name possibly contains "file", "url" or so.
     * @param in The input spec.
     * @throws InvalidSettingsException If no auto-configuration is possible.
     */
    void guessDefaults(final DataTableSpec in)
            throws InvalidSettingsException {
        String lastStringCol = null;
        String prefStringCol = null;
        for (DataColumnSpec col : in) {
            if (col.getType().isCompatible(StringValue.class)) {
                String name = col.getName();
                lastStringCol = name;
                String lowName = name.toLowerCase();
                if (lowName.contains("url") || lowName.contains("file")
                        || lowName.contains("location")) {
                    prefStringCol = name;
                }
            }
        }
        String winColumn;
        if (prefStringCol != null) {
            winColumn = prefStringCol;
        } else if (lastStringCol != null) {
            winColumn = lastStringCol;
        } else {
            throw new InvalidSettingsException(
                    "No auto-configuration possible:"
                            + " No string compatible column in input");
        }
        m_urlColName = winColumn;
        m_failOnInvalid = true;
        m_newColumnName = DataTableSpec.getUniqueColumnName(
                in, "PNG to " + m_urlColName);
    }

}
