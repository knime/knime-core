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
 * History
 *   18.06.2007 (thor): created
 */
package org.knime.base.util;

/**
 * Simple class to convert wildcard patterns into regular expressions.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public final class WildcardMatcher {
//    private static class Node {
//        private char[] m_stateTransitionChars = new char[4];
//
//        private Node[] m_nextStates = new Node[4];
//
//        private int m_index;
//
//        public void addTransition(final char c, final Node nextState) {
//            if (m_index >= m_stateTransitionChars.length) {
//                resize();
//            }
//
//            if ((m_index > 0) && (m_stateTransitionChars[m_index - 1] == '\0')) {
//                m_stateTransitionChars[m_index] =
//                        m_stateTransitionChars[m_index - 1];
//                m_nextStates[m_index] = m_nextStates[m_index - 1];
//                m_stateTransitionChars[m_index - 1] = c;
//                m_nextStates[m_index - 1] = nextState;
//            } else {
//                m_stateTransitionChars[m_index] = c;
//                m_nextStates[m_index] = nextState;
//            }
//            m_index++;
//        }
//
//        public void addAnyTransition(final Node nextState) {
//            if (m_index >= m_stateTransitionChars.length) {
//                resize();
//            }
//
//            m_stateTransitionChars[m_index] = '\0';
//            m_nextStates[m_index] = nextState;
//            m_index++;
//        }
//
//        public void changeStarToQuestion(final Node nextState) {
//            if (!(m_stateTransitionChars[m_index - 1] == '\0')) {
//                throw new IllegalStateException(
//                        "Last transition was not a star");
//            }
//            if (m_nextStates[m_index - 1] != this) {
//                throw new IllegalStateException(
//                        "Last transition was not a star");
//            }
//            m_nextStates[m_index - 1] = nextState;
//        }
//
//        public boolean isStarNode() {
//            return (m_index == 1) && (m_nextStates[0] == this);
//        }
//
//        private void resize() {
//            char[] temp = new char[m_stateTransitionChars.length + 4];
//            System.arraycopy(m_stateTransitionChars, 0, temp, 0,
//                    m_stateTransitionChars.length);
//            m_stateTransitionChars = temp;
//
//            Node[] temp2 = new Node[m_nextStates.length + 4];
//            System.arraycopy(m_nextStates, 0, temp2, 0, m_nextStates.length);
//            m_nextStates = temp2;
//        }
//
//        public Node nextState(final char c) {
//            for (int i = 0; i < m_index; i++) {
//                if (m_stateTransitionChars[i] == c) {
//                    return m_nextStates[i];
//                } else if (m_stateTransitionChars[i] == '\0') {
//                    return m_nextStates[i];
//                }
//            }
//            return null;
//        }
//
//    }
//
//    private final Node m_startNode;
//
//    public WildcardMatcher(final String pattern) {
//        m_startNode = new Node();
//        Node n = m_startNode;
//        Node lastStar = null;
//        for (int i = 0; i < pattern.length(); i++) {
//            if (pattern.charAt(i) == '*') {
//                n.addAnyTransition(n);
//                lastStar = n;
//            } else if (pattern.charAt(i) == '?') {
//                Node newNode = new Node();
//                if (n.isStarNode()) {
//                    n.changeStarToQuestion(newNode);
//                    newNode.addAnyTransition(newNode);
//                    lastStar = newNode;
//                } else {
//                    n.addAnyTransition(newNode);
//                }
//                n = newNode;
//            } else {
//                Node newNode = new Node();
//                n.addTransition(pattern.charAt(i), newNode);
//                if (lastStar != null) {
//                    newNode.addTransition(pattern.charAt(i), newNode);
//                    newNode.addAnyTransition(lastStar);
//                }
//                n = newNode;
//            }
//        }
//    }
//
//    public boolean matches(final String s) {
//        Node n = m_startNode;
//
//        for (int i = 0; i < s.length(); i++) {
//            n = n.nextState(s.charAt(i));
//            if (n == null) {
//                return false;
//            }
//        }
//
//        return (n.m_nextStates == null);
//    }
//
//    public static void main(final String[] args) {
//        WildcardMatcher m = new WildcardMatcher("ab*ab");
//        System.out.println(m.matches("abaab"));
//    }
    
    private WildcardMatcher() { }
    
    /**
     * Converts a wildcard pattern containing '*' and '?' as meta characters
     * into a regular expression.
     * 
     * @param wildcard a wildcard expression 
     * @return the corresponding regular expression
     */
    public static String wildcardToRegex(final String wildcard) {
        StringBuilder buf = new StringBuilder(wildcard.length() + 20);
        
        for (int i = 0; i < wildcard.length(); i++) {
            char c = wildcard.charAt(i);
            switch (c) {
                case '*':
                    buf.append(".*");
                    break;
                case '?':
                    buf.append(".");
                    break;
                case '\\':
                case '^':
                case '$':
                case '[':
                case ']':
                case '{':
                case '}':
                case '(':
                case ')':
                case '|':
                case '+':
                case '.':
                    buf.append("\\");
                    buf.append(c);
                    break;
                default:
                    buf.append(c);
            }
        }
        
        return buf.toString();
    }
}
