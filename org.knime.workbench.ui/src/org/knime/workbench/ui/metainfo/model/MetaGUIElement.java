/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
package org.knime.workbench.ui.metainfo.model;

import java.util.HashSet;
import java.util.Set;

import javax.xml.transform.sax.TransformerHandler;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.xml.sax.SAXException;

/**
 * 
 * @author Fabian Dill, KNIME.com GmbH
 */
public abstract class MetaGUIElement {
    
    public static final String ELEMENT = "element";
    public static final String FORM = "form";
    public static final String TEXT = "text";
    public static final String NAME = "name";
    public static final String DATE = "date";
    public static final String MULTILINE = "multiline";
    
    private final String m_label;
    private final String m_value;
    
    private Control m_control; 
    
    private final Set<ModifyListener> m_listener = new HashSet<ModifyListener>();
    
    public MetaGUIElement(final String label, final String value) {
        m_label = label;
        m_value = value;
    }
    
    public String getLabel() {
        return m_label;
    }
    
    protected String getValue() {
        return m_value;
    }
    
    protected void setControl(final Control control) {
        m_control = control;
    }
    
    protected Control getControl() {
        return m_control;
    }
    
    public void addListener(final ModifyListener listener) {
        m_listener.add(listener);
    }
    
    protected void fireModifiedEvent(final ModifyEvent e) {
        for (ModifyListener l : m_listener) {
            l.modifyText(e);
        }
    }
    
    /**
     * Creates and returns thereferring GUI element. The element must also be 
     * stored locally in order to be able to store the changes in the GUI 
     * element in the 
     * 
     * @param parent parent element
     * @param style {@link SWT} style
     * @return the created {@link Control}
     */
    public abstract Control createGUIElement(final FormToolkit toolkit, 
            final Composite parent);

    
    /**
     * Save the new value entered in the GUI element to the XML element, which 
     * has the following syntax:
     * <code>
     * &lt;element form="text" name="Author"&gt;Dill&lt;/element&gt;
     * </code>
     * where <em>form</em> is the GUI element to be displayed, <em>name</em> is
     * the label. 
     * 
     * @param parentElement to add the complete element to
     */
    public abstract void saveTo(final TransformerHandler parentElement) 
        throws SAXException;
    
   
    public static MetaGUIElement create(final String form, final String label,
            final String value) {
        MetaGUIElement element = null;
        if (form.trim().equals(TEXT)) {
            element = new TextMetaGUIElement(label, value);
        } else if (form.trim().equals(DATE)) {
            element = new DateMetaGUIElement(label, value);
        } else if (form.trim().equals(MULTILINE)) {
            element = new MultilineMetaGUIElement(label, value);   
        }
        return element;
    }
}
