/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
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
