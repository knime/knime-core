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
 * History
 *   Apr 13, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.scorer.entrop;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;


/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class EntropyNodeDialogPane extends NodeDialogPane {
    private final ColumnSelectionComboxBox m_comboReference;

    private final ColumnSelectionComboxBox m_comboCluster;

    /**
     * The dialog for the entropy scorer. 
     *
     */
    @SuppressWarnings("unchecked")
    public EntropyNodeDialogPane() {
        m_comboReference = new ColumnSelectionComboxBox((Border)null,
                DataValue.class);
        m_comboCluster = new ColumnSelectionComboxBox((Border)null,
                DataValue.class);
        int h = m_comboCluster.getPreferredSize().height;
        m_comboCluster.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        m_comboReference.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        JPanel layout = new JPanel(new GridLayout(0, 2, 15, 15));
        JPanel clusterFlower = new JPanel(new FlowLayout());
        clusterFlower.add(m_comboCluster);
        JPanel referenceFlower = new JPanel(new FlowLayout());
        referenceFlower.add(m_comboReference);
        JPanel referenceFlowerLbl = new JPanel(new FlowLayout());
        referenceFlowerLbl.add(new JLabel("Reference Column: ",
                SwingConstants.RIGHT));
        JPanel clusterFlowerLbl = new JPanel(new FlowLayout());
        clusterFlowerLbl.add(new JLabel("Clustering Column: ",
                SwingConstants.RIGHT));
        layout.add(referenceFlowerLbl);
        layout.add(referenceFlower);
        layout.add(clusterFlowerLbl);
        layout.add(clusterFlower);
        addTab("Default", layout);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        // determine the default values
        DataTableSpec reference = specs[EntropyNodeModel.INPORT_REFERENCE];
        DataTableSpec clustering = specs[EntropyNodeModel.INPORT_CLUSTERING];
        String defaultClustering = suggestColumn(clustering, false);
        // in case that someone connected the same spec to both inports,
        // use the last but one string column as reference. Otherwise, take
        // the last column
        String defaultReference = suggestColumn(reference,
                reference == clustering);

        String clusterSelected = settings.getString(
                EntropyNodeModel.CFG_CLUSTERING_COLUMN, defaultClustering);
        String referenceSelected = settings.getString(
                EntropyNodeModel.CFG_REFERENCE_COLUMN, defaultReference);
        m_comboCluster.update(clustering, clusterSelected);
        m_comboReference.update(reference, referenceSelected);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        String reference = m_comboReference.getSelectedColumn();
        String clustering = m_comboCluster.getSelectedColumn();
        settings.addString(EntropyNodeModel.CFG_CLUSTERING_COLUMN, clustering);
        settings.addString(EntropyNodeModel.CFG_REFERENCE_COLUMN, reference);
    }

    private static String suggestColumn(final DataTableSpec spec,
            final boolean takeLastButOne) {
        boolean takeNext = !takeLastButOne;
        int colCount = spec.getNumColumns();
        // traverse backwards
        for (int i = colCount - 1; i >= 0; i--) {
            DataColumnSpec colSpec = spec.getColumnSpec(i);
            if (colSpec.getType().isCompatible(StringValue.class)) {
                if (takeNext) {
                    return colSpec.getName();
                } else {
                    takeNext = true; // last one was rejected, take previous
                }
            }
        }
        // fallback - simply take the last column
        return colCount > 0 ? spec.getColumnSpec(colCount - 1).getName() : null;
    }
}
