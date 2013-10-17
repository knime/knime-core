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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * Created on 21.08.2013 by Christian Albrecht, KNIME.com AG, Zurich, Switzerland
 */
package org.knime.core.node.dialog;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 *
 * @author Christian Albrecht, KNIME.com AG, Zurich, Switzerland
 * @param <VAL> The node value type.
 * @since 2.9
 */
public abstract class DialogNodeRepresentation<VAL extends DialogNodeValue> {

    private static final String CFG_WEIGHT = "weight";

    private static final int DEFAULT_WEIGHT = 1;

    private int m_weight = DEFAULT_WEIGHT;

    /**
     * @param settings The settings
     */
    public void saveToNodeSettings(final NodeSettingsWO settings) {
        settings.addInt(CFG_WEIGHT, m_weight);
    }

    /**
     * @param settings The settings
     * @throws InvalidSettingsException If the settings could not be loaded
     */
    public void loadFromNodeSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_weight = settings.getInt(CFG_WEIGHT);
    }

    /**
     * @param settings The settings
     */
    public void loadFromNodeSettingsInDialog(final NodeSettingsRO settings) {
        m_weight = settings.getInt(CFG_WEIGHT, DEFAULT_WEIGHT);
    }

    /**
     * @return The panel to be shown as a dialog component.
     */
    public abstract DialogNodePanel<VAL> createDialogPanel();

    /**
     * @return the weight
     */
    public int getWeight() {
        return m_weight;
    }

    /**
     * @param weight the weight to set
     */
    public void setWeight(final int weight) {
        m_weight = weight;
    }

    /**
     * Resets a given DialogNodeValue to a default value.
     * @param value the value to reset.
     */
    public abstract void resetNodeValueToDefault(VAL value);
}
