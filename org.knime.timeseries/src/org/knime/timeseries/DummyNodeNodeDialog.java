package org.knime.timeseries;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

/**
 * <code>NodeDialog</code> for the "DummyNode" Node.
 * Dummy Node
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author KNIME GmbH
 */
public class DummyNodeNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring DummyNode node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected DummyNodeNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentNumber(
                new SettingsModelIntegerBounded(
                    DummyNodeNodeModel.CFGKEY_COUNT,
                    DummyNodeNodeModel.DEFAULT_COUNT,
                    Integer.MIN_VALUE, Integer.MAX_VALUE),
                    "Counter:", /*step*/ 1, /*componentwidth*/ 5));
                    
    }
}
