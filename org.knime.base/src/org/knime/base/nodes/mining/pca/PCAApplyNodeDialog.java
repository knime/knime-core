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
package org.knime.base.nodes.mining.pca;

import java.text.NumberFormat;
import java.util.Arrays;

import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Node dialog for PCA predictor node.
 * 
 * @author uwe, University of Konstanz
 */
public class PCAApplyNodeDialog extends DefaultNodeSettingsPane {
    private String[] m_dimensionChoices;

    private final DialogComponentStringSelection m_dimComponent;

    /**
     * construct dialog.
     */
    public PCAApplyNodeDialog() {
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
                PCANodeModel.FAIL_MISSING, false),
                "Fail if missing values are encountered (skipped per default)"));
        m_dimComponent =
                new DialogComponentStringSelection(new SettingsModelString(
                        PCANodeModel.RESULT_DIMENSIONS, ""),
                        "Number of dimensions to reduce to", new String[]{""});
        addDialogComponent(m_dimComponent);

        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
                PCAApplyNodeModel.REMOVE_COLUMNS, false),
                "Replace original data columns"));
    }

    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);
        final PCAModelPortObjectSpec pcaspec =
                (PCAModelPortObjectSpec)specs[PCAApplyNodeModel.MODEL_INPORT];
        final double[] eigenValues = pcaspec.getEigenValues();
        if (eigenValues == null) {
            throw new NotConfigurableException("need input data");

        }
        double sum = 0;
        final double[] partialSums = new double[eigenValues.length];
        for (int i = 0; i < eigenValues.length; i++) {
            sum += Math.abs(eigenValues[i]);
            for (int j = i; j < eigenValues.length; j++) {
                partialSums[j] +=
                        Math.abs(eigenValues[eigenValues.length - 1 - i]);
            }
        }
        final NumberFormat nf = NumberFormat.getPercentInstance();
        m_dimensionChoices = new String[eigenValues.length];
        for (int i = 0; i < eigenValues.length; i++) {
            if (i == 0) {
                m_dimensionChoices[i] =
                        i + 1 + " dimension ("
                                + nf.format(partialSums[i] / sum)
                                + " reproduction rate)";
            } else {
                m_dimensionChoices[i] =
                        i + 1 + " dimension ("
                                + nf.format(partialSums[i] / sum)
                                + " reproduction rate)";
            }
        }
        final String oldValue =
                ((SettingsModelString)m_dimComponent.getModel())
                        .getStringValue();
        if (oldValue.length() == 0) {
            m_dimComponent.replaceListItems(Arrays.asList(m_dimensionChoices),
                    m_dimensionChoices[m_dimensionChoices.length > 1 ? 1 : 0]);
        } else {
            m_dimComponent.replaceListItems(Arrays.asList(m_dimensionChoices),
                    oldValue);
        }
    }
}
