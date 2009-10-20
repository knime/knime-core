/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * Fuzzy norm interface which implements the fuzzy operator for conjunction and
 * disjunction, also known as t-norm and tco-norm.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public interface Norm {
    /** Choice of fuzzy norm. */
    public static final String NORM_KEY = "norm";

    /** Number of available norm choices. */
    public static final Norm[] NORMS = new Norm[]{MinMaxNorm.getInstance(),
            ProdNorm.getInstance(), LukaNorm.getInstance(),
            YagerNorm.getInstance(2.0), YagerNorm.getInstance(0.5)};

    /**
     * Computes the fuzzy disjunction.
     * 
     * @param a the membership degree of fuzzy membership function A
     * @param b the membership degree of fuzzy membership function B
     * @return the calculated fuzzy membership degree of a and b using TNorm
     */
    double computeTNorm(double a, double b);

    /**
     * Computes fuzzy conjunction.
     * 
     * @param a the membership degree of fuzzy membership function A
     * @param b the membership degree of fuzzy membership function B
     * @return the calculated fuzzy membership degree of a and b using TCoNorm
     */
    double computeTCoNorm(double a, double b);
}
