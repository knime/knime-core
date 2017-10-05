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
 * ---------------------------------------------------------------------
 *
 * History
 *   May 1, 2008 (wiswedel): created
 */
package org.knime.base.node.flowvariable.variabletotablerow2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.util.filter.variable.FlowVariableFilterConfiguration;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.Pair;

/** NodeModel for the "Variable To TableRow" node which converts variables into a single row values with the variable
 * names as column headers.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 *
 * @since 2.9
 */
public class VariableToTable2NodeModel extends NodeModel {

    /** Key for the filter configuration. */
    static final String CFG_KEY_FILTER = "variable-filter";

    private FlowVariableFilterConfiguration m_filter;

    private final SettingsModelString m_rowID = VariableToTable2NodeDialogPane.createSettingsModelRowID();

    /** One input, one output. */
    public VariableToTable2NodeModel() {
        super(new PortType[]{FlowVariablePortObject.TYPE_OPTIONAL}, new PortType[]{BufferedDataTable.TYPE});
        m_filter = new FlowVariableFilterConfiguration(CFG_KEY_FILTER);
        m_filter.loadDefaults(getAvailableFlowVariables(), false);
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        DataTableSpec spec = createOutSpec();
        BufferedDataContainer cont = exec.createDataContainer(spec);
        List<Pair<String, FlowVariable.Type>> vars = getVariablesOfInterest();
        DataCell[] specs = new DataCell[vars.size()];
        List<String> lostVariables = new ArrayList<String>();
        for (int i = 0; i < vars.size(); i++) {
            Pair<String, FlowVariable.Type> c = vars.get(i);
            String name = c.getFirst();
            DataCell cell = DataType.getMissingCell(); // fallback
            switch (c.getSecond()) {
                case DOUBLE:
                    try {
                        double dValue = peekFlowVariableDouble(c.getFirst());
                        cell = new DoubleCell(dValue);
                    } catch (NoSuchElementException e) {
                        lostVariables.add(name + " (Double)");
                    }
                    break;
                case INTEGER:
                    try {
                        int iValue = peekFlowVariableInt(c.getFirst());
                        cell = new IntCell(iValue);
                    } catch (NoSuchElementException e) {
                        lostVariables.add(name + " (Integer)");
                    }
                    break;
                case STRING:
                    try {
                        String sValue = peekFlowVariableString(c.getFirst());
                        sValue = sValue == null ? "" : sValue;
                        cell = new StringCell(sValue);
                    } catch (NoSuchElementException e) {
                        lostVariables.add(name + " (String)");
                    }
                    break;
            }
            specs[i] = cell;
        }
        cont.addRowToTable(new DefaultRow(m_rowID.getStringValue(), specs));
        cont.close();
        return new BufferedDataTable[]{cont.getTable()};
    }

    private List<Pair<String, FlowVariable.Type>> getVariablesOfInterest() {
        List<Pair<String, FlowVariable.Type>> result = new ArrayList<Pair<String, FlowVariable.Type>>();
        if (m_filter != null) {
            String[] names = m_filter.applyTo(getAvailableFlowVariables()).getIncludes();
            Map<String, FlowVariable> vars = getAvailableFlowVariables();
            for (String name : names) {
                result.add(new Pair<String, FlowVariable.Type>(name, vars.get(name).getType()));
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[]{createOutSpec()};
    }

    private DataTableSpec createOutSpec() throws InvalidSettingsException {
        List<Pair<String, FlowVariable.Type>> vars = getVariablesOfInterest();
        if (vars.isEmpty()) {
            throw new InvalidSettingsException("No variables selected");
        }
        DataColumnSpec[] specs = new DataColumnSpec[vars.size()];
        for (int i = 0; i < vars.size(); i++) {
            Pair<String, FlowVariable.Type> c = vars.get(i);
            DataType type;
            switch (c.getSecond()) {
                case DOUBLE:
                    type = DoubleCell.TYPE;
                    break;
                case INTEGER:
                    type = IntCell.TYPE;
                    break;
                case STRING:
                    type = StringCell.TYPE;
                    break;
                default:
                    throw new InvalidSettingsException("Unsupported variable type: " + c.getSecond());
            }
            specs[i] = new DataColumnSpecCreator(c.getFirst(), type).createSpec();
        }
        return new DataTableSpec(specs);
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_rowID.loadSettingsFrom(settings);
        FlowVariableFilterConfiguration conf = new FlowVariableFilterConfiguration(CFG_KEY_FILTER);
        conf.loadConfigurationInModel(settings);
        m_filter = conf;
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_rowID.saveSettingsTo(settings);
        m_filter.saveConfiguration(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_rowID.validateSettings(settings);
        FlowVariableFilterConfiguration conf = new FlowVariableFilterConfiguration(CFG_KEY_FILTER);
        conf.loadConfigurationInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
    }

}
