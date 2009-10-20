/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
