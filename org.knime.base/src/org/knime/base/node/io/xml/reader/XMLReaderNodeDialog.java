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
 *   16.12.2010 (hofer): created
 */
package org.knime.base.node.io.xml.reader;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.base.node.io.xml.ui.KeyValuePanel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.FilesHistoryPanel;

/**
 * This is the dialog for the XML reader;
 *
 * @author Heiko Hofer
 */
public class XMLReaderNodeDialog extends NodeDialogPane {
    private FilesHistoryPanel m_url;
    private JCheckBox m_useXPathFilter;
    private JTextField m_xpath;
    private KeyValuePanel m_nsPanel;


    /**
     * Creates a new dialog.
     */
    public XMLReaderNodeDialog() {
        super();

        addTab("Settings", createFilePanel());
    }

    private JPanel createFilePanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;

        m_url = new FilesHistoryPanel("org.knime.base.node.io.xml.reader", false);
        m_url.setBorder(BorderFactory.createTitledBorder("Selected File"));
        p.add(m_url, c);

        c.gridy++;
        m_useXPathFilter = new JCheckBox("Use XPath Filter");
        m_useXPathFilter.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                enableXPathComponents(m_useXPathFilter.isSelected());
            }
        });
        p.add(m_useXPathFilter, c);

        JPanel pXPath = new JPanel(new GridBagLayout());
        pXPath.setBorder(BorderFactory.createTitledBorder("XPath Query"));
        GridBagConstraints c1 = new GridBagConstraints();
        c1.fill = GridBagConstraints.HORIZONTAL;
        c1.insets = new Insets(0, 0, 5, 0);
        c1.gridx = 0;
        c1.gridy = 0;
        c1.gridwidth = 1;
        c1.weightx = 1;

        m_xpath = new JTextField();
        pXPath.add(m_xpath, c1);
        c1.gridy++;
        c1.insets = new Insets(0, 0, 0, 0);
        pXPath.add(new JLabel("Does not support XPath completely, "
                + "see node description"), c1);

        c.gridy++;
        p.add(pXPath, c);

        c.gridy++;
        c.weighty = 1;
        m_nsPanel = new KeyValuePanel();
        m_nsPanel.setKeyColumnLabel("Prefix");
        m_nsPanel.setValueColumnLabel("Namespace");
        m_nsPanel.setBorder(BorderFactory.createTitledBorder("Namespaces"));
        p.add(m_nsPanel, c);

        enableXPathComponents(m_useXPathFilter.isSelected());
        return p;
    }

    private void enableXPathComponents(final boolean enable) {
        m_xpath.setEnabled(enable);
        m_nsPanel.setEnabled(enable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        XMLReaderNodeSettings s = new XMLReaderNodeSettings();


        s.setFileURL(m_url.getSelectedFile());
        s.setUseXPathFilter(m_useXPathFilter.isSelected());
        s.setXpath(m_xpath.getText());
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
        XMLReaderNodeSettings s = new XMLReaderNodeSettings();
        s.loadSettingsDialog(settings, null);

        m_url.setSelectedFile(s.getFileURL());
        m_useXPathFilter.setSelected(s.getUseXPathFilter());

        m_xpath.setText(s.getXpath());
        m_nsPanel.setTableData(s.getNsPrefixes(), s.getNamespaces());
        enableXPathComponents(s.getUseXPathFilter());
    }

}
