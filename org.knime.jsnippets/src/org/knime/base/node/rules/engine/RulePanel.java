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
 * Created on 2013.04.25. by Gabor Bakos
 */
package org.knime.base.node.rules.engine;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.parser.Parser;
import org.fife.ui.rtextarea.Gutter;
import org.knime.base.node.rules.engine.rsyntax.AbstractRuleParser;
import org.knime.base.node.rules.engine.rsyntax.PMMLRuleParser;
import org.knime.base.node.rules.engine.rsyntax.RuleParser;
import org.knime.base.node.rules.engine.rsyntax.VariableRuleParser;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.util.rsyntaxtextarea.KnimeSyntaxTextArea;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.ThreadUtils;

/**
 * A Rule panel with a {@link RuleMainPanel} with further controls for output.
 *
 * @author Gabor Bakos
 * @since 2.8
 */
@SuppressWarnings({"serial"})
public class RulePanel extends JPanel {
    private JRadioButton m_top, m_bottom;

    private ButtonGroup m_includeButtons = new ButtonGroup();

    private JLabel m_outputType;

    private JTextField m_newColumnName;

    private ColumnSelectionPanel m_replaceColumn;

    private ButtonGroup m_outputGroup;

    private JRadioButton m_replaceColRadio;

    private volatile long m_outputTypeLastSet, m_outputMarkersLastSet;

    private RuleMainPanel m_mainPanel;

    private DataTableSpec m_spec;

    private static final ExecutorService threadPool = ThreadUtils.executorServiceWithContext(new ThreadPoolExecutor(1,
        4, 4, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(16)));

    private Map<String, FlowVariable> m_flowVariables;

    private AbstractRuleParser m_parser;

    private RuleNodeSettings m_nodeType;

    /** See {@link RuleEngineSettings#isDisallowLongOutputForCompatibility()} -- compatibility workaround */
    private boolean m_disallowLongOutputForCompatibility;

    /**
     * Constructs the {@link RulePanel}.
     *
     * @param nodeType The {@link RuleNodeSettings} to configure the {@link RulePanel}.
     */
    public RulePanel(final RuleNodeSettings nodeType) {
        this(nodeType, true);
    }

    /**
     * Constructs the {@link RulePanel}.
     *
     * @param nodeType The {@link RuleNodeSettings} to configure the {@link RulePanel}.
     * @param warnOnColRefsInStrings Warn on suspicious column references in strings?
     */
    public RulePanel(final RuleNodeSettings nodeType, final boolean warnOnColRefsInStrings) {
        this.m_nodeType = nodeType;
        setLayout(new BorderLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        m_mainPanel = new RuleMainPanel(nodeType);
        for (int i = 0; i < m_mainPanel.getTextEditor().getParserCount(); ++i) {
            Parser p = m_mainPanel.getTextEditor().getParser(i);
            if (p instanceof AbstractRuleParser) {
                final AbstractRuleParser parser = (AbstractRuleParser)p;
                m_parser = parser;
                m_parser.setWarnOnColRefsInStrings(warnOnColRefsInStrings);
                m_parser.setAllowNoOutcome(false);
                m_parser.setAllowTableReferences(nodeType.allowTableProperties());
            }
        }
        if (m_parser == null) {
            m_parser = createParser(warnOnColRefsInStrings, nodeType);
            m_mainPanel.getTextEditor().addParser(m_parser);
        }
        m_parser.setDataTableSpec(m_spec);
        add(m_mainPanel, BorderLayout.CENTER);
        gbc.gridy = 1;
        gbc.weighty = 0;
        add(createOutputControls(), BorderLayout.SOUTH);
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
     * Creates the {@link Parser} for the {@link RSyntaxTextArea}.
     *
     * @param warnOnColRefsInStrings Warn on suspicious references in {@link String}s?
     * @param nodeType The {@link RuleNodeSettings}.
     * @return The {@link Parser} for {@link RSyntaxTextArea}.
     */
    protected AbstractRuleParser createParser(final boolean warnOnColRefsInStrings, final RuleNodeSettings nodeType) {
        switch (nodeType) {
            case VariableRule:
                return new VariableRuleParser(warnOnColRefsInStrings);
            case PMMLRule:
                return new PMMLRuleParser(warnOnColRefsInStrings);
            case RuleEngine:
            case RuleFilter:
            case RuleSplitter:
                return new RuleParser(warnOnColRefsInStrings, nodeType);
            default:
                throw new UnsupportedOperationException("Not supported node type: " + nodeType);
        }
    }

    /**
     * @return The output controls. Either the column name with default value selection controls or the kind of
     *         filtering to do.
     */
    private JPanel createOutputControls() {
        JPanel ret = new JPanel();
        ret.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        if (m_nodeType.hasOutput()) {
            constraints.weighty = .25;
            String watermark = "prediction";
            int colWidth = 35;
            ret.add(
                m_nodeType.allowColumns() ? createNewColumnTextFieldWithReplace(watermark, colWidth, "Append Column: ")
                    : createNewColumnTextFieldWithLabel(watermark, colWidth, "New flow variable name"), constraints);

        } else {
            JLabel label = new JLabel(m_nodeType.selectionText());
            constraints.gridx = 0;
            constraints.gridy = 0;

            ret.add(label, constraints);
            constraints.gridx++;
            m_top = new JRadioButton(m_nodeType.topText());
            ret.add(m_top, constraints);
            constraints.gridx++;
            m_bottom = new JRadioButton(m_nodeType.bottomText());
            ret.add(m_bottom, constraints);
            m_includeButtons.add(m_top);
            m_includeButtons.add(m_bottom);
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

    private Component
        createNewColumnTextFieldWithReplace(final String watermark, final int colWidth, final String label) {
        Box ret = Box.createVerticalBox();
        JPanel addColumnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel replaceColumnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        m_outputGroup = new ButtonGroup();
        final JRadioButton newColumn = new JRadioButton(label);
        m_replaceColRadio = new JRadioButton("Replace Column:");
        addColumnPanel.add(newColumn);
        JTextField comp = Util.createTextFieldWithWatermark(watermark, colWidth, /*label*/null);
        m_newColumnName = comp;
        addColumnPanel.add(comp);
        ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_newColumnName.setEnabled(newColumn.isSelected());
                m_replaceColumn.setEnabled(m_replaceColRadio.isSelected());
                m_replaceColumn.setRequired(m_replaceColRadio.isSelected());
            }
        };
        newColumn.addActionListener(actionListener);
        m_replaceColRadio.addActionListener(actionListener);
        m_outputGroup.add(newColumn);
        m_outputGroup.add(m_replaceColRadio);
        @SuppressWarnings("unchecked")
        ColumnSelectionPanel colSelectionPanel =
            new ColumnSelectionPanel((Border)null, DoubleValue.class, IntValue.class, StringValue.class,
                BooleanValue.class);
        m_replaceColumn = colSelectionPanel;
        m_outputType = new JLabel(DataValue.UTILITY.getIcon());
        addColumnPanel.add(m_outputType);
        replaceColumnPanel.add(m_replaceColRadio);
        replaceColumnPanel.add(m_replaceColumn);
        ret.add(addColumnPanel);
        ret.add(replaceColumnPanel);
        return ret;
    }

    /**
     * Updates the {@link RSyntaxTextArea}'s markers to show the output type of the rules.
     */
    protected void updateOutputMarkers() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final long startDate = System.nanoTime();
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
        if (!m_nodeType.hasOutput()) {
            return;
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final long startDate = new Date().getTime();
                try {
                    final List<Rule> rules = computeRules();
                    final DataType outputType = RuleEngineNodeModel.computeOutputType(
                        rules, m_nodeType, m_disallowLongOutputForCompatibility);
                    ViewUtils.invokeLaterInEDT(new Runnable() {
                        @Override
                        public void run() {
                            setOutputType(startDate, outputType);
                        }
                    });
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
        if (startDate - m_outputMarkersLastSet > 0 && !textArea.getText().isEmpty()) {
            final Gutter gutter = m_mainPanel.getGutter();
            gutter.removeAllTrackingIcons();
            textArea.removeAllLineHighlights();
            String[] lines = textArea.getText().split("\n", -1);
            for (int i = outputTypes.length; i-- > 0;) {
                try {
                    if (outputTypes[i] == null && !(i < lines.length && lines[i].trim().isEmpty())) { //error
                        gutter.addLineTrackingIcon(i, RuleMainPanel.ERROR_ICON);
                        textArea.addLineHighlight(i, Color.PINK);
                    } else if (m_nodeType.hasOutput() && outputTypes[i] != null) {
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
        if (text == null) {
            return ret;
        }
        final Map<String, FlowVariable> availableFlowVariables =
            m_nodeType.allowFlowVariables() ? m_flowVariables : Collections.<String, FlowVariable> emptyMap();
        int lineNo = 0;
        final RuleFactory factory = RuleFactory.getInstance(m_nodeType);
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
                        new ParseException("line " + lineNo + ", col " + e.getErrorOffset() + ": " + e.getMessage(),
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
        m_mainPanel.update(text, m_spec,
            m_nodeType.allowFlowVariables() ? m_flowVariables : Collections.<String, FlowVariable> emptyMap(),
            m_nodeType.expressions());
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
        try {
            if (m_replaceColumn != null) {
                m_replaceColumn.update(spec, m_replaceColumn.getSelectedColumn());
            }
        } catch (NotConfigurableException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        m_mainPanel.update(m_mainPanel.getExpression(), spec, m_nodeType.allowFlowVariables() ? flowVariables
            : Collections.<String, FlowVariable> emptyMap(), m_nodeType.expressions());
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
        if (m_nodeType.hasOutput()) {
            ruleSettings.setNewcolName(m_newColumnName.getText());
            if (m_replaceColumn != null) {
                ruleSettings.setAppendColumn(m_newColumnName.isEnabled());
                ruleSettings.setReplaceColumn(m_replaceColumn.getSelectedColumn());
                if (m_outputGroup.isSelected(m_replaceColRadio.getModel())
                    && m_replaceColumn.getSelectedColumn() == null) {
                    throw new InvalidSettingsException("No column was selected for replacement!");
                }
            }
        } else {
            ruleSettings.setNewcolName("");
        }
        //Check the rule integrity, configure will also check.
        try {
            computeRules();
        } catch (ParseException e) {
            throw new InvalidSettingsException(e.getMessage(), e);
        }
        for (String r : m_mainPanel.getExpression().split("\n", -1)) {
            if (!r.trim().isEmpty()) {
                ruleSettings.addRule(r);
            }
        }
        ruleSettings.saveSettings(settings);
        if (!m_nodeType.hasOutput()) {
            final SettingsModelBoolean settingsModelBoolean =
                new SettingsModelBoolean(RuleEngineFilterNodeModel.CFGKEY_INCLUDE_ON_MATCH,
                    RuleEngineFilterNodeModel.DEFAULT_INCLUDE_ON_MATCH);
            settingsModelBoolean.setBooleanValue(m_top.isSelected());
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
    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs,
        final Map<String, FlowVariable> availableFlowVariables) throws NotConfigurableException {
        if (specs == null || specs.length == 0 || specs[0] == null /*|| specs[0].getNumColumns() == 0*/) {
            throw new NotConfigurableException("No columns available!");
        }
        m_spec = specs[0];
        m_parser.setDataTableSpec(m_spec);
        m_parser.setFlowVariables(availableFlowVariables);
        RuleEngineSettings ruleSettings = new RuleEngineSettings();
        ruleSettings.loadSettingsForDialog(settings);
        update(specs[0], availableFlowVariables);
        if (m_nodeType.hasOutput()) {
            String newColName = ruleSettings.getNewColName();
            m_newColumnName.setText(newColName);
            if (m_replaceColumn != null) {
                m_outputGroup.setSelected(m_outputGroup.getElements().nextElement().getModel(),
                    ruleSettings.isAppendColumn());
                m_outputGroup.setSelected(m_replaceColRadio.getModel(), !ruleSettings.isAppendColumn());
                for (ActionListener listener : m_replaceColRadio.getActionListeners()) {
                    listener.actionPerformed(null);
                }
                m_replaceColumn.setSelectedColumn(ruleSettings.getReplaceColumn());
            }
        }
        m_disallowLongOutputForCompatibility = ruleSettings.isDisallowLongOutputForCompatibility();
        update(m_spec, availableFlowVariables);
        final KnimeSyntaxTextArea textEditor = m_mainPanel.getTextEditor();
        textEditor.setText("");
        StringBuilder text = new StringBuilder();
        for (Iterator<String> it = ruleSettings.rules().iterator(); it.hasNext();) {
            final String rs = it.next();
            text.append(rs);
            if (it.hasNext()) {
                text.append('\n');
            }
        }
        if (!ruleSettings.rules().iterator().hasNext()) {
            final String defaultText = m_nodeType.defaultText();
            final String noText = RuleSupport.toComment(defaultText);
            textEditor.setText(noText);
            text.append(noText);
        }
        if (!m_nodeType.hasOutput()) {
            try {
                final SettingsModelBoolean includeOnMatch =
                    new SettingsModelBoolean(RuleEngineFilterNodeModel.CFGKEY_INCLUDE_ON_MATCH,
                        RuleEngineFilterNodeModel.DEFAULT_INCLUDE_ON_MATCH);
                includeOnMatch.loadSettingsFrom(settings);
                m_top.setSelected(includeOnMatch.getBooleanValue());
                m_bottom.setSelected(!includeOnMatch.getBooleanValue());
            } catch (InvalidSettingsException e) {
                boolean defaultTop = true;
                m_top.setSelected(defaultTop);
                m_bottom.setSelected(!defaultTop);
            }
        }
        updateText(text.toString());
    }
}
