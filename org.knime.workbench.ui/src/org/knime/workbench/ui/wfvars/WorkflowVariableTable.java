/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 */
package org.knime.workbench.ui.wfvars;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.knime.core.node.Node;
import org.knime.core.node.workflow.FlowVariable;


/**
 * Displays workflow variables ({@link FlowVariable}s) with name, type and 
 * default value in a table.
 *  
 * @author Fabian Dill, KNIME.com GmbH
 */
public class WorkflowVariableTable implements Iterable<FlowVariable> {
    
    private static final String VAR_NAME = "Name";
    private static final String VAR_TYPE = "Type";
    private static final String VAR_VALUE = "Value";

    private static Image doubleImg;
    private static Image intImg;
    private static Image stringImg;
    
    private final List<FlowVariable>m_params 
        = new ArrayList<FlowVariable>();
    
    private final TableViewer m_viewer;
    
    /**
     * 
     * @param parent parent composite
     */
    public WorkflowVariableTable(final Composite parent) {
        m_viewer = createViewer(parent);
        m_viewer.setInput(m_params);
    }
    
    /**
     * 
     * @param param variable to be added to the table
     * @return true if the variable was added, false if there was already a 
     *  variable with same name and type
     * 
     */
    public boolean add(final FlowVariable param) {
        // equals of ScopeVariable checks for name and type
        if (m_params.contains(param)) {
            return false;
        }
        m_params.add(param);
        return true;
    }
    
    /**
     * 
     * @param param variable to be removed from the table
     */
    public void remove(final FlowVariable param) {
        m_params.remove(param);
    }
    
    /**
     * 
     * @param idx index of the variable
     * @return the variable
     */
    public FlowVariable get(final int idx) {
        return m_params.get(idx);
    }
    

    /**
     * 
     * @return an unmodifiable list of the represented {@link FlowVariable}s.
     */
    public List<FlowVariable> getVariables() {
        return Collections.unmodifiableList(m_params); 
    }
    
    /**
     * Replaces the variable at the given index with the new one.
     * @param index index of variable to be replaced (starts at 0)
     * @param param new variable which should be inserted at position index
     */
    public void replace(final int index, final FlowVariable param) {
        m_params.set(index, param);
    }
    
    /**
     * 
     * @return the underlying {@link TableViewer}
     */
    public TableViewer getViewer() {
        return m_viewer;
    }
    
    /**
     * 
     * @param var the variable to get the string representation of the value of
     * @return the string representation of the value
     */
    public static String getValueFrom(final FlowVariable var) {
        if (var.getType().equals(FlowVariable.Type.DOUBLE)) {
            return Double.toString(var.getDoubleValue());
        } 
        if (var.getType().equals(FlowVariable.Type.STRING)) {
            return var.getStringValue();
        }
        if (var.getType().equals(FlowVariable.Type.INTEGER)) {
            return Integer.toString(var.getIntValue());
        }
        throw new IllegalArgumentException(
                "Unsupported type " + var.getType()
                + " for Scope Variable " + var.getName());
    }
    
    private TableViewer createViewer(final Composite parent) {
        TableViewer viewer = new TableViewer(parent, SWT.SINGLE 
                | SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL 
                | SWT.V_SCROLL);
        
        TableViewerColumn nameCol = new TableViewerColumn(viewer, SWT.NONE);
        nameCol.getColumn().setText(VAR_NAME);
        nameCol.getColumn().setWidth(100);
        
        TableViewerColumn typeCol = new TableViewerColumn(viewer, SWT.NONE);
        typeCol.getColumn().setText(VAR_TYPE);
        typeCol.getColumn().setWidth(100);
        
        TableViewerColumn valueCol = new TableViewerColumn(viewer, SWT.NONE);
        valueCol.getColumn().setText(VAR_VALUE);
        valueCol.getColumn().setWidth(100);
        
        viewer.getTable().setHeaderVisible(true);
        viewer.getTable().setLinesVisible(true);
        
        viewer.setContentProvider(new WorkflowVariableInputProvider());
        viewer.setLabelProvider(new WorkflowVariableLabelProvider());
        
        viewer.setInput(m_params);
        
        return viewer;
    }
    
    private static class WorkflowVariableLabelProvider 
        extends LabelProvider implements ITableLabelProvider { 

        @Override
        public Image getColumnImage(final Object arg0, final int arg1) {
            // type column
            if (arg1 == 1) {
                FlowVariable.Type type = ((FlowVariable)arg0).getType();
                if (type.equals(FlowVariable.Type.DOUBLE)) {
                    if (doubleImg == null) {
                        URL url = Node.class.getResource(
                                "icon/scopevar_double.png");
                        doubleImg = ImageDescriptor.createFromURL(url)
                            .createImage();
                    }
                    return doubleImg;
                } 
                if (type.equals(FlowVariable.Type.INTEGER)) {
                    if (intImg == null) {
                        intImg = ImageDescriptor.createFromURL(
                                Node.class.getResource(
                                        "icon/scopevar_integer.png"))
                                        .createImage(); 
                    }
                    return intImg;
                }
                if (type.equals(FlowVariable.Type.STRING)) {
                    if (stringImg == null) {
                        stringImg = ImageDescriptor.createFromURL(
                                Node.class.getResource(
                                "icon/scopevar_string.png")).createImage(); 
                    }
                    return stringImg;
                }
            }
            return null;
        }

        @Override
        public String getColumnText(final Object arg0, final int arg1) {
            FlowVariable parameter = (FlowVariable)arg0;
            switch (arg1) {
            case 0: return parameter.getName();
            case 1: return parameter.getType().name();
            case 2: return getValueFrom(parameter);
            default: throw new RuntimeException(
                    "Invalid number of columns defined: " + arg1);
            }
        }
        
    }
    
    private static class WorkflowVariableInputProvider 
        implements IStructuredContentProvider {

        @Override
        public Object[] getElements(final Object arg0) {
            List<FlowVariable>params = (List<FlowVariable>)arg0;
            return params.toArray();
        }

        @Override
        public void dispose() {
            // do nothing -> images are static
        }

        @Override
        public void inputChanged(final Viewer arg0, final Object arg1, 
                final Object arg2) {
            // do nothing
        }
        
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public Iterator<FlowVariable> iterator() {
        return m_params.iterator();
    }
}
