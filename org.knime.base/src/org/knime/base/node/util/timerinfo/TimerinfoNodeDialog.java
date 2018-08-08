package org.knime.base.node.util.timerinfo;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;

/**
 * <code>NodeDialog</code> for the <code>TimerInfo</code> Node.
 *
 * @author M. Berthold, KNIME AG, Zurich, Switzerland
 */
final class TimerinfoNodeDialog extends DefaultNodeSettingsPane {

    TimerinfoNodeDialog() {
        addDialogComponent(new DialogComponentNumber(TimerinfoNodeModel.createMaxDepthSettingsModel(), "Max Depth:",
            /*step*/ 1, /*componentwidth*/ 2));

    }
}

