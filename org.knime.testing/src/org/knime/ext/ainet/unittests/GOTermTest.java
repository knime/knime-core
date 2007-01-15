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
 *   12.06.2006 (koetter): created
 */
package org.knime.ext.ainet.unittests;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.knime.ext.ainet.agents.geneontology.GOTerm;

/**
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class GOTermTest extends TestCase {
    
    private GOTerm m_term;
    private GOTerm m_copyOfTerm;
    private GOTerm m_emptyParentCommentTerm;
    
    private static final String VALID_GOID = "GO:0000005";
    private static final String INVALID_GOID = "GO:00000050";

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        Set<String> parents = new HashSet<String>(2);
        parents.add(new String("GO:0000005"));
        parents.add(new String("GO:0000004"));
        this.m_term = new GOTerm("GO:0000001", "mitochondrion inheritance",
            "molecular_function",
            "This term was made obsolete because it refers to a class of "
            + "gene products and a biological process rather than a "
            + "molecular function. To update annotations, consider the "
            + "molecular function term \\'unfolded protein binding ; " 
            + "GO:0051082\\' and the biological process term \\'ribosome "
            + "biogenesis and assembly ; GO:0042254\\' and its children.\\r\\n",
             parents);
        
        Set<String> parentsCopy = new HashSet<String>(2);
        parentsCopy.add(new String("GO:0000005"));
        parentsCopy.add(new String("GO:0000004"));
        this.m_copyOfTerm = new GOTerm("GO:0000001", 
            "mitochondrion inheritance", "molecular_function",
            "This term was made obsolete because it refers to a class of "
            + "gene products and a biological process rather than a "
            + "molecular function. To update annotations, consider the "
            + "molecular function term \\'unfolded protein binding ; " 
            + "GO:0051082\\' and the biological process term \\'ribosome "
            + "biogenesis and assembly ; GO:0042254\\' and its children.\\r\\n",
            parentsCopy);
        
        this.m_emptyParentCommentTerm = new GOTerm(VALID_GOID,
                "mitochondrion inheritance", "molecular_function", null, null);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        this.m_term = null;
        this.m_copyOfTerm = null;
        this.m_emptyParentCommentTerm = null;
    }

    /*
     * Test method for 'org.knime.ext.ainet.goParser.GOTerm.GOTerm(
     * String, String, String, String, Collection<String>)'
     */
    public void testGOTerm() {
        Set<String> parent = new HashSet<String>(1);
        parent.add(VALID_GOID);
        //The GOID of this term has one digit to much
        try {
            //test for wrong GO ID
            GOTerm wrongGOID = new GOTerm(INVALID_GOID, 
                "mitochondrion inheritance", "molecular_function", "Comment",
                parent);
            //we should never reach this
            assertEquals(false, true);
            wrongGOID.toString();
        } catch (IllegalArgumentException e) { }
        try {
            //test for null name
            GOTerm wrongGOID = new GOTerm(VALID_GOID, 
                null, "molecular_function", "Comment",
                parent);
            //we should never reach this
            assertEquals(false, true);
            wrongGOID.toString();
        } catch (IllegalArgumentException e) { }
        try {
            //test for null function
            GOTerm wrongGOID = new GOTerm(VALID_GOID, 
                "mitochondrion inheritance", null, "Comment",
                parent);
            //we should never reach this
            assertEquals(false, true);
            wrongGOID.toString();
        } catch (IllegalArgumentException e) { }
        try {
            //test for invalid parent ids
            parent.clear();
            parent.add(INVALID_GOID);
            GOTerm wrongGOID = new GOTerm(VALID_GOID, 
                "mitochondrion inheritance", "molecular_function", "Comment",
                parent);
            //we should never reach this
            assertEquals(false, true);
            wrongGOID.toString();
        } catch (IllegalArgumentException e) { }
    }

    /*
     * Test method for 'org.knime.ext.ainet.goParser.GOTerm.getGOID()'
     */
    public void testGetGOID() {
        assertEquals(this.m_term.getGOID(), this.m_copyOfTerm.getGOID());
    }

    /*
     * Test method for 'org.knime.ext.ainet.goParser.GOTerm.getID()'
     */
    public void testGetID() {
        assertEquals(this.m_term.getID(), this.m_copyOfTerm.getID());
    }

    /*
     * Test method for 'org.knime.ext.ainet.goParser.GOTerm.getName()'
     */
    public void testGetName() {
        assertEquals(this.m_term.getName(), this.m_copyOfTerm.getName());
    }

    /*
     * Test method for 'org.knime.ext.ainet.goParser.GOTerm.getFunction()'
     */
    public void testGetFunction() {
        assertEquals(this.m_term.getFunction(), 
                this.m_copyOfTerm.getFunction());
    }

    /*
     * Test method for 'org.knime.ext.ainet.goParser.GOTerm.getComment()'
     */
    public void testGetComment() {
        assertEquals(this.m_term.getComment(), this.m_copyOfTerm.getComment());
        assertEquals(this.m_emptyParentCommentTerm.getComment(), 
                this.m_emptyParentCommentTerm.getComment());
    }

    /*
     * Test method for 'org.knime.ext.ainet.goParser.GOTerm.getParents()'
     */
    public void testGetParents() {
        assertEquals(this.m_term.getParents(), this.m_copyOfTerm.getParents());
        assertEquals(this.m_emptyParentCommentTerm.getParents(), 
                this.m_emptyParentCommentTerm.getParents());
    }

    /*
     * Test method for 'org.knime.ext.ainet.goParser.GOTerm.validGOID(
     * String)'
     */
    public void testValidGOID() {
        assertEquals(true, GOTerm.validGOID(VALID_GOID));
        assertEquals(false, GOTerm.validGOID(INVALID_GOID));
        
    }

    /*
     * Test method for 'org.knime.ext.ainet.goParser.GOTerm.equals(Object)'
     */
    public void testEqualsObject() {
        assertEquals(true, this.m_term.equals(this.m_copyOfTerm));
        assertEquals(false, this.m_emptyParentCommentTerm.equals(this.m_term));
        Set<String> parentsCopy = new HashSet<String>(2);
        parentsCopy.add(new String("GO:0000005"));
        parentsCopy.add(new String("GO:0000004"));
        GOTerm basicTerm = new GOTerm("GO:0000001", 
                "mitochondrion inheritance", "molecular_function",
                "Comment",
                parentsCopy);
        GOTerm testTerm = new GOTerm("GO:0000001", 
            "mitochondrion inheritance", "molecular_function",
            "Little change in comment",
            parentsCopy);
        //Only the comment has changed
        assertEquals(false, basicTerm.equals(testTerm));
        testTerm = new GOTerm("GO:0000001", 
                "changed name inheritance", "molecular_function",
                "Comment",
                parentsCopy);
        //Only the name has changed
        assertEquals(false, basicTerm.equals(testTerm));
        testTerm = new GOTerm("GO:0000001", 
                "mitochondrion inheritance", "changed_function",
                "Comment",
                parentsCopy);
        //Only the function has changed
        assertEquals(false, basicTerm.equals(testTerm));
        Set<String> parentsChanged = new HashSet<String>(2);
        parentsChanged.add(new String("GO:0000009"));
        parentsChanged.add(new String("GO:0000008"));
        testTerm = new GOTerm("GO:0000001", 
                "mitochondrion inheritance", "molecular_function",
                "Comment",
                parentsChanged);
        //Only the parents has changed
        assertEquals(false, basicTerm.equals(testTerm));
        
    }

    /*
     * Test method for 'org.knime.ext.ainet.goParser.GOTerm.hashCode()'
     */
    public void testHashCode() {
        assertEquals(this.m_term.hashCode(), this.m_copyOfTerm.hashCode());
        assertEquals(true, this.m_emptyParentCommentTerm.hashCode() 
                != this.m_term.hashCode());
    }

    /*
     * Test method for 'org.knime.ext.ainet.goParser.GOTerm.toString()'
     */
    public void testToString() {
        assertEquals(this.m_term.toString(), this.m_copyOfTerm.toString());
        assertEquals(this.m_emptyParentCommentTerm.toString(), 
                this.m_emptyParentCommentTerm.toString());
    }

}
