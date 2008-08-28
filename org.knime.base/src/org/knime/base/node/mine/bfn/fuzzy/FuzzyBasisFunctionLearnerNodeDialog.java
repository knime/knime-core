/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn.fuzzy;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.knime.base.node.mine.bfn.BasisFunctionLearnerNodeDialogPane;
import org.knime.base.node.mine.bfn.fuzzy.norm.Norm;
import org.knime.base.node.mine.bfn.fuzzy.shrink.Shrink;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * A dialog for the fuzzy basisfunction learner to set the following properties:
 * theta minus, theta plus, and a distance measurement.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class FuzzyBasisFunctionLearnerNodeDialog
        extends BasisFunctionLearnerNodeDialogPane {
    
    /** Holds all possible fuzzy norms. */
    private final JComboBox m_norm = new JComboBox(Norm.NORMS);

    /** Holds all possible shrink procedures. */
    private final JComboBox m_shrink = new JComboBox(Shrink.SHRINKS);

    /**
     * Creates a new dialog pane for fuzzy basis functions in order
     * to set theta minus, theta plus, and a choice of distance function.
     */
    public FuzzyBasisFunctionLearnerNodeDialog() {
        super();
        // panel with advance settings
        JPanel p = new JPanel(new GridLayout(2, 1));
        // norm combo box
        JPanel normPanel = new JPanel();
        normPanel.setBorder(BorderFactory.createTitledBorder(" Fuzzy Norm "));
        normPanel.add(m_norm);
        p.add(normPanel);
        // shrink procedure
        JPanel shrinkPanel = new JPanel();
        shrinkPanel.setBorder(BorderFactory
                .createTitledBorder(" Shrink Function "));
        shrinkPanel.add(m_shrink);
        p.add(shrinkPanel);
        // add fuzzy learner tab
        super.addTab("Fuzzy", p);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        // set shrink choice
        int shrink = settings.getInt(Shrink.SHRINK_KEY, 0);
        if (shrink >= 0 && shrink < m_shrink.getItemCount()) {
            m_shrink.setSelectedIndex(shrink);
        }
        // set norm function
        int norm = settings.getInt(Norm.NORM_KEY, 0);
        if (norm >= 0 && norm < m_norm.getItemCount()) {
            m_norm.setSelectedIndex(norm);
        }
    }

    /**
     * Updates this dialog by retrieving theta minus, theta plus, and the choice
     * of distance function from the underlying model.
     * 
     * @param settings the object to write the settings into
     * @throws InvalidSettingsException not thrown, but might be thrown by
     *             derived classes
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        super.saveSettingsTo(settings);
        // shrink procedure
        settings.addInt(Shrink.SHRINK_KEY, m_shrink.getSelectedIndex());
        // fuzzy norm
        settings.addInt(Norm.NORM_KEY, m_norm.getSelectedIndex());
    }
}
