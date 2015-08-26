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
 *   16.10.2014 (tibuch): created
 */
package org.knime.base.node.io.fixedwidthfr;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.ConfigurableDataCellFactory;
import org.knime.core.data.DataCellFactory;
import org.knime.core.data.DataCellFactory.FromSimpleString;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.util.ViewUtils;

/**
 *
 * @author Tim-Oliver Buchholz
 */
final class NewColumnDialog extends JDialog {
    private static final DataType[] TYPES;

    private static final String[] MISSING_VALUE_PATTERNS = new String[]{"<none>", "<whitespace>"};

    static {
        TYPES = DataTypeRegistry.getInstance().availableDataTypes().stream()
            .filter(d -> d.getCellFactory(null).orElse(null) instanceof FromSimpleString)
            .sorted((a, b) -> a.getName().compareTo(b.getName()))
            .toArray(DataType[]::new);
    }

    private JTextField m_colNameField;

    private JTextField m_colWidthField;

    private List<FixedWidthColProperty> m_result;

    private List<FixedWidthColProperty> m_allColumns;

    private int m_colIdx;

    private JComboBox<String> m_missValueChooser;

    private JLabel m_formatParameterLabel;

    private JComboBox<String> m_formatParameterChooser;

    private JComboBox<DataType> m_typeChooser;

    private JCheckBox m_skipColumn;

    private boolean m_hasRowHeader;

    private AbstractButton m_whitespaceAsMVP;

    private JSpinner m_numOfColSpinner;

    private JLabel m_nameLabel;

    private JPanel m_namePanel;

    private NewColumnDialog(final Frame parent, final List<FixedWidthColProperty> allColumns, final int colIdx,
        final boolean hasRowHeader) {

        super(parent, true);

        m_allColumns = allColumns;
        m_colIdx = colIdx;
        m_hasRowHeader = hasRowHeader;
        String name;
        int width;
        DataType type;
        boolean include;
        String mvpSelection;
        boolean adding;

        // set dialog values depending on add or edit
        if (colIdx >= allColumns.size()) {
            // add
            name = "column" + colIdx;
            width = 1;
            type = StringCell.TYPE;
            include = true;
            mvpSelection = MISSING_VALUE_PATTERNS[0];
            adding = true;
        } else {
            // edit
            name = allColumns.get(colIdx).getColSpec().getName();
            width = allColumns.get(colIdx).getColWidth();
            type = allColumns.get(colIdx).getColSpec().getType();
            include = allColumns.get(colIdx).getInclude();
            if (allColumns.get(colIdx).getMissingValuePattern() == null) {
                mvpSelection = MISSING_VALUE_PATTERNS[0];
            } else if (allColumns.get(colIdx).getMissingValuePattern().trim().isEmpty()) {
                mvpSelection = MISSING_VALUE_PATTERNS[1];
            } else {
                mvpSelection = allColumns.get(colIdx).getMissingValuePattern();
            }
            adding = false;
        }

        // instantiate the components of the dialog
        m_skipColumn = new JCheckBox("DON'T include column in output table");
        m_skipColumn.setSelected(!include);

        JPanel skipPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 5));
        skipPanel.add(Box.createHorizontalGlue());
        skipPanel.add(m_skipColumn);
        skipPanel.add(Box.createHorizontalGlue());

        // number of columns goes first
        JPanel numOfColPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 5));
        m_numOfColSpinner = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        m_numOfColSpinner.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                if ((int)m_numOfColSpinner.getValue() != 1) {
                    m_nameLabel.setText("Prefix: ");
                    m_colNameField.setText("prefix");
                } else {
                    m_nameLabel.setText("Name: ");
                    m_colNameField.setText("column" + colIdx);
                }
            }
        });
        JLabel spinnerLabel = new JLabel("Number of Columns to add:");
        numOfColPanel.add(m_numOfColSpinner);

        // column name is next
        m_namePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 5));
        m_colNameField = new JTextField();
        m_colNameField.setText(name);
        m_nameLabel = new JLabel("Name: ");
        m_nameLabel.setPreferredSize(new JLabel("Prefix: ").getPreferredSize());
        m_namePanel.add(m_nameLabel);
        m_namePanel.add(m_colNameField);

        // dimenstions
        Dimension prefDim = m_colNameField.getPreferredSize();

        // column width is next
        JPanel widthPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 5));
        m_colWidthField = new JTextField();
        m_colWidthField.setText(String.valueOf(width));
        widthPanel.add(new JLabel("Width: "));
        widthPanel.add(m_colWidthField);

        // panel for the type is next
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 5));
        typePanel.add(new JLabel("Type: "));
        m_typeChooser = new JComboBox<>(TYPES);
        m_typeChooser.setSelectedItem(type);
        m_typeChooser.addActionListener(e -> typeChanged());
        typePanel.add(m_typeChooser);

        // the missing value components
        JPanel missPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 5));
        missPanel.add(new JLabel("Missing value pattern:"));
        m_missValueChooser = new JComboBox<String>(MISSING_VALUE_PATTERNS);
        prefDim = m_missValueChooser.getPreferredSize();
        m_missValueChooser.setEditable(true);
        m_missValueChooser.setSelectedItem(mvpSelection);
        missPanel.add(m_missValueChooser);

        // the format parameter components
        JPanel formatPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 5));
        m_formatParameterLabel = new JLabel("Format:");
        formatPanel.add(m_formatParameterLabel);
        m_formatParameterChooser = new JComboBox<>();
        m_formatParameterChooser.setEditable(true);
        typeChanged();
        formatPanel.add(m_formatParameterChooser);


        // set size
        m_numOfColSpinner.setPreferredSize(prefDim);
        m_colNameField.setPreferredSize(prefDim);
        m_colWidthField.setPreferredSize(prefDim);
        m_typeChooser.setPreferredSize(prefDim);
        m_missValueChooser.setPreferredSize(prefDim);

        // the OK and Cancel button
        JPanel control = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("OK");
        // add action listener
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                onOK();
            }
        });
        JButton cancel = new JButton("Cancel");
        // add action listener
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                onCancel();
            }
        });
        control.add(ok);
        control.add(cancel);

        // group components nicely - without those buttons

        JPanel dlgPanel = new JPanel(new GridBagLayout());
        dlgPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Column Properties"));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        if (adding) {
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 1;
            JPanel sp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 5));
            sp.add(spinnerLabel);
            dlgPanel.add(sp, c);
            c.gridx = 1;
            c.gridy = 0;
            c.gridwidth = 1;
            // fake some whitespace for a good looking layout
            JLabel l = new JLabel();
            JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 5));
            l.setPreferredSize(missPanel.getComponent(0).getPreferredSize());
            p.add(l);
            dlgPanel.add(p, c);
            // fake end
            c.gridx = 2;
            c.gridwidth = 1;
            dlgPanel.add(numOfColPanel, c);
        }

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        dlgPanel.add(m_namePanel, c);

        c.gridx = 1;
        c.gridwidth = 2;
        dlgPanel.add(widthPanel, c);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        dlgPanel.add(typePanel, c);

        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 2;
        dlgPanel.add(missPanel, c);

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 3;
        c.anchor = GridBagConstraints.WEST;
        dlgPanel.add(formatPanel, c);

        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 3;
        dlgPanel.add(skipPanel, c);

        // add dialog and control panel to the content pane
        Container cont = getContentPane();
        cont.setLayout(new BoxLayout(cont, BoxLayout.Y_AXIS));
        cont.add(dlgPanel);
        cont.add(Box.createVerticalStrut(3));
        cont.add(control);

        setModal(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    private void typeChanged() {
        DataType type = (DataType)m_typeChooser.getSelectedItem();
        m_formatParameterChooser.removeAllItems();
        m_formatParameterLabel.setEnabled(false);
        m_formatParameterChooser.setEnabled(false);
        m_formatParameterLabel.setToolTipText(null);
        m_formatParameterChooser.setToolTipText(null);

        if (type != null) {
            DataCellFactory f = type.getCellFactory(null).orElse(null);
            if (f instanceof ConfigurableDataCellFactory) {
                ConfigurableDataCellFactory cfac = (ConfigurableDataCellFactory)f;

                for (String s : cfac.getPredefinedParameters()) {
                    m_formatParameterChooser.addItem(s);
                }
                if (m_allColumns.size() > m_colIdx) {
                    m_formatParameterChooser.setSelectedItem(m_allColumns.get(m_colIdx).getFormatParameter().orElse(null));
                } else {
                    m_formatParameterChooser.setSelectedItem(null);
                }

                m_formatParameterLabel.setEnabled(true);
                m_formatParameterChooser.setEnabled(true);
                m_formatParameterLabel.setToolTipText(cfac.getParameterDescription());
                m_formatParameterChooser.setToolTipText(cfac.getParameterDescription());
            }
        }
        pack();
    }

    /**
     * Called when user presses the ok button.
     */
    void onOK() {

        String name = m_colNameField.getText();
        String width = m_colWidthField.getText();
        String missingValuePattern;
        int numOfCol = (int)m_numOfColSpinner.getValue();

        DataType type = (DataType)m_typeChooser.getSelectedItem();
        if (checkName(name) && checkWidth(width)) {
            int w = Integer.parseInt(width);
            missingValuePattern = getMissingValuePattern(w);
            if (checkMissingValuePattern(missingValuePattern, w)) {
                m_result = new Vector<FixedWidthColProperty>();
                String colName;
                for (int i = 0; i < numOfCol; i++) {
                    if (numOfCol > 1) {
                        colName = name + i;
                    } else {
                        colName = name;
                    }
                    m_result.add(new FixedWidthColProperty(colName, type, w, !m_skipColumn.isSelected(),
                        missingValuePattern, (String) m_formatParameterChooser.getSelectedItem()));
                }
                shutDown();
            }
        }
    }

    /**
     *
     * @param width the column width
     * @return the missing value pattern
     */
    String getMissingValuePattern(final int width) {
        String result;
        int w = width;

        if (m_missValueChooser.getSelectedIndex() == 0) {
            result = null;
        } else if (m_missValueChooser.getSelectedIndex() == 1) {
            result = "";
            while (w > 0) {
                result += " ";
                w--;
            }
        } else {
            result = m_missValueChooser.getEditor().getItem().toString();
            if (result.isEmpty()) {
                result = null;
            }

        }
        return result;
    }

    /**
     * @param width
     * @return true if width is > 0 and an integer
     */
    private boolean checkWidth(final String width) {
        if (width.length() < 1) {
            JOptionPane.showMessageDialog(this, "Width cannot be empty. " + "Enter valid width or press cancel.",
                "Invalid width", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            if (0 > Integer.parseInt(width)) {
                throw new NumberFormatException();
            }

            return true;
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Width has to be an integer bigger or equal than 0. "
                + "Enter valid width or press cancel.", "Invalid width", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * @param name the string to check
     * @return true if name is not empty and unique
     */
    private boolean checkName(final String name) {

        if (name.length() < 1) {
            JOptionPane.showMessageDialog(this, "Column names cannot be empty. " + "Enter valid name or press cancel.",
                "Invalid column name", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        for (int i = 0; i < m_allColumns.size(); i++) {
            if (i != m_colIdx) {
                if (name.equals(m_allColumns.get(i).getColSpec().getName())) {
                    JOptionPane.showMessageDialog(null,
                        "Column name allready taken. Enter valid name or press cancel.", "Column name taken",
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                if (m_allColumns.get(i).getColSpec().getName().startsWith(name)) {
                    JOptionPane.showMessageDialog(null,
                        "Column prefix allready taken. Enter valid prefix or press cancel.", "Column prefix taken",
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkMissingValuePattern(final String mvp, final int width) {
        if (mvp != null) {
            if (mvp.length() != width) {
                JOptionPane.showMessageDialog(this,
                    "The missing value pattern has to be as long as the column widht. Enter a valid missing value "
                        + "pattern or press cancel.");
                return false;
            }
        }
        return true;
    }

    /**
     * Called when user presses the cancel button or closes the window.
     */
    void onCancel() {
        m_result = new Vector<FixedWidthColProperty>();
        shutDown();
    }

    /* blows away the dialog */
    private void shutDown() {
        setVisible(false);
    }

    private List<FixedWidthColProperty> showDialog() {
        setTitle("Set column properties");
        if (m_hasRowHeader && m_colIdx == 0) {
            m_skipColumn.setEnabled(false);
            m_colNameField.setEnabled(false);
            m_missValueChooser.setEnabled(false);
            m_whitespaceAsMVP.setEnabled(false);
            m_typeChooser.setEnabled(false);
        }
        pack();
        ViewUtils.centerLocation(this, getParent().getBounds());

        setVisible(true);
        return m_result;
    }

    /**
     * @param f the parent
     * @param colIdx the index of the column
     * @param allColProperties the properties of all columns
     * @param hasRowHeader is there a row header
     * @return new column properties or null (on cancel)
     */
    public static List<FixedWidthColProperty> openUserDialog(final Frame f,
        final List<FixedWidthColProperty> allColProperties, final int colIdx, final boolean hasRowHeader) {
        NewColumnDialog colPropDlg = new NewColumnDialog(f, allColProperties, colIdx, hasRowHeader);
        return colPropDlg.showDialog();

    }
}