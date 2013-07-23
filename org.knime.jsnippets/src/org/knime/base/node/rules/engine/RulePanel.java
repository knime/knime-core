/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * Created on 2013.04.25. by Gabor
 */
package org.knime.base.node.rules.engine;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.Gutter;
import org.knime.base.node.rules.engine.Rule.OutcomeKind;
import org.knime.base.node.util.KnimeCompletionProvider;
import org.knime.base.node.util.KnimeSyntaxTextArea;
import org.knime.base.node.util.ManipulatorProvider;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.FlowVariableModelButton;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.ThreadUtils;

/**
 * A Rule panel with a {@link RuleMainPanel} with further controls for output.
 *
 * @author Gabor Bakos
 * @since 2.8
 */
@SuppressWarnings({"serial"})
abstract class RulePanel extends JPanel {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(RulePanel.class);

    private final boolean m_hasOutputColumn;

    private ButtonGroup m_inclusionButtonGroup;

    private JRadioButton m_includeButton;

    private JRadioButton m_excludeButton;

    private final String m_inclusionLabel;

    private final String m_exclusionLabel;

    private final boolean m_hasDefaultOutcome;

    private ButtonGroup m_defaultLabelTypeGroup;

    private JRadioButton m_defaultIsText;

    private JRadioButton m_defaultIsColumnReference;

    private JRadioButton m_defaultIsFlowVariable;

    @SuppressWarnings("unused")
    ///To be used when the string interpolation gets supported.
    private JRadioButton m_defaultIsStringInterpolation;

    private final EnumMap<Rule.OutcomeKind, JRadioButton> m_outcomeControls =
            new EnumMap<Rule.OutcomeKind, JRadioButton>(Rule.OutcomeKind.class);

    private JComboBox/*<Object>*/m_flowVarOrColumn;

    private JLabel m_outputType;

    private JTextField m_newColumnName;

    private JTextField m_defaultLabelEditor;

    private volatile long m_outputTypeLastSet, m_outputMarkersLastSet;

    //    private JTextComponent m_lastUsedTextComponent;
    //
    private RuleMainPanel m_mainPanel;

    private DataTableSpec m_spec;

    private static final ExecutorService threadPool = ThreadUtils.executorServiceWithContext(new ThreadPoolExecutor(1,
        4, 4, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(16)));

    private Map<String, FlowVariable> m_flowVariables;

    private FlowVariableModel m_fvmDefaultLabel;

    private RuleParser m_parser;

    private final boolean m_showColumns;

    /**
     * Constructs the {@link RulePanel}. It shows the columns too.
     *
     * @param hasOutputColumn Should we create a control for output column?
     * @param hasDefaultOutcome The default outcome can be specified?
     * @param warnOnColRefsInStrings Whether warn if the outcomes contain $ signs.
     * @param inclusion The inclusion text for filter (matches go to the first outport).
     * @param exclusion The exclusion text for filter (matches go to the second (possibly non-existing) outport).
     * @param manipulatorProvider The {@link ManipulatorProvider} to use.
     * @param completionProvider The {@link CompletionProvider} to use.
     */
    public RulePanel(final boolean hasOutputColumn, final boolean hasDefaultOutcome,
                     final boolean warnOnColRefsInStrings, final String inclusion, final String exclusion,
                     final ManipulatorProvider manipulatorProvider, final KnimeCompletionProvider completionProvider) {
        this(hasOutputColumn, hasDefaultOutcome, warnOnColRefsInStrings, true/*showColumns*/, inclusion, exclusion,
                manipulatorProvider, completionProvider);
    }

    /**
     * Constructs the {@link RulePanel}.
     *
     * @param hasOutputColumn Should we create a control for output column?
     * @param hasDefaultOutcome The default outcome can be specified?
     * @param warnOnColRefsInStrings Whether warn if the outcomes contain $ signs.
     * @param showColumns Show the columns panel or hide it.
     * @param inclusion The inclusion text for filter (matches go to the first outport).
     * @param exclusion The exclusion text for filter (matches go to the second (possibly non-existing) outport).
     * @param manipulatorProvider The {@link ManipulatorProvider} to use.
     * @param completionProvider The {@link CompletionProvider} to use.
     */
    public RulePanel(final boolean hasOutputColumn, final boolean hasDefaultOutcome,
                     final boolean warnOnColRefsInStrings, final boolean showColumns, final String inclusion,
                     final String exclusion, final ManipulatorProvider manipulatorProvider,
                     final KnimeCompletionProvider completionProvider) {
        super(new BorderLayout());
        this.m_showColumns = showColumns;
        m_parser = new RuleParser(warnOnColRefsInStrings, !hasDefaultOutcome, showColumns);
        m_parser.setDataTableSpec(m_spec);

        this.m_hasOutputColumn = hasOutputColumn;
        this.m_hasDefaultOutcome = hasDefaultOutcome;
        this.m_inclusionLabel = inclusion;
        this.m_exclusionLabel = exclusion;
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(m_mainPanel = new RuleMainPanel(manipulatorProvider, completionProvider, showColumns), BorderLayout.CENTER);
        m_mainPanel.getTextEditor().addParser(m_parser);
        //        m_mainPanel.getTextEditor().addFocusListener(new FocusAdapter() {
        //            /**
        //             * {@inheritDoc}
        //             */
        //            @Override
        //            public void focusGained(final FocusEvent e) {
        //                m_lastUsedTextComponent = m_mainPanel.getTextEditor();
        //            }
        //        });
        gbc.gridy = 1;
        gbc.weighty = 0;
        add(createOutputControls(), BorderLayout.SOUTH/*gbc*/);
        m_mainPanel.getTextEditor().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(final DocumentEvent e) {
                onAnyChange();
            }

            @Override
            public void insertUpdate(final DocumentEvent e) {
                onAnyChange();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                onAnyChange();
            }

            private void onAnyChange() {
                updateOutputMarkers();
                updateOutputType();
            }
        });
    }

    /**
     * @return The output controls. Either the column name with default value selection controls or the kind of
     *         filtering to do.
     */
    private JPanel createOutputControls() {
        JPanel ret = new JPanel();
        ret.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        if (m_hasOutputColumn) {
            constraints.weighty = .25;
            ret.add(createNewColumnTextFieldWithLabel("prediction", 35, m_showColumns ? "Appended column name"
                            : "New flow variable name"), constraints);
        } else {
            m_inclusionButtonGroup = new ButtonGroup();
            m_includeButton = new JRadioButton(m_inclusionLabel);
            m_excludeButton = new JRadioButton(m_exclusionLabel);
            m_inclusionButtonGroup.add(m_includeButton);
            m_inclusionButtonGroup.add(m_excludeButton);
            ret.add(m_includeButton, constraints);
            constraints.gridx = 1;
            ret.add(m_excludeButton, constraints);
        }
        if (m_hasDefaultOutcome) {
            constraints.gridy = 1;
            constraints.weighty = .75;
            final GridBagLayout gbl = new GridBagLayout();
            JPanel outcomeBox = new JPanel(gbl);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            outcomeBox.add(createDefaultTextFieldWithLabel("default", 35, "Default label (if no rule matches)"), gbc);
            m_defaultLabelTypeGroup = new ButtonGroup();
            m_defaultIsText = new JRadioButton("is plain text");
            m_defaultIsColumnReference = new JRadioButton("is a column reference");
            m_defaultIsFlowVariable = new JRadioButton("is a flow variable");
            m_defaultIsStringInterpolation = new JRadioButton("is an expression");//not used yet
            m_outcomeControls.put(Rule.OutcomeKind.PlainText, m_defaultIsText);
            m_outcomeControls.put(Rule.OutcomeKind.Column, m_defaultIsColumnReference);
            m_outcomeControls.put(Rule.OutcomeKind.FlowVariable, m_defaultIsFlowVariable);
            //            outcomeControls.put(OutcomeKind.StringInterpolation, m_defaultIsStringInterpolation);
            m_defaultLabelTypeGroup.add(m_defaultIsText);
            m_defaultLabelTypeGroup.add(m_defaultIsColumnReference);
            m_defaultLabelTypeGroup.add(m_defaultIsFlowVariable);
            //            m_defaultLabelTypeGroup.add(m_defaultIsStringInterpolation);
            gbc.gridy = 0;
            gbc.gridx = 1;
            outcomeBox.add(m_defaultIsText, gbc);
            gbc.gridy = 1;
            gbc.gridx = 0;
            gbc.gridheight = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            outcomeBox.add(m_flowVarOrColumn = new JComboBox/*<Object>*/(), gbc);
            gbc.gridx = 1;
            gbc.gridy = 1;
            gbc.gridheight = 1;
            outcomeBox.add(m_defaultIsColumnReference, gbc);
            gbc.gridy = 2;
            outcomeBox.add(m_defaultIsFlowVariable, gbc);
            m_defaultIsText
                    .setToolTipText("The outcome's default value is not interpreted besides number or text, results as is.");
            m_defaultIsColumnReference.setToolTipText("The outcome's default value is from this column.");
            m_defaultIsFlowVariable.setToolTipText("The outcome's default value is from this flow variable.");
            final ItemListener radioItemListener = new ItemListener() {
                @Override
                public void itemStateChanged(final ItemEvent e) {
                    updateOutputType();
                    adjustOutputControls();
                }
            };
            m_flowVarOrColumn.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(final ItemEvent e) {
                    updateOutputType();
                }
            });
            m_defaultIsText.getModel().addItemListener(radioItemListener);
            m_defaultIsColumnReference.getModel().addItemListener(radioItemListener);
            m_defaultIsFlowVariable.getModel().addItemListener(radioItemListener);
            ret.add(outcomeBox, constraints);
        }
        return ret;
    }

    /**
     * @param watermark The text to show when nothing entered.
     * @param colWidth The width of text field.
     * @param label The explanation text for the field.
     * @return The output column name {@link JTextField} with a {@link JLabel}.
     */
    private Component createNewColumnTextFieldWithLabel(final String watermark, final int colWidth, final String label) {
        Box ret = Box.createHorizontalBox();
        if (label != null) {
            ret.add(new JLabel(label));
        }
        JTextField comp = Util.createTextFieldWithWatermark(watermark, colWidth, label);
        m_newColumnName = comp;
        ret.add(comp);
        m_outputType = new JLabel(DataValue.UTILITY.getIcon());
        ret.add(m_outputType);
        return ret;
    }

    /**
     * @param watermark The watermark to show when the textfield is empty.
     * @param colWidth The width of the textfield.
     * @param label The tooltip and the label before the textfield.
     * @return The {@link JTextField} with a {@link JLabel} to used for the default label.
     */
    private Component createDefaultTextFieldWithLabel(final String watermark, final int colWidth, final String label) {
        Box ret = Box.createHorizontalBox();
        if (label != null) {
            ret.add(new JLabel(label));
        }
        JTextField comp = Util.createTextFieldWithWatermark(watermark, colWidth, label);
        m_defaultLabelEditor = comp;
        //                m_defaultLabelEditor.addFocusListener(new FocusAdapter() {
        //                    @Override
        //                    public void focusGained(final FocusEvent e) {
        //                        m_lastUsedTextComponent = m_defaultLabelEditor;
        //                    }
        //                });
        ret.add(comp);
        m_defaultLabelEditor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                onAnyUpdate();
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
                onAnyUpdate();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                onAnyUpdate();
            }

            private void onAnyUpdate() {
                updateOutputType();
            }
        });
        m_fvmDefaultLabel = createFlowVariableModel();
        m_fvmDefaultLabel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent evt) {
                if (!m_fvmDefaultLabel.isVariableReplacementEnabled()) {
                    m_defaultIsFlowVariable.setEnabled(true);
                }
                adjustOutputControls();
            }
        });
        ret.add(new FlowVariableModelButton(m_fvmDefaultLabel));
        return ret;
    }

    /**
     * Updates the {@link RSyntaxTextArea}'s markers to show the output type of the rules.
     */
    protected void updateOutputMarkers() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final long startDate = new Date().getTime();
                final List<Rule> rules = computeRulesRobust();
                final DataType[] outputTypes = new DataType[rules.size()];
                int i = 0;
                for (Rule rule : rules) {
                    if (rule != null) {
                        outputTypes[i] = rule.getOutcome().getType();
                    }
                    ++i;
                }
                ViewUtils.invokeLaterInEDT(new Runnable() {
                    @Override
                    public void run() {
                        setOutputMarkers(startDate, outputTypes);
                    }
                });
            }
        };
        threadPool.execute(runnable);
    }

    /**
     * Refreshes the outcome's type.
     */
    protected void updateOutputType() {
        if (!m_hasOutputColumn) {
            return;
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final Rule.OutcomeKind outcome = currentOutcomeType();
                final Object selectedItem = m_flowVarOrColumn.getSelectedItem();
                final String defaultLabel;
                DataType preferredDefaultType = null;
                if (outcome == null) {
                    defaultLabel = "";
                } else {
                    switch (outcome) {
                        case PlainText:
                            defaultLabel = m_defaultLabelEditor.getText();
                            break;
                        case Column:
                            defaultLabel =
                                    selectedItem instanceof DataColumnSpec ? "$"
                                            + ((DataColumnSpec)selectedItem).getName() + "$" : "";
                            break;
                        case FlowVariable:
                            if (selectedItem instanceof FlowVariable) {
                                FlowVariable fv = (FlowVariable)selectedItem;
                                defaultLabel = "$$" + fv.getName() + "$$";
                                preferredDefaultType = Util.toDataType(fv.getType());
                            } else {
                                defaultLabel = null;
                                preferredDefaultType = DataType.getMissingCell().getType();
                            }
                            break;
                        case StringInterpolation:
                            throw new UnsupportedOperationException("String interpolation is not supported yet.");
                        default:
                            throw new UnsupportedOperationException("Not supported outcome type: " + outcome);
                    }
                }

                final long startDate = new Date().getTime();
                try {
                    final int defaultLabelColumnIndex =
                            RuleEngineNodeModel.findDefaultLabelColumnIndex(m_spec, outcome, defaultLabel);
                    final List<Rule> rules = computeRules();
                    final DataType outputType =
                            RuleEngineNodeModel.computeOutputType(m_spec, rules, defaultLabelColumnIndex, defaultLabel,
                                                                  preferredDefaultType);
                    ViewUtils.invokeLaterInEDT(new Runnable() {
                        @Override
                        public void run() {
                            setOutputType(startDate, outputType);
                        }
                    });
                } catch (InvalidSettingsException e) {
                    setOutputType(startDate, DataType.getMissingCell().getType());
                } catch (ParseException e) {
                    setOutputType(startDate, DataType.getMissingCell().getType());
                }
            }
        };
        threadPool.execute(runnable);
    }

    /**
     * Sets the icon of output type according to the {@code outputType} parameter. <br/>
     * <strong>Should be on EDT</strong>
     *
     * @param startDate When the computation of the output type started.
     * @param outputType The new output type.
     */
    protected synchronized void setOutputType(final long startDate, final DataType outputType) {
        if (startDate > m_outputTypeLastSet) {
            m_outputType.setIcon(outputType.getIcon());
            m_outputTypeLastSet = startDate;
        }
    }

    /**
     * Sets the icon of output type markers in the {@link RSyntaxTextArea} according to the {@code outputTypes}
     * parameter. <br/>
     * <strong>Should be on EDT</strong>
     *
     * @param startDate When the computation of the output types started.
     * @param outputTypes The new output type.
     */
    protected synchronized void setOutputMarkers(final long startDate, final DataType[] outputTypes) {
        final KnimeSyntaxTextArea textArea = m_mainPanel.getTextEditor();
        String text = textArea.getText();
        if (startDate > m_outputMarkersLastSet && !text.isEmpty()) {
            String[] lines = text.split("\n", -1);
            final Gutter gutter = m_mainPanel.getGutter();
            gutter.removeAllTrackingIcons();
            textArea.removeAllLineHighlights();
            for (int i = outputTypes.length; i-- > 0;) {
                try {
                    if (outputTypes[i] == null && !(i < lines.length && lines[i].trim().isEmpty())) {//error
                        gutter.addLineTrackingIcon(i, RuleMainPanel.ERROR_ICON);
                        textArea.addLineHighlight(i, Color.PINK);
                    } else if (i < lines.length && RuleSupport.isComment(lines[i])) {//comment
                        textArea.addLineHighlight(i, Color.YELLOW);
                    } else if (m_hasOutputColumn && outputTypes[i] != null) {
                        gutter.addLineTrackingIcon(i, outputTypes[i].getIcon());
                    }
                } catch (BadLocationException e) {
                    //Do nothing. This can happen when the text is not up-to-date with the rules.
                    //Example config dialog closed with cancel after added some lines.
                    //Next invocations will fix it.
                }
            }
            m_outputMarkersLastSet = startDate;
        }
    }

    /**
     * Sets the selected values/enabled state for the output controls based on the selection of them.
     */
    protected void adjustOutputControls() {
        final boolean editable = !m_fvmDefaultLabel.isVariableReplacementEnabled();
        if (m_spec.getNumColumns() == 0) {
            m_defaultIsColumnReference.setEnabled(false);
        }
        if (m_flowVariables.isEmpty()) {
            m_defaultIsFlowVariable.setEnabled(false);
        }
        final Rule.OutcomeKind type = currentOutcomeType();
        if (type == null) {
            m_defaultLabelEditor.setEnabled(false);
            m_flowVarOrColumn.setEnabled(false);
            return;
        }
        switch (type) {
            case PlainText:
                m_flowVarOrColumn.setEnabled(false);
                m_defaultLabelEditor.setEditable(editable);
                if (!editable) {
                    m_defaultLabelEditor.setText(m_flowVariables.get(m_fvmDefaultLabel.getInputVariableName())
                            .getValueAsString());
                }
                break;
            case Column:
                m_flowVarOrColumn.setEnabled(editable);
                m_defaultLabelEditor.setEditable(false);
                m_flowVarOrColumn.removeAllItems();
                m_flowVarOrColumn.setRenderer(new DataColumnSpecListCellRenderer());
                m_flowVarOrColumn.setSelectedItem(null);
                {
                    DataColumnSpec found = null;
                    FlowVariable fv = m_flowVariables.get(m_fvmDefaultLabel.getInputVariableName());
                    for (DataColumnSpec column : m_spec) {
                        m_flowVarOrColumn.addItem(column);
                        if (!editable && column.getName().equals(fv.getValueAsString())) {
                            m_flowVarOrColumn.setSelectedItem(found = column);
                            break;
                        }
                    }
                    m_flowVarOrColumn.setSelectedItem(found);
                }
                break;
            case FlowVariable:
                if (!editable) {
                    m_defaultIsFlowVariable.setEnabled(false);
                    m_defaultLabelTypeGroup.setSelected(m_defaultIsText.getModel(), true);
                    adjustOutputControls();
                    return;
                }
                m_flowVarOrColumn.setEnabled(editable);
                m_defaultLabelEditor.setEditable(false);
                m_flowVarOrColumn.removeAllItems();
                m_flowVarOrColumn.setRenderer(new FlowVariableListCellRenderer());
                for (FlowVariable flowVar : m_flowVariables.values()) {
                    m_flowVarOrColumn.addItem(flowVar);
                }
                m_flowVarOrColumn.setSelectedItem(null);
                if (!editable) {
                    FlowVariable fv = m_flowVariables.get(m_fvmDefaultLabel.getInputVariableName());
                    if (fv == null) {
                        m_flowVarOrColumn.setSelectedItem(null);
                    } else {
                        final FlowVariable fvReferenced = m_flowVariables.get(fv.getValueAsString());
                        m_flowVarOrColumn.setSelectedItem(fvReferenced);
                        if (fvReferenced != null) {
                            m_defaultLabelEditor.setText(fvReferenced.getValueAsString());
                        }
                    }
                }
                break;
            case StringInterpolation:
                m_flowVarOrColumn.setEnabled(false);
                m_defaultLabelEditor.setEditable(editable);
                break;
        }
    }

    /**
     * @return The computed {@link OutcomeKind} (based on the rules and the default value).
     */
    private OutcomeKind currentOutcomeType() {
        final ButtonModel selection = m_defaultLabelTypeGroup.getSelection();
        Rule.OutcomeKind type = null;
        for (Entry<Rule.OutcomeKind, JRadioButton> entry : m_outcomeControls.entrySet()) {
            if (entry.getValue().getModel() == selection) {
                type = entry.getKey();
                break;
            }
        }
        return type;
    }

    /**
     * Computes the rules, but do not stop on errors and processes comments too.
     *
     * @return The parsed {@link Rule}s.
     * @see #computeRules(boolean, boolean)
     */
    protected List<Rule> computeRulesRobust() {
        try {
            return computeRules(true, true);
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Computes the rules.
     *
     * @param includingComments Also keeps the comments.
     * @param continueOnError Do not stop processing on errors.
     * @return The parsed {@link Rule}s.
     * @throws ParseException If we stop processing on errors this is the cause.
     */
    protected List<Rule> computeRules(final boolean includingComments, final boolean continueOnError)
            throws ParseException {
        List<Rule> ret = new ArrayList<Rule>();
        String text = m_mainPanel.getExpression();
        final Map<String, FlowVariable> availableFlowVariables = m_flowVariables;
        int lineNo = 0;
        RuleFactory factory = m_hasOutputColumn ? RuleFactory.getInstance() : RuleFactory.getFilterInstance();
        for (String line : text.split("\n", -1)) {
            lineNo++;
            //Skip empty lines
            if (line.trim().isEmpty()) {
                if (continueOnError) {
                    ret.add(null);
                }
                continue;
            }
            try {
                final Rule r = factory.parse(line, m_spec, availableFlowVariables);
                if (r.getCondition().isEnabled() || includingComments) {
                    ret.add(r);
                }
            } catch (ParseException e) {
                if (continueOnError) {
                    ret.add(null);
                } else {
                    ParseException error =
                            new ParseException(
                                    "line " + lineNo + ", col " + e.getErrorOffset() + ": " + e.getMessage(),
                                    e.getErrorOffset());
                    error.setStackTrace(e.getStackTrace());
                    throw error;
                }
            }
        }
        return ret;
    }

    /**
     * Parses the {@link Rule}s stopping on errors.
     *
     * @param includingComments Include or exclude the rules representing comments.
     * @return The parsed {@link Rule}s.
     * @throws ParseException Parse error.
     * @see #computeRules(boolean, boolean)
     */
    protected List<Rule> computeRules(final boolean includingComments) throws ParseException {
        return computeRules(includingComments, false);
    }

    /**
     * Parses the {@link Rule}s. Stops on errors, exclude the comments.
     *
     * @return The parsed {@link Rule}.s
     * @throws ParseException Parse error.
     * @see #computeRules(boolean, boolean)
     */
    protected List<Rule> computeRules() throws ParseException {
        return computeRules(false);
    }

    /**
     * When new text should be used for the {@link RSyntaxTextArea} this method will update it.
     *
     * @param text The new text to show.
     */
    public void updateText(final String text) {
        m_mainPanel.update(text, m_spec, m_flowVariables);
    }

    /**
     * Updates the state for a new {@code spec}, {@code flowVariables}.
     *
     * @param spec The new {@link DataTableSpec}.
     * @param flowVariables The new {@link FlowVariable}s.
     */
    public void update(final DataTableSpec spec, final Map<String, FlowVariable> flowVariables) {
        m_spec = spec;
        this.m_flowVariables = flowVariables;
        m_mainPanel.update(m_mainPanel.getExpression(), spec, flowVariables);
    }

    /**
     * Saves the settings to the model ({@code settings}).
     *
     * @param settings The container for the model.
     * @throws InvalidSettingsException When there should be a column/flow variable outcome, but no column/flow variable
     *             is selected or the rules do not parse without error.
     */
    public void saveSettings(final NodeSettingsWO settings) throws InvalidSettingsException {
        RuleEngineSettings ruleSettings = new RuleEngineSettings();
        if (m_hasOutputColumn) {
            OutcomeKind selectedOutcome = currentOutcomeType();
            if (selectedOutcome == null) {
                throw new InvalidSettingsException("The default value type is not selected");
            }
            switch (selectedOutcome) {
                case PlainText:
                    ruleSettings.setDefaultLabel(m_defaultLabelEditor.getText());
                    break;
                case Column:
                    if (m_flowVarOrColumn.getSelectedItem() == null) {
                        throw new InvalidSettingsException("No column selected");
                    }
                    ruleSettings.setDefaultLabel("$" + ((DataColumnSpec)m_flowVarOrColumn.getSelectedItem()).getName()
                            + "$");
                    break;
                case FlowVariable:
                    if (!(m_flowVarOrColumn.getSelectedItem() instanceof FlowVariable)) {
                        throw new InvalidSettingsException("No flow variable selected");
                    }
                    final FlowVariable fv = (FlowVariable)m_flowVarOrColumn.getSelectedItem();
                    ruleSettings.setDefaultLabel(Util.escapeFlowVariableName(fv));
                    break;
                case StringInterpolation:
                    ruleSettings.setDefaultLabel(m_defaultLabelEditor.getText());
                    throw new InvalidSettingsException("String interpolation is not supported yet.");
                default:
                    throw new InvalidSettingsException("Not supported outcome type: " + selectedOutcome);
            }
            ruleSettings.setDefaultOutputType(selectedOutcome);
            ruleSettings.setNewcolName(m_newColumnName.getText());
        } else {
            ruleSettings.setDefaultLabel("");
            ruleSettings.setDefaultOutputType(Rule.OutcomeKind.PlainText);
            ruleSettings.setNewcolName("");
        }
        try {
            computeRules();
        } catch (ParseException e) {
            throw new InvalidSettingsException(e.getMessage(), e);
        }
        for (String r : m_mainPanel.getExpression().split("\n", -1)) {
            ruleSettings.addRule(r);
        }
        ruleSettings.saveSettings(settings);
        if (!m_hasOutputColumn) {
            final SettingsModelBoolean settingsModelBoolean =
                    new SettingsModelBoolean(RuleEngineFilterNodeModel.CFGKEY_INCLUDE_ON_MATCH,
                            RuleEngineFilterNodeModel.DEFAULT_INCLUDE_ON_MATCH);
            settingsModelBoolean.setBooleanValue(m_inclusionButtonGroup.getSelection() != m_excludeButton.getModel());
            settingsModelBoolean.saveSettingsTo(settings);
        }
    }

    /**
     * Loads the settings from the serialized model ({@code settings}).
     *
     * @param settings The container for the model.
     * @param specs The current {@link DataTableSpec}s.
     * @param availableFlowVariables The {@link FlowVariable}s.
     * @throws NotConfigurableException The information in the {@code settings} model is invalid.
     */
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs,
                                    final Map<String, FlowVariable> availableFlowVariables)
            throws NotConfigurableException {
        if (specs == null || specs.length == 0 || specs[0] == null /*|| specs[0].getNumColumns() == 0*/) {
            throw new NotConfigurableException("No columns available!");
        }
        //m_ruleEditor.setText("");
        m_spec = specs[0];
        m_parser.setDataTableSpec(m_spec);
        m_parser.setFlowVariables(availableFlowVariables);
        update(m_spec, availableFlowVariables);
        RuleEngineSettings ruleSettings = new RuleEngineSettings();
        ruleSettings.loadSettingsForDialog(settings);
        String defaultLabel = ruleSettings.getDefaultLabel();
        if (m_hasOutputColumn) {
            final Rule.OutcomeKind defaultOutputType = ruleSettings.getDefaultOutputType();
            final ButtonModel model = m_outcomeControls.get(defaultOutputType).getModel();
            if (model != null) {
                m_defaultLabelTypeGroup.setSelected(model, true);
            }
            if (defaultOutputType == null) {
                throw new NotConfigurableException("No output type is selected for default outcome.");
            }
            switch (defaultOutputType) {
                case PlainText:
                    m_defaultLabelEditor.setText(defaultLabel);
                    break;
                case Column: {
                    boolean found = false;
                    final String colName;
                    if (defaultLabel.length() < 3 || defaultLabel.charAt(0) != '$' || !defaultLabel.endsWith("$")) {
                        colName = "";
                    } else {
                        colName = defaultLabel.substring(1, defaultLabel.length() - 1);
                    }
                    for (DataColumnSpec col : m_spec) {
                        if (col.getName().equals(colName)) {
                            m_flowVarOrColumn.setSelectedItem(col);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        m_flowVarOrColumn.setSelectedItem(null);
                    }
                }
                    break;
                case FlowVariable: {
                    final String flowVarName;
                    if (defaultLabel.length() < 8 || !defaultLabel.startsWith("$${") || !defaultLabel.endsWith("}$$")) {
                        flowVarName = "";
                    } else {
                        flowVarName = defaultLabel.substring(4, defaultLabel.length() - 3);
                    }
                    m_flowVarOrColumn.setSelectedItem(availableFlowVariables.get(flowVarName));
                }
                    break;
                case StringInterpolation:
                    m_defaultLabelEditor.setText(defaultLabel);
                    throw new UnsupportedOperationException("String interpolation is not supported yet.");
                default:
                    throw new UnsupportedOperationException("Not supported default outcome type: " + defaultOutputType);
            }
            String newColName = ruleSettings.getNewColName();
            m_newColumnName.setText(newColName);
        }
        final KnimeSyntaxTextArea textEditor = m_mainPanel.getTextEditor();
        textEditor.setText("");
        int line = 0;
        final RuleFactory factory = m_hasOutputColumn ? RuleFactory.getInstance() : RuleFactory.getFilterInstance();
        StringBuilder text = new StringBuilder();
        for (Iterator<String> it = ruleSettings.rules().iterator(); it.hasNext();) {
            final String rs = it.next();
            try {
                factory.parse(rs, m_spec, availableFlowVariables);
                text.append(rs);
                if (it.hasNext()) {
                    text.append('\n');
                }
            } catch (ParseException e) {
                LOGGER.warn("Rule '" + rs + "' (" + line + ") removed, because of " + e.getMessage());
            } finally {
                ++line;
            }
        }
        if (!ruleSettings.rules().iterator().hasNext()) {
            final String noText = RuleSupport.toComment(RuleEngineNodeDialog.RULE_LABEL);
            textEditor.setText(noText);
            text.append(noText);
        }
        if (!m_hasOutputColumn) {
            try {
                final SettingsModelBoolean includeOnMatch =
                        new SettingsModelBoolean(RuleEngineFilterNodeModel.CFGKEY_INCLUDE_ON_MATCH,
                                RuleEngineFilterNodeModel.DEFAULT_INCLUDE_ON_MATCH);
                includeOnMatch.loadSettingsFrom(settings);
                m_inclusionButtonGroup.setSelected(includeOnMatch.getBooleanValue() ? m_includeButton.getModel()
                        : m_excludeButton.getModel(), true);
            } catch (InvalidSettingsException e) {
                m_inclusionButtonGroup.clearSelection();
            }
        }
        //       m_lastUsedTextComponent = null;
        updateText(text.toString());
    }

    /**
     * Called when the default label has to be specified and we want to ease the flow variable support.
     *
     * @return The {@link FlowVariableModel}.
     */
    protected abstract FlowVariableModel createFlowVariableModel();
}
