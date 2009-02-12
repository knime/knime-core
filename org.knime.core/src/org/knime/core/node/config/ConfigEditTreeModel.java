/*
 * ------------------------------------------------------------------ *
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
 *   Mar 29, 2008 (wiswedel): created
 */
package org.knime.core.node.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.workflow.ScopeVariable;

/**
 * Config editor that keeps a mask of variables to overwrite existing settings.
 * This class is used to modify node settings with values assigned from
 * scope variables. It also keeps a list of &quot;exposed variables&quot;, that
 * is each individual setting can be exported as a new variable.
 * <p>This class is not meant to be used anywhere else than in the KNIME 
 * framework classes.
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class ConfigEditTreeModel extends DefaultTreeModel {

    /** Factory method that parses the settings tree and constructs a new 
     * object of this class. It will use the mask as given by the second 
     * argument (which may be null, however).
     * @param settingsTree The original settings object.
     * @param variableTree The variables mask.
     * @return a new object of this class.
     * @throws InvalidSettingsException If setting can't be parsed
     */
    public static ConfigEditTreeModel create(final Config settingsTree, 
            final Config variableTree) throws InvalidSettingsException {
        ConfigEditTreeModel result = create(settingsTree);
        ((ConfigEditTreeNode)result.getRoot()).readVariablesFrom(
                variableTree, false);
        return result;
    }
    
    /** Parses a settings tree and creates an empty mask 
     * (for later modification).
     * @param settingsTree to be parsed.
     * @return a new object of this class.
     */
    public static ConfigEditTreeModel create(final Config settingsTree) {
        ConfigEditTreeNode rootNode = new ConfigEditTreeNode(settingsTree);
        recursiveAdd(rootNode, settingsTree);
        return new ConfigEditTreeModel(rootNode);
    }
    
    /** Recursive construction of tree. */
    private static void recursiveAdd(final ConfigEditTreeNode treeNode, 
            final Config configValue) {
        assert (configValue == treeNode.getUserObject().m_configEntry);
        for (String s : configValue.keySet()) {
            AbstractConfigEntry childValue = configValue.getEntry(s);
            ConfigEditTreeNode childTreeNode = 
                new ConfigEditTreeNode(childValue);
            if (childValue.getType().equals(ConfigEntries.config)) {
                recursiveAdd(childTreeNode, (Config)childValue);
            }
            treeNode.add(childTreeNode);
        }
    }
    
    /** @param rootNode root node. */
    private ConfigEditTreeModel(final ConfigEditTreeNode rootNode) {
        super(rootNode);
    }
    
    /** @return true if there is any mask (overwriting settings) below this
     * branch of the tree. */
    public boolean hasConfiguration() {
        return ((ConfigEditTreeNode)getRoot()).hasConfiguration();
    }
    
    /** Write the mask to a config object (for storage in node settings object).
     * @param config To save to. */
    public void writeVariablesTo(final Config config) {
        // false = do not write "model" as root
        ((ConfigEditTreeNode)getRoot()).writeVariablesTo(config, false);
    }
    
    /** Modifies the first argument to reflect the values of the mask 
     * represented by this object.
     * @param settingsTree settings tree to modify (supposed to have 
     * equivalent tree structure)
     * @param variables The has of variables-values to apply.
     * @return A list of exposed variables
     * @throws InvalidSettingsException If reading fails
     */
    public List<ScopeVariable> overwriteSettings(final Config settingsTree, 
            final Map<String, ScopeVariable> variables) 
        throws InvalidSettingsException {
        return ((ConfigEditTreeNode)getRoot()).overwriteSettings(
                settingsTree, variables, false);
    }
    
    /** {@inheritDoc} */
    @Override
    public String toString() {
        return ((ConfigEditTreeNode)getRoot()).toString();
    }
    
    /** Single Tree node implementation. */
    static final class ConfigEditTreeNode extends DefaultMutableTreeNode {
        
        /** Constructs new tree node based on a representative config entry.
         * @param entry To wrap. */
        ConfigEditTreeNode(final AbstractConfigEntry entry) {
            super(new Wrapper(entry));
            setAllowsChildren(!getUserObject().isLeaf());
        }
        
        /** {@inheritDoc} */
        @Override
        public void setUserObject(final Object arg) {
            throw new IllegalStateException("Not intended to be called");
        }
        
        /** {@inheritDoc} */
        @Override
        public Wrapper getUserObject() {
            return (Wrapper)super.getUserObject();
        }
        
        /** @return associated config entry. */
        public AbstractConfigEntry getConfigEntry() {
            return getUserObject().m_configEntry;
        }
        
        /** @param value the new variable to use. */
        public void setUseVariableName(final String value) {
            if (value == null || value.length() == 0) {
                getUserObject().m_useVarName = null;
            } else {
                getUserObject().m_useVarName = value;
            }
        }
        
        /** @return the new variable to use. */
        public String getUseVariableName() {
            return getUserObject().m_useVarName;
        }
        
        /** @param variableName The name of the variable, which represents this
         * node's value. */
        public void setExposeVariableName(final String variableName) {
            if (variableName == null || variableName.length() == 0) {
                getUserObject().m_exposeVarName = null; 
            } else {
                getUserObject().m_exposeVarName = variableName;
            }
        }
        
        /** @return the exported variable name. */
        public String getExposeVariableName() {
            return getUserObject().m_exposeVarName;
        }
        
        /** Implementation of {@link ConfigEditTreeModel#hasConfiguration()}.
         * @return if mask exists in this node or any child node. */
        public boolean hasConfiguration() {
            if (getUserObject().isLeaf()) {
                return getUseVariableName() != null 
                    || getExposeVariableName() != null;
            }
            for (Enumeration<?> e = children(); e.hasMoreElements();) {
                ConfigEditTreeNode c = (ConfigEditTreeNode)e.nextElement();
                if (c.hasConfiguration()) {
                    return true;
                }
            }
            return false;
        }
        
        private static final String CFG_USED_VALUE = "used_variable";
        private static final String CFG_EXPOSED_VALUE = "exposed_variable";
        
        /** Persistence method to restore the tree. */
        private void readVariablesFrom(final Config variableTree, 
                final boolean readThisEntry) throws InvalidSettingsException {
            Config subConfig;
            if (readThisEntry) {
                String key = getConfigEntry().getKey();
                if (!variableTree.containsKey(key)) {
                    return;
                }
                subConfig = variableTree.getConfig(key);
            } else {
                subConfig = variableTree;
            }
            if (getUserObject().isLeaf()) {
                setUseVariableName(subConfig.getString(CFG_USED_VALUE));
                setExposeVariableName(subConfig.getString(CFG_EXPOSED_VALUE));
            } else {
                for (Enumeration<?> e = children(); e.hasMoreElements();) {
                    ConfigEditTreeNode c = (ConfigEditTreeNode)e.nextElement();
                    c.readVariablesFrom(subConfig, true);
                }
            }
        }
        
        /** Persistence method to save the tree. */
        private void writeVariablesTo(final Config variableTree, 
                final boolean writeThisEntry) {
            if (!hasConfiguration()) {
                return;
            }
            Config subConfig;
            if (writeThisEntry) {
                String key = getConfigEntry().getKey();
                subConfig = variableTree.addConfig(key);
            } else {
                subConfig = variableTree;
            }
            if (getUserObject().isLeaf()) {
                subConfig.addString(CFG_USED_VALUE, getUseVariableName());
                subConfig.addString(CFG_EXPOSED_VALUE, getExposeVariableName());
            } else {
                for (Enumeration<?> e = children(); e.hasMoreElements();) {
                    ConfigEditTreeNode c = (ConfigEditTreeNode)e.nextElement();
                    c.writeVariablesTo(subConfig, true);
                }
            }
        }
        
        /** Implementation of {@link ConfigEditTreeModel#overwriteSettings(
         * Config, Map)}, see above method description for details. */
        private List<ScopeVariable> overwriteSettings(final Config counterpart,
                final Map<String, ScopeVariable> variables, 
                final boolean isCounterpartParent) 
            throws InvalidSettingsException {
            if (!hasConfiguration()) {
                return Collections.emptyList();
            }
            List<ScopeVariable> result = null;
            AbstractConfigEntry thisEntry = getConfigEntry();
            String key = thisEntry.getKey();
            AbstractConfigEntry original;
            if (isCounterpartParent) {
                original = counterpart.getEntry(key);
            } else {
                original = counterpart;
            }
            if (original == null) {
                throw new InvalidSettingsException(
                        "No matching element found for entry with key: " + key);
            }
            if (!original.getType().equals(thisEntry.getType())) {
                throw new InvalidSettingsException("Non matching config "
                        + "elements for key \"" + key + "\", "
                        + original.getType() + " vs. " + thisEntry.getType());
            }
            String varName = getUseVariableName();
            if (varName != null) {
                switch (original.getType()) {
                case xboolean:
                    String bool = 
                        getStringVariable(varName, variables);
                    if (bool == null) {
                        throw new InvalidSettingsException("Value of \"" 
                                + varName + "\" is null");
                    }
                    bool = bool.toLowerCase();
                    if (bool.equals("true") || bool.equals("false")) {
                        counterpart.addBoolean(key, Boolean.parseBoolean(bool));
                    } else {
                        throw new InvalidSettingsException("Unable to parse \""
                                + bool + "\" (variable \"" + varName + "\")"
                                + " as boolean expression (settings "
                                + "parameter \"" + key + "\")");
                    }
                    break;
                case xchar:
                    String charS = getStringVariable(varName, variables);
                    if (charS != null && charS.length() == 1) {
                        counterpart.addChar(key, charS.charAt(0));
                    } else {
                        throw new InvalidSettingsException(
                                "Unable to parse \"" + charS + "\" (variable \""
                                + varName + "\") as char "
                                + "(settings parameter \"" + key + "\")");
                    }
                    break;
                case xstring:
                    String s = getStringVariable(varName, variables);
                    counterpart.addString(key, s);
                    break;
                case xlong:
                case xint:
                case xshort:
                case xbyte:
                    int value = getIntVariable(varName, variables);
                    int min, max;
                    switch (original.getType()) {
                    case xlong:
                        counterpart.addLong(key, value);
                        min = Integer.MIN_VALUE;
                        max = Integer.MAX_VALUE;
                        break;
                    case xint:
                        min = Integer.MIN_VALUE;
                        max = Integer.MAX_VALUE;
                        counterpart.addInt(key, value);
                        break;
                    case xshort:
                        min = Short.MIN_VALUE;
                        max = Short.MAX_VALUE;
                        counterpart.addShort(key, (short)value);
                        break;
                    case xbyte:
                        min = Byte.MIN_VALUE;
                        max = Byte.MAX_VALUE;
                        counterpart.addByte(key, (byte)value);
                        break;
                    default:
                        assert false : "Unreachable case";
                        min = Integer.MIN_VALUE;
                        max = Integer.MAX_VALUE;
                    }
                    if (value < min || value > max) {
                        throw new InvalidSettingsException(
                                "Value of variable \"" + varName 
                                + "\" can't be cast to " + original.getType() 
                                + "(settings parameter " + "\"" + key 
                                + "\"), out of range: " + value);
                    }
                    break;
                case xfloat:
                case xdouble:
                    double doubleValue = getDoubleVariable(varName, variables);
                    if (original.getType().equals(ConfigEntries.xdouble)) {
                        counterpart.addDouble(key, doubleValue);
                    } else {
                        counterpart.addFloat(key, (float)doubleValue);
                    }
                    break;
                default:
                    assert false : "Unreachable case: " + original.getType();
                }
            }
            String newVar = getExposeVariableName();
            if (newVar != null) {
                assert isLeaf() && isCounterpartParent;
                AbstractConfigEntry newValue = counterpart.getEntry(key);
                ScopeVariable exposed;
                switch (newValue.getType()) {
                case xboolean:
                    boolean b = ((ConfigBooleanEntry)newValue).getBoolean();
                    exposed = new ScopeVariable(newVar, Boolean.toString(b));
                    break;
                case xstring:
                    String s = ((ConfigStringEntry)newValue).getString();
                    exposed = new ScopeVariable(newVar, s);
                    break;
                case xchar:
                    char c = ((ConfigCharEntry)newValue).getChar();
                    exposed = new ScopeVariable(newVar, Character.toString(c));
                    break;
                case xbyte:
                    byte by = ((ConfigByteEntry)newValue).getByte();
                    exposed = new ScopeVariable(newVar, by);
                    break;
                case xshort:
                    short sh = ((ConfigShortEntry)newValue).getShort();
                    exposed = new ScopeVariable(newVar, sh);
                    break;
                case xint:
                    int i = ((ConfigIntEntry)newValue).getInt();
                    exposed = new ScopeVariable(newVar, i);
                    break;
                case xlong:
                    long l = ((ConfigLongEntry)newValue).getLong();
                    if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
                        throw new InvalidSettingsException(
                                "Can't export value \"" + l + "\" as " 
                                + "variable \"" + newVar + "\", out of range");
                    }
                    exposed = new ScopeVariable(newVar, (int)l);
                    break;
                case xfloat:
                    float f = ((ConfigFloatEntry)newValue).getFloat();
                    exposed = new ScopeVariable(newVar, f);
                    break;
                case xdouble:
                    double d = ((ConfigDoubleEntry)newValue).getDouble();
                    exposed = new ScopeVariable(newVar, d);
                    break;
                default:
                    throw new InvalidSettingsException("Can't export " 
                            + newValue.getType() + " as variable \"" 
                            + newVar + "\"");
                }
                result = new ArrayList<ScopeVariable>();
                result.add(exposed);
            }

            if (!isLeaf()) {
                for (Enumeration<?> e = children(); e.hasMoreElements();) {
                    ConfigEditTreeNode c = (ConfigEditTreeNode)e.nextElement();
                    List<ScopeVariable> r = c.overwriteSettings(
                            (Config)original, variables, true);
                    if (!r.isEmpty()) {
                        if (result == null) {
                            result = new ArrayList<ScopeVariable>();
                        }
                        result.addAll(r);
                    }
                }
            }
            if (result == null) {
                result = Collections.emptyList();
            }
            return result;
        }
        
        /** {@inheritDoc} */
        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            toString(b, "");
            return b.toString();
        }
        
        /** Recursion method to get a string representation of this tree.
         * @param b to append to
         * @param indent indentation.
         */
        public void toString(final StringBuilder b, final String indent) {
            boolean edited = getUseVariableName() != null
                || getExposeVariableName() != null;
            String thisEntry = getUserObject().toString() + (edited ? "*" : "");
            b.append(indent + thisEntry + "\n");
            for (Enumeration<?> e = children(); e.hasMoreElements();) {
                ConfigEditTreeNode c = (ConfigEditTreeNode)e.nextElement();
                c.toString(b, indent + "  ");
            }
        }
        
        /** Getter method throws excpetion of variable name does not exist
         * in map. */
        private static ScopeVariable getVariable(final String varString,
                final Map<String, ScopeVariable> variables) 
            throws InvalidSettingsException {
            ScopeVariable var = variables.get(varString);
            if (var == null) {
                throw new InvalidSettingsException(
                        "Unknown variable \"" + varString + "\"");
            }
            return var;
        }
        
        /** Getter method to get double value. */
        private static double getDoubleVariable(final String varString,
                final Map<String, ScopeVariable> variables) 
            throws InvalidSettingsException {
            ScopeVariable v = getVariable(varString, variables);
            switch (v.getType()) {
            case DOUBLE:
                return v.getDoubleValue();
            case INTEGER:
                return v.getIntValue();
            default:
                throw new InvalidSettingsException("Can't evaluate variable \""
                        + varString + "\" as double expression, it is a "
                        + v.getType() + " (\"" + v + "\")");
            }
        }
        
        /** Getter method to get int value. */
        private static int getIntVariable(final String varString,
                final Map<String, ScopeVariable> variables) 
        throws InvalidSettingsException {
            ScopeVariable v = getVariable(varString, variables);
            switch (v.getType()) {
            case INTEGER:
                return v.getIntValue();
            default:
                throw new InvalidSettingsException("Can't evaluate variable \""
                        + varString + "\" as integer expression, it's a "
                        + v.getType() + " (\"" + v + "\")");
            }
        }
        
        /** Getter method to get string value. */
        private static String getStringVariable(final String varString,
                final Map<String, ScopeVariable> variables) 
        throws InvalidSettingsException {
            ScopeVariable v = getVariable(varString, variables);
            switch (v.getType()) {
            case INTEGER:
                return Integer.toString(v.getIntValue());
            case DOUBLE:
                return Double.toString(v.getDoubleValue());
            case STRING:
                return v.getStringValue();
            default:
                throw new InvalidSettingsException("Can't evaluate variable \""
                        + varString + "\" as string expression, it's a "
                        + v.getType() + " (\"" + v + "\")");
            }
        }
    }
    
    /** User object in tree node wrapping config entry, used variable name and
     * possibly a variable name for exporting the value as variable. */
    private static final class Wrapper {
        private final AbstractConfigEntry m_configEntry;
        private String m_useVarName;
        private String m_exposeVarName;
        
        /** @param entry Entry to wrap. */
        public Wrapper(final AbstractConfigEntry entry) {
            m_configEntry = entry;
        }
        
        /** @return true if this represents a leaf (not a config object) */
        boolean isLeaf() {
            return !(m_configEntry instanceof Config);
        }
        
        /** {@inheritDoc} */
        @Override
        public String toString() {
            return m_configEntry.toString();
        }
    }
    
}
