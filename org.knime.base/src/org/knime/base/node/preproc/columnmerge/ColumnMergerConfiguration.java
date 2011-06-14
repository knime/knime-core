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
 */
package org.knime.base.node.preproc.columnmerge;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/** Configuration to column merger node.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class ColumnMergerConfiguration {

    private String m_primaryColumn;
    private String m_secondaryColumn;
    private OutputPlacement m_outputPlacement;
    private String m_outputName;

    /** Policy how to place output. */
    enum OutputPlacement {
        /** Replace primary column. */
        ReplacePrimary,
        /** Replace secondary column. */
        ReplaceSecondary,
        /** Replace both columns, put output at position of primary column. */
        ReplaceBoth,
        /** Append as new column. */
        AppendAsNewColumn
    }

    /** @return the primaryColumn */
    String getPrimaryColumn() {
        return m_primaryColumn;
    }

    /** @param primaryColumn the primaryColumn to set */
    void setPrimaryColumn(final String primaryColumn) {
        m_primaryColumn = primaryColumn;
    }

    /** @return the secondaryColumn */
    String getSecondaryColumn() {
        return m_secondaryColumn;
    }

    /** @param secondaryColumn the secondaryColumn to set */
    void setSecondaryColumn(final String secondaryColumn) {
        m_secondaryColumn = secondaryColumn;
    }

    /** @return the outputPlacement */
    OutputPlacement getOutputPlacement() {
        return m_outputPlacement;
    }

    /** @return the outputName */
    String getOutputName() {
        return m_outputName;
    }

    /** @param outputName the outputName to set */
    void setOutputName(final String outputName) {
        m_outputName = outputName;
    }

    /** @param outputPlacement the outputPlacement to set */
    void setOutputPlacement(final OutputPlacement outputPlacement) {
        if (outputPlacement == null) {
            throw new NullPointerException();
        }
        m_outputPlacement = outputPlacement;
    }

    /**  Save current config to argument.
     * @param settings */
    void saveConfiguration(final NodeSettingsWO settings) {
        settings.addString("primaryColumn", m_primaryColumn);
        settings.addString("secondaryColumn", m_secondaryColumn);
        settings.addString("outputPlacement", m_outputPlacement.name());
        settings.addString("outputName", m_outputName);
    }

    /** Load config from argument.
      * @param settings To load from.
      * @throws InvalidSettingsException If inconsistent/missing. */
    void loadConfigurationInModel(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        m_primaryColumn = settings.getString("primaryColumn");
        m_secondaryColumn = settings.getString("secondaryColumn");
        String outputPlacement = settings.getString("outputPlacement");
        try {
            m_outputPlacement = OutputPlacement.valueOf(outputPlacement);
        } catch (Exception e) {
            throw new InvalidSettingsException("Illegal value for output "
                    + "placement parameter: " + outputPlacement, e);
        }
        m_outputName = settings.getString("outputName");
        switch (m_outputPlacement) {
        case AppendAsNewColumn:
            if (m_outputName == null || m_outputName.length() == 0) {
                throw new InvalidSettingsException(
                        "Illegal (empty) output column name");
            }
            break;
        default:
            // ignore
        }
    }

    /** Load config in dialog, init defaults if necessary.
      * @param settings to load from.
      * @param spec The input spec to load defaults from. */
    void loadConfigurationInDialog(final NodeSettingsRO settings,
            final DataTableSpec spec) {
        String firstStringCol = null;
        String secondStringCol = null;
        for (DataColumnSpec col : spec) {
            if (col.getType().isCompatible(StringValue.class)) {
                if (firstStringCol == null) {
                    firstStringCol = col.getName();
                } else if (secondStringCol == null) {
                    secondStringCol = col.getName();
                } else {
                    break;
                }
            }
        }
        m_primaryColumn =
            settings.getString("primaryColumn", firstStringCol);
        m_secondaryColumn =
            settings.getString("secondaryColumn", secondStringCol);
        String outputPlacement = settings.getString("outputPlacement",
                OutputPlacement.ReplaceBoth.name());
        try {
            m_outputPlacement = OutputPlacement.valueOf(outputPlacement);
        } catch (Exception e) {
            m_outputPlacement = OutputPlacement.ReplaceBoth;
        }
        m_outputName = settings.getString("outputName", m_outputName);
    }

}
