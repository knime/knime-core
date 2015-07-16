package org.knime.base.node.rules.engine.totable;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;

/**
 * <code>NodeDialog</code> for the "Rules to Table" and the "Decision Tree to Rules" Node.
 * Converts PMML RuleSets (with <tt>firstHit</tt>) to table containing the rules.
 *
 * @author Gabor Bakos
 */
public class RulesToTableNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the RulesToTable node.
     */
    public RulesToTableNodeDialog() {
        RulesToTableSettings settings = new RulesToTableSettings();
        addDialogComponent(new DialogComponentBoolean(settings.getSplitRules(), "Split rules to condition and outcome columns"));
        addDialogComponent(new DialogComponentBoolean(settings.getConfidenceAndWeight(), "Add confidence and weight columns"));
        addDialogComponent(new DialogComponentBoolean(settings.getProvideStatistics(), "Add Record count and Number of correct statistics columns"));
        addDialogComponent(new DialogComponentBoolean(settings.getAdditionalParentheses(), "Use additional parentheses to document precedence rules"));
    }
}

