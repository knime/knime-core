/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
 *   09.06.2006 (koetter): created
 */
package org.knime.ext.ainet.unittests;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.knime.ext.ainet.agents.geneontology.GOTerm;
import org.knime.ext.ainet.core.AnnotationType;
import org.knime.ext.ainet.data.geneontology.GOEntry;
import org.knime.ext.ainet.data.geneontology.GOEntryFactory;

/**
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class GOEntryTest extends TestCase {
    
    private GOEntry m_entry;
    private GOEntry m_anotherEntry;
    private GOTerm m_term;
    private GOTerm m_anotherTerm;

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        this.m_term =
            new GOTerm("GO:0014035", "Hello GO name", "F", null, null);
        this.m_entry = GOEntryFactory.create(m_term);
        //test if the annotation type is correct
        assertEquals("This should be a GO_TERM type.",
                AnnotationType.GO_TERM, m_entry.getType());
        Set<String> termParents = new HashSet<String>(2);
        
        termParents.add("GO:0002506");
        termParents.add("GO:0004806");
        this.m_anotherTerm = new GOTerm("GO:0001709", "GO name 2", "B", 
                "This could be a comment", termParents);
        this.m_anotherEntry = GOEntryFactory.create(m_anotherTerm);
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        this.m_entry = null;
        this.m_term = null;
        this.m_anotherTerm = null;
    }
    
    /*
     * Test is the constructor checks for null arguments.
    public void testIllegalArgumentException() {
        try {
            GOEntry nullEntry = new GOEntry(null);
            //this should never reached
            nullEntry.getGOTerm();
        } catch (IllegalArgumentException e) {
            assertEquals(true, true);
            return;
        }
        assertEquals("this method should never reached!", true, false);
    }*/
    
    /**
     * Test method for 'org.knime.ext.ainet.GOEntry.hashCode()'.
     */
    public final void testHashCode() {
        assertEquals(this.m_entry.hashCode(), this.m_entry.hashCode());
        assertEquals(true, 
                this.m_entry.hashCode() != this.m_anotherEntry.hashCode());
    }

    /**
     * Test method for 'org.knime.ext.ainet.GOEntry.equals(Object)'.
     */
    public final void testEqualsObject() {
        assertEquals(false, this.m_anotherEntry.equals(this.m_entry));
        assertEquals(false, this.m_entry.equals(this.m_anotherEntry));
        assertEquals(true, this.m_entry.equals(this.m_entry));
        assertEquals(true, this.m_anotherEntry.equals(this.m_anotherEntry));
    }

    /**
     * Test method for 'org.knime.ext.ainet.GOEntry.toString()'.
     */
    public final void testToString() {
        this.m_entry.toString();
        this.m_anotherEntry.toString();
    }

}
