/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
