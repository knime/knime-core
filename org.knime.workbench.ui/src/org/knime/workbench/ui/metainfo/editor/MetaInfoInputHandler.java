/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
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
 * @author Fabian Dill, KNIME.com AG
 */
public class MetaInfoInputHandler extends DefaultHandler {


    private StringBuffer m_buffer = new StringBuffer();

    private final List<MetaGUIElement>m_elements
        = new ArrayList<MetaGUIElement>();

    private String m_currentForm;
    private String m_currentLabel;
    private boolean m_isReadOnly;

    @Override
    public void characters(final char[] ch, final int start, final int length)
            throws SAXException {
        m_buffer.append(ch, start, length);
    }


    @Override
    public void startElement(final String uri, final String localName, final String name,
            final Attributes atts) throws SAXException {
        if (localName.equals(MetaGUIElement.ELEMENT)) {
            m_currentForm = atts.getValue(MetaGUIElement.FORM);
            m_currentLabel = atts.getValue(MetaGUIElement.NAME);
            m_isReadOnly = Boolean.valueOf(
                    atts.getValue(MetaGUIElement.READ_ONLY));

        }
    }

    @Override
    public void endElement(final String uri, final String localName, final String name)
            throws SAXException {
        if (localName.equals(MetaGUIElement.ELEMENT)) {
            m_elements.add(MetaGUIElement.create(m_currentForm, m_currentLabel,
                    m_buffer.toString(), m_isReadOnly));
            m_buffer = new StringBuffer();
        }
    }

    public List<MetaGUIElement>getElements() {
        return m_elements;
    }


}
