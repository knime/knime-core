/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   09.03.2011 (hofer): created
 */
package org.knime.core.data.xml.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

import org.knime.core.node.InvalidSettingsException;

/**
 *
 * @author Heiko Hofer
 */
public class LimitedXPathMatcher {
    private Map<QName[], Integer> m_inAktivePaths;
    private Set<QName[]> m_aktivePaths;
    private int m_depth;
    private boolean m_rootMatches;
    private boolean m_nodeMatches;

    LimitedXPathMatcher() {
        try {
            init("/", null);
        } catch (InvalidSettingsException e) {
            // this should never happen, if it does it is an programming error
            throw new IllegalStateException(e);
        }
    }


    public LimitedXPathMatcher(final String xpath, final NamespaceContext nsContext)
                throws InvalidSettingsException {
        m_rootMatches = false;
        init(xpath, nsContext);
    }

    private void init(final String xpath, final NamespaceContext nsContext)
        throws InvalidSettingsException {
        m_aktivePaths = new HashSet<QName[]>();
        StringTokenizer tokenizer = new StringTokenizer(xpath.trim(),
                "| \t\n");
        while(tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.trim().equals("/")) {
                m_rootMatches = true;
            } else {
                StringTokenizer tok = new StringTokenizer(token.trim(),
                "/ \t\n");
                List<QName> xpathTokens = new ArrayList<QName>();
                while(tok.hasMoreTokens()) {
                    String qname = tok.nextToken();
                    int colon = qname.indexOf(':');
                    String prefix = colon > -1
                        ? qname.substring(0, colon)
                        : XMLConstants.DEFAULT_NS_PREFIX;
                    String localName = colon > -1
                        ? qname.substring(colon + 1)
                        : qname;
                    String nsURI = prefix.isEmpty()
                        ? XMLConstants.NULL_NS_URI
                        : nsContext.getNamespaceURI(prefix);
                    if (null == nsURI) {
                        throw new InvalidSettingsException("Please specify a "
                                + "namespace for the prefix: \"" + prefix
                                + "\"");
                    }

                    xpathTokens.add(new QName(nsURI, localName, prefix));
                    m_nodeMatches = true;

                }
                m_aktivePaths.add(
                        xpathTokens.toArray(new QName[xpathTokens.size()]));
            }
        }
        m_inAktivePaths = new HashMap<QName[], Integer>();

        m_depth = 0;
    }

    /**
     * Called in the XMLStreamConstants.START_ELEMENT call back of the
     * XMLStreamReader. Returns this element matches the xpath expression
     * given in the constructor.
     * @param name name of the element
     * @return true when element matches the xpath
     */
    boolean startElement(final QName name) {
        if (m_aktivePaths.isEmpty()) {
            m_depth++;
           return false;
        } else {
            boolean match = false;
            for (Iterator<QName[]> iter = m_aktivePaths.iterator();
                iter.hasNext();) {
                QName[] xpath = iter.next();
                if (m_depth < xpath.length && m_depth >= 0
                        && xpath[m_depth].getLocalPart().equals(name.getLocalPart())
                        && xpath[m_depth].getNamespaceURI().equals(name.getNamespaceURI())) {
                    if (xpath.length == m_depth + 1) {
                        match = true;
                    }
                } else {
                    iter.remove();
                    m_inAktivePaths.put(xpath, m_depth);
                }
            }
            m_depth++;
            return match;
        }
    }

    /**
     * Called in the XMLStreamConstants.START_ELEMENT call back of the
     * XMLStreamReader.
     */
    void endElement() {
        m_depth--;
        for (Iterator<Entry<QName[], Integer>> iter =
            m_inAktivePaths.entrySet().iterator(); iter.hasNext();) {
            Entry<QName[], Integer> entry = iter.next();
            if (m_depth <= entry.getValue()) {
                iter.remove();
                m_aktivePaths.add(entry.getKey());
            }
        }
    }

    /**
     * @return
     */
    public boolean rootMatches() {
        return m_rootMatches;
    }

    /**
     * @return
     */
    public boolean nodeMatches() {
        return m_nodeMatches;
    }

}
