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
 *   14.07.2009 (mb): created
 */
package org.knime.core.node;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.node.workflow.VariableType.CredentialsType;
import org.knime.core.node.workflow.VariableType.DoubleType;
import org.knime.core.node.workflow.VariableType.IntType;
import org.knime.core.node.workflow.VariableType.StringType;
import org.knime.core.node.workflow.VariableTypeRegistry;


/** Container holding information regarding variables which represent
 * settings of a node and/or are used to replace settings of a node.
 *
 * This allows NodeDialogPane implementations to encapsulate all of the
 * information related to variable/settings replacements.
 *
 * @author Michael Berthold, University of Konstanz
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class FlowVariableModel {

    // private members
    private final NodeDialogPane m_parent;
    private final String[] m_keys;  // the hierarchy of Config Keys for this object
    private final VariableType<?> m_variableType;

    /* variable names that are to be used for the corresponding settings
     * as "input" resp. "output". If one or both are null, the replacement
     * will not happen.
     */
    private String m_inputVariableName;
    private String m_outputVariableName;

    private final CopyOnWriteArrayList<ChangeListener> m_listeners = new CopyOnWriteArrayList<>();

    /** Create a new WVM object.
     *
     * @param parent NodeDialogPane (needed to retrieve visible variables)
     * @param keys of corresponding settings object
     * @param type of variable/settings object
     * @deprecated use {@link FlowVariableModel#FlowVariableModel(NodeDialogPane, String[], VariableType)} instead
     */
    @Deprecated
    FlowVariableModel(final NodeDialogPane parent, final String[] keys,
            final FlowVariable.Type type) {
        m_parent = parent;
        m_keys = keys.clone();
        m_variableType = getVariableTypeFromType(type);
    }

    // the whole purpose of the method is to map from the deprecated Type to VariableType
    @SuppressWarnings("deprecation")
    private static VariableType<?> getVariableTypeFromType(final FlowVariable.Type type) {
        switch (type) {
            case DOUBLE:
                return DoubleType.INSTANCE;
            case INTEGER:
                return IntType.INSTANCE;
            case STRING:
                return StringType.INSTANCE;
            case CREDENTIALS:
            case OTHER:
            default:
                // this is safe because FlowVariableModel never worked for any type other than Double, Integer or String
                throw new IllegalArgumentException("Unsupported type " + type);

        }
    }

    // this method acts as an adapter between the new VariableType and the deprecated FlowVariable.Type
    @SuppressWarnings("deprecation")
    private static FlowVariable.Type getType(final VariableType<?> variableType) {
        if (variableType.equals(StringType.INSTANCE)) {
            return FlowVariable.Type.STRING;
        } else if (variableType.equals(IntType.INSTANCE)) {
            return FlowVariable.Type.INTEGER;
        } else if (variableType.equals(DoubleType.INSTANCE)) {
            return FlowVariable.Type.DOUBLE;
        } else if (variableType.equals(CredentialsType.INSTANCE)) {
            return FlowVariable.Type.CREDENTIALS;
        } else {
            return FlowVariable.Type.OTHER;
        }
    }

    /** Create a new WVM object.
    *
    * @param parent NodeDialogPane (needed to retrieve visible variables)
    * @param keys of corresponding settings object
    * @param type of variable/settings object
    */
    FlowVariableModel(final NodeDialogPane parent, final String[] keys, final VariableType<?> type) {
        m_variableType = type;
        m_keys = keys.clone();
        m_parent = parent;
    }

    /**
     *  @return parent NodeDialogPane
     */
    NodeDialogPane getParent() {
        return m_parent;
    }

    /**
     * @return the key of the corresponding setting object.
     */
    public String[] getKeys() {
        return m_keys;
    }

    /**
     * @return the type of the variable/settings object.
     * @deprecated use {@link FlowVariableModel#getVariableType()} instead
     */
    @Deprecated
    public FlowVariable.Type getType() {
        return getType(m_variableType);
    }

    /**
     * @return the type of the variable
     * @since 4.2
     */
    public VariableType<?> getVariableType() {
        return m_variableType;
    }

    /**
     * @return the inputVariableName
     */
    public String getInputVariableName() {
        return m_inputVariableName;
    }

    /**
     * Returns the value of the selected flow variable or an empty optional if no flow variable has been selected.
     *
     * @return the flow variable or an empty optional
     * @since 3.3
     */
    public Optional<FlowVariable> getVariableValue() {
        return Arrays.stream(getMatchingVariables()).filter(v -> v.getName().equals(m_inputVariableName)).findFirst();
    }

    /**
     * @param variableName the inputVariableName to set. Set to null
     * if no replacement is wanted.
     */
    public void setInputVariableName(final String variableName) {
        if (!Objects.equals(variableName, m_inputVariableName)) {
            m_inputVariableName = variableName;
            notifyChangeListeners();
        }
    }

    /**
     * @return true if variable replacement is activated.
     */
    public boolean isVariableReplacementEnabled() {
        return getInputVariableName() != null;
    }

    /**
     * @return the outputVariableName
     */
    public String getOutputVariableName() {
        return m_outputVariableName;
    }

    /**
     * @param variableName the outputVariableName to set. Set to null
     * if no replacement is wanted.
     */
    public void setOutputVariableName(final String variableName) {
        if (!Objects.equals(variableName, m_outputVariableName)) {
            m_outputVariableName = variableName;
            notifyChangeListeners();
        }
    }

    /**
     * @return array of variables names that match the type of this model.
     */
    FlowVariable[] getMatchingVariables() {
        VariableType<?>[] allTypes = VariableTypeRegistry.getInstance().getAllTypes();
        Map<String, FlowVariable> variables = getParent().getAvailableFlowVariables(allTypes);
        return variables.values().stream()
            .filter(v -> v.getVariableType().isConvertible(m_variableType)).toArray(FlowVariable[]::new);
    }

    /**
     * Adds a listener which is notified whenever a new value is set in the
     * model. Does nothing if the listener is already registered.
     *
     * @param l listener to add.
     */
    public void addChangeListener(final ChangeListener l) {
        if (!m_listeners.contains(l)) {
            m_listeners.add(l);
        }
    }

    /**
     * Remove a specific listener.
     *
     * @param l listener to remove.
     */
    public void removeChangeListener(final ChangeListener l) {
        m_listeners.remove(l);
    }

    /**
     * Notifies all registered listeners about a new model content.
     */
    protected void notifyChangeListeners() {
        for (ChangeListener l : m_listeners) {
            l.stateChanged(new ChangeEvent(this));
        }
    }

}
