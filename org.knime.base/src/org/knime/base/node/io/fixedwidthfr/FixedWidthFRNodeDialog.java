/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   15.10.2014 (tibuch): created
 */
package org.knime.base.node.io.fixedwidthfr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.knime.base.node.io.filereader.PreviewTableContentView;
import org.knime.base.node.util.BufferedFileReader;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.tableview.TableRowHeaderView;
import org.knime.core.node.tableview.TableView;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.util.FilesHistoryPanel.LocationValidation;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.FlowVariable.Type;
import org.knime.core.util.FileUtil;

/**
 *
 * @author Tim-Oliver Buchholz
 */
public class FixedWidthFRNodeDialog extends NodeDialogPane {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FixedWidthFRNodeDialog.class);

    // Layout constants

    private static final int COMP_HEIGHT = 30;

    private static final int PANEL_WIDTH = 4000;

    // Column names for column property table
    private static final String[] PROPERTY_COLUMN_NAMES =
        {"Name", "Width", "Type", "Missing Value Pattern", "Included"};

    private static final String FILEREADER_HISTORY_ID = "FixedWidthASCIIfile";

    private final JPanel m_dialogPanel;

    private JPanel m_previewPanel;

    private JPanel m_previewArea;

    private FilesHistoryPanel m_url;

    private JButton m_edit;

    private JButton m_remove;

    private TableView m_previewTableView;

    private FixedWidthFRPreviewTable m_previewTable;

    /*
     * the settings object holding the current state of all settings.
     * The components immediately write their state into this object.
     */
    private FixedWidthFRSettings m_nodeSettings;

    private int m_colIdx;

    private JLabel m_errorLabel;

    private JLabel m_errorDetail;

    private JCheckBox m_hasRowHeaders;

    private JCheckBox m_hasColHeaders;

    private boolean m_loadSettings;

    private DefaultTableModel m_colPropModel;

    private JTable m_colTable;

    private JCheckBox m_preserveSettings;

    private JButton m_add;

    /**
     * Creates a new fixed width file reader node dialog.
     */
    FixedWidthFRNodeDialog() {
        super();

        m_nodeSettings = new FixedWidthFRSettings();

        m_dialogPanel = new JPanel();
        m_dialogPanel.setLayout(new BoxLayout(m_dialogPanel, BoxLayout.Y_AXIS));

        m_dialogPanel.add(Box.createVerticalGlue());
        m_dialogPanel.add(createFileNamePanel());
        m_dialogPanel.add(createColPropertyPanel());
        m_dialogPanel.add(createSettingsPanel());
        m_previewArea = createPreviewArea();
        m_dialogPanel.add(m_previewArea);
        m_dialogPanel.add(Box.createVerticalGlue());
        updateEnables();
        super.addTab("Settings", m_dialogPanel);

    }

    private JPanel createFileNamePanel() {

        JPanel panel = new JPanel();

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
            "Enter ASCII data file location: (press 'Enter' to update preview)"));

        FlowVariableModel flowVarBrowseModel = createFlowVariableModel(FixedWidthFRSettings.CFGKEY_URL, Type.STRING);

        m_url = new FilesHistoryPanel(flowVarBrowseModel, FILEREADER_HISTORY_ID, LocationValidation.FileInput,
            new String[]{".txt", ".csv"});

        m_url.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                if(!m_loadSettings ){
                    try {
                        URL newUrl = FileUtil.toURL(m_url.getSelectedFile());
                        m_nodeSettings.setFileLocation(newUrl);
                    } catch (IOException | InvalidPathException ioe) {
                        m_nodeSettings.setFileLocation(null);
                    }

                    if (!m_preserveSettings.isSelected() && !m_url.isVariableReplacementEnabled()) {
                        m_nodeSettings.reset();
                    }
                    m_hasColHeaders.setSelected(m_nodeSettings.getHasColHeaders());
                    m_hasRowHeaders.setSelected(m_nodeSettings.getHasRowHeader());
                    updateColPropTable();
                    updatePreview();
                    updateEnables();
                }
            }
        });

        // the checkbox for preserving the current settings on file change
        Box preserveBox = Box.createHorizontalBox();
        m_preserveSettings = new JCheckBox("Preserve user settings for new location");
        m_preserveSettings.setToolTipText("if not checked, the settings you"
            + " have set are reset, if a new location is entered");
        m_preserveSettings.setSelected(false);
        m_preserveSettings.setEnabled(true);
        preserveBox.add(Box.createHorizontalGlue());
        preserveBox.add(m_preserveSettings);
        preserveBox.add(Box.createHorizontalGlue());

        panel.add(m_url);
        panel.add(preserveBox);
        panel.setMaximumSize(new Dimension(PANEL_WIDTH, 70));
        panel.setMinimumSize(new Dimension(PANEL_WIDTH, 70));

        return panel;
    }

    private JPanel createColPropertyPanel() {
        m_colTable = new JTable() {
            private final Color m_grey = new Color(125, 125, 125);

            // set font color according to included/excluded
            @Override
            public Component prepareRenderer(final TableCellRenderer renderer, final int row, final int column) {
                Component c = super.prepareRenderer(renderer, row, column);

                if (!(boolean)m_colPropModel.getValueAt(row, 4)) {
                    c.setForeground(m_grey);
                } else {
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        };

        m_colTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            // dis/enable remove button according to row selection
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                updateEnables();
            }
        });

        m_colTable.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(final FocusEvent e) {
                // enable edit and remove button according to row selection
                m_colIdx = m_colTable.getSelectedRow();
                updateEnables();
            }

            @Override
            public void focusLost(final FocusEvent e) {
                // nothing
            }
        });

        updateColPropTable();

        JScrollPane st = new JScrollPane(m_colTable);
        st.setPreferredSize(new Dimension(getPanel().getPreferredSize().width, 150));

        m_edit = new JButton("Edit");
        m_edit.setToolTipText("Change settings of the selected column.");
        // add action listener
        m_edit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                onEdit(m_colTable.getSelectedRow());
            }
        });

        m_add = new JButton("Add");
        m_add.setToolTipText("Add a new column to the table.");
        // add action listener
        m_add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                onAdd();
            }
        });

        m_remove = new JButton("Remove");
        m_remove.setToolTipText("Remove the selected column.");
        // add action listener
        m_remove.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                onRemove(m_colTable.getSelectedRows());
            }
        });

        JPanel listButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        listButtons.add(m_add);
        listButtons.add(m_edit);
        listButtons.add(m_remove);

        // group components nicely
        JPanel dlgPanel = new JPanel();
        dlgPanel.setLayout(new BoxLayout(dlgPanel, BoxLayout.Y_AXIS));
        dlgPanel
            .setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Column configuration"));
        dlgPanel.add(st);
        dlgPanel.add(listButtons);

        return dlgPanel;
    }

    private JPanel createSettingsPanel() {

        m_hasRowHeaders = new JCheckBox("Set first column as row IDs");
        m_hasRowHeaders.setToolTipText("Check to set first column as row id.");
        m_hasColHeaders = new JCheckBox("read column headers");
        m_hasColHeaders.setToolTipText("Check if the file contains column headers in the first line");

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1, 3));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Basic Settings"));

        Box rowBox = Box.createHorizontalBox();
        rowBox.add(m_hasRowHeaders);
        rowBox.add(Box.createGlue());

        Box colBox = Box.createHorizontalBox();
        colBox.add(m_hasColHeaders);
        colBox.add(Box.createGlue());

        // now fill the grid
        panel.add(rowBox);
        panel.add(colBox);
        int componentsHeight = (2 * COMP_HEIGHT) + 50;
        panel.setMaximumSize(new Dimension(PANEL_WIDTH, componentsHeight));

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

        // add listeners
        m_hasRowHeaders.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                rowHeadersSettingsChanged();
            }
        });

        m_hasColHeaders.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                try {
                    setErrorLabelText("");
                    colHeaderSettingsChanged();
                } catch (NoSuchElementException ex) {
                    setErrorLabelText("File is empty. No column headers available.");
                    LOGGER.error("Probably empty file", ex);
                }
            }
        });

        return result;
    }

    private JPanel createPreviewArea() {
        m_previewPanel = createPreviewPanel();
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(m_previewPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * @return jpanel with all the preview components
     */
    private JPanel createPreviewPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Preview"));

        Box tableBox = Box.createHorizontalBox();

        PreviewTableContentView ptcv = new PreviewTableContentView();
        m_previewTableView = new TableView(ptcv);

        tableBox.add(m_previewTableView);

        panel.add(Box.createGlue());
        panel.add(tableBox);

        ptcv.addPropertyChangeListener(PreviewTableContentView.PROPERTY_SPEC_CHANGED, new PropertyChangeListener() {

            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                m_colIdx = (int)evt.getNewValue();
                m_colIdx = m_nodeSettings.getColIdxIncluded(m_colIdx);
                if (m_colIdx < m_nodeSettings.getNumberOfColumns() - 1) {
                    // don't open edit dialog if clicked column equals
                    // remaining characters column
                    onEdit(m_colIdx);
                }
            }
        });

        return panel;
    }

    /**
     * @return data array for the column property table model
     */
    private Object[][] createDataColPropertyTable() {
        int len = m_nodeSettings.getNumberOfColumns() - 1;
        List<FixedWidthColProperty> cols = m_nodeSettings.getColProperties();
        Object[][] result = new Object[len][5];

        for (int i = 0; i < len; i++) {
            FixedWidthColProperty c = cols.get(i);
            result[i][0] = c.getColSpec().getName();
            result[i][1] = c.getColWidth();
            result[i][2] = c.getColSpec().getType().toString();
            result[i][3] = missingValuePatternString(c);
            result[i][4] = c.getInclude();
        }
        return result;
    }

    private String missingValuePatternString(final FixedWidthColProperty c) {
        String mvp = c.getMissingValuePattern();
        if (mvp == null) {
            return "<none>";
        } else if (mvp.trim().isEmpty()) {
            return "<whitespace>";
        } else {
            return "<" + mvp + ">";
        }
    }

    /**
     * save new column header checkbox settings into nodeSettings object and update dialog.
     */
    protected void colHeaderSettingsChanged() {
        if (!m_loadSettings) {
            boolean selected = m_hasColHeaders.isSelected();
            if (selected && m_nodeSettings.getFileLocation() != null) {
                // set with column names used - for faster uniquification
                Set<String> colNames = new HashSet<String>();
                try (BufferedFileReader fr = m_nodeSettings.createNewInputReader()) {

                    FixedWidthTokenizer tokenizer = new FixedWidthTokenizer(fr, m_nodeSettings);

                    if (tokenizer.nextToken() != null) {
                        if (!m_nodeSettings.getHasRowHeader()) {
                            tokenizer.pushBack();
                        }
                    } else {
                        throw new NoSuchElementException("The row iterator proceeded beyond the last line of '"
                            + m_nodeSettings.getFileLocation().toString() + "'. File is probably empty.");
                    }

                    DataColumnSpecCreator c;
                    String name = null;

                    for (int i = 0; i < m_nodeSettings.getNumberOfIncludedColumns() - 1; i++) {
                        String prefix = tokenizer.nextToken();
                        name = prefix;
                        int count = 2;
                        while (colNames.contains(name)) {
                            name = prefix + "(" + count++ + ")";
                        }
                        colNames.add(name);
                        if (!tokenizer.getReachedEndOfLine() && name != null) {

                            int includedIdx = m_nodeSettings.getColIdxIncluded(i);
                            c =
                                new DataColumnSpecCreator(name, m_nodeSettings.getColPropertyAt(includedIdx)
                                    .getColSpec().getType());
                            m_nodeSettings.getColPropertyAt(includedIdx).setColSpec(c.createSpec());
                        }

                    }

                    m_nodeSettings.setHasColHeader(selected);
                } catch (IOException e) {
                    setErrorLabelText("Could not create new tokenizer to read column headers.", e.getMessage());
                }
            } else {
                m_nodeSettings.setHasColHeader(selected);
            }

            updateColPropTable();
            updateEnables();
            updatePreview();
        }

    }

    /**
     * save new row header checkbox settings into nodeSettings object and update dialog.
     */
    protected void rowHeadersSettingsChanged() {
        if (!m_loadSettings) {
            boolean selected = m_hasRowHeaders.isSelected();
            m_nodeSettings.setHasRowHeader(selected);
            m_nodeSettings.getColPropertyAt(0).setInclude(!selected);
            if (selected) {
                m_nodeSettings.getColPropertyAt(0).setMissingValuePattern(null);
            }

            updateColPropTable();

            updateEnables();
            updatePreview();
        }

    }

    /**
     * Updates the column property table.
     */
    protected void updateColPropTable() {
        m_colPropModel = new DefaultTableModel(createDataColPropertyTable(), PROPERTY_COLUMN_NAMES) {
            @Override
            public boolean isCellEditable(final int row, final int column) {
                //all cells false
                return false;
            }
        };
        m_colTable.setModel(m_colPropModel);
    }

    /**
     * @param i the index of the selected column
     *
     */
    protected void onEdit(final int i) {

        List<FixedWidthColProperty> editColumn =
            NewColumnDialog.openUserDialog(getFrame(), m_nodeSettings.getColProperties(), i,
                m_nodeSettings.getHasRowHeader());
        if (!editColumn.isEmpty()) {
            if (editColumn.size() > 1) {
                LOGGER.error("Had more than one column edited.");
            }
            // not really an edit. we replace the old column with the new one.
            m_nodeSettings.removeColAt(i);
            m_nodeSettings.insertNewColAt(editColumn.get(0), i);

            m_colPropModel.removeRow(i);
            m_colPropModel.insertRow(i, createRow(editColumn.get(0)));
            m_colTable.setModel(m_colPropModel);
            m_colTable.setRowSelectionInterval(i, i);

            updateEnables();
            updatePreview();
        }
    }

    /**
     * @param colProp the column properties
     * @return a new row containing name, width, type, missing value pattern, included
     */
    private Object[] createRow(final FixedWidthColProperty colProp) {

        return new Object[]{colProp.getColSpec().getName(), colProp.getColWidth(), colProp.getColSpec().getType(),
            missingValuePatternString(colProp), colProp.getInclude()};
    }

    /**
     * @param rows the index of the selected column
     */
    protected void onRemove(final int[] rows) {

        for (int i = rows.length - 1; i >= 0; i--) {
            if (!(m_nodeSettings.getHasRowHeader() && rows[i] == 0)) {
                m_nodeSettings.removeColAt(rows[i]);
                m_colPropModel.removeRow(rows[i]);
            }
        }
        if (m_colTable.getRowCount() > 0) {
            int r = Math.max(0, rows[0] - 1);
            m_colTable.setRowSelectionInterval(r, r);
        }
        m_nodeSettings.getNumberOfColumns();
        updateEnables();
        updatePreview();
    }

    /**
     * Adds a new column at the last position or after the selected one.
     */
    protected void onAdd() {
        // new columns are inserted after the selected one or if none is selected at the end
        int insertAt;
        if (m_colTable.getSelectedRow() > -1) {
            insertAt = m_colTable.getSelectedRow() + 1;
        } else {
            insertAt = m_colTable.getRowCount();
        }

        List<FixedWidthColProperty> newColumns =
            NewColumnDialog.openUserDialog(getFrame(), m_nodeSettings.getColProperties(),
                m_nodeSettings.getNumberOfColumns(), m_nodeSettings.getHasRowHeader());
        if (!newColumns.isEmpty()) {
            for (int i = 0; i < newColumns.size(); i++) {
                m_nodeSettings.insertNewColAt(newColumns.get(i), insertAt + i);

                m_colPropModel.insertRow(insertAt + i, createRow(newColumns.get(i)));
            }
            m_nodeSettings.getNumberOfColumns();
            m_colTable.setModel(m_colPropModel);
            m_colTable.setRowSelectionInterval(insertAt + newColumns.size() - 1, insertAt + newColumns.size() - 1);
            updateEnables();
            updatePreview();
        }
    }

    /**
     * @return the parent frame
     */
    protected Frame getFrame() {
        Frame f = null;
        Container c = getPanel().getParent();
        while (c != null) {
            if (c instanceof Frame) {
                f = (Frame)c;
                break;
            }
            c = c.getParent();
        }
        return f;
    }

    /**
     * updates the enable status of all dialog settings.
     */
    private void updateEnables() {
        // row/column header checkboxes
        if (m_nodeSettings.getFileLocation() != null && m_colTable.getRowCount() != 0) {
            // we can only set a column/row header if we have a file and at least one row
            m_hasColHeaders.setEnabled(true);
            m_hasRowHeaders.setEnabled(true);
        } else {
            m_hasColHeaders.setEnabled(false);
            m_hasRowHeaders.setEnabled(false);
        }

        // add button
        if (m_nodeSettings.getFileLocation() == null) {
            m_add.setEnabled(false);
        } else {
            m_add.setEnabled(true);
        }

        // edit/remove buttons
        if (m_colTable.getSelectedRow() > -1) {
            if (m_colTable.getSelectedRows().length > 1) {
                m_remove.setEnabled(true);
                m_add.setEnabled(false);
                m_edit.setEnabled(false);
            } else {
                // we need at least one row if we want to edit something
                m_edit.setEnabled(true);
                // the user can remove all columns which he created but not the 'remaining characters' column
                if (m_nodeSettings.getHasRowHeader() && m_colTable.getSelectedRow() == 0) {
                    // if the first column is the row id column, we can't delete this column
                    m_remove.setEnabled(false);
                } else {
                    m_remove.setEnabled(true);
                }
            }
        } else {
            m_remove.setEnabled(false);
            m_edit.setEnabled(false);
        }
    }

    /*
     * places the preview table component in it designated panel
     */
    private void showPreviewTable() {
        ViewUtils.runOrInvokeLaterInEDT(new Runnable() {
            @Override
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

    private void updatePreview() {
        setErrorLabelText("");
        if (validateSettings()) {

            FixedWidthFRSettings previewSettings = new FixedWidthFRSettings(m_nodeSettings);

            DataTableSpec tSpec = previewSettings.createDataTableSpec();

            FixedWidthFRPreviewTable newTable = new FixedWidthFRPreviewTable(tSpec, previewSettings, null);

            setPreviewTable(newTable);
            showPreviewTable();
        }
    }

    /**
     * @return true if settings are valid
     */
    private boolean validateSettings() {
        try {
            URL fileLocation = m_nodeSettings.getFileLocation();
            String warning = CheckUtils.checkSourceFile(fileLocation != null ? fileLocation.toString() : null);
            if (warning != null) {
                setErrorLabelText(warning);
                return false;
            }
            m_nodeSettings.createNewInputReader();
        } catch (InvalidSettingsException e) {
            setErrorLabelText(e.getMessage());
            return false;
        } catch (IOException ioe) {
            setErrorLabelText("Can't create input reader for preview table.", ioe.getMessage());
            return false;
        } catch (NullPointerException npe) {
            //
        }
        return true;
    }

    /**
     * Updates the preview view with the specified table. Updates the member variable. Disposes of the old table.
     *
     * @param newTable the new table to store and to display
     */
    private void setPreviewTable(final FixedWidthFRPreviewTable newTable) {

        final FixedWidthFRPreviewTable oldTable = m_previewTable;

        // register a listener for error messages with the new table
        if (newTable != null) {
            newTable.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(final ChangeEvent e) {
                    if (m_previewTable != null) {
                        setErrorLabelText(m_previewTable.getErrorMsg(), m_previewTable.getErrorDetail());
                    }
                }
            });
        }

        // set this - even before displaying it
        m_previewTable = newTable;

        ViewUtils.invokeLaterInEDT(new Runnable() {

            @Override
            public void run() {
                // set the new table in the view
                m_previewTableView.setDataTable(newTable);
                if (newTable != null) {
                    final TableColumn column = m_previewTableView.getHeaderTable().getColumnModel().getColumn(0);
                    // bug fix 4418 and 4903 -- the row header column does not have a good width on windows.
                    // (due to some SWT_AWT bridging)
                    ViewUtils.invokeLaterInEDT(new Runnable() {
                        @Override
                        public void run() {
                            final int width = 75;
                            column.setMinWidth(75);
                            TableRowHeaderView headerTable = m_previewTableView.getHeaderTable();
                            Dimension newSize = new Dimension(width, 0);
                            headerTable.setPreferredScrollableViewportSize(newSize);
                        }
                    });
                }
                // properly dispose of the old table
                if (oldTable != null) {
                    oldTable.close();
                }
            }
        });

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_nodeSettings.saveToConfiguration(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_loadSettings = true;

        try {
            // no settings if we create the node
            m_nodeSettings = new FixedWidthFRSettings(settings);
        } catch (InvalidSettingsException ice) {
            m_nodeSettings = new FixedWidthFRSettings();
        }

        m_hasRowHeaders.setSelected(m_nodeSettings.getHasRowHeader());
        m_hasColHeaders.setSelected(m_nodeSettings.getHasColHeaders());

        if (m_nodeSettings.getNumberOfColumns() > 0) {

            updateColPropTable();
        }

        setErrorLabelText("");

        if (m_nodeSettings.getFileLocation() != null) {
            m_url.setSelectedFile(m_nodeSettings.getFileLocation().toString());
            updateEnables();
            updatePreview();
        } else {
            m_url.setSelectedFile(null);
        }
        m_preserveSettings.setSelected(false);
        m_loadSettings = false;
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
}
