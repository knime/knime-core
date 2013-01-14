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
 * History
 *   Dec 24, 2011 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble.node.learner.classification;

import javax.swing.JScrollPane;

import org.knime.base.node.mine.treeensemble.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.base.node.mine.treeensemble.node.learner.panels.AttributeSelectionPanel;
import org.knime.base.node.mine.treeensemble.node.learner.panels.EnsembleOptionsPanel;
import org.knime.base.node.mine.treeensemble.node.learner.panels.TreeOptionsPanel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class TreeEnsembleClassificationLearnerNodeDialogPane extends NodeDialogPane {

    private final AttributeSelectionPanel m_attributeSelectionPanel;
    private final TreeOptionsPanel m_treeOptionsPanel;
    private final EnsembleOptionsPanel m_ensembleOptionsPanel;

    /**
     *  */
    public TreeEnsembleClassificationLearnerNodeDialogPane() {
        m_attributeSelectionPanel = new AttributeSelectionPanel(false);
        addTab("Attribute Selection",
                new JScrollPane(m_attributeSelectionPanel));

        m_treeOptionsPanel = new TreeOptionsPanel(m_attributeSelectionPanel);
        addTab("Tree Options", new JScrollPane(m_treeOptionsPanel));

        m_ensembleOptionsPanel = new EnsembleOptionsPanel();
        addTab("Ensemble Configuration",
                new JScrollPane(m_ensembleOptionsPanel));

    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        TreeEnsembleLearnerConfiguration cfg =
                new TreeEnsembleLearnerConfiguration(false);
        m_attributeSelectionPanel.saveSettings(cfg);
        m_treeOptionsPanel.saveSettings(cfg);
        m_ensembleOptionsPanel.saveSettings(cfg);
        cfg.save(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        final DataTableSpec inSpec = specs[0];
        TreeEnsembleLearnerConfiguration cfg =
                new TreeEnsembleLearnerConfiguration(false);
        cfg.loadInDialog(settings, inSpec);
        m_attributeSelectionPanel.loadSettingsFrom(inSpec, cfg);
        m_treeOptionsPanel.loadSettingsFrom(inSpec, cfg);
        m_ensembleOptionsPanel.loadSettings(cfg);
    }


}
