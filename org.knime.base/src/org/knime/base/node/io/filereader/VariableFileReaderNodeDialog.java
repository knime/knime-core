/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   18.09.2008 (ohl): created
 */
package org.knime.base.node.io.filereader;

import java.awt.BorderLayout;
import java.awt.Color;
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
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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

import org.knime.base.node.io.filetokenizer.Comment;
import org.knime.base.node.io.filetokenizer.Delimiter;
import org.knime.base.node.io.filetokenizer.FileTokenizerException;
import org.knime.base.node.io.filetokenizer.FileTokenizerSettings;
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
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.tableview.TableView;
import org.knime.core.node.util.ConvenientComboBoxRenderer;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.NodeProgressEvent;
import org.knime.core.node.workflow.NodeProgressListener;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.MutableBoolean;

/**
 *
 * @author ohl, University of Konstanz
 */
public class VariableFileReaderNodeDialog extends NodeDialogPane implements
        ItemListener {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(FileReaderNodeDialog.class);

    private static final int HORIZ_SPACE = 10;

    private static final int COMP_HEIGHT = 30;

    private static final int PANEL_WIDTH = 5000;

    private static final Delimiter[] DEFAULT_DELIMS =
            new Delimiter[]{
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
    private VariableFileReaderNodeSettings m_frSettings;

    private JComboBox m_urlCombo;

    private TableView m_previewTableView;

    private JCheckBox m_hasRowHeaders;

    private JCheckBox m_hasColHeaders;

    private JComboBox m_delimField;

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
    VariableFileReaderNodeDialog() {
        super();
        m_frSettings = new VariableFileReaderNodeSettings();
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
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(),
                "Select variable holding file location: (press 'Enter' to "
                        + "update preview)"));

        Box fileBox = Box.createHorizontalBox();

        // Creating the brows button here in order to get its preferred height
        int buttonHeight = 20;

        m_urlCombo = new JComboBox();
        m_urlCombo.setEditable(false);
        m_urlCombo.setRenderer(new ConvenientComboBoxRenderer());
        m_urlCombo.setMaximumSize(new Dimension(PANEL_WIDTH, buttonHeight));
        m_urlCombo.setMinimumSize(new Dimension(350, buttonHeight));
        m_urlCombo.setPreferredSize(new Dimension(350, buttonHeight));
        m_urlCombo
                .setToolTipText("Select a variable whose value is a file URL");

        fileBox.add(Box.createHorizontalGlue());
        fileBox.add(new JLabel("Scope Variable Name:"));
        fileBox.add(Box.createHorizontalStrut(HORIZ_SPACE));
        fileBox.add(m_urlCombo);
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
            String err = e.getMessage();
            if (err == null || err.isEmpty()) {
                err = "ERROR - no details available.";
            }
            setErrorLabelText(err);
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
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Preview"));

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
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Basic Settings"));
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

        m_frSettings.setDelimiterUserSet(true);

        // to avoid unnecessary re-analyzing of the file, find out if the
        // delimiter actually changed.
        String newDelim = null;
        if (m_delimField.getSelectedIndex() > -1) {
            newDelim =
                    ((Delimiter)m_delimField.getSelectedItem()).getDelimiter();
        } else {
            newDelim =
                    FileTokenizerSettings.unescapeString((String)m_delimField
                            .getSelectedItem());
        }
        for (Delimiter delim : m_frSettings.getAllDelimiters()) {
            if (delim.getDelimiter().equals(newDelim)) {
                // the entered pattern is already a delimiter. Nothing changed.
                m_insideDelimChange = false;
                return;
            }
        }

        // remove all delimiters except row delimiters
        for (Delimiter delim : m_frSettings.getAllDelimiters()) {
            if (m_frSettings.isRowDelimiter(delim.getDelimiter())) {
                continue;
            }
            m_frSettings.removeDelimiterPattern(delim.getDelimiter());
        }
        m_frSettings.setIgnoreEmptyTokensAtEndOfRow(false);

        // now set the selected one

        // index 0 is the <none> placeholder
        if (m_delimField.getSelectedIndex() != 0) {

            String delimStr = null;
            if (m_delimField.getSelectedIndex() > -1) {
                // user selected one from the list (didn't edit a new one)
                try {
                    // add that delimiter:
                    Delimiter selDelim =
                            (Delimiter)m_delimField.getSelectedItem();
                    delimStr = selDelim.getDelimiter();
                    m_frSettings.addDelimiterPattern(delimStr, selDelim
                            .combineConsecutiveDelims(), selDelim
                            .returnAsToken(), selDelim.includeInToken());
                } catch (IllegalArgumentException iae) {
                    setErrorLabelText(iae.getMessage());
                    m_insideDelimChange = false;
                    return;
                }

            } else {
                delimStr = (String)m_delimField.getSelectedItem();
                delimStr = FileTokenizerSettings.unescapeString(delimStr);

                if ((delimStr != null) && (!delimStr.equals(""))) {
                    try {
                        m_frSettings.addDelimiterPattern(delimStr, false,
                                false, false);
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

        }

        // make sure \n is always a row delimiter
        if (!m_frSettings.isRowDelimiter("\n")) {
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
        for (Delimiter delim : m_frSettings.getAllDelimiters()) {
            if (m_frSettings.isRowDelimiter(delim.getDelimiter())) {
                continue;
            }

            if (((DefaultComboBoxModel)m_delimField.getModel())
                    .getIndexOf(delim) < 0) {
                // add all delimiters to the selection list of the combo box
                m_delimField.addItem(delim);
            }
            m_delimField.setSelectedItem(delim);

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
            final PortObjectSpec[] specs) throws NotConfigurableException {

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
            final PortObjectSpec[] specs) {
        assert (settings != null && specs != null);

        // loading of the variable names would trigger an item changed event.
        m_urlCombo.removeItemListener(this);

        m_urlCombo.removeAllItems();
        Map<String, FlowVariable> stack = getAvailableFlowVariables();
        for (String str : stack.keySet()) {
            m_urlCombo.addItem(str);
        }

        m_urlCombo.addItemListener(this);

        try {
            // this will fail if the settings are invalid (which will be the
            // case when they come from an uninitialized model). We create
            // an empty settings object in the catch block.
            m_frSettings = new VariableFileReaderNodeSettings(settings);
        } catch (InvalidSettingsException ice) {
            m_frSettings = new VariableFileReaderNodeSettings();
        }

        String loadedLocation = m_frSettings.getDataFileLocation().toString();

        // check the specified variable
        if (stack.get(m_frSettings.getVariableName()) == null) {
            // the variable is not on the stack anymore
            m_frSettings.setVariableName("");
            m_frSettings.setDataFileLocationAndUpdateTableName(null);
        } else {
            String varVal =
                    stack.get(m_frSettings.getVariableName()).getStringValue();
            try {
                URL varURL = textToURL(varVal);
                if (!varURL.toString().equals(
                        m_frSettings.getDataFileLocation().toString())) {
                    // variable points to a different location
                    m_frSettings.setDataFileLocationAndUpdateTableName(null);
                }
            } catch (Exception e) {
                // the variable is still there - but has a invalid location
                m_frSettings.setDataFileLocationAndUpdateTableName(null);
            }
        }

        // set the URL, if available, or clear an invalid variable name
        try {
            m_frSettings = m_frSettings.createSettingsFrom(stack);
        } catch (Exception e) {
            m_frSettings.setVariableName("");
            m_frSettings.setDataFileLocationAndUpdateTableName(null);

        }
        m_urlCombo.setSelectedItem(m_frSettings.getVariableName());

        // transfer settings from the structure in the dialog's components
        if ((m_frSettings.getDataFileLocation() != null)
                && m_frSettings.getDataFileLocation().toString().equals(
                        loadedLocation)
                && (m_frSettings.getColumnProperties() != null)
                && (m_frSettings.getColumnProperties().size() > 0)) {
            // do not analyze file if we got settings to use
            m_urlCombo.setSelectedItem(m_frSettings.getVariableName());
            m_urlCombo.setToolTipText(m_frSettings.getDataFileLocation()
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

        // if no valid settings exist, we need to analyze the file.
        if (m_frSettings.getNumberOfColumns() < 0) {
            setErrorLabelText("Waiting for file analysis to finish..."
                    + "Click \"Stop\" to cut it short.");

            waitForAnalyzeAction();
            // the analysis thread should override the error label
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

        VariableFileReaderNodeSettings settingsToSave = m_frSettings;

        /*
         * if an analysis is currently running we ask the user what to do
         */
        synchronized (m_analysisRunning) {
            if (m_analysisRunning.booleanValue()) {
                // quickly create a clone of the current settings before it
                // finishes
                VariableFileReaderNodeSettings clone =
                        new VariableFileReaderNodeSettings(m_frSettings);
                if (JOptionPane.showOptionDialog(getPanel(),
                        "A file analysis is currently running. "
                                + "Do you want to wait for it to "
                                + "finish or use the " + "current settings?",
                        "File Analysis Running", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null, new String[]{
                                "Use current settings, cancel analysis",
                                "Wait for analysis to finish"},
                        "Wait for analysis to finish") == 1) {
                    throw new InvalidSettingsException(
                            "Please check the settings"
                                    + "after analysis finishes and "
                                    + "click OK or Apply again");
                }
                // stop it.
                m_analysisExecMonitor.setExecuteInterrupted();

                settingsToSave = clone;
            }

        }

        // transfers the URL from the textfield into the setting object
        saveSettings(settingsToSave);

        // file existence is not checked during model#loadSettings. Do it here.
        Reader reader = null;
        VariableFileReaderNodeSettings s;
        try {
            s = settingsToSave.createSettingsFrom(getAvailableFlowVariables());
        } catch (Exception e) {
            throw new InvalidSettingsException("Error while converting value "
                    + " to URL ");
        }
        try {
            reader = s.createNewInputReader();
            if (reader == null) {
                throw new InvalidSettingsException("I/O Error while "
                        + "accessing '" + s.getDataFileLocation().toString()
                        + "'.");
            }
        } catch (Exception ioe) {
            throw new InvalidSettingsException("I/O Error while accessing '"
                    + s.getDataFileLocation().toString() + "'.");
        }
        try {
            reader.close();
        } catch (IOException ioe) {
            // then don't close it.
        }

        settingsToSave.saveToConfiguration(settings);

    }

    /*
     * Reads the entered variable name from the edit field and stores the new
     * value in the settings object. Throws an exception if the entered URL is
     * invalid (and clears the URL in the settings object before). Returns true
     * if the entered location (string) is different from the one previously
     * set.
     */
    private boolean takeOverNewFileLocation() throws InvalidSettingsException {

        String varName = (String)m_urlCombo.getSelectedItem();

        if (getAvailableFlowVariables().get(varName) == null) {
            // oops.
            throw new InvalidSettingsException(
                    "Selected variable not available anymore. "
                            + "Select a different one.");
        }
        m_frSettings.setVariableName(varName);

        URL oldUrl = m_frSettings.getDataFileLocation();
        try {
            m_frSettings =
                    m_frSettings
                            .createSettingsFrom(getAvailableFlowVariables());
        } catch (Exception e) {
            m_frSettings.setDataFileLocationAndUpdateTableName(null);
            throw new InvalidSettingsException(e.getMessage());
        }

        String oldString = "";
        if (oldUrl != null) {
            oldString = oldUrl.toString();
        }

        return !oldString.equals(m_frSettings.getDataFileLocation().toString());

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
        VariableFileReaderNodeSettings newFRSettings =
                new VariableFileReaderNodeSettings(m_frSettings);

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
    private void analyzeInThread(
            final VariableFileReaderNodeSettings userSettings) {

        String threadName = "FileReaderAnalyze";
        // go!
        new Thread(new Runnable() {
            public void run() {

                try {

                    // analyze the file now.
                    VariableFileReaderNodeSettings sWithLoc =
                            userSettings
                                    .createSettingsFrom(getAvailableFlowVariables());
                    FileReaderNodeSettings newSettings =
                            FileAnalyzer.analyze(sWithLoc,
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

                        String varName = userSettings.getVariableName();
                        m_frSettings =
                                new VariableFileReaderNodeSettings(newSettings);
                        m_frSettings.setVariableName(varName);

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
                } catch (FileTokenizerException fte) {
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

        }, threadName).start();

    }

    /*
     * from the current settings it creates a data table and displays it in the
     * preview pane. Will not analyze the file. Will not change things.
     */
    private void updatePreview() {
        // the settings changed - clear error message first
        setErrorLabelText("");

        // update preview
        if ((m_frSettings.getVariableName() == null)
                || (m_frSettings.getVariableName().isEmpty())) {
            // if there is no data file specified display empty table
            setPreviewTable(null);
            showPreviewTable();
            return;
        }
        VariableFileReaderNodeSettings sWithLoc;
        try {
            sWithLoc =
                    m_frSettings
                            .createSettingsFrom(getAvailableFlowVariables());
        } catch (Exception e) {
            setPreviewTable(null);
            showPreviewTable();
            return;
        }

        FileReaderNodeSettings previewSettings =
                createPreviewSettings(sWithLoc);
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

        // set the new table in the view
        m_previewTableView.setDataTable(table);

        // properly dispose of the old table
        if (m_previewTable != null) {
            m_previewTable.removeAllChangeListeners();
            m_previewTable.dispose();
        }

        m_previewTable = table;

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
        VariableFileReaderNodeSettings newSettings =
                new VariableFileReaderNodeSettings();
        newSettings.setDataFileLocationAndUpdateTableName(m_frSettings
                .getDataFileLocation());
        newSettings.setVariableName(m_frSettings.getVariableName());
        m_frSettings = newSettings;
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
    private void saveSettings(final VariableFileReaderNodeSettings settings)
            throws InvalidSettingsException {
        settings.setVariableName((String)m_urlCombo.getSelectedItem());
        try {
            VariableFileReaderNodeSettings tmp =
                    settings.createSettingsFrom(getAvailableFlowVariables());
            settings.setDataFileLocationAndUpdateTableName(tmp
                    .getDataFileLocation());
        } catch (MalformedURLException mfue) {
            throw new InvalidSettingsException("Invalid URL in variable '"
                    + settings.getVariableName() + "'", mfue);
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
            JOptionPane.showConfirmDialog(getPanel(), "Not supported anymore");
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

}
