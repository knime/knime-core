/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *   17.12.2010 (hofer): created
 */
package org.knime.base.node.io.xml.xpath;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.knime.base.node.io.xml.ui.KeyValuePanel;
import org.knime.base.node.io.xml.xpath.XPathNodeSettings.XPathOutput;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.xml.XMLValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * This is the dialog for the XPath node.
 *
 * @author Heiko Hofer
 */
public class XPathNodeDialog extends NodeDialogPane {
    private ColumnSelectionComboxBox m_inputColumn;
    private JTextField m_newColumn;
    private JCheckBox m_removeInputColumn;
    private JTextArea m_xpath;
    private JRadioButton m_returnBoolean;
    private JRadioButton m_returnNumber;
    private JRadioButton m_returnString;
    private JRadioButton m_returnNode;
    private JRadioButton m_returnNodeSet;
    private KeyValuePanel m_nsPanel;

    /**
     * Creates a new dialog.
     */
    public XPathNodeDialog() {
        super();

        JPanel settings = createSettingsPanel();
        settings.setPreferredSize(new Dimension(600, 500));
        addTab("Settings", settings);
    }

    private JPanel createSettingsPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 4, 2, 4);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;

        p.add(new JLabel("XML column:"), c);
        c.gridx++;
        c.weightx = 1;
        m_inputColumn = new ColumnSelectionComboxBox(XMLValue.class);
        m_inputColumn.setBorder(null);
        p.add(m_inputColumn, c);

        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        p.add(new JLabel("New column name:"), c);
        c.gridx++;
        c.weightx = 1;
        m_newColumn = new JTextField();
        p.add(m_newColumn, c);

        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 2;
        m_removeInputColumn = new JCheckBox("Remove source column.");
        p.add(m_removeInputColumn, c);
        c.gridwidth = 1;

        Insets insets1 = c.insets;
        c.insets = new Insets(6, 4, 6, 4);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 2;

        m_xpath = new JTextArea();


        JScrollPane xpathScrollPane = new JScrollPane(m_xpath);
        xpathScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        xpathScrollPane.setPreferredSize(new Dimension(250, 100));
        xpathScrollPane.setPreferredSize(new Dimension(250, 100));
        xpathScrollPane.setBorder(BorderFactory.createTitledBorder("XPath query"));
        p.add(xpathScrollPane, c);
        c.insets = insets1;
        c.gridwidth = 1;

        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy++;
        JPanel returnTypePanel = createReturnTypePanel();
        returnTypePanel.setBorder(BorderFactory.createTitledBorder(
                "Return type (XPath -> KNIME type)"));
        p.add(returnTypePanel, c);

        c.gridy++;
        c.weighty = 1;
        m_nsPanel = new KeyValuePanel();
        m_nsPanel.setKeyColumnLabel("Prefix");
        m_nsPanel.setValueColumnLabel("Namespace");
        m_nsPanel.setBorder(BorderFactory.createTitledBorder("Namespaces"));
        p.add(m_nsPanel, c);
        return p;
    }

    private JPanel createReturnTypePanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 4, 0);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;

        m_returnBoolean = new JRadioButton("Boolean -> Boolean cell type");
        m_returnNumber = new JRadioButton("Number -> Double cell type");
        m_returnString = new JRadioButton("String -> String cell type");
        m_returnNode = new JRadioButton("Node -> XML cell type");
        m_returnNodeSet = new JRadioButton(
                "Node-Set -> Collection of XML cells");

        ButtonGroup group = new ButtonGroup();
        group.add(m_returnBoolean);
        group.add(m_returnNumber);
        group.add(m_returnString);
        group.add(m_returnNode);
        group.add(m_returnNodeSet);
        p.add(m_returnBoolean, c);
        c.gridy++;
        p.add(m_returnNumber, c);
        c.gridy++;
        p.add(m_returnString, c);
        c.gridy++;
        p.add(m_returnNode, c);
        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        p.add(m_returnNodeSet, c);
        return p;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        XPathNodeSettings s = new XPathNodeSettings();


        s.setInputColumn(m_inputColumn.getSelectedColumn());
        s.setNewColumn(m_newColumn.getText());
        s.setRemoveInputColumn(m_removeInputColumn.isSelected());
        s.setXpathQuery(m_xpath.getText());
        if (m_returnBoolean.isSelected()) {
            s.setReturnType(XPathOutput.Boolean);
        } else if (m_returnNumber.isSelected()) {
            s.setReturnType(XPathOutput.Number);
        } else if (m_returnString.isSelected()) {
            s.setReturnType(XPathOutput.String);
        } else if (m_returnNode.isSelected()) {
            s.setReturnType(XPathOutput.Node);
        } else if (m_returnNodeSet.isSelected()) {
            s.setReturnType(XPathOutput.NodeSet);
        }
        s.setNsPrefixes(m_nsPanel.getKeys());
        s.setNamespaces(m_nsPanel.getValues());
        s.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        XPathNodeSettings s = new XPathNodeSettings();
        s.loadSettingsDialog(settings, null);

        m_inputColumn.update(specs[0], s.getInputColumn());
        m_newColumn.setText(s.getNewColumn());
        m_removeInputColumn.setSelected(s.getRemoveInputColumn());
        m_xpath.setText(s.getXpathQuery());
        if (s.getReturnType().equals(XPathOutput.Boolean)) {
            m_returnBoolean.setSelected(true);
        } else if (s.getReturnType().equals(XPathOutput.Number)) {
            m_returnNumber.setSelected(true);
        } else if (s.getReturnType().equals(XPathOutput.String)) {
            m_returnString.setSelected(true);
        } else if (s.getReturnType().equals(XPathOutput.Node)) {
            m_returnNode.setSelected(true);
        } else if (s.getReturnType().equals(XPathOutput.NodeSet)) {
            m_returnNodeSet.setSelected(true);
        }
        m_nsPanel.setTableData(s.getNsPrefixes(), s.getNamespaces());

    }

}
