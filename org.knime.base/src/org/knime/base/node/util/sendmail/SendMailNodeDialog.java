/*
 * ------------------------------------------------------------------------
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
 * Created on Aug 22, 2013 by wiswedel
 */
package org.knime.base.node.util.sendmail;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

import org.knime.base.node.util.FlowVariableResolvable.FlowVariableResolver;
import org.knime.base.node.util.sendmail.SendMailConfiguration.ConnectionSecurity;
import org.knime.base.node.util.sendmail.SendMailConfiguration.EMailFormat;
import org.knime.base.node.util.sendmail.SendMailConfiguration.EMailPriority;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.util.MultipleURLList;
import org.knime.core.node.util.StringHistoryPanel;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Plain dialog with smtp host name field, address fields, subject and content field.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class SendMailNodeDialog extends NodeDialogPane {

    private final StringHistoryPanel m_smtpHostPanel;
    private final StringHistoryPanel m_smtpPortPanel;
    private final JCheckBox m_useAuthenticationChecker;
    private final JCheckBox m_useCredentialsChecker;
    private final JComboBox m_credentialsCombo;
    private final StringHistoryPanel m_smtpUserPanel;
    private final JPasswordField m_smtpPasswordField;
    private final JComboBox m_connectionSecurityCombo;
    private final JComboBox m_connectionPriorityCombo;
    private final StringHistoryPanel m_fromPanel;
    private final StringHistoryPanel m_toPanel;
    private final StringHistoryPanel m_ccPanel;
    private final StringHistoryPanel m_bccPanel;
    private final StringHistoryPanel m_subjectPanel;
    private final JList m_flowVarList;
    private final JTextArea m_textArea;
    private final JRadioButton m_formatTextButton;
    private final JRadioButton m_formatHTMLButton;
    private final MultipleURLList m_attachmentList;

    /** Inits GUI. */
    public SendMailNodeDialog() {
        m_smtpHostPanel = new StringHistoryPanel(SendMailConfiguration.getSmtpHostStringHistoryID());
        m_smtpPortPanel = new StringHistoryPanel(SendMailConfiguration.getSmtpPortStringHistoryID());
        m_useAuthenticationChecker = new JCheckBox("SMTP host needs authentication");
        m_useCredentialsChecker = new JCheckBox("");
        m_credentialsCombo = new JComboBox(new DefaultComboBoxModel());
        m_smtpUserPanel = new StringHistoryPanel(SendMailConfiguration.getSmtpUserStringHistoryID());
        m_smtpPasswordField = new JPasswordField();
        m_connectionSecurityCombo = new JComboBox(ConnectionSecurity.values());
        m_connectionPriorityCombo = new JComboBox(EMailPriority.values());
        m_fromPanel = new StringHistoryPanel(SendMailConfiguration.getFromStringHistoryID());
        m_toPanel = new StringHistoryPanel(SendMailConfiguration.getToStringHistoryID());
        m_ccPanel = new StringHistoryPanel(SendMailConfiguration.getToStringHistoryID());
        m_bccPanel = new StringHistoryPanel(SendMailConfiguration.getToStringHistoryID());
        m_subjectPanel = new StringHistoryPanel(SendMailConfiguration.getSubjectStringHistoryID());
        final ItemListener itemListener = new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                updateAuthenticationFieldEnablement();
            }
        };
        m_useAuthenticationChecker.addItemListener(itemListener);
        m_useCredentialsChecker.addItemListener(itemListener);
        updateAuthenticationFieldEnablement();
        ButtonGroup bg = new ButtonGroup();
        m_formatTextButton = new JRadioButton("Text");
        m_formatHTMLButton = new JRadioButton("HTML");
        bg.add(m_formatTextButton);
        bg.add(m_formatHTMLButton);
        m_formatTextButton.doClick();
        m_textArea = new JTextArea(10, 30);
        m_flowVarList = new JList(new DefaultListModel());
        m_flowVarList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_flowVarList.setCellRenderer(new FlowVariableListCellRenderer());
        m_flowVarList.addMouseListener(new MouseAdapter() {
            /** {@inheritDoc} */
            @Override
            public final void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    FlowVariable o = (FlowVariable)m_flowVarList.getSelectedValue();
                    if (o != null) {
                        m_textArea.replaceSelection(FlowVariableResolver.getPlaceHolderForVariable(o));
                        m_flowVarList.clearSelection();
                        m_textArea.requestFocus();
                    }
                }
            }
        });
        m_attachmentList = new MultipleURLList(
            SendMailConfiguration.getAttachmentListStringHistoryID(), true, new String[0]);
        addTab("Mail", initMailLayout());
        addTab("Attachments", initAttachmentLayout());
        addTab("Mail Host (SMTP)", initSMTPLayout());
    }

    private void updateAuthenticationFieldEnablement() {
        final boolean useAuthentication = m_useAuthenticationChecker.isSelected();
        final boolean useCredentials = m_useCredentialsChecker.isSelected();
        m_useCredentialsChecker.setEnabled(useAuthentication);
        m_credentialsCombo.setEnabled(useAuthentication && useCredentials);
        m_smtpUserPanel.setEnabled(useAuthentication && !useCredentials);
        m_smtpPasswordField.setEnabled(useAuthentication && !useCredentials);
    }

    private JPanel initMailLayout() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        p.add(new JLabel("To:"), gbc);
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        p.add(m_toPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.weightx = 0.0;
        p.add(new JLabel("CC:"), gbc);
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        p.add(m_ccPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.weightx = 0.0;
        p.add(new JLabel("BCC:"), gbc);
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        p.add(m_bccPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.gridwidth = 2;
        p.add(new JLabel(" "), gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        p.add(new JLabel("Subject:"), gbc);
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        p.add(m_subjectPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        p.add(createEMailTextPanel(), gbc);
        return p;
    }

    private JPanel initSMTPLayout() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        p.add(new JLabel("SMTP Host"), gbc);
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        p.add(m_smtpHostPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.weightx = 0.0;
        p.add(new JLabel("SMTP Port"), gbc);
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        p.add(m_smtpPortPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.weightx = 0.0;
        p.add(new JLabel("FROM (your email)"), gbc);
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        p.add(m_fromPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        p.add(new JSeparator(), gbc);

        gbc.gridy += 1;
        p.add(m_useAuthenticationChecker, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 5, 5, 5);
        gbc.weightx = 1.0;
        p.add(createAuthenticationPanel(), gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        p.add(new JLabel("Connection Security"), gbc);
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        p.add(m_connectionSecurityCombo, gbc);

        return p;
    }

    private JPanel initAttachmentLayout() {
        JPanel p = new JPanel(new BorderLayout());
        p.add(m_attachmentList);
        return p;
    }

    private JPanel createAuthenticationPanel() {
        // stolen from DBDialogPane
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));
// create and add credential box
        final JPanel credPanel = new JPanel(new BorderLayout());
        credPanel.setBorder(BorderFactory.createTitledBorder(" Workflow Credentials "));
        credPanel.add(m_useCredentialsChecker, BorderLayout.WEST);
        credPanel.add(m_credentialsCombo, BorderLayout.CENTER);
        result.add(credPanel);

// create and user name field
        final JPanel userPanel = new JPanel(new BorderLayout());
        userPanel.setBorder(BorderFactory.createTitledBorder(" User Name "));
        userPanel.add(m_smtpUserPanel, BorderLayout.CENTER);
        result.add(userPanel);

// create and add password panel
        final JPanel passPanel = new JPanel(new BorderLayout());
        passPanel.setBorder(BorderFactory.createTitledBorder(" Password "));
        passPanel.add(m_smtpPasswordField, BorderLayout.CENTER);
        result.add(passPanel);
        return result;
    }

    private JPanel createEMailTextPanel() {
        JPanel p = new JPanel(new BorderLayout());
        JSplitPane splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitter.setRightComponent(new JScrollPane(m_textArea));
        JScrollPane flowScroller = new JScrollPane(m_flowVarList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                                                   ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        splitter.setLeftComponent(flowScroller);
        splitter.setResizeWeight(0.3);
        splitter.setDividerLocation(0.3);
        p.add(splitter, BorderLayout.CENTER);
        p.add(ViewUtils.getInFlowLayout(FlowLayout.RIGHT, new JLabel("Priority: "), m_connectionPriorityCombo,
            new JLabel("   "), m_formatTextButton, m_formatHTMLButton), BorderLayout.SOUTH);
        return p;
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) {
        SendMailConfiguration config = new SendMailConfiguration();
        config.loadConfigurationInDialog(settings);
        setValueInStringHistoryPanel(m_smtpHostPanel, config.getSmtpHost());
        setValueInStringHistoryPanel(m_smtpPortPanel, Integer.toString(config.getSmtpPort()));
        if (m_useAuthenticationChecker.isSelected() != config.isUseAuthentication()) {
            m_useAuthenticationChecker.doClick();
        }
        if (m_useCredentialsChecker.isSelected() != config.isUseCredentials()) {
            m_useCredentialsChecker.doClick();
        }
        DefaultComboBoxModel model = (DefaultComboBoxModel)m_credentialsCombo.getModel();
        model.removeAllElements();
        for (String s : getCredentialsNames()) {
            model.addElement(s);
        }
        m_credentialsCombo.setSelectedItem(config.getCredentialsId());
        setValueInStringHistoryPanel(m_smtpUserPanel, config.getSmtpUser());
        m_smtpPasswordField.setText(config.getSmtpPassword());
        m_connectionSecurityCombo.setSelectedItem(config.getConnectionSecurity());
        m_connectionPriorityCombo.setSelectedItem(config.getPriority());
        setValueInStringHistoryPanel(m_fromPanel, config.getFrom());
        setValueInStringHistoryPanel(m_toPanel, config.getTo());
        setValueInStringHistoryPanel(m_ccPanel, config.getCc());
        setValueInStringHistoryPanel(m_bccPanel, config.getBcc());
        setValueInStringHistoryPanel(m_subjectPanel, config.getSubject());
        DefaultListModel listModel = (DefaultListModel)m_flowVarList.getModel();
        listModel.removeAllElements();
        for (FlowVariable e : getAvailableFlowVariables().values()) {
            listModel.addElement(e);
        }
        m_textArea.setText(config.getText());
        switch (config.getFormat()) {
            case Html: m_formatHTMLButton.doClick(); break;
            case Text: m_formatTextButton.doClick(); break;
            default: throw new RuntimeException("Unsupported format");
        }
        URL[] attachedURLs = config.getAttachedURLs();
        m_attachmentList.setSelectedURLs(Arrays.asList(attachedURLs));
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        SendMailConfiguration config = new SendMailConfiguration();
        config.setSmtpHost(m_smtpHostPanel.getSelectedString());
        final String portS = m_smtpPortPanel.getSelectedString();
        int port;
        try {
            port = Integer.parseInt(portS);
        } catch (Exception e) {
            throw new InvalidSettingsException("Can't parse port (no integer): " + portS);
        }
        config.setSmtpPort(port);
        config.setUseAuthentication(m_useAuthenticationChecker.isSelected());
        config.setUseCredentials(m_useCredentialsChecker.isSelected());
        final Object selectedCredentialsObject = m_credentialsCombo.getSelectedItem();
        config.setCredentialsId(selectedCredentialsObject == null ? "" : selectedCredentialsObject.toString());
        config.setSmtpUser(m_smtpUserPanel.getSelectedString());
        config.setSmtpPassword(new String(m_smtpPasswordField.getPassword()));
        config.setConnectionSecurity((ConnectionSecurity)m_connectionSecurityCombo.getSelectedItem());
        config.setPriority((EMailPriority)m_connectionPriorityCombo.getSelectedItem());
        config.setFrom(m_fromPanel.getSelectedString());
        config.setTo(m_toPanel.getSelectedString());
        config.setCc(m_ccPanel.getSelectedString());
        config.setBcc(m_bccPanel.getSelectedString());
        config.setSubject(m_subjectPanel.getSelectedString());
        config.setText(m_textArea.getText());
        config.setFormat(m_formatHTMLButton.isSelected() ? EMailFormat.Html : EMailFormat.Text);
        List<URL> urls = new ArrayList<URL>();
        for (String url : m_attachmentList.getSelectedURLs()) {
            try {
                urls.add(MultipleURLList.convertToUrl(url));
            } catch (MalformedURLException ex) {
                throw new InvalidSettingsException("Malformed URL or non-existing file: " + url);
            }
        }
        config.setAttachedURLs(urls.toArray(new URL[urls.size()]));
        config.saveConfiguration(settings);
        m_smtpHostPanel.commitSelectedToHistory();
        m_smtpPortPanel.commitSelectedToHistory();
        m_smtpUserPanel.commitSelectedToHistory();
        m_fromPanel.commitSelectedToHistory();
        m_toPanel.commitSelectedToHistory();
        m_ccPanel.commitSelectedToHistory();
        m_bccPanel.commitSelectedToHistory();
        m_subjectPanel.commitSelectedToHistory();
    }

    private static void setValueInStringHistoryPanel(final StringHistoryPanel panel, final String value) {
        panel.updateHistory();
        panel.setSelectedString(value);
    }

}
