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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   09.04.2008 (gabriel): created
 */
package org.knime.base.node.preproc.transpose;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class TransposeTableNodeDialogPane extends DefaultNodeSettingsPane {

    /**
     * Create new dialog with option to set number of columns to chunk.
     */
    TransposeTableNodeDialogPane() {
        super.addDialogComponent(new DialogComponentNumber(
                createChunkSizeModel(), "Chunk size (columns): ", 10)); 
    }
    
    /**
     * @return bounded integer model for chunk size
     */
    static final SettingsModelIntegerBounded createChunkSizeModel() {
        return new SettingsModelIntegerBounded(
                "chunk_size", 10, 1, Integer.MAX_VALUE);
    }
    
}
