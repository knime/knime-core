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
 * -------------------------------------------------------------------
 * 
 * History
 *   11.02.2005 (ohl): created
 */
package org.knime.base.node.io.arffreader;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ConvenientComboBoxRenderer;


/**
 * Contains the dialog for the ARFF file reader.
 * 
 * @author Peter Ohl, University of Konstanz
 */
public class ARFFReaderNodeDialog extends NodeDialogPane implements
        ItemListener {

    private static final int VERT_SPACE = 5;

    private static final int HORIZ_SPACE = 5;

    private static final int COMPONENT_HEIGHT = 25;

    private static final int TEXTFIELD_WIDTH = 350;

    private JComboBox m_url;

    private JLabel m_urlError;

    private JTextField m_rowPrefix;

    private static final int TABWIDTH = 600;

    /**
     * Creates a new ARFF file reader dialog.
     */
    public ARFFReaderNodeDialog() {
        super();

        addTab("Specify ARFF file", createFilePanel());

    }

    private JPanel createFilePanel() {
        int sumOfCompHeigth = 0;

        JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.Y_AXIS));
        filePanel.add(Box.createGlue());

        JPanel innerBox = new JPanel();
        innerBox.setLayout(new BoxLayout(innerBox, BoxLayout.Y_AXIS));
        innerBox.add(Box.createVerticalStrut(COMPONENT_HEIGHT + VERT_SPACE));
        sumOfCompHeigth += COMPONENT_HEIGHT + VERT_SPACE;

        JPanel fPanel = new JPanel();
        fPanel.setLayout(new BoxLayout(fPanel, BoxLayout.X_AXIS));
        fPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Enter location of ARFF file:"));
        Container fileBox = Box.createHorizontalBox();
        fileBox.add(Box.createHorizontalStrut(20));
        fileBox.add(new JLabel("valid URL:"));
        fileBox.add(Box.createHorizontalStrut(HORIZ_SPACE));

        sumOfCompHeigth += COMPONENT_HEIGHT;

        m_url = new JComboBox();
        m_url.setMaximumSize(new Dimension(TEXTFIELD_WIDTH, COMPONENT_HEIGHT));
        m_url.setMinimumSize(new Dimension(TEXTFIELD_WIDTH, COMPONENT_HEIGHT));
        m_url
                .setPreferredSize(new Dimension(TEXTFIELD_WIDTH,
                        COMPONENT_HEIGHT));
        m_url.setEditable(true);
        m_url.setRenderer(new ConvenientComboBoxRenderer());
        fileBox.add(m_url);
        fileBox.add(Box.createHorizontalStrut(HORIZ_SPACE));

        JButton browse = new JButton("Browse...");
        browse.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                // sets the path in the file text field.
                popupFileChooser();
            }
        });
        fileBox.add(browse);
        fileBox.add(Box.createHorizontalStrut(VERT_SPACE));
        sumOfCompHeigth += COMPONENT_HEIGHT + VERT_SPACE;

        fPanel.add(fileBox);
        innerBox.add(fPanel);
        innerBox.add(Box.createVerticalStrut(VERT_SPACE));
        sumOfCompHeigth += VERT_SPACE;

        Container errBox = Box.createHorizontalBox();
        errBox.add(Box.createHorizontalGlue());
        // force certain height even if textfield is empty
        errBox.add(Box.createVerticalStrut(COMPONENT_HEIGHT));
        m_urlError = new JLabel("");
        m_urlError.setForeground(Color.RED);
        errBox.add(m_urlError);
        errBox.add(Box.createHorizontalGlue());
        errBox.add(Box.createHorizontalGlue());
        innerBox.add(errBox);
        innerBox.add(Box.createVerticalStrut(VERT_SPACE));
        sumOfCompHeigth += COMPONENT_HEIGHT + VERT_SPACE;

        Container prefixBox = Box.createHorizontalBox();
        JPanel prefixPanel = new JPanel();
        prefixPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Row IDs are build from prefix + "
                + "row number"));
        prefixPanel.setLayout(new BoxLayout(prefixPanel, BoxLayout.X_AXIS));

        prefixBox.add(Box.createHorizontalStrut(20));
        prefixBox.add(new JLabel("RowID prefix:"));
        prefixBox.add(Box.createHorizontalStrut(HORIZ_SPACE));
        m_rowPrefix = new JTextField();
        m_rowPrefix.setMaximumSize(new Dimension(TEXTFIELD_WIDTH / 2,
                COMPONENT_HEIGHT));
        prefixBox.add(m_rowPrefix);

        prefixPanel.add(prefixBox);
        innerBox.add(prefixPanel);
        innerBox.add(Box.createVerticalStrut(VERT_SPACE));
        sumOfCompHeigth += COMPONENT_HEIGHT + (2 * VERT_SPACE);

        innerBox.setMaximumSize(new Dimension(TABWIDTH, sumOfCompHeigth));

        filePanel.add(innerBox);
        filePanel.add(Box.createGlue());

        // set a filter to the filename that fills the error text label
        Component editor = m_url.getEditor().getEditorComponent();
        if (editor instanceof JTextComponent) {
            Document d = ((JTextComponent)editor).getDocument();
            d.addDocumentListener(new DocumentListener() {
                public void changedUpdate(final DocumentEvent e) {
                    updateFileError();
                }

                public void insertUpdate(final DocumentEvent e) {
                    updateFileError();
                }

                public void removeUpdate(final DocumentEvent e) {
                    updateFileError();
                }
            });
        }
        // itemListener must be this in order to be able to remove and reset it.
        m_url.addItemListener(this);
        m_url.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
                updateFileError();
            }
        });

        return filePanel;
    }

    /**
     * {@inheritDoc}
     */
    public void itemStateChanged(final ItemEvent e) {
        updateFileError();
    }

    /**
     * Updates the file error label whenever the file URL entered changed.
     */
    protected void updateFileError() {
        String urlInput = m_url.getEditor().getItem().toString();
        URL urli = null;
        String text = null;

        if ((urlInput == null) || urlInput.equals("")) {
            text = "";
            m_urlError.setText("Specify valid file location");
            return;
        }

        try {
            urli = ARFFReaderNodeModel.stringToURL(urlInput);
            if (urli != null) {
                try {
                    InputStream is = urli.openStream();
                    if (is != null) {
                        text = null;
                    } else {
                        text = "Can't open: " + urli.toString();
                    }
                } catch (IOException ioe) {
                    text = "Can't open: " + urli.toString();
                }
            } else {
                text = "Invalid URL";
            }
        } catch (Exception e) {
            text = "Invalid file location";
        }

        if (text == null) {
            m_urlError.setText("");
        } else {
            m_urlError.setText(text);
            m_urlError.setVisible(true);
        }

    }

    /**
     * Pops up the file selection dialog and sets the selected file path into
     * the url combox box.
     */
    protected void popupFileChooser() {
        // before opening the dialog, try extracting the file that might
        // be specified already:
        String startingDir = "";
        try {
            URL newURL = ARFFReaderNodeModel.stringToURL(m_url.getEditor()
                    .getItem().toString());
            if (newURL.getProtocol().equals("file")) {
                File tmpFile = new File(newURL.getPath());
                startingDir = tmpFile.getAbsolutePath();
            }
        } catch (Exception e) {
            // no valid path - start in the default dir of the file chooser
        }

        JFileChooser chooser;
        chooser = new JFileChooser(startingDir);
        chooser.setFileFilter(new ARFFReaderNodeModel.ARFFFileFilter());
        int returnVal = chooser.showOpenDialog(getPanel().getParent());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String path;
            try {
                path = chooser.getSelectedFile().getAbsoluteFile()
                        .toURI().toURL().toString();
            } catch (Exception e) {
                path = "<Error: Couldn't create URL for file>";
            }
            m_url.setSelectedItem(path);
            updateFileError();
        }
        // user canceled - do nothing.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        
        // set the file history for the combo box.
        // disconnect the ItemChangelistener first
        m_url.removeItemListener(this);
        m_url.removeAllItems();
        for (String str : ARFFReaderNodeModel.getFileHistory(
                /* removeNotExistingFiles */true)) {
            m_url.addItem(str);
        }
        m_url.addItemListener(this);

        m_url.setSelectedItem(settings.getString(
                ARFFReaderNodeModel.CFGKEY_FILEURL, ""));
        m_rowPrefix.setText(settings.getString(
                ARFFReaderNodeModel.CFGKEY_ROWPREFIX, ""));

        updateFileError();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        updateFileError();
        if (!m_urlError.getText().equals("")) {
            throw new InvalidSettingsException("Specify valid file location. "
                    + "Or press 'Cancel'.");
        }
        settings.addString(ARFFReaderNodeModel.CFGKEY_FILEURL, m_url
                .getEditor().getItem().toString());
        settings.addString(ARFFReaderNodeModel.CFGKEY_ROWPREFIX, m_rowPrefix
                .getText());
    }
}
