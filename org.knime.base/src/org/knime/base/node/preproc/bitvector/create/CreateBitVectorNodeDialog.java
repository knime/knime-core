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
 * -------------------------------------------------------------------
 *
 * History
 *   06.12.2005 (dill): created
 */
package org.knime.base.node.preproc.bitvector.create;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.preproc.bitvector.create.CreateBitVectorNodeModel.ColumnType;
import org.knime.base.util.WildcardMatcher;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.vector.bitvector.BitVectorType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.DataValueColumnFilter;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;


/**
 * The dialog for the Create Bit Vector node. Simply provides a threshold above
 * all incoming values are presented with a bit set to one, items below that
 * threshold are presented as a bit set to zero.
 *
 * @author Tobias Koetter
 */
public class CreateBitVectorNodeDialog extends NodeDialogPane {

    private static final Insets INSET = new Insets(0, 25, 0, 0);
    private static final Insets NEUTRAL_INSET = new Insets(0, 0, 0, 0);

    private JLabel m_regexErrText;

    private final SettingsModelString m_columnType = CreateBitVectorNodeModel.createColumnTypeModel();

    private final DialogComponentButtonGroup m_columnTypeComp = new DialogComponentButtonGroup(m_columnType,
        null, true, CreateBitVectorNodeModel.ColumnType.values());

    private final SettingsModelString m_singleColumn = CreateBitVectorNodeModel.createSingleColumnModel();
    @SuppressWarnings("unchecked")
    private DialogComponentColumnNameSelection m_singleColumnComp = new DialogComponentColumnNameSelection(
        m_singleColumn, "Single column to be parsed", 0, false, StringValue.class);

    private final SettingsModelString m_singleStringColumnType =
            CreateBitVectorNodeModel.createSingleStringColumnTypeModel();
    private DialogComponentStringSelection m_stringStringColumnTypeComp = new DialogComponentStringSelection(
        m_singleStringColumnType, "Kind of string representation: ",
        CreateBitVectorNodeModel.StringType.getStringValues());

    private final SettingsModelBoolean m_useMean = CreateBitVectorNodeModel.createUseMeanModel();
    private final DialogComponentBoolean m_useMeanComp =
            new DialogComponentBoolean(m_useMean, "Use percentage of the mean");

    private final SettingsModelInteger m_meanPercentage = CreateBitVectorNodeModel.createMeanPercentageModel();
    private final DialogComponentNumber m_meanPercentageComp =
            new DialogComponentNumber(m_meanPercentage, "Percentage:", 10);

    private final SettingsModelDouble m_threshold = CreateBitVectorNodeModel.createThresholdModel();
    private final DialogComponentNumber m_thresholdComp = new DialogComponentNumber(m_threshold, "Threshold:", 0.1, 6);

    private final DialogComponentBoolean m_removeBoxComp = new DialogComponentBoolean(
        CreateBitVectorNodeModel.createRemoveColumnsModel(), "Remove column(s) used for bit vector creation");

    private final DialogComponentString m_outputColumnComp =
            new DialogComponentString(CreateBitVectorNodeModel.createOutputColumnModel(), "Output column: ", true, 10);
    private final DialogComponentBoolean m_failOnErrorComp =
            new DialogComponentBoolean(CreateBitVectorNodeModel.createFailOnErrorModel(), "Fail on invalid input");
    private final DialogComponentButtonGroup m_vectorTypeComp = new DialogComponentButtonGroup(
        CreateBitVectorNodeModel.createVectorTypeModel(), null, true,
        BitVectorType.values());

    private DataColumnSpecFilterConfiguration m_multiColumnsConfig =
            CreateBitVectorNodeModel.createMultiColumnConfig(null);
    private final DataColumnSpecFilterPanel m_multiColumnsPanel;
    private final SettingsModelString m_mscPattern = CreateBitVectorNodeModel.createMSCPattern();
    private final DialogComponentString m_mscPatternComp =
            new DialogComponentString(m_mscPattern, "Pattern:", true, 30);
    private final SettingsModelString m_mscSetMatching = CreateBitVectorNodeModel.createMSCSetMatchingModel();
    private final DialogComponentButtonGroup m_mscSetMatchingComp = new DialogComponentButtonGroup(
        m_mscSetMatching, null, true, CreateBitVectorNodeModel.SetMatching.values());
    private JLabel m_mscSetMatchingLabel = new JLabel("Set bit if pattern ");
    private final SettingsModelBoolean m_mscRegex = CreateBitVectorNodeModel.createMSCRegexModel();
    private final DialogComponentBoolean m_mscRegexComp = new DialogComponentBoolean(m_mscRegex, "Regular expression");
    private final SettingsModelBoolean m_mscHasWildcards = CreateBitVectorNodeModel.createMSCHasWildcardsModel();
    private final DialogComponentBoolean m_mscHasWildcardsComp =
            new DialogComponentBoolean(m_mscHasWildcards, "Contains wildcards");
    private final SettingsModelBoolean m_mscCaseSensitive = CreateBitVectorNodeModel.createMSCCaseSensitiveModel();
    private final DialogComponentBoolean m_mscCaseSensitiveComp =
            new DialogComponentBoolean(m_mscCaseSensitive, "Case sensitive match");

    /**
     * Creates an instance of the BitVectorGeneratorNodeDialog, containing an
     * adjustable threshold. All values above or equal to that threshold are
     * represented by a bit set to 1.
     */
    CreateBitVectorNodeDialog() {
        super();
        final ColumnType initialType =
                CreateBitVectorNodeModel.ColumnType.getType(m_columnType.getStringValue());
        m_multiColumnsPanel  =  new DataColumnSpecFilterPanel();
        m_multiColumnsPanel.setEnabled(initialType.isMultiColumn());
        m_mscSetMatchingLabel.setEnabled(CreateBitVectorNodeModel.ColumnType.MULTI_STRING.equals(initialType));
        m_regexErrText = new JLabel();
        setErrMsg("");
        m_regexErrText.setForeground(Color.RED);
        m_mscHasWildcardsComp.setToolTipText(
            "insert '?' or '*' to match any one character or any sequence (including none) of characters.");
        m_columnType.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                final ColumnType type =
                        CreateBitVectorNodeModel.ColumnType.getType(m_columnType.getStringValue());
//multi column options
                m_multiColumnsPanel.setEnabled(type.isMultiColumn());
                //numerical
                m_useMean.setEnabled(CreateBitVectorNodeModel.ColumnType.MULTI_NUMERICAL.equals(type));
                m_threshold.setEnabled(!m_useMean.getBooleanValue()
                    && CreateBitVectorNodeModel.ColumnType.MULTI_NUMERICAL.equals(type));
                m_meanPercentage.setEnabled(m_useMean.getBooleanValue()
                    && CreateBitVectorNodeModel.ColumnType.MULTI_NUMERICAL.equals(type));
                //string
                m_mscPattern.setEnabled(CreateBitVectorNodeModel.ColumnType.MULTI_STRING.equals(type));
                m_mscSetMatchingLabel.setEnabled(CreateBitVectorNodeModel.ColumnType.MULTI_STRING.equals(type));
                m_mscSetMatching.setEnabled(CreateBitVectorNodeModel.ColumnType.MULTI_STRING.equals(type));
                m_mscRegex.setEnabled(CreateBitVectorNodeModel.ColumnType.MULTI_STRING.equals(type));
                m_mscHasWildcards.setEnabled(CreateBitVectorNodeModel.ColumnType.MULTI_STRING.equals(type));
                m_mscCaseSensitive.setEnabled(CreateBitVectorNodeModel.ColumnType.MULTI_STRING.equals(type));


//single column options
                m_singleColumn.setEnabled(!type.isMultiColumn());
                //string
                if (CreateBitVectorNodeModel.ColumnType.SINGLE_STRING.equals(type)) {
                    regExprChanged();
                }
                m_singleStringColumnType.setEnabled(CreateBitVectorNodeModel.ColumnType.SINGLE_STRING.equals(type));

//update the corresponding columns fields
                if (type.isMultiColumn()) {
                    m_multiColumnsConfig = CreateBitVectorNodeModel.createMultiColumnConfig(type);
                    m_multiColumnsPanel.updateWithNewConfiguration(m_multiColumnsConfig);
                } else {
                    try {
                        m_singleColumnComp.setColumnFilter(new DataValueColumnFilter(type.getSupportedClasses()));
                    } catch (NotConfigurableException e1) {
                        throw new IllegalStateException(e1.getMessage());
                    }
                }
            }
        });

        m_mscHasWildcards.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                wildRegExprChanged(e);
                regExprChanged();
            }
        });
        m_mscRegex.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                wildRegExprChanged(e);
                regExprChanged();
            }
        });
        m_mscPattern.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                regExprChanged();
            }
        });

        m_useMean.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_meanPercentage.setEnabled(m_useMean.getBooleanValue());
                m_threshold.setEnabled(!m_useMeanComp.isSelected());
            }
        });

        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        panel.add(createMultiColumnPanel(), c);
        c.gridy++;
        c.weighty = 0;
        panel.add(createSingleColumnPanel(), c);
        c.gridy++;
        panel.add(createGeneralOptionPanel(), c);
        addTab("Default Settings", panel);
    }

    /**
     * Called when the 'is regular expression' or 'has wildcards' checkbox was
     * clicked. Ensures only one of them is checked.
     *
     * @param e the event flying
     */
    private void wildRegExprChanged(final ChangeEvent e) {
        if ((e.getSource() == m_mscRegex) && m_mscRegex.getBooleanValue()) {
            m_mscHasWildcards.setBooleanValue(false);
        }
        if ((e.getSource() == m_mscHasWildcards) && m_mscHasWildcards.getBooleanValue()) {
            m_mscRegex.setBooleanValue(false);
        }
    }
    /**
     * Checks the entered (or selected) regular expression and sets an error.
     */
    private void regExprChanged() {
        setErrMsg("");
        if (!m_mscPattern.isEnabled()) {
            return;
        }
        String pattern = m_mscPattern.getStringValue();
        if (pattern == null || pattern.length() <= 0) {
            setErrMsg("Enter valid pattern");
            getPanel().validate();
            return;
        }
        try {
            if (m_mscHasWildcards.getBooleanValue()) {
                pattern = WildcardMatcher.wildcardToRegex(pattern);
                Pattern.compile(pattern);
            } else if (m_mscRegex.getBooleanValue()) {
                Pattern.compile(pattern);
            }
        } catch (PatternSyntaxException pse) {
            setErrMsg("Error in pattern. ('" + pse.getMessage() + "')");
            getPanel().validate();
        }
    }

    private void setErrMsg(final String msg) {
        String htmlMsg = msg.replaceAll("(\r\n|\n)", "<br/>");
        htmlMsg = htmlMsg.replaceAll("(\\s+)", " ");
        m_regexErrText.setText("<html>" + htmlMsg + "</html>");
        m_regexErrText.setToolTipText(msg);
    }

    private JPanel createMultiColumnPanel() {
        final JPanel stringPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        c.gridy = 0;
        c.gridx = 0;
        stringPanel.add(m_columnTypeComp.getButton(
            CreateBitVectorNodeModel.ColumnType.MULTI_STRING.getActionCommand()), c);
        c.gridwidth = 4;
        c.gridx++;
        c.weightx = 1;
        stringPanel.add(m_regexErrText, c);
        c.weightx = 0;

        c.gridy++;
        c.gridwidth = 1;
        c.gridx = 0;
        c.insets = INSET;
        stringPanel.add(m_mscPatternComp.getComponentPanel(), c);
        c.insets = NEUTRAL_INSET;
        c.gridx++;
        stringPanel.add(m_mscHasWildcardsComp.getComponentPanel(), c);
        c.gridx++;
        c.gridheight = 2;
        stringPanel.add(m_mscSetMatchingLabel , c);
        c.gridx++;
        stringPanel.add(m_mscSetMatchingComp.getComponentPanel(), c);
        c.gridx++;
        c.weightx = 1;
        stringPanel.add(Box.createGlue(), c);
        c.weightx = 0;
        c.gridheight = 1;

        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_END;
        c.gridx = 0;
        c.insets = INSET;
        stringPanel.add(m_mscCaseSensitiveComp.getComponentPanel(), c);
        c.insets = NEUTRAL_INSET;
        c.gridx++;
        c.anchor = GridBagConstraints.LINE_START;
        stringPanel.add(m_mscRegexComp.getComponentPanel(), c);
        //we do not need any more elements since the set matching have hight 2

        final JPanel numericPanel = new JPanel(new GridBagLayout());
        c = new GridBagConstraints();
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 4;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        numericPanel.add(m_columnTypeComp.getButton(
            CreateBitVectorNodeModel.ColumnType.MULTI_NUMERICAL.getActionCommand()), c);
        c.weightx = 0;
        c.gridwidth = 1;
        c.gridx = 0;

        c.gridy++;
        c.insets = INSET;
        numericPanel.add(m_thresholdComp.getComponentPanel(), c);
        c.insets = NEUTRAL_INSET;
        c.gridx++;
        numericPanel.add(m_useMeanComp.getComponentPanel(), c);
        c.gridx++;
        numericPanel.add(m_meanPercentageComp.getComponentPanel(), c);
        c.gridx++;
        c.weightx = 1;
        numericPanel.add(Box.createGlue(), c);

        final JPanel rootPanel = new JPanel(new GridBagLayout());
        rootPanel.setBorder(BorderFactory.createTitledBorder("Bit vectors from multiple columns"));
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        rootPanel.add(stringPanel, c);

        c.gridy++;
        rootPanel.add(numericPanel, c);
        c.gridy++;
        c.weighty = 1;
        rootPanel.add(m_multiColumnsPanel, c);
        return rootPanel;
    }

    private JPanel createSingleColumnPanel() {
        final JPanel stringPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        c.gridy = 0;
        c.gridx = 0;
        c.gridwidth = 2;
        stringPanel.add(
            m_columnTypeComp.getButton(CreateBitVectorNodeModel.ColumnType.SINGLE_STRING.getActionCommand()), c);
        c.gridy++;
        c.insets = INSET;
        stringPanel.add(m_stringStringColumnTypeComp.getComponentPanel(), c);
        c.gridx++;
        c.weightx = 1;
        stringPanel.add(Box.createGlue(), c);

        final JPanel collectionPanel = new JPanel(new GridBagLayout());
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        c.gridy = 0;
        c.gridx = 0;
        collectionPanel.add(
            m_columnTypeComp.getButton(CreateBitVectorNodeModel.ColumnType.SINGLE_COLLECTION.getActionCommand()),
            c);
        c.weightx = 1;
        c.gridx++;
        collectionPanel.add(Box.createGlue(), c);

        final JPanel singleColPanel = new JPanel(new GridBagLayout());
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        c.gridy = 0;
        c.gridx = 0;
        singleColPanel.add(m_singleColumnComp.getComponentPanel(), c);
        c.weightx = 1;
        c.gridx++;
        singleColPanel.add(Box.createGlue(), c);

        final JPanel rootPanel = new JPanel(new GridBagLayout());
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 1;
        rootPanel.setBorder(BorderFactory.createTitledBorder("Bit vectors from a single column"));
        rootPanel.add(stringPanel, c);
        c.gridy++;
        rootPanel.add(collectionPanel, c);
        c.gridy++;
        rootPanel.add(singleColPanel, c);
        return rootPanel;
    }

    private JPanel createGeneralOptionPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("General"));
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        panel.add(m_removeBoxComp.getComponentPanel(), c);
        c.gridx++;
        panel.add(m_outputColumnComp.getComponentPanel(), c);
        c.gridx++;
        panel.add(m_failOnErrorComp.getComponentPanel(), c);
        c.gridx++;
        c.insets = new Insets(0, 10, 0, 0);
        panel.add(new JLabel("Bit vector type:"), c);
        c.insets = NEUTRAL_INSET;
        c.gridx++;
        panel.add(m_vectorTypeComp.getComponentPanel(), c);
        c.gridx++;
        c.weightx = 1;
        panel.add(Box.createGlue(), c);
        return panel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        if ((specs[0] == null) || (specs[0].getNumColumns() < 1)) {
            throw new NotConfigurableException("Need DataTableSpec at input "
                    + "port. Connect node and/or execute predecessor");
        }
        m_multiColumnsConfig.loadConfigurationInDialog(settings, specs[0]);
        m_multiColumnsPanel.loadConfiguration(m_multiColumnsConfig, specs[0]);

        m_mscPatternComp.loadSettingsFrom(settings, specs);
        m_mscHasWildcardsComp.loadSettingsFrom(settings, specs);
        m_mscCaseSensitiveComp.loadSettingsFrom(settings, specs);
        m_mscRegexComp.loadSettingsFrom(settings, specs);
        m_mscSetMatchingComp.loadSettingsFrom(settings, specs);

        m_thresholdComp.loadSettingsFrom(settings, specs);
        m_useMeanComp.loadSettingsFrom(settings, specs);
        m_meanPercentageComp.loadSettingsFrom(settings, specs);

        m_singleColumnComp.loadSettingsFrom(settings, specs);
        m_stringStringColumnTypeComp.loadSettingsFrom(settings, specs);

        m_removeBoxComp.loadSettingsFrom(settings, specs);
        m_outputColumnComp.loadSettingsFrom(settings, specs);
        m_failOnErrorComp.loadSettingsFrom(settings, specs);
        m_vectorTypeComp.loadSettingsFrom(settings, specs);
        //load the column type last since it triggers the update of the other components
        m_columnTypeComp.loadSettingsFrom(settings, specs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_columnTypeComp.saveSettingsTo(settings);

        m_multiColumnsPanel.saveConfiguration(m_multiColumnsConfig);
        m_mscPatternComp.saveSettingsTo(settings);
        m_mscHasWildcardsComp.saveSettingsTo(settings);
        m_mscCaseSensitiveComp.saveSettingsTo(settings);
        m_mscRegexComp.saveSettingsTo(settings);
        m_mscSetMatchingComp.saveSettingsTo(settings);

        m_multiColumnsConfig.saveConfiguration(settings);
        m_thresholdComp.saveSettingsTo(settings);
        m_useMeanComp.saveSettingsTo(settings);
        m_meanPercentageComp.saveSettingsTo(settings);

        m_singleColumnComp.saveSettingsTo(settings);
        m_stringStringColumnTypeComp.saveSettingsTo(settings);


        m_removeBoxComp.saveSettingsTo(settings);
        m_outputColumnComp.saveSettingsTo(settings);
        m_failOnErrorComp.saveSettingsTo(settings);
        m_vectorTypeComp.saveSettingsTo(settings);
    }
}
