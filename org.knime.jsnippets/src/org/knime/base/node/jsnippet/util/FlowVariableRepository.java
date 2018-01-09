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
 * ------------------------------------------------------------------------
 *
 * History
 *   15.03.2012 (hofer): created
 */
package org.knime.base.node.jsnippet.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.knime.base.node.jsnippet.expression.FlowVariableException;
import org.knime.base.node.jsnippet.expression.TypeException;
import org.knime.base.node.jsnippet.type.TypeProvider;
import org.knime.base.node.jsnippet.type.flowvar.TypeConverter;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Used by Java Snippet as a storage for flow variables during execution.
 * <p>This class might change and is not meant as public API.
 *
 * @author Heiko Hofer
 * @since 2.12
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public class FlowVariableRepository {
    private Map<String, FlowVariable> m_input;
    private Map<String, FlowVariable> m_modified;
    private Set<String> m_flowVarNames;

    /**
     * Create a new repository.
     * @param input the flow variables from the input stack.
     */
    public FlowVariableRepository(final Map<String, FlowVariable> input) {
        super();
        m_input = input;
        m_modified = new LinkedHashMap<>();
        m_flowVarNames = new LinkedHashSet<>();
        m_flowVarNames.addAll(input.keySet());
    }

    /**
     * Get the modified or new flow variables.
     * @return the modified or new flow variables
     */
    public Collection<FlowVariable> getModified() {
        return m_modified.values();
    }


    /**
     * Get the value of a flow variable in the given type.
     * @param <T> the type to expect
     * @param name the name of the flow variable
     * @param t the type to retrieve
     * @return the value of a flow variable in a specific type
     * @throws TypeException if type conversion fails
     * @throws FlowVariableException if flow variable does not exist
     */
    @SuppressWarnings("unchecked")
    public <T> T getValueAs(final String name, final T t)
            throws TypeException, FlowVariableException {
        return (T)getValueOfType(name, t.getClass());
    }

    /**
     * Get value of a flow variable. The type of the returned object is equal
     * to the given className.
     * @param name the name of the flow variable
     * @param className the type of the returned object
     * @return the value of the flow variable
     */
    @SuppressWarnings("rawtypes")
    public Object getValueOfType(final String name, final Class className) {
        FlowVariable flowVar = getFlowVariable(name);
        if (null == flowVar) {
            throw new FlowVariableException("The flow variable with name \""
                    + name + "\" does not exist.");
        }
        TypeConverter converter =
            TypeProvider.getDefault().getTypeConverter(flowVar.getType());
        return converter.getValue(flowVar, className);
    }

    /**
     * Returns true when getValueOfType(String, Class) does not throw
     * an TypeException when called with the given flow variable and the given
     * class name.
     * @param name the name of the flow variable
     * @param className the type
     * @return true when flow variable is of type.
     */
    @SuppressWarnings("rawtypes")
    public boolean isOfType(final String name, final Class className) {
        FlowVariable flowVar = getFlowVariable(name);
        if (null == flowVar) {
            throw new FlowVariableException("The flow variable with name \""
                    + name + "\" does not exist.");
        }
        TypeConverter converter =
            TypeProvider.getDefault().getTypeConverter(flowVar.getType());
        return converter.canProvideJavaType(className);
    }

    /**
     * Get the current flow variable associated with the given name or null if
     * a flow variable with the given name does not exist.
     * @param name the name of the flow variable r null if
     * a flow variable with the given name does not exist.
     * @return the flow variable
     */
    public FlowVariable getFlowVariable(final String name) {
        FlowVariable var = m_modified.get(name);
        return null != var ? var : m_input.get(name);
    }


    /**
     * Add a new or updated flow variable.
     * @param flowVar the flow variable associated with name
     */
    public void put(final FlowVariable flowVar) {
        m_modified.put(flowVar.getName(), flowVar);
        // re-add element since natural ordering of flow variables is a stack
        m_flowVarNames.remove(flowVar.getName());
        m_flowVarNames.add(flowVar.getName());
    }

    /**
     * Get an iterator over flow variables. Note, that this iterator does not support the
     * remove() method.
     * @return iterator over flow variables
     */
    public Iterator<String> iterator() {
        final Iterator<String> iter = m_flowVarNames.iterator();
        return new Iterator<String>() {

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public String next() {
                return iter.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Get the number of flow variables in the repository.
     * @return the number of flow variables in the repository
     */
    public int size() {
        return m_flowVarNames.size();
    }

    /**
     * Get the identifiers of the flow variables in the repository.
     * @return the list of flow variable names.
     */
    public Collection<String> getFlowVariables() {
        return Collections.unmodifiableCollection(m_flowVarNames);
    }

    /**
     * Get the identifiers of the flow variables with given type.
     * @param className the type
     * @return the identifiers of the flow variables with given type.
     */
    public Collection<String> getFlowVariables(final Class<?> className) {
        Collection<String> flowVarNames = new LinkedHashSet<>();
        for (String name : m_flowVarNames) {
            if (isOfType(name, className)) {
                flowVarNames.add(name);
            }
        }
        return flowVarNames;
    }
}
