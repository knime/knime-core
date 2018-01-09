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
 *   25.10.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.node.predictor;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * Root interface for predictor configurations in the tree ensemble plugin.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public interface PredictorConfiguration {

    /**
     * @param targetColumnName the name of the target column
     * @return the default prediction column name
     */
    public static String getDefaultPredictionColumnName(final String targetColumnName) {
        return "Prediction (" + targetColumnName + ")";
    }

    /**
     * @return the name of the prediction column
     */
    public String getPredictionColumnName();

    /**
     * @param predictionColumnName the new name for the prediction column
     */
    public void setPredictionColumnName(final String predictionColumnName);

    /**
     * @return true if the prediction column name should be changed from the default
     */
    public boolean isChangePredictionColumnName();

    /**
     * @param changePredictionColumnName whether the prediction column name
     * should be changed from the default
     */
    public void setChangePredictionColumnName(final boolean changePredictionColumnName);

    /**
     * Saves the configuration to the provided settings object.
     *
     * @param settings to save to
     */
    public void save(final NodeSettingsWO settings);

    /**
     * Used to load the configuration in the node dialog.
     *
     * @param settings to load configuration from
     * @throws NotConfigurableException if the node is currently not configurable
     */
    public void loadInDialog(final NodeSettingsRO settings) throws NotConfigurableException;

    /**
     * Used to load the configuration in the node model.
     *
     * @param settings to load configuration from
     * @throws InvalidSettingsException if any of the settings are invalid
     */
    public void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException;
}
