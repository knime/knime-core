/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 * and KNIME GmbH, Konstanz, Germany
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   21.12.2006 (ohl): created
 */
package org.knime.testing.core;

import java.util.LinkedList;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.varia.LevelRangeFilter;

/**
 * A custom implementation of an Appender that stores all messages it receives 
 * of a certain level (within a certain level range). An upper level for the
 * number of messages stored must be provided.
 * 
 * @author ohl, University of Konstanz
 */
public class TestingAppender extends AppenderSkeleton {
    
    private int m_maxlines;
    
    private LinkedList<String> m_messageQueue;

    /**
     * Constructor. Adds itself as appender to the root logger.
     * 
     * @param minLevel the minimum level of messages that should be stored.
     * @param maxLevel the maximum level of messages that should be stored.
     * @param maxLines the last maxLines messages will be stored.
     */
    public TestingAppender(final Level minLevel, final Level maxLevel, 
            final int maxLines) {
        m_messageQueue = new LinkedList<String>();
        m_maxlines = maxLines;
        
        Logger root = Logger.getRootLogger();
        LevelRangeFilter filter = new LevelRangeFilter();
        filter.setLevelMin(minLevel);
        filter.setLevelMax(maxLevel);
        this.addFilter(filter);
        root.addAppender(this);
    }
    
    /**
     * 
     * @see org.apache.log4j.AppenderSkeleton#
     * append(org.apache.log4j.spi.LoggingEvent)
     */
    @Override
    protected void append(final LoggingEvent aEvent) {
        
        m_messageQueue.add(aEvent.getRenderedMessage());
        
        // make sure the list won't get too long
        if (m_messageQueue.size() > m_maxlines) {
            m_messageQueue.removeFirst();
        }
        
    }

    /**
     * Disconnects this appender from the root logger.
     */
    public void disconnect() {
        Category.getRoot().removeAppender(this);
        
    }
    
    /**
     * @return the error messages received so far.
     */
    public String[] getReceivedMessages() {
        return m_messageQueue.toArray(new String[m_messageQueue.size()]);
    }
    
    /**
     * @return the number of error messages received so far.
     */
    public int getMessageCount() {
        return m_messageQueue.size();
    }
    
    /**
     * 
     * @see org.apache.log4j.AppenderSkeleton#close()
     */
    @Override
    public void close() {
    }

    /**
     * does not require layout.
     * @see org.apache.log4j.AppenderSkeleton#requiresLayout()
     */
    @Override
    public boolean requiresLayout() {
        return false;
    }
}
