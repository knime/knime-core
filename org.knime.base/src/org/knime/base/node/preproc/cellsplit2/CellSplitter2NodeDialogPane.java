/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Jun 19, 2007 (ohl): created
 */
package org.knime.base.node.preproc.cellsplit2;

import java.awt.Dimension;
import java.awt.event.ItemListener;
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * The cell splitter node dialog pane.
 * <p>
 * Note: This class replaces the (deprecated) CellSplitterNodeDialogPane.
 * </p>
 *
 * @author ohl, University of Konstanz
 */
final class CellSplitter2NodeDialogPane extends NodeDialogPane {
    @SuppressWarnings("unchecked")
    private final ColumnSelectionComboxBox m_column = new ColumnSelectionComboxBox((Border)null, StringValue.class);

    private final JTextField m_delimiter = new JTextField();

    private final JTextField m_quote = new JTextField();

    private final JSpinner m_columnNumber = new JSpinner();

    private final JRadioButton m_outputAsList = new JRadioButton("As list");

    private final JRadioButton m_outputAsSet = new JRadioButton("As set (remove duplicates)");

    private final JRadioButton m_outputAsColumns = new JRadioButton("As new columns");

    private final JCheckBox m_trim = new JCheckBox("Remove leading and trailing white space chars (trim)");

    private final JRadioButton m_fixedSize = new JRadioButton("Set array size");

    private final JRadioButton m_guessSize =
        new JRadioButton("Guess size and column types (requires additional " + "data table scan)");

    private final JCheckBox m_useEmptyString =
        new JCheckBox("Create empty string cells " + "instead of missing string cells");

    private final JCheckBox m_useEscapeCharacter = new JCheckBox("Use \\ as escape character");

    private final JCheckBox m_hasScanLimit = new JCheckBox("Scan limit (number of lines to guess on) ");

    private final JSpinner m_scanLimit = new JSpinner(new SpinnerNumberModel(25, 1, Integer.MAX_VALUE, 50));

    private final JCheckBox m_splitColumnNames = new JCheckBox("Split input column name for output column names");

    private final JCheckBox m_removeInputColumn = new JCheckBox("Remove input column");

    /**
     * Creates a new panel for the dialog and inits all components.
     */
    CellSplitter2NodeDialogPane() {
        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));

        // the column selection
        m_column.setMaximumSize(new Dimension(300, 25));
        m_column.setMinimumSize(new Dimension(17, 25));
        m_column.setPreferredSize(new Dimension(200, 25));
        Box colSelBox = Box.createHorizontalBox();
        colSelBox.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Column to split"));
        colSelBox.add(Box.createHorizontalGlue());
        colSelBox.add(new JLabel("Select a column:"));
        colSelBox.add(Box.createHorizontalStrut(3));
        colSelBox.add(m_column);
        colSelBox.add(Box.createHorizontalGlue());
        colSelBox.add(m_removeInputColumn);
        colSelBox.add(Box.createHorizontalGlue());

        // settings panel
        Box settingsBox = Box.createVerticalBox();
        settingsBox.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Settings"));
        // - the delimiter
        Box delimBox = Box.createHorizontalBox();
        delimBox.add(new JLabel("Enter a delimiter:"));
        delimBox.add(Box.createHorizontalStrut(3));
        delimBox.add(m_delimiter);
        delimBox.add(Box.createHorizontalStrut(20));
        delimBox.add(m_useEscapeCharacter);
        m_delimiter.setMaximumSize(new Dimension(300, 25));
        m_delimiter.setMinimumSize(new Dimension(17, 25));
        m_delimiter.setPreferredSize(new Dimension(150, 25));
        m_delimiter.setColumns(6);
        delimBox.add(Box.createHorizontalGlue());
        settingsBox.add(delimBox);

        // - the quotes
        final Box quoteBox = Box.createHorizontalBox();
        quoteBox.add(new JLabel("Enter a quotation character:"));
        quoteBox.add(Box.createHorizontalStrut(3));
        quoteBox.add(m_quote);
        m_quote.setMaximumSize(new Dimension(300, 25));
        m_quote.setMinimumSize(new Dimension(30, 25));
        m_quote.setPreferredSize(new Dimension(150, 25));
        m_quote.setColumns(6);
        quoteBox.add(Box.createHorizontalStrut(3));
        quoteBox.add(new JLabel("(leave empty for none.)"));
        quoteBox.add(Box.createHorizontalGlue());
        settingsBox.add(quoteBox);

        // - the trim checkbox
        final Box trimBox = Box.createHorizontalBox();
        trimBox.add(m_trim);
        trimBox.add(Box.createHorizontalGlue());
        settingsBox.add(trimBox);

        // output box
        final Box outputBox = Box.createVerticalBox();
        outputBox.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Output"));

        // - the output group (as list, as set, as columns)
        ButtonGroup obg = new ButtonGroup();
        obg.add(m_outputAsList);
        obg.add(m_outputAsSet);
        obg.add(m_outputAsColumns);
        m_outputAsColumns.setSelected(true);
        m_outputAsColumns.addItemListener(e -> {
            m_fixedSize.setEnabled(m_outputAsColumns.isSelected());
            m_guessSize.setEnabled(m_outputAsColumns.isSelected());
            m_columnNumber.setEnabled(m_outputAsColumns.isSelected() && m_fixedSize.isSelected());
            m_splitColumnNames.setEnabled(m_outputAsColumns.isSelected());

            final boolean enableScanLimit = m_outputAsColumns.isSelected() && m_guessSize.isSelected();
            m_hasScanLimit.setEnabled(enableScanLimit);
            m_scanLimit.setEnabled(enableScanLimit && m_hasScanLimit.isSelected());
        });
        final Box outputColBox = Box.createHorizontalBox();
        outputColBox.add(Box.createVerticalStrut(10));
        outputColBox.add(m_outputAsList);
        outputColBox.add(Box.createHorizontalStrut(5));
        outputColBox.add(m_outputAsSet);
        outputColBox.add(Box.createHorizontalStrut(5));
        outputColBox.add(m_outputAsColumns);
        outputColBox.add(Box.createHorizontalGlue());
        outputBox.add(outputColBox);

        // - the split column names checkbox
        final Box splitBox = Box.createHorizontalBox();
        splitBox.add(m_splitColumnNames);
        splitBox.add(Box.createHorizontalGlue());
        outputBox.add(splitBox);

        // - the size group
        final ButtonGroup bg = new ButtonGroup();
        bg.add(m_fixedSize);
        bg.add(m_guessSize);
        m_fixedSize.setSelected(true);
        m_columnNumber.setEnabled(true);
        final ItemListener listener = e -> {
            m_columnNumber.setEnabled(m_fixedSize.isSelected());

            final boolean enableScanLimit = m_outputAsColumns.isSelected() && m_guessSize.isSelected();
            m_hasScanLimit.setEnabled(enableScanLimit);
            m_scanLimit.setEnabled(enableScanLimit && m_hasScanLimit.isSelected());
        };
        m_fixedSize.addItemListener(listener);
        m_guessSize.addItemListener(listener);

        // the size spinner
        m_columnNumber.setModel(new SpinnerNumberModel(1000, 1, Integer.MAX_VALUE, 1));
        m_columnNumber.setMaximumSize(new Dimension(200, 25));
        m_columnNumber.setMinimumSize(new Dimension(50, 25));
        m_columnNumber.setPreferredSize(new Dimension(100, 25));

        final JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor)m_columnNumber.getEditor();
        editor.getTextField().setColumns(4);
        editor.getTextField().setFocusLostBehavior(JFormattedTextField.COMMIT);

        final Box fixSizeBox = Box.createHorizontalBox();
        fixSizeBox.add(m_fixedSize);
        fixSizeBox.add(Box.createHorizontalStrut(5));
        fixSizeBox.add(m_columnNumber);
        fixSizeBox.add(Box.createHorizontalGlue());

        final Box guessSizeBox = Box.createHorizontalBox();
        guessSizeBox.add(m_guessSize);
        guessSizeBox.add(Box.createHorizontalGlue());

        final Box scanLimitBox = Box.createHorizontalBox();
        scanLimitBox.add(m_hasScanLimit);
        scanLimitBox.add(m_scanLimit);
        m_scanLimit.setEnabled(m_hasScanLimit.isSelected());
        m_hasScanLimit.addItemListener(e -> {
            m_scanLimit.setEnabled(m_hasScanLimit.isSelected());
        });

        final Box sizeBox = Box.createVerticalBox();
        sizeBox.add(fixSizeBox);
        sizeBox.add(Box.createVerticalStrut(3));
        sizeBox.add(guessSizeBox);
        sizeBox.add(scanLimitBox);

        outputBox.add(Box.createVerticalStrut(7));
        outputBox.add(sizeBox);
        outputBox.add(Box.createVerticalGlue());

        // the missing value handling box
        m_useEmptyString
            .setToolTipText("If checked, empty string cells are " + "created for missing or short input cells");
        final Box missValBox = Box.createHorizontalBox();
        missValBox
            .setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Missing Value Handling"));
        missValBox.add(m_useEmptyString);
        missValBox.add(Box.createHorizontalGlue());

        pane.add(colSelBox);
        pane.add(Box.createVerticalStrut(7));
        pane.add(settingsBox);
        pane.add(Box.createVerticalStrut(7));
        pane.add(outputBox);
        pane.add(Box.createVerticalStrut(7));
        pane.add(missValBox);

        addTab("Settings", pane);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        if ((specs == null) || (specs.length == 0) || (specs[0] == null)) {
            throw new NotConfigurableException("Node can't be configured without input table spec");
        }

        CellSplitter2UserSettings csSettings;
        try {
            csSettings = new CellSplitter2UserSettings(settings);
        } catch (InvalidSettingsException ise) {
            // if settings are invalid, create default settings
            csSettings = new CellSplitter2UserSettings();
            csSettings.setDelimiter(",");
            csSettings.setGuessNumOfCols(true);
            csSettings.setNumOfCols(6);
            csSettings.setQuotePattern("\"");
            csSettings.setRemoveQuotes(true);
            csSettings.setUseEmptyString(false);
            csSettings.setUseEscapeCharacter(false);
            csSettings.setOutputAsList(false);
            csSettings.setOutputAsSet(false);
            csSettings.setOutputAsCols(true);
            csSettings.setTrim(true);
            // set the first string column as default column
            for (DataColumnSpec cSpec : specs[0]) {
                if (cSpec.getType().isCompatible(StringValue.class)) {
                    csSettings.setColumnName(cSpec.getName());
                }
            }

        }

        // transfer settings into dialog components:
        m_column.update(specs[0], csSettings.getColumnName());

        if (csSettings.getDelimiter() != null) {
            m_delimiter.setText(csSettings.getDelimiter());
        } else {
            m_delimiter.setText("");
        }
        if (csSettings.getQuotePattern() != null) {
            m_quote.setText(csSettings.getQuotePattern());
        } else {
            m_quote.setText("");
        }
        if (csSettings.getNumOfCols() > 0) {
            ((SpinnerNumberModel)m_columnNumber.getModel()).setValue(csSettings.getNumOfCols());
        } else {
            ((SpinnerNumberModel)m_columnNumber.getModel()).setValue(6);
        }
        if (csSettings.isGuessNumOfCols()) {
            m_guessSize.setSelected(true);
        } else {
            m_fixedSize.setSelected(true);
        }
        m_useEmptyString.setSelected(csSettings.isUseEmptyString());
        m_useEscapeCharacter.setSelected(csSettings.isUseEscapeCharacter());

        if (csSettings.isOutputAsCols()) {
            m_outputAsColumns.setSelected(true);
        } else if (csSettings.isOutputAsList()) {
            m_outputAsList.setSelected(true);
        } else {
            m_outputAsSet.setSelected(true);
        }

        m_trim.setSelected(csSettings.isTrim());

        m_splitColumnNames.setSelected(csSettings.isSplitColumnNames());

        m_hasScanLimit.setSelected(csSettings.hasScanLimit());
        m_scanLimit.setValue(csSettings.scanLimit());

        m_removeInputColumn.setSelected(csSettings.isRemoveInputColumn());
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

        // transfer settings from dialog components into our settings object
        CellSplitter2UserSettings csSettings = new CellSplitter2UserSettings();

        csSettings.setColumnName(m_column.getSelectedColumn());

        // commit the spinner
        try {
            m_columnNumber.commitEdit();
            final Integer numOfCols = ((SpinnerNumberModel)m_columnNumber.getModel()).getNumber().intValue();
            csSettings.setNumOfCols(numOfCols);
        } catch (ParseException pe) {
            if (m_columnNumber.isEnabled()) {
                throw new InvalidSettingsException("Please enter a valid size");
            }
            csSettings.setNumOfCols(-1);
        }

        csSettings.setDelimiter(m_delimiter.getText());

        if (m_quote.getText().isEmpty()) {
            csSettings.setQuotePattern(null); // disable quoting
        } else {
            csSettings.setQuotePattern(m_quote.getText());
        }

        csSettings.setGuessNumOfCols(m_guessSize.isSelected());
        csSettings.setUseEmptyString(m_useEmptyString.isSelected());
        csSettings.setUseEscapeCharacter(m_useEscapeCharacter.isSelected());

        csSettings.setOutputAsList(m_outputAsList.isSelected());
        csSettings.setOutputAsSet(m_outputAsSet.isSelected());
        csSettings.setOutputAsCols(m_outputAsColumns.isSelected());
        csSettings.setTrim(m_trim.isSelected());

        csSettings.setSplitColumnNames(m_splitColumnNames.isSelected());

        csSettings.setHasScanLimit(m_hasScanLimit.isSelected());
        csSettings.setScanLimit(((SpinnerNumberModel)m_scanLimit.getModel()).getNumber().intValue());

        csSettings.setRemoveInputColumn(m_removeInputColumn.isSelected());

        csSettings.saveSettingsTo(settings);

    }
}
