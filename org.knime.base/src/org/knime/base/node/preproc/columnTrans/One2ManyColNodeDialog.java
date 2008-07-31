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
 */
package org.knime.base.node.preproc.columnTrans;



import org.knime.core.data.NominalValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class One2ManyColNodeDialog extends DefaultNodeSettingsPane {
    
    
    private DialogComponentColumnFilter m_columnFilter;


    /**
     * A node dialog with one column filter to select those columns,
     * which should be transformed into many columns.
     *
     */
    @SuppressWarnings("unchecked")
    public One2ManyColNodeDialog() {
        super();
        m_columnFilter = new DialogComponentColumnFilter(
                new SettingsModelFilterString(
                        One2ManyColNodeModel.CFG_COLUMNS), 0, 
                        NominalValue.class);
        setDefaultTabTitle("Columns to transform");
        addDialogComponent(m_columnFilter);
    }
}
