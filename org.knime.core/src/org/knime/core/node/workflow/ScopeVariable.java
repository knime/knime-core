/*
 * ------------------------------------------------------------------ *
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
 *   Mar 15, 2007 (mb): created
 */
package org.knime.core.node.workflow;



/**
 * ScopeContext interface holding local variables of basic type.
 * 
 * @author M. Berthold, University of Konstanz
 */
public final class ScopeVariable extends ScopeObject {

    public static enum Type {DOUBLE, INTEGER, STRING};
    
    private final Type m_type;
    private final String m_name;
    private String m_valueS = null;
    private double m_valueD = Double.NaN;
    private int m_valueI = 0;
    
    public ScopeVariable(final String name, final String valueS) {
        m_type = Type.STRING;
        m_name = name;
        m_valueS = valueS;
    }

    public ScopeVariable(final String name, final double valueD) {
        m_type = Type.DOUBLE;
        m_name = name;
        m_valueD = valueD;
    }

    public ScopeVariable(final String name, final int valueI) {
        m_type = Type.INTEGER;
        m_name = name;
        m_valueI = valueI;
    }
    
    public String getName() {
        return m_name;
    }
    
    /** @return the type */
    public Type getType() {
        return m_type;
    }
    
    public String getStringValue() {
        if (m_type != Type.STRING) {
            return null;
        }
        return m_valueS;
    }

    public double getDoubleValue() {
        if (m_type != Type.DOUBLE) {
            return Double.NaN;
        }
        return m_valueD;
    }

    public int getIntValue() {
        if (m_type != Type.INTEGER) {
            return 0;
        }
        return m_valueI;
    }
    
    /** {@inheritDoc} */
    @Override
    public String toString() {
        String value;
        switch (m_type) {
        case DOUBLE: value = Double.toString(m_valueD); break;
        case INTEGER: value = Integer.toString(m_valueI); break;
        case STRING: value = m_valueS; break;
        default: throw new InternalError("m_type must not be null");
        }
        return "SV: \"" + m_name + "\" (" + m_type + ": " + value + ")"; 
    }
    
}
