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
package org.knime.workbench.ui.metainfo.editor;

import java.util.ArrayList;
import java.util.List;

import org.knime.workbench.ui.metainfo.model.MetaGUIElement;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 
 * @author Fabian Dill, KNIME.com GmbH
 */
public class MetaInfoInputHandler extends DefaultHandler {
    
    
    private StringBuffer m_buffer = new StringBuffer();

    private final List<MetaGUIElement>m_elements 
        = new ArrayList<MetaGUIElement>();
    
    private String m_currentForm;
    private String m_currentLabel;

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        m_buffer.append(ch, start, length);
    }

    
    @Override
    public void startElement(String uri, String localName, String name,
            Attributes atts) throws SAXException {
        if (name.equals(MetaGUIElement.ELEMENT)) {
            m_currentForm = atts.getValue(MetaGUIElement.FORM);
            m_currentLabel = atts.getValue(MetaGUIElement.NAME);
        }
    }

    @Override
    public void endElement(String uri, String localName, String name)
            throws SAXException {
        if (name.equals(MetaGUIElement.ELEMENT)) {
            m_elements.add(MetaGUIElement.create(m_currentForm, m_currentLabel, 
                    m_buffer.toString()));
            m_buffer = new StringBuffer();
        }        
    }
    
    public List<MetaGUIElement>getElements(){
        return m_elements;
    }


}
