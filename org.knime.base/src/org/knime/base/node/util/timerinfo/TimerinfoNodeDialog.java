package org.knime.base.node.util.timerinfo;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

/**
 * <code>NodeDialog</code> for the <code>TimerInfo</code> Node.
 *
 * @author M. Berthold, KNIME AG, Zurich, Switzerland
 */
public class TimerinfoNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring MyExampleNode node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected TimerinfoNodeDialog() {
        super();

        addDialogComponent(new DialogComponentNumber(
                new SettingsModelIntegerBounded(
                    TimerinfoNodeModel.CFGKEY_MAXDEPTH,
                    /* default */ 2,
                    /*range */ 0, Integer.MAX_VALUE),
                    "Max Depth:", /*step*/ 1, /*componentwidth*/ 2));

    }
}

