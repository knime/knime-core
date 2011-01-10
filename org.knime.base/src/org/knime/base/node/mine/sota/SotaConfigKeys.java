/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Nov 16, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public final class SotaConfigKeys {
    private SotaConfigKeys() {
    }

    /**
     * Settings object key for the learningrate of the winnner.
     */
    public static final String CFGKEY_LR_WINNER = "LrWinner";

    /**
     * Settings object key for the learningrate of the ancestor.
     */
    public static final String CFGKEY_LR_ANCESTOR = "LrAncestor";

    /**
     * Settings object key for the learningrate of the sister.
     */
    public static final String CFGKEY_LR_SISTER = "LrSister";

    /**
     * Settings object key for the variability.
     */
    public static final String CFGKEY_VARIABILITY = "Variability";

    /**
     * Settings object key for the resource.
     */
    public static final String CFGKEY_RESOURCE = "Resource";

    /**
     * Settings object key for the minimal error.
     */
    public static final String CFGKEY_MIN_ERROR = "MinError";

    /**
     * Settings object key for the flag of the usage of variability.
     */
    public static final String CFGKEY_USE_VARIABILITY = "UseVar";

    /**
     * Settings object key for the distance to use.
     */
    public static final String CFGKEY_USE_DISTANCE = "UseDist";

    /**
     * Settings object key for the use of hierarchical fuzzy data.
     */
    public static final String CFGKEY_HIERARCHICAL_FUZZY_DATA = "HFD";

    /**
     * Settings object key for the use of hierarchical fuzzy level.
     */
    public static final String CFGKEY_HIERARCHICAL_FUZZY_LEVEL = "HFL";
    
    
    /**
     * Settings object key for the specification of the calls column.
     */
    public static final String CFGKEY_CLASSCOL = "CallColumn";
    
    /**
     * Settings object key for the specification of the usage of the class 
     * column.
     */
    public static final String CFGKEY_USE_CLASS_DATA = "UseClassColumn";
}
