/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 */
package org.knime.base.node.mine.pca;

import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Node dialog for PCA predictor node.
 * 
 * @author uwe, University of Konstanz
 */
public class PCAApplyNodeDialog extends DefaultNodeSettingsPane {
    // private String[] m_dimensionChoices;

    private final SettingsModelPCADimensions m_pcaModel;

    private final DialogComponentChoiceConfig m_pcaConfig;

    /**
     * construct dialog.
     */
    public PCAApplyNodeDialog() {
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
                PCANodeModel.FAIL_MISSING, false),
                "Fail if missing values are encountered (skipped per default)"));
        m_pcaModel =
                new SettingsModelPCADimensions(
                        PCAApplyNodeModel.MIN_QUALPRESERVATION, 2, 100, false);
        m_pcaConfig = new DialogComponentChoiceConfig(m_pcaModel, true);
        addDialogComponent(m_pcaConfig);
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
                PCAApplyNodeModel.REMOVE_COLUMNS, false),
                "Replace original data columns"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);
        if (specs != null && specs[PCAApplyNodeModel.MODEL_INPORT] != null) {
            final PCAModelPortObjectSpec modelPort =
                    (PCAModelPortObjectSpec)specs[PCAApplyNodeModel.MODEL_INPORT];

            m_pcaModel.setEigenValues(modelPort.getEigenValues());
            m_pcaConfig.updateComponent();
        }
    }

}
