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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   07.09.2011 (meinl): created
 */
package org.knime.testing.node.config;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.NodeMessage.Type;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * This is the dialog for the testflow configuration node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class TestConfigNodeDialog extends NodeDialogPane {
    private final TestConfigSettings m_settings = new TestConfigSettings();

    private final JTextField m_owner = new JTextField(15);

    private final DefaultListModel m_allNodesModel = new DefaultListModel();

    private final DefaultListModel m_logErrorsModel = new DefaultListModel();

    private final DefaultListModel m_logWarningsModel = new DefaultListModel();

    private final DefaultListModel m_logInfosModel = new DefaultListModel();

    private final JList m_allNodes = new JList(m_allNodesModel);

    private final JCheckBox m_mustFail = new JCheckBox();

    private final JTextField m_requiredError = new JTextField();

    private final JTextField m_requiredWarning = new JTextField();

    private int m_lastSelectedIndex = -1;

    /**
     * Creates a new dialog.
     */
    @SuppressWarnings("serial")
    public TestConfigNodeDialog() {
        m_allNodes.setCellRenderer(new DefaultListCellRenderer() {
            /**
             * {@inheritDoc}
             */
            @Override
            public Component getListCellRendererComponent(final JList list,
                    final Object value, final int index,
                    final boolean isSelected, final boolean cellHasFocus) {
                SingleNodeContainer cont = (SingleNodeContainer)value;
                if (value != null) {
                    String text = cont.getNameWithID();
                    return super.getListCellRendererComponent(list, text,
                            index, isSelected, cellHasFocus);
                } else {
                    return super.getListCellRendererComponent(list, value,
                            index, isSelected, cellHasFocus);
                }
            }
        });

        addTab("Workflow settings", createWorkflowSettingsPanel());
        addTab("Node settings", createNodeSettingsPanel());
    }

    private JPanel createWorkflowSettingsPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.insets = new Insets(2, 0, 2, 0);
        c.gridx = 0;
        c.gridy = 0;
        p.add(new JLabel("Workflow owner's mail address:   "), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        p.add(m_owner, c);

        c.gridx = 0;
        c.gridy++;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = 2;
        c.insets = new Insets(10, 0, 2, 0);
        JTabbedPane logLevels = new JTabbedPane();
        p.add(logLevels, c);

        logLevels.addTab("Log Errors", createLogLevelTab(m_logErrorsModel));
        logLevels.addTab("Log Warnings", createLogLevelTab(m_logWarningsModel));
        logLevels.addTab("Log Infos", createLogLevelTab(m_logInfosModel));
        return p;
    }

    private JPanel createLogLevelTab(final DefaultListModel listModel) {
        JPanel p = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 0, 2, 0);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        final JTextField input = new JTextField();
        p.add(input, c);
        c.gridy++;

        c.gridx = 0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.NONE;
        JButton add = new JButton("Add");
        p.add(add, c);
        c.gridx = 1;
        JButton remove = new JButton("Remove");
        p.add(remove, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        final JList list = new JList(listModel);
        p.add(new JScrollPane(list), c);

        add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                String text = input.getText();
                if (text.trim().length() > 0) {
                    listModel.addElement(text.trim());
                    input.setText("");
                }
            }
        });

        remove.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (list.getSelectedIndex() >= 0) {
                    listModel.remove(list.getSelectedIndex());
                }
            }
        });

        return p;
    }

    private JPanel createNodeSettingsPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        c.insets = new Insets(2, 0, 10, 0);
        p.add(new JScrollPane(m_allNodes), c);

        JPanel p2 = new JPanel(new GridBagLayout());
        c.gridy++;
        c.insets = new Insets(2, 0, 0, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0.1;
        p.add(p2, c);

        p2.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        GridBagConstraints c2 = new GridBagConstraints();

        c2.anchor = GridBagConstraints.WEST;
        c2.insets = new Insets(2, 2, 2, 2);
        c2.gridx = 0;
        c2.gridy = 0;
        p2.add(new JLabel("Node must fail   "), c2);
        c2.gridx = 1;
        p2.add(m_mustFail, c2);

        c2.gridx = 0;
        c2.gridy++;
        p2.add(new JLabel("Required error message   "), c2);
        c2.gridx = 1;
        c2.fill = GridBagConstraints.HORIZONTAL;
        c2.weightx = 1;
        p2.add(m_requiredError, c2);

        c2.gridx = 0;
        c2.gridy++;
        c2.weightx = 0;
        c2.fill = GridBagConstraints.NONE;
        p2.add(new JLabel("Required warning message   "), c2);
        c2.gridx = 1;
        c2.fill = GridBagConstraints.HORIZONTAL;
        c2.weightx = 1;
        p2.add(m_requiredWarning, c2);

        m_allNodes.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                int selected = m_allNodes.getSelectedIndex();
                if (!e.getValueIsAdjusting()) {
                    m_lastSelectedIndex = selected;
                    if (selected >= 0) {
                        updateNodeConfigurationFields(selected);
                    }
                }
            }
        });

        FocusListener focusListener = new FocusListener() {
            @Override
            public void focusLost(final FocusEvent e) {
                if (m_lastSelectedIndex >= 0) {
                    e.getComponent().setForeground(Color.BLACK);
                    storeNodeConfiguration(m_lastSelectedIndex);
                }
            }

            @Override
            public void focusGained(final FocusEvent e) {
            }
        };

        m_mustFail.addFocusListener(focusListener);
        m_requiredError.addFocusListener(focusListener);
        m_requiredWarning.addFocusListener(focusListener);

        return p;
    }

    private void storeNodeConfiguration(final int index) {
        SingleNodeContainer cont =
                (SingleNodeContainer)m_allNodesModel.get(index);
        String nodeID = cont.getID().getIDWithoutRoot();

        if (m_mustFail.isSelected()) {
            m_settings.addFailingNode(nodeID);
        } else {
            m_settings.removeFailingNode(nodeID);
        }

        if (m_requiredWarning.getText().trim().length() > 0) {
            m_settings.setRequiredNodeWarning(nodeID, m_requiredWarning
                    .getText().trim());
        } else {
            m_settings.setRequiredNodeWarning(nodeID, null);
        }

        if (m_requiredError.getText().trim().length() > 0) {
            m_settings.setRequiredNodeError(nodeID, m_requiredError.getText()
                    .trim());
        } else {
            m_settings.setRequiredNodeError(nodeID, null);
        }
    }

    private void updateNodeConfigurationFields(final int index) {
        SingleNodeContainer cont =
                (SingleNodeContainer)m_allNodesModel.get(index);
        String nodeID = cont.getID().getIDWithoutRoot();

        m_mustFail.setSelected(m_settings.failingNodes().contains(nodeID));

        String reqError = m_settings.requiredNodeErrors().get(nodeID);
        if (reqError == null) {
            NodeMessage msg = cont.getNodeMessage();
            if (msg.getMessageType() == Type.ERROR) {
                reqError = msg.getMessage();
                m_requiredError.setForeground(Color.GRAY);
            }
        } else {
            m_requiredError.setForeground(Color.BLACK);
        }
        m_requiredError.setText(reqError != null ? reqError : "");

        String reqWarning = m_settings.requiredNodeWarnings().get(nodeID);
        if (reqWarning == null) {
            NodeMessage msg = cont.getNodeMessage();
            if (msg.getMessageType() == Type.WARNING) {
                reqWarning = msg.getMessage();
                m_requiredWarning.setForeground(Color.GRAY);
            }
        } else {
            m_requiredWarning.setForeground(Color.BLACK);
        }
        m_requiredWarning.setText(reqWarning != null ? reqWarning : "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_settings.owner(m_owner.getText());

        List<String> temp = new ArrayList<String>();
        for (int i = 0; i < m_logErrorsModel.getSize(); i++) {
            temp.add((String)m_logErrorsModel.get(i));
        }
        m_settings.requiredLogErrors(temp);

        temp.clear();
        for (int i = 0; i < m_logWarningsModel.getSize(); i++) {
            temp.add((String)m_logWarningsModel.get(i));
        }
        m_settings.requiredLogWarnings(temp);

        temp.clear();
        for (int i = 0; i < m_logInfosModel.getSize(); i++) {
            temp.add((String)m_logInfosModel.get(i));
        }
        m_settings.requiredLogInfos(temp);

        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_settings.loadSettingsForDialog(settings);
        m_owner.setText(m_settings.owner());

        m_logErrorsModel.removeAllElements();
        for (String l : m_settings.requiredLogErrors()) {
            m_logErrorsModel.addElement(l);
        }

        m_logWarningsModel.removeAllElements();
        for (String l : m_settings.requiredLogWarnings()) {
            m_logWarningsModel.addElement(l);
        }

        m_logInfosModel.removeAllElements();
        for (String l : m_settings.requiredLogInfos()) {
            m_logInfosModel.addElement(l);
        }

        fillNodeList();
    }

    private void fillNodeList() {
        m_allNodesModel.removeAllElements();
        WorkflowManager root = findWorkflowManager(WorkflowManager.ROOT);
        fillNodeList(root);
    }

    /**
     * Fills the list of nodes with all nodes inside the current workflows. The
     * testflow configuration node is excluded.
     *
     * @param root the workflow manager from where to start the search
     */
    private void fillNodeList(final WorkflowManager root) {
        for (NodeContainer cont : root.getNodeContainers()) {
            if (cont instanceof SingleNodeContainer) {
                if (((SingleNodeContainer)cont).getNode().getDialogPane() != this) {
                    m_allNodesModel.addElement(cont);
                }
            } else if (cont instanceof WorkflowManager) {
                fillNodeList((WorkflowManager)cont);
            }
        }
    }

    /**
     * Finds the manager for the workflow in which the current node is
     * contained.
     *
     * @param root the root workflow manager from which the search should start
     * @return a workflow manager or <code>null</code> if no appropriate manager
     *         could be found (which would be strange)
     */
    private WorkflowManager findWorkflowManager(final WorkflowManager root) {
        for (NodeContainer cont : root.getNodeContainers()) {
            if (cont instanceof SingleNodeContainer) {
                if (((SingleNodeContainer)cont).getNode().getDialogPane() == this) {
                    return root;
                }
            } else if (cont instanceof WorkflowManager) {
                WorkflowManager wfm =
                        findWorkflowManager((WorkflowManager)cont);
                if (wfm != null) {
                    return wfm;
                }
            }
        }
        return null;
    }
}
