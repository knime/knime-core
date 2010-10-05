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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn.fuzzy.norm;

/**
 * SCHWEIZER and SKLAR norm.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public final class YagerNorm implements Norm {
    /** 
     * Current parameter p.
     * if lim(p) -> 1 the more product norm otherwise min/max norm
     */
    private double m_p;

    /**
     * Inits a new NormYager object.
     * 
     * @param  p the power for this norm
     * @throws IllegalArgumentException if the parameter <code>p</code> less or 
     *         equal to zero
     */
    private YagerNorm(final double p) {
        // check p
        if (p <= 0.0) { throw new IllegalArgumentException(); }
        m_p = p;
    }

    /**
     * @param p current value of this norm
     * @return instance of this class 
     */
    public static final Norm getInstance(final double p) {
        return new YagerNorm(p);
    }

    /** 
     * @return instance of this class; default p = 2f 
     */
    public static final Norm getInstance() {
        return new YagerNorm(2.0);
    }

    /**
     * @param  a membership degree of fuzzy membership function
     * @param  b membership degree of fuzzy membership function
     * @return calculated fuzzy membership degree of <code>a</code> and 
     *         <code>b</code> using Yager_p norm
     */
    public double computeTNorm(final double a, final double b) {
        return 1 - Math.min(1, Math.pow(
            Math.pow(1 - a, m_p) + Math.pow(1 - b, m_p), 1 / m_p));
    }

    /** 
     * @param  a membership degree of fuzzy membership function
     * @param  b membership degree of fuzzy membership function
     * @return calculated fuzzy membership degree of <code>a</code> and 
     *         <code>b</code> using Yager_p Norm
     */
    public double computeTCoNorm(final double a, final double b) {
        return Math.min(1, Math.pow(
            Math.pow(a, m_p) + Math.pow(b, m_p), 1 / m_p));
    }
    
    /**
     * Returns the string representation as <b>Yager[p] Norm</b> of this norm.
     * @return an identifier for this norm
     */
    @Override
    public final String toString() {
        return "Yager[" + m_p + "] Norm";
    }
}
