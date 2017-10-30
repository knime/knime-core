/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   25.10.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.node.predictor;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;


/**
 *
 * @author Adrian Nembach, KNIME
 */
public abstract class AbstractPredictorConfiguration implements PredictorConfiguration {

    private static final String CFG_PREDICTION_COLUMN_NAME = "predictionColumnName";
    private static final String CFG_CHANGE_PREDICTION_COLUMN_NAME = "changePredictionColumnName";

    private String m_predictionColumnName;
    private boolean m_changePredictionColumnName;

    /**
     * @param targetColumnName the name of the target column
     *
     */
    public AbstractPredictorConfiguration(final String targetColumnName) {
        m_predictionColumnName = PredictorConfiguration.getDefaultPredictionColumnName(targetColumnName);
        m_changePredictionColumnName = false;
    }

    @Override
    public String getPredictionColumnName() {
        return m_predictionColumnName;
    }

    @Override
    public void setPredictionColumnName(final String predictionColumnName) {
        m_predictionColumnName = predictionColumnName;
    }

    @Override
    public boolean isChangePredictionColumnName() {
        return m_changePredictionColumnName;
    }

    @Override
    public void setChangePredictionColumnName(final boolean changePredictionColumnName) {
        m_changePredictionColumnName = changePredictionColumnName;
    }



    @Override
    public final void save(final NodeSettingsWO settings) {
        settings.addString(CFG_PREDICTION_COLUMN_NAME, m_predictionColumnName);
        settings.addBoolean(CFG_CHANGE_PREDICTION_COLUMN_NAME, m_changePredictionColumnName);
        internalSave(settings);
    }

    /**
     * Use this method to save subclass specific settings.
     *
     * @param settings to save to
     */
    protected abstract void internalSave(final NodeSettingsWO settings);


    @Override
    public final void loadInDialog(final NodeSettingsRO settings) throws NotConfigurableException {
        String defColName = PredictorConfiguration.getDefaultPredictionColumnName("");
        m_predictionColumnName = settings.getString(CFG_PREDICTION_COLUMN_NAME, defColName);
        if (m_predictionColumnName == null || m_predictionColumnName.isEmpty()) {
            m_predictionColumnName = defColName;
        }
        m_changePredictionColumnName = settings.getBoolean(CFG_CHANGE_PREDICTION_COLUMN_NAME, true);
        internalLoadInDialog(settings);
    }

    /**
     * Use this to load subclass specific settings in the dialog.
     *
     * @param settings to load from
     * @throws NotConfigurableException if the node is currently not configurable
     */
    protected abstract void internalLoadInDialog(final NodeSettingsRO settings) throws NotConfigurableException;


    @Override
    public final void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_predictionColumnName = settings.getString(CFG_PREDICTION_COLUMN_NAME);
        if (m_predictionColumnName == null || m_predictionColumnName.isEmpty()) {
            throw new InvalidSettingsException("Prediction column name must not be empty");
        }
        m_changePredictionColumnName = settings.getBoolean(CFG_CHANGE_PREDICTION_COLUMN_NAME, true);
        internalLoadInModel(settings);
    }

    /**
     * Use this method to load subclass specific settings in the node model.
     *
     * @param settings to load from
     * @throws InvalidSettingsException if any of the settings are invalid
     */
    protected abstract void internalLoadInModel(final NodeSettingsRO settings) throws InvalidSettingsException;


}
