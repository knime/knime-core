/*
 * ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   07.03.2008 (Kilian Thiel): created
 */
package org.knime.base.node.mine.mds;

/**
 * Contains the configuration keys of the MDS settings.
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class MDSConfigKeys {

    /**
     * The configuration key for the number of rows to use.
     */
    public static final String CFGKEY_ROWS = "Rows";

    /**
     * The configuration key for the usage of the number of max rows.
     */
    public static final String CFGKEY_USE_ROWS = "UseMaxRows";
    
    /**
     * The configuration key for the learning rate.
     */
    public static final String CFGKEY_LEARNINGRATE = "Learningrate";
    
    /**
     * The configuration key for the epochs.
     */
    public static final String CFGKEY_EPOCHS = "Epochs";
    
    /**
     * The configuration key for the distance.
     */
    public static final String CFGKEY_DISTANCE = "Distance";
    
    /**
     * The configuration key for output dimension.
     */
    public static final String CFGKEY_OUTDIMS = "OutDims";
    
    /**
     * The configuration key for columns to use.
     */
    public static final String CFGKEY_COLS = "Cols";
    
    /**
     * The configuration key for columns to use.
     */
    public static final String CFGKEY_SEED = "Seed";    
}
