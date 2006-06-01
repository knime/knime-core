/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   30.10.2005 (mb): created
 */
package de.unikn.knime.base.node.io.predictor;

import de.unikn.knime.core.node.defaultnodedialog.DefaultNodeDialogPane;
import de.unikn.knime.core.node.defaultnodedialog.DialogComponentFileChooser;

/** Dialog for the PredictorParams Reader Node - allows user to choose file name
 * and directory.
 * 
 * @author M. Berthold, University of Konstanz
 */
public class PredictorReaderNodeDialog extends DefaultNodeDialogPane {
    /** Constructor: create NodeDialog with just one default component,
     * the file chooser entry.
     */
    public PredictorReaderNodeDialog() {
        super("Reader Options");
        DialogComponentFileChooser fcComp
        = new DialogComponentFileChooser(PredictorReaderNodeModel.FILENAME, 
                new String[] {".pmml"});
        this.addDialogComponent(fcComp);
    }
    
}
