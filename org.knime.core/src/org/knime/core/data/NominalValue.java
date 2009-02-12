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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   15.12.2006 (M. Berthold): created
 */
package org.knime.core.data;

/**
 * Interface of a {@link org.knime.core.data.def.StringCell}, which does
 * not enforce additional functionality but indicates that cells implementing
 * this value can be used as nominal values. This allows for example to
 * avoid nominal value enumeration for data cells holding smiles.
 * 
 * @author mb, University of Konstanz
 */
public interface NominalValue extends DataValue {

    /** Meta information for this value type - reuses underlying base
     * class because this value does not offer new functionality but
     * is really only used to differentiate the ability of types.
     * 
     * @see DataValue#UTILITY
     */
    public static final UtilityFactory UTILITY = DataValue.UTILITY;

}
