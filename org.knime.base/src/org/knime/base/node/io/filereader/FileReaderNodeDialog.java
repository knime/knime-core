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
 *   23.03.2005 (ohl): created
 */
package org.knime.base.node.io.filereader;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.tableview.TableView;
import org.knime.core.node.util.ConvenientComboBoxRenderer;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.NodeProgressEvent;
import org.knime.core.node.workflow.NodeProgressListener;
import org.knime.core.util.FileReaderFileFilter;
import org.knime.core.util.MutableBoolean;
import org.knime.core.util.tokenizer.Comment;
import org.knime.core.util.tokenizer.Delimiter;
import org.knime.core.util.tokenizer.SettingsStatus;
import org.knime.core.util.tokenizer.TokenizerException;
import org.knime.core.util.tokenizer.TokenizerSettings;

/**
 *
 * @author Peter Ohl, University of Konstanz
 *
 *         Implements the {@link java.awt.event.ItemListener} for the file
 *         location ComboBox (because we need to remove it and add it again from
 *         time to time.
 */
class FileReaderNodeDialog extends NodeDialogPane implements ItemListener {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(FileReaderNodeDialog.class);

    private static final int HORIZ_SPACE = 10;

    private static final int COMP_HEIGHT = 30;

    private static final int PANEL_WIDTH = 5000;

    private static final Delimiter[] DEFAULT_DELIMS = new Delimiter[]{
            // the <none> MUST be the first one (index zero!)!!!
            new Delimiter("<none>", false, false, false),
            new Delimiter(",", false, false, false),
            new Delimiter(" ", true, false, false),
            new Delimiter("\t", false, false, false),
            new Delimiter(";", false, false, false)};

    /*
     * the settings object holding the current state of all settings. The
     * components immediately write their state into this object. There is a
     * load function transferring the settings from this object into the
     * component.
     */
    private FileReaderNodeSettings m_frSettings;

    private JComboBox m_urlCombo;

    private TableView m_previewTableView;

    private JCheckBox m_hasRowHeaders;

    private JCheckBox m_hasColHeaders;

    private JComboBox m_delimField;

    /*
     * to determine whether the entered delimiter was applied we store the last
     * delim (problem: we don't get focus lost events (or similar events).
     */
    private String m_delimApplied;

    private JCheckBox m_cStyleComment;

    private JTextField m_singleLineComment;

    /*
     * the properties of the first column in case people are (un)checking the
     * 'fileHasRowHeaders' box, we save it here.
     */
    private ColProperty m_firstColProp;

    /* flag to break recursion */
    private boolean m_insideLoadDelim;

    private boolean m_insideDelimChange;

    private boolean m_insideLoadComment;

    private boolean m_insideCommentChange;

    private boolean m_insideWSChange;

    private boolean m_insideLoadWS;

    private boolean m_insideRowHdrChange;

    private boolean m_insideLoadRowHdr;

    private boolean m_insideColHdrChange;

    private boolean m_insideLoadColHdr;

    private JPanel m_dialogPanel;

    private JCheckBox m_ignoreWS;

    // the dialog stores the previous WSs, because they can be deleted with
    // only one click - boom gone.
    private Vector<String> m_prevWhiteSpaces;

    private JLabel m_errorLabel;

    private JLabel m_errorDetail;

    private JLabel m_analyzeWarn;

    private JPanel m_previewPanel;

    private JPanel m_analysisPanel;

    private JPanel m_previewArea;

    private FileReaderPreviewTable m_previewTable;

    private JButton m_analyzeCancel;

    private final JLabel m_analyzeProgressMsg = new JLabel("");

    private final MutableBoolean m_analysisRunning = new MutableBoolean(false);

    private FileReaderExecutionMonitor m_analysisExecMonitor;

    private JProgressBar m_analyzeProgressBar;

    private JCheckBox m_preserveSettings;

    /**
     * Creates a new file reader dialog pane.
     */
    FileReaderNodeDialog() {
        super();
        m_frSettings = new FileReaderNodeSettings();
        m_insideLoadDelim = false;
        m_insideDelimChange = false;
        m_insideLoadComment = false;
        m_insideCommentChange = false;
        m_insideLoadColHdr = false;
        m_insideColHdrChange = false;
        m_insideLoadRowHdr = false;
        m_insideRowHdrChange = false;
        m_analysisExecMonitor = null;

        m_prevWhiteSpaces = null;

        m_dialogPanel = new JPanel();
        m_dialogPanel.setLayout(new BoxLayout(m_dialogPanel, BoxLayout.Y_AXIS));

        m_dialogPanel.add(Box.createVerticalGlue());

        m_dialogPanel.add(createFileNamePanel());
        m_dialogPanel.add(createSettingsPanel());
        m_previewArea = createPreviewArea();
        m_dialogPanel.add(m_previewArea);
        m_dialogPanel.add(Box.createVerticalGlue());
        super.addTab("Settings", m_dialogPanel);
    }

    private JPanel createFileNamePanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Enter ASCII data file location: (press 'Enter' to update "
                        + "preview)"));

        Box fileBox = Box.createHorizontalBox();

        // Creating the brows button here in order to get its preferred height
        JButton browse = new JButton("Browse...");
        int buttonHeight = browse.getPreferredSize().height;

        m_urlCombo = new JComboBox();
        m_urlCombo.setEditable(true);
        m_urlCombo.setRenderer(new ConvenientComboBoxRenderer());
        m_urlCombo.setMaximumSize(new Dimension(PANEL_WIDTH, buttonHeight));
        m_urlCombo.setMinimumSize(new Dimension(350, buttonHeight));
        m_urlCombo.setPreferredSize(new Dimension(350, buttonHeight));
        m_urlCombo.setToolTipText("Enter an URL of an ASCII data"
                + "file, select from recent files, or browse");

        fileBox.add(Box.createHorizontalGlue());
        fileBox.add(new JLabel("valid URL:"));
        fileBox.add(Box.createHorizontalStrut(HORIZ_SPACE));
        fileBox.add(m_urlCombo);
        fileBox.add(Box.createHorizontalStrut(HORIZ_SPACE));
        fileBox.add(browse);
        fileBox.add(Box.createHorizontalStrut(HORIZ_SPACE));
        fileBox.add(Box.createVerticalStrut(50));
        fileBox.add(Box.createHorizontalGlue());

        // the checkbox for preserving the current settings on file change
        Box preserveBox = Box.createHorizontalBox();
        m_preserveSettings =
                new JCheckBox("Preserve user settings for new location");
        m_preserveSettings.setToolTipText("if not checked, the settings you"
                + " have set are reset, if a new location is entered");
        m_preserveSettings.setSelected(false);
        m_preserveSettings.setEnabled(true);
        m_preserveSettings.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                m_frSettings.setPreserveSettings(m_preserveSettings
                        .isSelected());
            }
        });
        preserveBox.add(Box.createHorizontalGlue());
        preserveBox.add(m_preserveSettings);
        preserveBox.add(Box.createHorizontalGlue());

        panel.add(fileBox);
        panel.add(preserveBox);
        panel.setMaximumSize(new Dimension(PANEL_WIDTH, 70));
        panel.setMinimumSize(new Dimension(PANEL_WIDTH, 70));

        m_urlCombo.addItemListener(this);

        /* install action listeners */
        // set stuff to update preview when file location changes
        m_urlCombo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
                fileLocationChanged();
            }
        });
        Component editor = m_urlCombo.getEditor().getEditorComponent();
        if (editor instanceof JTextComponent) {
            Document d = ((JTextComponent)editor).getDocument();
            d.addDocumentListener(new DocumentListener() {
                public void changedUpdate(final DocumentEvent e) {
                    setPreviewTable(null);
                }

                public void insertUpdate(final DocumentEvent e) {
                    setPreviewTable(null);
                }

                public void removeUpdate(final DocumentEvent e) {
                    setPreviewTable(null);
                }
            });
        }

        browse.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                // sets the path in the file text field.
                String newFile =
                        popupFileChooser(m_urlCombo.getEditor().getItem()
                                .toString(), false);
                if (newFile != null) {
                    m_urlCombo.setSelectedItem(newFile);
                    // fileLocationChanged();
                }
            }
        });
        return panel;
    }

    /*
     * Stores the new location in the settings object, sets default settings,
     * and starts analysis if needed.
     */
    private void fileLocationChanged() {

        boolean fileChanged = false;

        try {
            fileChanged = takeOverNewFileLocation();

            if (fileChanged && !m_frSettings.getPreserveSettings()) {
                resetSettings();
            }

        } catch (final InvalidSettingsException e) {
            // clear the URL in the settings
            m_frSettings.setDataFileLocationAndUpdateTableName(null);

            setErrorLabelText("Malformed URL '"
                    + m_urlCombo.getEditor().getItem() + "'.");
            setPreviewTable(null);
        }

        // also "analyze" an invalid file (hides "analyze" buttons)
        analyzeDataFileAndUpdatePreview(fileChanged);
    }

    /**
     * This dialog implements the {@link ItemListener} for the file selection
     * combo box. This way we can remove it when we load the file history in the
     * drop down list (because this triggers a useless event), and add it
     * afterwards again.
     *
     * @param e the event
     * @see java.awt.event.ItemListener
     *      #itemStateChanged(java.awt.event.ItemEvent)
     */
    public void itemStateChanged(final ItemEvent e) {
        if ((e.getSource() == m_urlCombo)
                && (e.getStateChange() == ItemEvent.SELECTED)) {
            fileLocationChanged();
        }
    }

    /*
     * The preview area contains either the preview panel or the analysis panel
     */
    private JPanel createPreviewArea() {

        // the panel for the preview table
        m_previewPanel = createPreviewPanel();
        // the panel for the analyze button and stuff
        m_analysisPanel = createAnalysisPanel();

        JPanel result = new JPanel();
        result.setLayout(new BorderLayout());
        result.add(m_previewPanel, BorderLayout.CENTER);
        return result;
    }

    private JPanel createAnalysisPanel() {

        m_analyzeCancel = new JButton("Quick Scan");
        m_analyzeCancel.setToolTipText("Analyze the first "
                + FileAnalyzer.NUMOFLINES + " lines only.");
        m_analyzeCancel.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_analyzeCancel.setEnabled(false);
                m_analyzeCancel.setText("Scanning quickly");
                m_analysisExecMonitor.setExecuteCanceled();
            }
        });
        m_analyzeCancel.setEnabled(false);

        m_analyzeProgressBar = new JProgressBar();
        m_analyzeProgressBar.setIndeterminate(false);
        m_analyzeProgressBar.setStringPainted(false);
        m_analyzeProgressBar.setValue(0);

        Box msgBox = Box.createHorizontalBox();
        msgBox.add(Box.createVerticalStrut(25));
        msgBox.add(m_analyzeProgressMsg);
        msgBox.add(Box.createGlue());

        Box progressBox = Box.createVerticalBox();
        progressBox.add(msgBox);
        progressBox.add(Box.createVerticalStrut(3));
        progressBox.add(m_analyzeProgressBar);

        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(m_analyzeCancel);
        buttonBox.add(Box.createHorizontalGlue());

        Box allBox = Box.createVerticalBox();
        allBox.add(progressBox);
        allBox.add(Box.createVerticalStrut(5));
        allBox.add(buttonBox);

        Box hBox = Box.createHorizontalBox();
        hBox.add(Box.createGlue());
        hBox.add(Box.createGlue());
        hBox.add(allBox);
        hBox.add(Box.createGlue());
        hBox.add(Box.createGlue());

        JPanel result = new JPanel();
        result.setLayout(new BorderLayout());
        result.add(hBox, BorderLayout.NORTH);
        return result;
    }

    private JPanel createPreviewPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Preview"));

        Box hintBox = Box.createHorizontalBox();
        Box tableBox = Box.createHorizontalBox();

        hintBox.add(Box.createGlue());
        hintBox.add(new JLabel("Click column header to change "
                + "column properties (* = name/type user settings)"));
        hintBox.add(Box.createGlue());

        PreviewTableContentView ptcv = new PreviewTableContentView();
        m_previewTableView = new TableView(ptcv);

        tableBox.add(m_previewTableView);

        // add the analyzer warning at the bottom
        m_analyzeWarn = new JLabel("");
        m_analyzeWarn.setForeground(Color.red);
        JPanel analBox = new JPanel();
        analBox.setLayout(new BoxLayout(analBox, BoxLayout.X_AXIS));
        analBox.add(Box.createHorizontalGlue());
        analBox.add(m_analyzeWarn);
        // reserve a certain height for the (in the beginning invisible) label
        analBox.add(Box.createVerticalStrut(25));
        analBox.add(Box.createHorizontalGlue());

        panel.add(Box.createGlue());
        panel.add(hintBox);
        panel.add(Box.createVerticalStrut(10));
        panel.add(tableBox);
        panel.add(analBox);
        panel.add(Box.createGlue());

        // this is the callback for the preview table header click
        ptcv.addPropertyChangeListener(
                PreviewTableContentView.PROPERTY_SPEC_CHANGED,
                new PropertyChangeListener() {
                    public void propertyChange(final PropertyChangeEvent evt) {
                        // thats the col idx the mouse was clicked on
                        Integer colNr = (Integer)evt.getNewValue();
                        setNewUserSettingsForColumn(colNr.intValue());
                    }
                });

        return panel;

    }

    private JPanel createSettingsPanel() {
        JButton advanced = new JButton("Advanced...");
        int buttonHeight = advanced.getPreferredSize().height;
        m_hasRowHeaders = new JCheckBox("read row IDs");
        m_hasRowHeaders.setToolTipText("Check if the file contains row IDs"
                + " in the first column");
        m_hasColHeaders = new JCheckBox("read column headers");
        m_hasColHeaders.setToolTipText("Check if the file contains column"
                + " headers in the first line");
        JLabel deliLabel = new JLabel("Column delimiter:");
        m_delimField = new JComboBox();
        m_delimField.setMaximumSize(new Dimension(70, buttonHeight));
        m_delimField.setMinimumSize(new Dimension(70, buttonHeight));
        m_delimField.setPreferredSize(new Dimension(70, buttonHeight));
        m_delimField.setEditable(true);
        Delimiter[] selDelims = DEFAULT_DELIMS;
        m_delimField.setModel(new DefaultComboBoxModel(selDelims));
        deliLabel.setToolTipText("Specify the data delimiter character(s)");
        m_delimField.setToolTipText("Specify the data delimiter character(s)");
        m_cStyleComment = new JCheckBox("Java-style comments");
        m_cStyleComment.setToolTipText("Check to add support for '//' and "
                + "\"'/*' and '*/'\" comment");
        m_singleLineComment = new JTextField(2);
        m_singleLineComment.setMaximumSize(new Dimension(55, buttonHeight));
        m_singleLineComment.setMinimumSize(new Dimension(55, buttonHeight));
        m_singleLineComment.setPreferredSize(new Dimension(55, buttonHeight));
        JLabel commentLabel = new JLabel("Single line comment:");
        m_ignoreWS = new JCheckBox("ignore spaces and tabs");
        m_ignoreWS.setToolTipText("If checked, whitespaces (spaces and tabs)"
                + " will be discarded (if not quoted)");
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 3));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Basic Settings"));
        // top row
        Box rowBox = Box.createHorizontalBox();
        rowBox.add(m_hasRowHeaders);
        rowBox.add(Box.createGlue());
        Box delimBox = Box.createHorizontalBox();
        delimBox.add(Box.createHorizontalStrut(4));
        delimBox.add(deliLabel);
        delimBox.add(Box.createHorizontalStrut(3));
        delimBox.add(m_delimField);
        delimBox.add(Box.createGlue());
        Box advBox = Box.createHorizontalBox();
        advBox.add(Box.createGlue());
        advBox.add(advanced);
        advBox.add(Box.createGlue());
        // middle row
        Box colBox = Box.createHorizontalBox();
        colBox.add(m_hasColHeaders);
        colBox.add(Box.createGlue());
        Box wsBox = Box.createHorizontalBox();
        wsBox.add(m_ignoreWS);
        wsBox.add(Box.createGlue());
        // bottom row
        Box pValBox = Box.createHorizontalBox();
        pValBox.add(new JLabel("")); // placeholder
        pValBox.add(Box.createGlue());
        Box cCmtBox = Box.createHorizontalBox();
        cCmtBox.add(m_cStyleComment);
        cCmtBox.add(Box.createGlue());
        Box slcBox = Box.createHorizontalBox();
        slcBox.add(commentLabel);
        slcBox.add(Box.createHorizontalStrut(3));
        slcBox.add(m_singleLineComment);
        slcBox.add(Box.createGlue());
        // now fill the grid: first row
        panel.add(rowBox);
        panel.add(delimBox);
        panel.add(advBox);
        // second row
        panel.add(colBox);
        panel.add(wsBox);
        panel.add(new JLabel(""));
        // third row
        panel.add(pValBox);
        panel.add(cCmtBox);
        panel.add(slcBox);
        int componentsHeight = (2 * COMP_HEIGHT) + 30 + buttonHeight;
        panel.setMaximumSize(new Dimension(PANEL_WIDTH, componentsHeight));
        advanced.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                advancedSettings();
            }
        });
        m_hasRowHeaders.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                rowHeadersSettingsChanged();
            }
        });
        m_hasColHeaders.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                colHeadersSettingsChanged();
            }
        });
        m_cStyleComment.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                commentSettingsChanged();
            }
        });
        m_delimField.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                delimSettingsChanged();
            }
        });

        m_ignoreWS.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                ignoreWSChanged();
            }
        });
        m_singleLineComment.getDocument().addDocumentListener(
                new DocumentListener() {
                    public void changedUpdate(final DocumentEvent e) {
                        commentSettingsChanged();
                    }

                    public void insertUpdate(final DocumentEvent e) {
                        commentSettingsChanged();
                    }

                    public void removeUpdate(final DocumentEvent e) {
                        commentSettingsChanged();
                    }
                });
        // add a panel for the errors:
        m_errorLabel = new JLabel("");
        m_errorLabel.setForeground(Color.red);
        m_errorDetail = new JLabel("");
        m_errorDetail.setForeground(Color.red);
        JPanel errorBox = new JPanel();
        errorBox.setLayout(new BoxLayout(errorBox, BoxLayout.X_AXIS));
        errorBox.add(Box.createHorizontalGlue());
        errorBox.add(m_errorLabel);
        // reserve a certain height for the (in the beginning invisible) label
        errorBox.add(Box.createVerticalStrut(17));
        errorBox.add(Box.createHorizontalGlue());
        JPanel detailBox = new JPanel();
        detailBox.setLayout(new BoxLayout(detailBox, BoxLayout.X_AXIS));
        detailBox.add(Box.createHorizontalGlue());
        detailBox.add(m_errorDetail);
        // reserve a certain height for the (in the beginning invisible) label
        detailBox.add(Box.createVerticalStrut(17));
        detailBox.add(Box.createHorizontalGlue());
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));
        result.add(panel);
        result.add(errorBox);
        result.add(detailBox);
        return result;
    }

    private void loadRowHdrSettings() {
        if (m_insideRowHdrChange) {
            return;
        }

        m_insideLoadRowHdr = true;

        m_hasRowHeaders.setSelected(m_frSettings.getFileHasRowHeaders());

        m_insideLoadRowHdr = false;
    }

    /**
     * Reads the settings of the 'fileHasRowHeaders' checkbox and transfers them
     * into the internal settings object.
     */
    protected void rowHeadersSettingsChanged() {

        if (m_insideLoadRowHdr) {
            return;
        }

        m_insideRowHdrChange = true;

        m_frSettings.setFileHasRowHeadersUserSet(true);

        if (m_frSettings.getFileHasRowHeaders()
                && !m_hasRowHeaders.isSelected()) {
            // settings changed to not reading row headers from file.
            // that adds one column to the table
            m_frSettings.setFileHasRowHeaders(false);
            m_frSettings
                    .setNumberOfColumns(m_frSettings.getNumberOfColumns() + 1);
            // we must create a new colProperty for it - if not already created
            if (m_firstColProp == null) {
                DataColumnSpec firstColSpec =
                        new DataColumnSpecCreator("Col0", StringCell.TYPE)
                                .createSpec();
                m_firstColProp = new ColProperty();
                m_firstColProp.setColumnSpec(firstColSpec);
                // this will cause it to be ignored when re-analyzing:
                m_firstColProp.setUserSettings(false);
            }

            Vector<ColProperty> colProps = m_frSettings.getColumnProperties();
            colProps.add(0, m_firstColProp);
            m_frSettings.setColumnProperties(colProps);
            if (!m_frSettings.getFileHasColumnHeaders()) {
                // re-generate the column names - and 'fix' the first column
                // if it's a duplicate - even if it's set by the user.
                recreateColNames(true);
            }
            analyzeDataFileAndUpdatePreview(true);
        } else if (!m_frSettings.getFileHasRowHeaders()
                && m_hasRowHeaders.isSelected()) {
            // somebody checked the hasRowheader box - that removes one column
            m_frSettings.setFileHasRowHeaders(true);
            if (m_frSettings.getNumberOfColumns() > 0) {
                m_frSettings.setNumberOfColumns(m_frSettings
                        .getNumberOfColumns() - 1);
                Vector<ColProperty> colProps =
                        m_frSettings.getColumnProperties();
                // save the first colProp in case user changes his mind...
                m_firstColProp = colProps.remove(0);
                m_frSettings.setColumnProperties(colProps);
                if (!m_frSettings.getFileHasColumnHeaders()) {
                    // re-generate the column names
                    recreateColNames(false);
                }
            }
            analyzeDataFileAndUpdatePreview(true);
        }

        m_insideRowHdrChange = false;
    }

    private void loadColHdrSettings() {

        if (m_insideColHdrChange) {
            return;
        }
        m_insideLoadColHdr = true;

        m_hasColHeaders.setSelected(m_frSettings.getFileHasColumnHeaders());

        m_insideLoadColHdr = false;
    }

    /**
     * Reads the settings of the 'fileHasColHeaders' checkbox and transfers them
     * into the internal settings object.
     */
    protected void colHeadersSettingsChanged() {

        if (m_insideLoadColHdr) {
            return;
        }

        m_insideColHdrChange = true;

        m_frSettings.setFileHasColumnHeadersUserSet(true);
        m_frSettings.setFileHasColumnHeaders(m_hasColHeaders.isSelected());
        // recreate artificial names if file has no col headers
        if (!m_frSettings.getFileHasColumnHeaders()) {
            recreateColNames(false);
        }
        analyzeDataFileAndUpdatePreview(true);

        m_insideColHdrChange = false;
    }

    /**
     * The item changed listener to the 'ignore whitespaces' check box.
     */
    protected void ignoreWSChanged() {

        if (m_insideLoadWS) {
            return;
        }
        m_insideWSChange = true;

        m_frSettings.setWhiteSpaceUserSet(true);

        boolean checked = m_ignoreWS.isSelected();

        // if the box gets unchecked we store the previous whitespaces. That is
        // for support of extended settings which may allow for defining
        // whitespaces, other than space and tab. Then, - if we wouldn't store
        // the previous WSs the user could delete his definitions with one click
        // by unchecking the box, and wouldn't get them back by checking it
        // again.

        if (checked) {
            if (m_prevWhiteSpaces != null) {
                // we just re-set the old whitespaces again
                for (String ws : m_prevWhiteSpaces) {
                    m_frSettings.addWhiteSpaceCharacter(ws);
                }
            } else {
                // no previously user defined whitespaces, use our space+tab
                // default.
                m_frSettings.addWhiteSpaceCharacter(" ");
                m_frSettings.addWhiteSpaceCharacter("\t");
            }
        } else {
            // save the current whitespace definitions
            m_prevWhiteSpaces = m_frSettings.getAllWhiteSpaces();
            // and blow them all away.
            m_frSettings.removeAllWhiteSpaces();
        }

        analyzeDataFileAndUpdatePreview(true); // force re-analyze

        m_insideWSChange = false;
    }

    private void loadWhiteSpaceSettings() {

        if (m_insideWSChange) {
            return;
        }
        m_insideLoadWS = true;

        m_ignoreWS.setSelected(m_frSettings.getAllWhiteSpaces().size() > 0);
        m_prevWhiteSpaces = null;

        m_insideLoadWS = false;
    }

    /**
     * Reads the settings of the column delimiter box and transfers them into
     * the internal settings object.
     */
    protected void delimSettingsChanged() {

        if (m_insideLoadDelim) {
            // when we load delimiter settings the delimStettings change - of
            // course. We are not triggering any action then.
            return;
        }

        m_insideDelimChange = true;

        // to avoid unnecessary re-analyzing of the file, find out if the
        // delimiter actually changed.
        String newDelim = null;

        Object o = m_delimField.getEditor().getItem();
        if (o instanceof Delimiter) {
            newDelim = ((Delimiter)o).getDelimiter();
        } else {
            newDelim = TokenizerSettings.unescapeString((String)o);
        }
        if (newDelim.equals(m_delimApplied)) {
            // m_delimApplied is the delimiter stored in the settings or <none>
            // if none is selected.
            m_insideDelimChange = false;
            return;
        }

        m_frSettings.setDelimiterUserSet(true);
        m_delimApplied = null; // clear it in case things go wrong

        // remove all delimiters except row delimiters
        for (Delimiter delim : m_frSettings.getAllDelimiters()) {
            if (m_frSettings.isRowDelimiter(delim.getDelimiter(), false)) {
                continue;
            }
            m_frSettings.removeDelimiterPattern(delim.getDelimiter());
        }
        m_frSettings.setIgnoreEmptyTokensAtEndOfRow(false);

        // now set the selected one

        // index 0 is the <none> placeholder
        if (o != DEFAULT_DELIMS[0]) {

            String delimStr = null;
            if (o instanceof Delimiter) {
                // user selected one from the list (didn't edit a new one)
                try {
                    // add that delimiter:
                    Delimiter selDelim = (Delimiter)o;
                    delimStr = selDelim.getDelimiter();
                    m_frSettings
                            .addDelimiterPattern(delimStr,
                                    selDelim.combineConsecutiveDelims(),
                                    selDelim.returnAsToken(),
                                    selDelim.includeInToken());
                    m_delimApplied = delimStr;
                } catch (IllegalArgumentException iae) {
                    setErrorLabelText(iae.getMessage());
                    m_insideDelimChange = false;
                    return;
                }

            } else {
                delimStr = (String)o;
                delimStr = TokenizerSettings.unescapeString(delimStr);

                if ((delimStr != null) && (!delimStr.equals(""))) {
                    try {
                        m_frSettings.addDelimiterPattern(delimStr, false,
                                false, false);
                        m_delimApplied = delimStr;
                    } catch (IllegalArgumentException iae) {
                        setErrorLabelText(iae.getMessage());
                        m_insideDelimChange = false;
                        return;
                    }
                }
            }

            if ((delimStr != null)
                    && (delimStr.equals(" ") || delimStr.equals("\t"))) {
                // with whitespaces we ignore (by default) extra delims at EOR
                if (m_frSettings.ignoreDelimsAtEORUserSet()) {
                    m_frSettings.setIgnoreEmptyTokensAtEndOfRow(m_frSettings
                            .ignoreDelimsAtEORUserValue());
                } else {
                    m_frSettings.setIgnoreEmptyTokensAtEndOfRow(true);
                }
            }

        } else {
            m_delimApplied = DEFAULT_DELIMS[0].getDelimiter();
        }

        // make sure \n is always a row delimiter
        if (!m_frSettings.isRowDelimiter("\n", false)) {
            m_frSettings.addRowDelimiter("\n", true);
        }

        analyzeDataFileAndUpdatePreview(true); // force re-analyze
        m_insideDelimChange = false;
    }

    /**
     * Loads the settings from the global settings object into the delimiter box
     * and creates the basicDelim vector.
     */
    private void loadDelimSettings() {

        if (m_insideDelimChange) {
            return;
        }

        m_insideLoadDelim = true;

        m_delimField.removeAllItems();

        m_delimField.setModel(new DefaultComboBoxModel(DEFAULT_DELIMS));
        // the above selects the first in the list - which is the <none>.
        m_delimApplied = DEFAULT_DELIMS[0].getDelimiter();

        for (Delimiter delim : m_frSettings.getAllDelimiters()) {
            if (m_frSettings.isRowDelimiter(delim.getDelimiter(), false)) {
                continue;
            }

            if (((DefaultComboBoxModel)m_delimField.getModel())
                    .getIndexOf(delim) < 0) {
                // add all delimiters to the selection list of the combo box
                m_delimField.addItem(delim);
            }
            m_delimField.setSelectedItem(delim);
            m_delimApplied = delim.getDelimiter();

        }
        m_insideLoadDelim = false;
    }

    /**
     * Called whenever the Java-Style comment box is clickered.
     */
    protected void commentSettingsChanged() {

        if (m_insideLoadComment) {
            return;
        }

        m_insideCommentChange = true;

        m_frSettings.setCommentUserSet(true);

        m_frSettings.removeAllComments();

        boolean addedJSingleLine = false;

        if (m_cStyleComment.isSelected()) {
            m_frSettings.addBlockCommentPattern("/*", "*/", false, false);
            m_frSettings.addSingleLineCommentPattern("//", false, false);
            addedJSingleLine = true;
        }

        String slc = m_singleLineComment.getText().trim();
        if (slc.length() > 0) {
            if (!(slc.equals("//") && addedJSingleLine)) {
                try {
                    m_frSettings.addSingleLineCommentPattern(slc, false, false);
                } catch (IllegalArgumentException iae) {
                    setErrorLabelText(iae.getMessage());
                    return;
                }
            }
        }

        analyzeDataFileAndUpdatePreview(true);

        m_insideCommentChange = false;
    }

    /*
     * sets the Java-Style comment check box from the current settings object
     */
    private void loadCommentSettings() {

        if (m_insideCommentChange) {
            return;
        }

        m_insideLoadComment = true;

        boolean jBlockFound = false;
        boolean jSingleLineFound = false;
        Comment singleLine = null; // there might be an extra sl comment

        for (Comment comment : m_frSettings.getAllComments()) {
            if (comment.getEnd().equals("\n")) {
                // its a single line comment
                if (comment.getBegin().equals("//")) {
                    jSingleLineFound = true;
                } else {
                    singleLine = comment;
                }
            } else {
                // its a block comment
                if (comment.getBegin().equals("/*")
                        && comment.getEnd().equals("*/")) {
                    jBlockFound = true;
                }
                // all other block comments we ignore - but the analyzer doesnt
                // add them - and the user cant (without expert settings!)
            }
        }

        m_cStyleComment.setSelected(jBlockFound && jSingleLineFound);
        String singlePattern = "";
        if (singleLine != null) {
            singlePattern = singleLine.getBegin();
        }
        m_singleLineComment.setText(singlePattern);

        m_insideLoadComment = false;

    }

    /**
     * Pops open the dialog of the columnProperties object of the specified
     * column. This will allow the user to enter new column name, type and
     * missing value. Also changes the domain and 'read from file' flag.
     *
     * @param colIdx the index of the column to get new user settings for
     */
    protected void setNewUserSettingsForColumn(final int colIdx) {

        assert colIdx >= 0;
        assert colIdx < m_frSettings.getColumnProperties().size();

        Vector<ColProperty> cProps = m_frSettings.getColumnProperties();

        Frame f = null;
        Container c = getPanel().getParent();
        while (c != null) {
            if (c instanceof Frame) {
                f = (Frame)c;
                break;
            }
            c = c.getParent();
        }

        Vector<ColProperty> newColProps =
                ColPropertyDialog.openUserDialog(f, colIdx, cProps);

        if (newColProps != null) {
            // user pressed okay for new settings
            m_frSettings.setColumnProperties(newColProps);
            // user changed column type/name, we can clear that warning
            setAnalWarningText("");
            updatePreview();
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {

        /*
         * TODO: We need to synchronize the NodeSettings object
         */

        if (SwingUtilities.isEventDispatchThread()) {
            loadSettingsFromInternal(settings, specs);
        } else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        loadSettingsFromInternal(settings, specs);
                    }
                });
            } catch (InterruptedException ie) {
                LOGGER.warn("Exception while setting new table.", ie);
            } catch (InvocationTargetException ite) {
                LOGGER.warn("Exception while setting new table.", ite);
            }
        }
    }

    /**
     * We do the entire load settings in the Event/GUI thread as it accesses a
     * lot of GUI components.
     */
    private void loadSettingsFromInternal(final NodeSettingsRO settings,
            final DataTableSpec[] specs) {
        assert (settings != null && specs != null);

        String[] history = FileReaderNodeModel.getFileHistory();
        // loading the history would trigger an item changed event.
        m_urlCombo.removeItemListener(this);

        m_urlCombo.removeAllItems();
        for (String str : history) {
            m_urlCombo.addItem(str);
        }

        m_urlCombo.addItemListener(this);

        try {
            // this will fail if the settings are invalid (which will be the
            // case when they come from an uninitialized model). We create
            // an empty settings object in the catch block.
            m_frSettings = new FileReaderNodeSettings(settings);
        } catch (InvalidSettingsException ice) {
            m_frSettings = new FileReaderNodeSettings();
        }

        /*
         * This allows for setting a file location in the settings object
         * without any other settings. It happens, when a file is dropped on the
         * editor, only the source is set and the dialog opens. We must preserve
         * the source then.
         */
        try {
            URL dataFileLocation =
                    new URL(
                            settings.getString(FileReaderSettings.CFGKEY_DATAURL));
            m_frSettings
                    .setDataFileLocationAndUpdateTableName(dataFileLocation);
        } catch (MalformedURLException mfue) {
            // don't set the data location if it bombs
        } catch (InvalidSettingsException ice) {
            // don't set the data location if it bombs
        }

        // transfer settings from the structure in the dialog's components
        if ((m_frSettings.getDataFileLocation() != null)
                && (m_frSettings.getColumnProperties() != null)
                && (m_frSettings.getColumnProperties().size() > 0)) {
            // do not analyze file if we got settings to use
            m_urlCombo.setSelectedItem(m_frSettings.getDataFileLocation()
                    .toString());

            loadSettings(false);
        } else {
            // load settings and analyze file
            loadSettings(true);
        }
        // after loading settings we can clear the analyze warning
        setAnalWarningText("");
        updatePreview();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {

        /*
         * TODO: We need to synchronize the NodeSettings object
         */
        // make sure the filename entered gets committed
        fileLocationChanged();

        // make sure the delimiter is committed in case user entered a new one
        // and didn't hit enter - starts an analysis if things changed
        delimSettingsChanged();

        // if no valid settings exist, we need to analyze the file.
        if (m_frSettings.getNumberOfColumns() < 0) {
            synchronized (m_analysisRunning) {
                // start analysis only, if it is not already running
                if (!m_analysisRunning.booleanValue()) {
                    // the analysis thread should override the error label
                    setErrorLabelText("Waiting for file analysis to finish..."
                            + "Click \"Quick Scan\" to cut it short.");
                    analyzeAction();
                }
            }
        }

        FileReaderNodeSettings settingsToSave;

        // don't close dialog if analysis is running
        synchronized (m_analysisRunning) {
            if (m_analysisRunning.booleanValue()) {
                throw new InvalidSettingsException(
                        "File analysis currently running. Please wait for it to"
                                + " finish, check the settings, and "
                                + "click OK or Apply again");
            } else {
                // while we have the lock, clone the settings
                settingsToSave = new FileReaderNodeSettings(m_frSettings);
            }
        }

        String errLabel = getErrorLabelText();
        if ((errLabel != null) && (errLabel.trim().length() > 0)) {
            throw new InvalidSettingsException("With the current settings"
                    + " an error occurs: " + errLabel);
        }
        if (m_previewTable.getErrorOccurred()) {
            throw new InvalidSettingsException("With the current settings"
                    + " an error occurs when reading the file (line "
                    + m_previewTable.getErrorLine() + "): "
                    + m_previewTable.getErrorMsg());
        }

        // transfers the URL from the textfield into the setting object
        saveSettings(settingsToSave);

        // file existence is not checked during model#loadSettings. Do it here.
        Reader reader = null;
        try {
            reader = settingsToSave.createNewInputReader();
        } catch (IOException ioe) {
            throw new InvalidSettingsException("I/O Error while accessing '"
                    + settingsToSave.getDataFileLocation().toString() + "'.");
        }
        try {
            reader.close();
        } catch (IOException ioe) {
            // then don't close it.
        }

        settingsToSave.saveToConfiguration(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCancel() {
        // bug 1482: if an analysis is currently running stop it
        synchronized (m_analysisRunning) {
            if (m_analysisRunning.booleanValue()) {
                m_analysisExecMonitor.setExecuteInterrupted();
            }
        }

    }

    /*
     * Reads the entered file location from the edit field and stores the new
     * value in the settings object. Throws an exception if the entered URL is
     * invalid (and clears the URL in the settings object before). Returns true
     * if the entered location (string) is different from the one previously
     * set.
     */
    private boolean takeOverNewFileLocation() throws InvalidSettingsException {

        URL newURL;

        try {
            newURL = textToURL(m_urlCombo.getEditor().getItem().toString());
        } catch (Exception e) {
            m_frSettings.setDataFileLocationAndUpdateTableName(null);
            throw new InvalidSettingsException("Invalid URL entered.");
        }

        URL oldUrl = m_frSettings.getDataFileLocation();
        String oldString = "";
        if (oldUrl != null) {
            oldString = oldUrl.toString();
        }

        m_frSettings.setDataFileLocationAndUpdateTableName(newURL);

        return !oldString.equals(newURL.toString());

    }

    /**
     * Updates the preview table, if a new and valid URL was specified in the
     * data file name text field or the force parameter is set true. It
     * overrides all current settings with the settings from the file analyzer -
     * except when the URL didn't changed and the user has explicitly set some
     * values. For big files, it just shows a button to trigger analysis. The
     * analysis runs in the background, and if it finishes it shows the new
     * content of the file.
     * <p>
     * NOTE: May change the global settings object completely.
     *
     * @param forceAnalyze forces the analysis of the datafile even though it
     *            might be the one set in the global settings (and thus already
     *            being analyzed).
     *
     */
    protected void analyzeDataFileAndUpdatePreview(final boolean forceAnalyze) {

        if (forceAnalyze && (m_frSettings.getDataFileLocation() != null)) {

            // errors are from previous runs
            setErrorLabelText("");

            // if an analysis is currently running we must cancel it first
            synchronized (m_analysisRunning) {

                // wait until we have a chance to run the analysis
                while (m_analysisRunning.booleanValue()) {
                    // kill off any other analysis
                    // we are the only and truly one
                    m_analysisExecMonitor.setExecuteInterrupted();
                    // wait until it finishes
                    try {
                        m_analysisRunning.wait();
                    } catch (InterruptedException ie) {
                        // huh?!?
                    }
                }

                // invalidate the current settings
                m_frSettings.setNumberOfColumns(-1);
                showAnalyzeButton();
                analyzeAction();
            }

        } else {
            showPreviewTable();
            updatePreview();
        }
    }

    /*
     * Places the analyze button, progress bar etc. in the preview table area
     */
    private void showAnalyzeButton() {

        ViewUtils.runOrInvokeLaterInEDT(new Runnable() {
            public void run() {
                // first remove the preview panel
                m_previewArea.removeAll();
                // show the analyze button
                m_previewArea.add(m_analysisPanel, BorderLayout.CENTER);
                // enable button!!
                m_analyzeCancel.setText("Quick Scan");
                m_analyzeCancel.setEnabled(false);
                m_analyzeProgressMsg.setText("");
                m_analyzeProgressBar.setValue(0);
                getPanel().revalidate();
                getPanel().repaint();
            }
        });
    }

    /*
     * places the preview table component in it designated panel
     */
    private void showPreviewTable() {
        ViewUtils.runOrInvokeLaterInEDT(new Runnable() {
            public void run() {
                // first remove the analysis panel
                m_previewArea.removeAll();
                // show preview table
                m_previewArea.add(m_previewPanel, BorderLayout.CENTER);
                getPanel().revalidate();
                getPanel().repaint();
            }
        });
    }

    /**
     * If an analysis is currently running it will interrupt it (which causes
     * any analysis results to be discarded). If no analysis is running this
     * method does nothing. The method returns immediately (and doesn't wait for
     * the analysis to finish). It does try to aquire the "m_analysisRunning"
     * lock!
     */
    private void interruptAnalysis() {
        synchronized (m_analysisRunning) {
            if (m_analysisRunning.booleanValue()) {
                m_analysisExecMonitor.setExecuteInterrupted();
            }
        }
    }

    /**
     * If no analysis is running it triggers one and waits until its done,
     * otherwise it just waits for the running analysis to finish.
     */
    protected void waitForAnalyzeAction() {
        synchronized (m_analysisRunning) {
            if (!m_analysisRunning.booleanValue()) {
                analyzeAction();
            }
            try {
                m_analysisRunning.wait();
            } catch (InterruptedException ie) {
                // do nothing.
            }
        }
    }

    /**
     * triggers analysis.
     */
    protected void analyzeAction() {

        synchronized (m_analysisRunning) {
            // wait until we have a chance to run the analysis
            while (m_analysisRunning.booleanValue()) {
                LOGGER.error("Internal error: Re-entering analysis thread - "
                        + "canceling it - waiting for it to finish...");
                m_analysisExecMonitor.setExecuteInterrupted();
                // wait until it finishes
                try {
                    m_analysisRunning.wait();
                    LOGGER.error("Alright - continuing with new analysis...");
                } catch (InterruptedException ie) {
                    // huh?!?
                }
            }

            // Create execution context for progress and cancellations
            // We use our own progress monitor, we need to distinguish
            // between user cancel and code interrupts.
            m_analysisExecMonitor = new FileReaderExecutionMonitor();
            m_analysisExecMonitor.getProgressMonitor().addProgressListener(
                    new NodeProgressListener() {
                        public void progressChanged(
                                final NodeProgressEvent pEvent) {
                            if (pEvent.getNodeProgress().getMessage() != null) {
                                ViewUtils.runOrInvokeLaterInEDT(new Runnable() {
                                    public void run() {
                                        Double p =
                                                pEvent.getNodeProgress()
                                                        .getProgress();
                                        if (p == null) {
                                            p = new Double(0.0);
                                        }
                                        m_analyzeProgressMsg
                                                .setText(pEvent
                                                        .getNodeProgress()
                                                        .getMessage());
                                        m_analyzeProgressBar.setValue((int)Math
                                                .round(100 * p.doubleValue()));
                                        getPanel().revalidate();
                                        getPanel().repaint();
                                    }
                                });
                            }
                        }
                    });

            // the analysis thread, when finished, clears this flag.
            m_analysisRunning.setValue(true);
            // allow for quickies from now on
            m_analyzeCancel.setEnabled(true);
            setPreviewTable(null);
            setErrorLabelText("");

        }

        // clone current settings
        FileReaderNodeSettings newFRSettings =
                new FileReaderNodeSettings(m_frSettings);

        Vector<ColProperty> oldColProps = m_frSettings.getColumnProperties();

        // prepare the settings object for re-analysis
        newFRSettings.setNumberOfColumns(-1);
        Vector<ColProperty> newProps = new Vector<ColProperty>();
        if (oldColProps != null) {
            for (ColProperty cProp : oldColProps) {
                // take over only the ones modified by the user
                if ((cProp != null) && (cProp.getUserSettings())) {
                    newProps.add(cProp);
                } else {
                    newProps.add(null);
                }
            }
        }
        newFRSettings.setColumnProperties(newProps);

        analyzeInThread(newFRSettings);

    }

    /**
     * Triggers analysis and returns. After analysis is done, settings are
     * loaded and the preview is shown.
     */
    private void analyzeInThread(final FileReaderNodeSettings userSettings) {

        // go!
        new Thread(new Runnable() {
            public void run() {

                try {

                    // analyze the file now.
                    FileReaderNodeSettings newSettings =
                            FileAnalyzer.analyze(userSettings,
                                    m_analysisExecMonitor);

                    if (m_analysisExecMonitor.wasInterrupted()) {
                        // if the code stopped us, do nothing more
                        return;
                    }

                    if (m_analysisExecMonitor.wasCanceled()) {
                        /*
                         * if user canceled and we did get an result back from
                         * analyze we could use these settings after partial
                         * analysis
                         */
                        if (newSettings != null) {
                            setAnalWarningText("WARNING: suggested settings "
                                    + "are based on a partial file analysis "
                                    + "only! Please verify.");
                        }
                    } else {
                        setAnalWarningText("");
                    }

                    if ((newSettings != null)
                            && !m_analysisExecMonitor.wasInterrupted()) {

                        m_frSettings = newSettings;

                        loadSettings(false); // false = don't load URL

                        updatePreview();
                    }

                } catch (IOException ioe) {
                    setPreviewTable(null);
                    String msg = ioe.getMessage();
                    if ((msg == null) || (msg.length() == 0)) {
                        msg = "No details, sorry.";
                    }
                    updatePreview();
                    setAnalWarningText("I/O Error while analyzing file: ");
                    return;
                } catch (TokenizerException fte) {
                    updatePreview();
                    String msg = fte.getMessage();
                    if ((msg == null) || (msg.length() == 0)) {
                        msg = "Invalid Settings: No error message, sorry.";
                    }
                    setErrorLabelText(msg);
                    setPreviewTable(null);
                    return;

                } finally {

                    synchronized (m_analysisRunning) {
                        m_analysisExecMonitor = null;
                        m_analysisRunning.setValue(false);

                        // disable quicky
                        m_analyzeCancel.setEnabled(false);
                        m_analyzeProgressMsg.setText("");
                        m_analyzeProgressBar.setValue(0);

                        // wake all threads waiting for us to finish.
                        m_analysisRunning.notifyAll();
                    }

                }

            };

        }, "FileReaderAnalyze").start();

    }

    /*
     * from the current settings it creates a data table and displays it in the
     * preview pane. Will not analyze the file. Will not change things.
     */
    private void updatePreview() {
        // the settings changed - clear error message first
        setErrorLabelText("");

        // update preview
        if (m_frSettings.getDataFileLocation() == null) {
            // if there is no data file specified display empty table
            setPreviewTable(null);
            showPreviewTable();
            return;
        }
        FileReaderNodeSettings previewSettings =
                createPreviewSettings(m_frSettings);
        SettingsStatus status = previewSettings.getStatusOfSettings(true, null);
        if (status.getNumOfErrors() > 0) {
            setErrorLabelText(status.getErrorMessage(0));
            setPreviewTable(null);
            showPreviewTable();
            return;
        }
        DataTableSpec tSpec = previewSettings.createDataTableSpec();
        FileReaderPreviewTable newTable =
                new FileReaderPreviewTable(tSpec, previewSettings, null);
        setPreviewTable(newTable);
        showPreviewTable();
    }

    /**
     * Updates the preview view with the specified table. Updates the member
     * variable. Disposes of the old table.
     *
     * @param table the new table to store and to display
     */
    private void setPreviewTable(final FileReaderPreviewTable table) {

        final FileReaderPreviewTable oldTable = m_previewTable;

        // register a listener for error messages with the new table
        if (table != null) {
            table.addChangeListener(new ChangeListener() {
                public void stateChanged(final ChangeEvent e) {
                    if (m_previewTable != null) {
                        setErrorLabelText(m_previewTable.getErrorMsg(),
                                m_previewTable.getErrorDetail());
                    }
                }
            });
        }

        // set this - even before displaying it
        m_previewTable = table;

        ViewUtils.runOrInvokeLaterInEDT(new Runnable() {
            @Override
            public void run() {
                // set the new table in the view
                m_previewTableView.setDataTable(table);

                // properly dispose of the old table
                if (oldTable != null) {
                    oldTable.removeAllChangeListeners();
                    oldTable.dispose();
                }
            }
        });

    }

    /*
     * Creates a new settings object that has the same settings, except for the
     * column names and the "skipColumn" flag. Names are tagged with an asterisk
     * if the name and type of the column is user set, and specially marked if
     * the user chose to skip (not read in) this column. Additionally all
     * columns are marked "don't skip", so they appear in the preview table.
     */
    private FileReaderNodeSettings createPreviewSettings(
            final FileReaderNodeSettings settings) {
        assert settings != null;
        if (settings == null) {
            return null;
        }

        // create a clone of the specified settings object.

        FileReaderNodeSettings result = new FileReaderNodeSettings(settings);

        int numCols = result.getNumberOfColumns();
        Vector<ColProperty> colProps = result.getColumnProperties();
        // set of used names for uniquification
        Set<String> colNames = new HashSet<String>();

        for (int c = 0; c < numCols; c++) {

            ColProperty cProp = colProps.get(c);
            assert (cProp != null);
            String name = cProp.getColumnSpec().getName();

            // mark columns marked "skip"
            if (cProp.getSkipThisColumn()) {
                name = "[SKIP: \"" + name + "\"]";
                // change type to string because it accepts all data
                cProp.changeColumnType(StringCell.TYPE);
            } else {
                // mark user set column names with an "*"
                if (cProp.getUserSettings()) {
                    name = "*" + name;
                }
            }

            // need to make it unique
            int idx = 2; // the index we append to the name to make it unique
            String colName = name;
            while (colNames.contains(colName)) {
                colName = name + "(" + idx++ + ")";
            }
            colNames.add(colName);

            // set the new name
            cProp.changeColumnName(colName);
            cProp.setSkipThisColumn(false);
            // no need to set it in the result as the ColProperty object is
            // the same in our vector and in the result.
        }

        return result;

    }

    /*
     * replaces the member m_frSettings with a settings object holding default
     * values - EXCEPT FOR the file location which is taken over from the old
     * settings object. Loads the new settings into the dialog components.
     */
    private void resetSettings() {
        FileReaderNodeSettings newSettings = new FileReaderNodeSettings();
        newSettings.setDataFileLocationAndUpdateTableName(m_frSettings
                .getDataFileLocation());
        m_frSettings = newSettings;
        m_firstColProp = null;
        // don't load location - don't trigger analysis
        loadSettings(false);
    }

    /*
     * transfers the settings from the private member m_frSettings into the
     * components. Will not transfer the file location unless specified by the
     * parameter.
     */
    private void loadSettings(final boolean loadFileLocation) {

        assert m_frSettings != null;

        if (loadFileLocation) {
            if (m_frSettings.getDataFileLocation() != null) {
                m_urlCombo.setSelectedItem(m_frSettings.getDataFileLocation()
                        .toString());
            } else {
                m_urlCombo.setSelectedItem("");
            }
            analyzeDataFileAndUpdatePreview(true);
        }
        m_preserveSettings.setSelected(m_frSettings.getPreserveSettings());
        loadRowHdrSettings();
        loadColHdrSettings();
        // dis/enable the select recent files button
        loadDelimSettings();
        loadCommentSettings();
        loadWhiteSpaceSettings();

    }

    /*
     * transfers the components settings into the FileNodeSettings object.
     * Actually, as all components immediately set their state in the object,
     * the only thing left is the file name from the data file location text
     * field.
     */
    private void saveSettings(final FileReaderNodeSettings settings)
            throws InvalidSettingsException {
        try {
            URL dataURL =
                    textToURL(m_urlCombo.getEditor().getItem().toString());
            settings.setDataFileLocationAndUpdateTableName(dataURL);
        } catch (MalformedURLException mfue) {
            throw new InvalidSettingsException("Invalid (malformed) URL for "
                    + "the data file location.", mfue);
        }
    }

    /*
     * traverses through the col properties and re-sets the column names that
     * are not set by the user. The flag indicating a user set name is the
     * 'useFileHeader' property. We start with "Col0" and make sure names are
     * unique.
     */
    private void recreateColNames(final boolean uniquifyFirstColName) {
        Vector<ColProperty> colProps = m_frSettings.getColumnProperties();

        // set with column names used - for faster uniquification
        Set<String> colNames = new HashSet<String>();
        // pre-load it with the user set names (which we are not gonna change)
        for (int c = 0; c < colProps.size(); c++) {
            ColProperty cProp = colProps.get(c);
            if ((cProp != null) && (cProp.getUserSettings())
                    && (!((c == 0) && uniquifyFirstColName))) {
                colNames.add(cProp.getColumnSpec().getName());
            }
        }

        for (int c = 0; c < colProps.size(); c++) {
            ColProperty cProp = colProps.get(c);
            if (cProp == null) {
                continue;
            }
            if ((cProp.getUserSettings())
                    && (!((c == 0) && uniquifyFirstColName))) {
                // name was set by user - consider it fixed.
                assert colNames.contains(cProp.getColumnSpec().getName());
                continue;
            }
            String namePrefix; // the name we add a number to, to uniquify it
            if ((c == 0) && uniquifyFirstColName) {
                // if we uniquify even the first col - we at least start with
                // its current name as default
                namePrefix = cProp.getColumnSpec().getName();
            } else {
                namePrefix = "Col" + c;
            }
            // make sure the name is unique
            String name = namePrefix;
            int count = 2;
            while (colNames.contains(name)) {
                name = namePrefix + "(" + count++ + ")";
            }

            cProp.changeColumnName(name);
            colNames.add(name);
        }
        // We've changed the names in the colProperty objects - no need to
        // write back the vector
    }

    /**
     * Called when the user presses the "Advanced Settings..." button.
     */
    protected void advancedSettings() {

        // figure out the parent to be able to make the dialog modal
        Frame f = null;
        Container c = getPanel().getParent();
        while (c != null) {
            if (c instanceof Frame) {
                f = (Frame)c;
                break;
            }
            c = c.getParent();
        }
        // pop open the advanced settings dialog with our current settings
        FileReaderAdvancedDialog advDlg =
                new FileReaderAdvancedDialog(f, m_frSettings);
        advDlg.setModal(true);
        advDlg.setVisible(true);
        // will not continue until user closes the dialog

        // first check if user closed via the readXML button
        if (advDlg.closedViaReadXML()) {
            // stop a possibly running analysis
            interruptAnalysis();
            readXMLSettings();
            analyzeDataFileAndUpdatePreview(false); // don't reanalyze
        } else if (advDlg.closedViaOk()) {
            // stop a possibly running analysis
            interruptAnalysis();
            // call with the actual settings
            advDlg.overrideSettings(m_frSettings);
            analyzeDataFileAndUpdatePreview(advDlg.needsReAnalyze());
        }
        advDlg.dispose();
    }

    /**
     * Pops up a file chooser dialog and reads the settings fromt the selected
     * xml file.
     */
    protected void readXMLSettings() {
        String xmlPath =
                popupFileChooser(m_urlCombo.getEditor().getItem().toString(),
                        true);

        if (xmlPath == null) {
            // user canceled.
            return;
        }

        FileReaderNodeSettings newFrns;

        try {

            newFrns = FileReaderNodeSettings.readSettingsFromXMLFile(xmlPath);

        } catch (IllegalStateException ise) {
            JOptionPane.showConfirmDialog(m_dialogPanel, ise.getMessage(),
                    "Attention: Settings not read!",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE);
            return;
        }

        m_frSettings = newFrns;
        loadSettings(false); // don't trigger file analysis
        m_urlCombo.setSelectedItem(m_frSettings.getDataFileLocation()
                .toString());

        // clear analyze warning after loading settings from XML file
        setAnalWarningText("");
        updatePreview();
    }

    /**
     * Pops up the file selection dialog and returns the path to the selected
     * file - or <code>null</code> if the user canceled.
     *
     * @param startingPath the path the dialog starts in
     * @param readXml if <code>true</code> the filter will be set to '*.xml'
     *            files
     * @return the path to the selected file, or <code>null</code> if user
     *         canceled
     */
    protected String popupFileChooser(final String startingPath,
            final boolean readXml) {

        String startingDir = "";
        try {
            URL newURL = textToURL(startingPath);
            if (newURL.getProtocol().equals("file")) {
                File tmpFile = new File(newURL.toURI().getPath());
                startingDir = tmpFile.getAbsolutePath();
            }
        } catch (MalformedURLException e) {
            // no valid path - start in the default dir of the file chooser
        } catch (URISyntaxException ex) {
            // no valid path - start in the default dir of the file chooser
        }

        JFileChooser chooser;
        chooser = new JFileChooser(startingDir);
        if (readXml) {
            chooser.setFileFilter(new FileReaderFileFilter("xml",
                    "XML settings files"));
        }

        // make dialog modal
        int returnVal = chooser.showOpenDialog(getPanel().getParent());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String path;
            try {
                path =
                        chooser.getSelectedFile().getAbsoluteFile().toURI()
                                .toURL().toString();
            } catch (Exception e) {
                path = "<Error: Couldn't create URL for file>";
            }
            return path;
        }
        // user canceled - return null
        return null;
    }

    /**
     * Tries to create an URL from the passed string.
     *
     * @param url the string to transform into an URL
     * @return URL if entered value could be properly tranformed, or
     * @throws MalformedURLException if the value passed was invalid
     */
    static URL textToURL(final String url) throws MalformedURLException {

        if ((url == null) || (url.equals(""))) {
            throw new MalformedURLException("Specify a not empty valid URL");
        }

        URL newURL;
        try {
            newURL = new URL(url);
        } catch (Exception e) {
            // see if they specified a file without giving the protocol
            File tmp = new File(url);

            // if that blows off we let the exception go up the stack.
            newURL = tmp.getAbsoluteFile().toURI().toURL();
        }
        return newURL;
    }

    private void setErrorLabelText(final String text) {
        setErrorLabelText(text, null);
    }

    private void setErrorLabelText(final String text, final String detailMsg) {
        m_errorLabel.setText(text);
        if (detailMsg == null) {
            m_errorDetail.setText("");
        } else {
            m_errorDetail.setText(detailMsg);
        }
        getPanel().revalidate();
        getPanel().repaint();
    }

    private String getErrorLabelText() {
        return m_errorLabel.getText();
    }

    private void setAnalWarningText(final String text) {
        m_analyzeWarn.setText(text);
        getPanel().revalidate();
        getPanel().repaint();
    }

    @Override
    public void onClose() {
    	if (m_previewTable != null) {
    	    m_previewTableView.setDataTable(null);
    		m_previewTable.dispose();
    		m_previewTable = null;
    	}
    }
}
