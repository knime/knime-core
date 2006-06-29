/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   23.03.2005 (ohl): created
 */
package de.unikn.knime.base.node.io.filereader;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Insets;
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
import java.net.MalformedURLException;
import java.net.URL;
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
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import de.unikn.knime.base.node.io.filetokenizer.Comment;
import de.unikn.knime.base.node.io.filetokenizer.Delimiter;
import de.unikn.knime.base.node.io.filetokenizer.FileTokenizerSettings;
import de.unikn.knime.core.data.DataColumnSpec;
import de.unikn.knime.core.data.DataColumnSpecCreator;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.DoubleValue;
import de.unikn.knime.core.data.def.StringCell;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeDialogPane;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.NotConfigurableException;
import de.unikn.knime.core.node.tableview.TableView;

/**
 * 
 * @author ohl, University of Konstanz
 */
class FileReaderNodeDialog extends NodeDialogPane {

    private static final int HORIZ_SPACE = 10;

    private static final int COMP_HEIGHT = 30;

    private static final int PANEL_WIDTH = 5000;

    private static final Delimiter[] DEFAULT_DELIMS = new Delimiter[]{
            // the <none> MUST be the first one!!!
            new Delimiter("<none>", false, false, false),
            new Delimiter(",", false, false, false),
            new Delimiter(" ", false, false, false),
            new Delimiter("\t", false, false, false),
            new Delimiter(";", false, false, false)};

    /*
     * the settings object holding the current state of all settings. The
     * components immediately write their state into this object. There is a
     * load function transfering the settings from this object into the
     * component.
     */
    private FileReaderNodeSettings m_frSettings;

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

    /* flag to break recusrion */
    private boolean m_insideLoadDelim;

    private boolean m_insideDelimChange;

    private boolean m_insideLoadComment;

    private boolean m_insideCommentChange;

    private boolean m_insideWSChange;

    private boolean m_insideLoadWS;

    private JPanel m_dialogPanel;

    private JCheckBox m_readPosValues;

    private JCheckBox m_ignoreWS;

    // the dialog stores the previous WSs, because they can be deleted with
    // only one click - boom gone.
    private Vector<String> m_prevWhiteSpaces;

    private JLabel m_errorLabel;
    
    private JLabel m_analyzeWarn; 

    private FileReaderPreviewTable m_previewTable;

    /**
     * Creates a new file reader dialog pain.
     */
    FileReaderNodeDialog() {
        super("ASCII Data File Reader");
        m_frSettings = new FileReaderNodeSettings();
        m_insideLoadDelim = false;
        m_insideDelimChange = false;
        m_insideLoadComment = false;
        m_insideCommentChange = false;

        m_prevWhiteSpaces = null;

        m_dialogPanel = new JPanel();
        m_dialogPanel.setLayout(new BoxLayout(m_dialogPanel, BoxLayout.Y_AXIS));

        m_dialogPanel.add(Box.createVerticalGlue());

        m_dialogPanel.add(createFileNamePanel());
        m_dialogPanel.add(createSettingsPanel());
        m_dialogPanel.add(createPreviewPanel());

        m_dialogPanel.add(Box.createVerticalGlue());
        super.addTab("Settings", m_dialogPanel);
    }

    private JPanel createFileNamePanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(),
                "Enter ASCII data file location: (press 'Enter' to update "
                        + "preview)"));

        Box fileBox = Box.createHorizontalBox();

        // Creating the brows button here in order to get its preferred height
        JButton browse = new JButton("Browse...");
        int buttonHeight = browse.getPreferredSize().height;

        m_urlCombo = new JComboBox();
        m_urlCombo.setEditable(true);
        m_urlCombo.setRenderer(new MyComboBoxRenderer());
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
        fileBox.add(Box.createVerticalStrut(70));
        fileBox.add(Box.createHorizontalGlue());

        panel.add(fileBox);
        panel.setMaximumSize(new Dimension(PANEL_WIDTH, 70));
        panel.setMinimumSize(new Dimension(PANEL_WIDTH, 70));
        m_urlCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                analyzeDataFileAndUpdatePreview(false);
            }
        });
        /* install action listeners */
        // set stuff to update preview when file location changes
        m_urlCombo.getEditor().addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                analyzeDataFileAndUpdatePreview(false);
            }
        });
        m_urlCombo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
                // analyze file on focus lost.
                analyzeDataFileAndUpdatePreview(false);
            }
        });
        Component editor = m_urlCombo.getEditor().getEditorComponent();
        if (editor instanceof JTextComponent) {
            Document d = ((JTextComponent)editor).getDocument();
            d.addDocumentListener(new DocumentListener() {
                public void changedUpdate(final DocumentEvent e) {
                    m_previewTableView.setDataTable(null);
                }

                public void insertUpdate(final DocumentEvent e) {
                    m_previewTableView.setDataTable(null);
                }

                public void removeUpdate(final DocumentEvent e) {
                    m_previewTableView.setDataTable(null);
                }
            });
        }

        browse.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                // sets the path in the file text field.
                String newFile = popupFileChooser(m_urlCombo.getSelectedItem()
                        .toString(), false);
                if (newFile != null) {
                    m_urlCombo.setSelectedItem(newFile);
                    analyzeDataFileAndUpdatePreview(false);
                }
            }
        });
        return panel;
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
        m_hasRowHeaders = new JCheckBox("read row headers");
        m_hasRowHeaders.setToolTipText("Check if the file contains row headers"
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
        m_readPosValues = new JCheckBox("read all poss. values");
        m_readPosValues.setVisible(false); // supposed to always read values!
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
        pValBox.add(m_readPosValues);
        pValBox.add(Box.createGlue());
        Box cCmtBox = Box.createHorizontalBox();
        cCmtBox.add(m_cStyleComment);
        cCmtBox.add(Box.createGlue());
        Box slcBox = Box.createHorizontalBox();
        slcBox.add(commentLabel);
        slcBox.add(Box.createHorizontalStrut(3));
        slcBox.add(m_singleLineComment);
        slcBox.add(Box.createGlue());
        // now fill the grid
        // first row
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
        m_readPosValues.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                readPosValuesChanged();
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
        JPanel errorBox = new JPanel();
        errorBox.setLayout(new BoxLayout(errorBox, BoxLayout.X_AXIS));
        errorBox.add(Box.createHorizontalGlue());
        errorBox.add(m_errorLabel);
        // reserve a certain height for the (in the beginning invisible) label
        errorBox.add(Box.createVerticalStrut(25));
        errorBox.add(Box.createHorizontalGlue());
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));
        result.add(panel);
        result.add(errorBox);
        return result;
    }

    /**
     * reads the settings of the 'fileHasRowHeaders' checkbox and transfers them
     * into the internal settings object.
     */
    protected void rowHeadersSettingsChanged() {

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
                DataColumnSpec firstColSpec = new DataColumnSpecCreator("Col0",
                        StringCell.TYPE).createSpec();
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
            m_frSettings
                    .setNumberOfColumns(m_frSettings.getNumberOfColumns() - 1);
            Vector<ColProperty> colProps = m_frSettings.getColumnProperties();
            // save the first colProp in case user changes his mind...
            m_firstColProp = colProps.remove(0);
            m_frSettings.setColumnProperties(colProps);
            if (!m_frSettings.getFileHasColumnHeaders()) {
                // re-generate the column names
                recreateColNames(false);
            }
            analyzeDataFileAndUpdatePreview(true);
        }
    }

    /**
     * reads the settings of the 'fileHasColHeaders' checkbox and transfers them
     * into the internal settings object.
     */
    protected void colHeadersSettingsChanged() {
        m_frSettings.setFileHasColumnHeadersUserSet(true);
        m_frSettings.setFileHasColumnHeaders(m_hasColHeaders.isSelected());
        // recreate artificial names if file has no col headers
        if (!m_frSettings.getFileHasColumnHeaders()) {
            recreateColNames(false);
        }
        analyzeDataFileAndUpdatePreview(true);

    }

    /**
     * called whenever the state of the "read possible Values" checkbox changes.
     * Transfers the current state into the global settings object. Sets the
     * readRange flag for double and int cols, and the readPossValues flag for
     * String columns.
     */
    protected void readPosValuesChanged() {

        boolean readPosVals = m_readPosValues.isSelected();
        m_readPosValues.setText("read all poss. values");

        for (int c = 0; c < m_frSettings.getNumberOfColumns(); c++) {
            ColProperty cProp = m_frSettings.getColumnProperties()
                    .get(c);

            if (cProp.getColumnSpec().getType().equals(StringCell.TYPE)) {
                // read nominal values for string columns
                cProp.setReadPossibleValuesFromFile(readPosVals);
                cProp.setMaxNumberOfPossibleValues(2000);
                cProp.setReadBoundsFromFile(false);
            } else if (cProp.getColumnSpec().getType().isCompatible(
                    DoubleValue.class)) {
                // read ranges for numerical cells (this should cover IntCells)
                cProp.setReadPossibleValuesFromFile(false);
                cProp.setReadBoundsFromFile(readPosVals);
            } else {
                // Int, Double, and StringCells is all the FileReader supports.
                assert false : "Unsupported column type";
                cProp.setReadPossibleValuesFromFile(false);
                cProp.setReadBoundsFromFile(false);
            }
        }

    }

    /**
     * the item changed listener to the 'ignore whitespaces' check box.
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
     * loads the settings from the global settings object into the panel's
     * checkbox.
     * 
     */
    private void loadReadPosValuesSettings() {

        // count the number of cols we should read vals or range for
        int readPosValsCols = 0;
        int userSpecified = 0;

        for (int c = 0; c < m_frSettings.getNumberOfColumns(); c++) {
            ColProperty cProp = m_frSettings.getColumnProperties().get(c);
            if (cProp.getReadPossibleValuesFromFile()
                    || cProp.getReadBoundsFromFile()) {
                readPosValsCols++;
            }
            if (cProp.getUserSettings()) {
                userSpecified++;
            }
        }
        // now set the checkbox accordingly
        m_readPosValues.setText("read all poss. values");
        if (readPosValsCols == 0) {
            m_readPosValues.setSelected(false);
        } else if (readPosValsCols == m_frSettings.getNumberOfColumns()) {
            m_readPosValues.setSelected(true);
        } else {
            m_readPosValues.setSelected(true);
            m_readPosValues.setText("read values of some cols");
        }

        // as soon as we have some user specified settings, we disable this box
        if (userSpecified > 0) {
            m_readPosValues.setEnabled(false);
            m_readPosValues.setToolTipText("Disable due to user domain "
                    + "settings. Click on preview header to change.");
        } else {
            m_readPosValues.setEnabled(true);
            m_readPosValues.setToolTipText("Check to analyze the entire file "
                    + "for possible values and ranges of all attributes");
        }

    }

    /**
     * reads the settings of the column delimiter box and transfers them into
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

        // remove all delimiters except row delimiters
        for (Delimiter delim : m_frSettings.getAllDelimiters()) {
            if (m_frSettings.isRowDelimiter(delim.getDelimiter())) {
                continue;
            }
            m_frSettings.removeDelimiterPattern(delim.getDelimiter());
        }

        // now set the selected one
        String delimStr = null;
        if (m_delimField.getSelectedIndex() > -1) {
            if (m_delimField.getSelectedIndex() == 0) {
                // user selected the <none> delimiter
                delimStr = null;
            } else {
                delimStr = ((Delimiter)m_delimField.getSelectedItem())
                        .getDelimiter();
            }
        } else {
            delimStr = (String)m_delimField.getSelectedItem();
            delimStr = FileTokenizerSettings.unescapeString(delimStr);
        }
        if ((delimStr != null) && (!delimStr.equals(""))) {
            try {
                m_frSettings.addDelimiterPattern(delimStr, false, false, false);
            } catch (IllegalArgumentException iae) {
                setErrorLabelText(iae.getMessage());
                m_insideDelimChange = false;
                return;
            }
        }

        analyzeDataFileAndUpdatePreview(true); // force re-analyze
        m_insideDelimChange = false;
    }

    /**
     * loads the settings from the global settings object into the delimiter box
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
     * called whenever the Java-Style comment box is clickered.
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

        Vector<ColProperty> newColProps = ColPropertyDialog.openUserDialog(f,
                colIdx, cProps);

        if (newColProps != null) {
            // user pressed okay for new settings
            m_frSettings.setColumnProperties(newColProps);
            loadReadPosValuesSettings();
            updatePreview();
        }

    }

    /**
     * @see NodeDialogPane#loadSettingsFrom(NodeSettings,DataTableSpec[])
     */
    @Override
    protected void loadSettingsFrom(final NodeSettings settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        assert (settings != null && specs != null);

        String[] history = FileReaderNodeModel.getFileHistory();
        m_urlCombo.removeAllItems();
        for (String str : history) {
            m_urlCombo.addItem(str);
        }
        try {
            // this will fail if the settings are invalid (which will be the
            // case when they come from an uninitialized model). We create
            // an empty settings object in the catch block.
            m_frSettings = new FileReaderNodeSettings(settings);
        } catch (InvalidSettingsException ice) {
            m_frSettings = new FileReaderNodeSettings();
        }

        /*
         * This is a hack to speed up test flows. This will allow for setting a
         * default data file location in the reader dialog. With this the only
         * action in the file reader dialog is clicking okay (assuming the
         * default settings of the analyzer are okay). This is a hack because
         * settings that only contain the file name are not valid. So we accept
         * a part of invalid settings here. It's really not good practice - but
         * may help testing.
         */
        try {
            URL dataFileLocation = new URL(settings
                    .getString(FileReaderSettings.CFGKEY_DATAURL));
            m_frSettings
                    .setDataFileLocationAndUpdateTableName(dataFileLocation);
        } catch (MalformedURLException mfue) {
            // don't set the data location if it bombs
        } catch (InvalidSettingsException ice) {
            // don't set the data location if it bombs
        }
        /* end of hack */

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

        updatePreview();
    }

    /**
     * @see NodeDialogPane#saveSettingsTo(NodeSettings)
     */
    @Override
    protected void saveSettingsTo(final NodeSettings settings)
            throws InvalidSettingsException {
        saveSettings();
        if (m_previewTable.getErrorOccured()) {
            throw new InvalidSettingsException("With the current settings"
                    + " an error occures when reading the file (line "
                    + m_previewTable.getErrorLine() + "): "
                    + m_previewTable.getErrorMsg());
        }
        m_frSettings.saveToConfiguration(settings);

    }

    /**
     * updates the preview table, if a new and valid URL was specified in the
     * data file name textfield. It will override all current settings with the
     * settings from the file analyzer and display the data file contents with
     * these new settings. It will do this only when a new and valid URL is set;
     * if its invalid it will just clear the preview leaving the settings
     * unchanged, and if the URL is the same than in the global settings object
     * it will not (re)analyze the data file (and thus not change settings).
     * Unless the parameter forceAnalyze is set true.
     * 
     * @param forceAnalyze forces the analysis of the datafile eventhough it
     *            might be the one set in the global settings (and thus already
     *            being analyzed).
     *            <p>
     *            NOTE: May change the global settings object completely.
     * 
     */
    protected void analyzeDataFileAndUpdatePreview(final boolean forceAnalyze) {

        URL newURL;

        // clear preview first.
        m_previewTableView.setDataTable(null);

        try {
            newURL = textToURL(m_urlCombo.getSelectedItem().toString());
        } catch (Exception e) {
            // leave settings unchanged.
            setErrorLabelText("Malformed URL '"
                    + m_urlCombo.getSelectedItem() + "'.");
            return;
        }

        boolean displayAnalWarning = false;

        if (forceAnalyze 
                || !newURL.equals(m_frSettings.getDataFileLocation())) {

            // get new settings from the analyzer

            if (!newURL.equals(m_frSettings.getDataFileLocation())) {
                // start from scratch
                FileReaderNodeSettings newFRNS = new FileReaderNodeSettings();
                newFRNS.setDataFileLocationAndUpdateTableName(newURL);
                try {
                    m_frSettings = FileAnalyzer.analyze(newFRNS);
                    displayAnalWarning = !m_frSettings.analyzeUsedAllRows();
                } catch (IOException ioe) {
                    setErrorLabelText("Can't access '" + newURL + "'");
                    return;
                }
            } else {
                // keep the old user settings - just blow away generated names
                // and number of cols.
                Vector<ColProperty> oldColProps = m_frSettings
                        .getColumnProperties();

                // prepare the settings object for re-analysis
                m_frSettings.setNumberOfColumns(-1);
                Vector<ColProperty> newProps = new Vector<ColProperty>();
                if (oldColProps != null) {
                    for (ColProperty cProp : oldColProps) {
                        // take over only the ones modified by the user
                        if (cProp.getUserSettings()) {
                            newProps.add(cProp);
                        } else {
                            newProps.add(null);
                        }
                    }
                }
                m_frSettings.setColumnProperties(newProps);
                m_frSettings.setDataFileLocationAndUpdateTableName(newURL);
                try {
                    m_frSettings = FileAnalyzer.analyze(m_frSettings);
                    displayAnalWarning = !m_frSettings.analyzeUsedAllRows();
                } catch (IOException ioe) {
                    m_frSettings.setColumnProperties(oldColProps);
                    setErrorLabelText("Can't access '" + newURL + "'");
                    return;
                }
            }
        }

        loadSettings(false);

        updatePreview();

        // updatePreview clears the analWarning
        if (displayAnalWarning) {
            setAnalWarningText("WARNING: suggested settings are based on "
                    + "a partial file analysis only! Please verify.");
        }
    }

    /*
     * from the current settings it creates a data table and displays it in the
     * preview pane. Will not analyze the file. Will not change things. 
     */
    private void updatePreview() {
        // something in the settings changed - clear warning
        setAnalWarningText("");
        setErrorLabelText("");
        
        // update preview
        if ((m_frSettings.getDataFileLocation() == null)
                || (m_frSettings.getDataFileLocation().equals(""))) {
            // if there is no data file specified display empty table
            m_previewTableView.setDataTable(null);
            return;
        }
        SettingsStatus status = m_frSettings.getStatusOfSettings(true, null);
        if (status.getNumOfErrors() > 0) {
            setErrorLabelText(status.getErrorMessage(0));
            m_previewTableView.setDataTable(null);
            return;
        }
        DataTableSpec tSpec = m_frSettings.createDataTableSpec();
        tSpec = modifyPreviewColNames(tSpec);
        m_previewTable = new FileReaderPreviewTable(tSpec, m_frSettings);
        m_previewTable.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                setErrorLabelText(m_previewTable.getErrorMsg());
            }
        });
        m_previewTableView.setDataTable(m_previewTable);
    }

    /*
     * returns a new table spec object with the same content - just different
     * column names. If the current settings have the 'user settings' flag set
     * true for a column a asterisk is appended to the column name.
     */
    private DataTableSpec modifyPreviewColNames(final DataTableSpec tSpec) {
        assert tSpec != null;
        if (tSpec == null) {
            return null;
        }

        int numCols = tSpec.getNumColumns();
        DataColumnSpec[] colSpecs = new DataColumnSpec[numCols];
        for (int c = 0; c < numCols; c++) {
            DataColumnSpec cSpec = tSpec.getColumnSpec(c);
            String name;
            boolean userSet = m_frSettings.getColumnProperties().get(c)
                    .getUserSettings();
            if (userSet) {
                name = cSpec.getName().toString() + "*";
            } else {
                name = cSpec.getName().toString();
            }
            DataColumnSpecCreator dcsc = 
                new DataColumnSpecCreator(name, cSpec.getType());
            dcsc.setDomain(cSpec.getDomain());
            DataColumnSpec newSpec = dcsc.createSpec();
            colSpecs[c] = newSpec;
        }

        return new DataTableSpec(colSpecs);

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
        m_hasRowHeaders.setSelected(m_frSettings.getFileHasRowHeaders());
        m_hasColHeaders.setSelected(m_frSettings.getFileHasColumnHeaders());
        // dis/enable the select recent files button
        loadDelimSettings();
        loadCommentSettings();
        loadReadPosValuesSettings();
        loadWhiteSpaceSettings();

    }

    /*
     * transfers the components settings into the FileNodeSettings object.
     * Actually, as all components immediately set their state in the object,
     * the only thing left is the file name from the data file location text
     * field.
     */
    private void saveSettings() throws InvalidSettingsException {
        try {
            URL dataURL = textToURL(m_urlCombo.getSelectedItem().toString());
            m_frSettings.setDataFileLocationAndUpdateTableName(dataURL);
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

        for (int c = 0; c < colProps.size(); c++) {
            ColProperty cProp = colProps.get(c);
            if ((cProp.getUserSettings())
                    && (!((c == 0) && uniquifyFirstColName))) {
                // name was set by user - consider it fixed.
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
            boolean unique = false;
            String name = namePrefix;
            int count = 1;
            while (!unique) {
                unique = true;
                for (int comp = 0; comp < colProps.size(); comp++) {
                    // compare it with all user set columns til the end, because
                    // we can't change the fixed column names.
                    if (comp == c) {
                        // don't compare it with it's old name. Gonna change.
                        continue;
                    }
                    ColProperty compProp = colProps.get(comp);
                    if (!compProp.getUserSettings()) {
                        // don't compare to generated headers - gonna change.
                        continue;
                    }
                    String compName = compProp.getColumnSpec().getName();
                    if (compName.equals(name)) {
                        unique = false;
                        count++;
                        name = namePrefix + "(" + count + ")";
                        break; // start all over again
                    }
                }
            }
            cProp.changeColumnName(name);
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
        FileReaderAdvancedDialog advDlg = new FileReaderAdvancedDialog(f,
                m_frSettings);
        advDlg.setModal(true);
        advDlg.setVisible(true);
        // will not continue until user closes the dialog

        // first check if user closed via the readXML button
        if (advDlg.closedViaReadXML()) {
            readXMLSettings();
            analyzeDataFileAndUpdatePreview(false); // don't reanalyze
        } else if (advDlg.closedViaOk()) {
            advDlg.overrideSettings(m_frSettings);
            analyzeDataFileAndUpdatePreview(true); // re-analyze
        }
        advDlg.dispose();
    }

    /**
     * pops up a file chooser dialog and reads the settings fromt the selected
     * xml file.
     */
    protected void readXMLSettings() {
        String xmlPath = popupFileChooser(m_urlCombo.getSelectedItem()
                .toString(), true);

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
        updatePreview();
    }

    /**
     * pops up the file selection dialog and returns the path to the selected
     * file - or null if the user canceled.
     * 
     * @param startingPath the path the dialog starts in.
     * @param readXml if true the filter will be set to '*.xml' files
     * @return the path to the selected file, or null if user canceled.
     */
    protected String popupFileChooser(final String startingPath,
            final boolean readXml) {
        // before opening the dialog, try to figure out a nice starting dir
        String tryThis = startingPath;
        if ((tryThis == null) || (tryThis.length() == 0)) {
            // if we didn't get anything, use the first path in the
            // file history (if we got any) - should be better than nothing
            String[] hist = FileReaderNodeModel.getFileHistory();
            if ((hist != null) && (hist.length > 0)) {
                tryThis = hist[0];
            }
        }
        String startingDir = "";
        try {
            URL newURL = textToURL(tryThis);
            if (newURL.getProtocol().equals("file")) {
                File tmpFile = new File(newURL.getPath());
                startingDir = tmpFile.getAbsolutePath();
            }
        } catch (Exception e) {
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
                path = chooser.getSelectedFile().getAbsoluteFile().toURL()
                        .toString();
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
            newURL = tmp.getAbsoluteFile().toURL();
        }
        return newURL;
    }

    private void setErrorLabelText(final String text) {
        m_errorLabel.setText(text);
        getPanel().invalidate();
        getPanel().validate();
    }
    
    private void setAnalWarningText(final String text) {
        m_analyzeWarn.setText(text);
        getPanel().invalidate();
        getPanel().validate();
        getPanel().repaint();
    }
    
    /** Renderer that also supports to show customized tooltip. */
    private static class MyComboBoxRenderer extends BasicComboBoxRenderer {

        /**
         * @see BasicComboBoxRenderer#getListCellRendererComponent(
         *      javax.swing.JList, java.lang.Object, int, boolean, boolean)
         */
        @Override
        public Component getListCellRendererComponent(final JList list,
                final Object value, final int index, final boolean isSelected,
                final boolean cellHasFocus) {
            if (index > -1) {
                list.setToolTipText(value.toString());
            }
            return super.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);
        }

        /**
         * Does the clipping automatically, clips off characters from the middle
         * of the string.
         * 
         * @see JLabel#getText()
         */
        @Override
        public String getText() {
            Insets ins = getInsets();
            int width = getWidth() - ins.left - ins.right;
            String s = super.getText();
            FontMetrics fm = getFontMetrics(getFont());
            String clipped = s;
            while (clipped.length() > 5 && fm.stringWidth(clipped) > width) {
                clipped = format(s, clipped.length() - 3);
            }
            return clipped;
        }

        /**
         * builds strings with the following pattern: if size is smaller than
         * 30, return the last 30 chars in the string; if the size is larger
         * than 30: return the first 12 chars + ... + chars from the end. Size
         * more than 55: the first 28 + ... + rest from the end.
         */
        private String format(final String str, final int size) {
            String result;
            if (str.length() <= size) {
                // short enough - return it unchanged
                return str;
            }
            if (size <= 30) {
                result = "..."
                        + str.substring(str.length() - size + 3, str.length());
            } else if (size <= 55) {
                result = str.substring(0, 12)
                        + "..."
                        + str.subSequence(str.length() - size + 15, str
                                .length());
            } else {
                result = str.substring(0, 28)
                        + "..."
                        + str.subSequence(str.length() - size + 31, str
                                .length());
            }
            return result;
        }
    }

}
