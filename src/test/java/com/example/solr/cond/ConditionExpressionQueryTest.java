package com.example.solr.cond;

import org.apache.solr.SolrTestCaseJ4;
import org.junit.BeforeClass;
import org.junit.Test;

public class ConditionExpressionQueryTest extends SolrTestCaseJ4 {

    @BeforeClass
    public static void beforeClass() throws Exception {
        initCore("solr/collection1/conf/solrconfig.xml", "solr/collection1/conf/schema.xml");
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        clearIndex();
    }

    @Test
    public void testSingleCondition() {
        assertU(adoc("id", "1", "cond_expr", "A"));
        assertU(adoc("id", "2", "cond_expr", "B"));
        assertU(adoc("id", "3", "cond_expr", "C"));
        assertU(adoc("id", "4", "cond_expr", "A"));
        assertU(adoc("id", "5", "cond_expr", "B"));
        assertU(commit());

        assertQ(req("q", "{!cond field=cond_expr true_ids=A}"),
                "//*[@numFound='2']",
                "//doc/str[@name='id'][.='1']",
                "//doc/str[@name='id'][.='4']");
    }

    @Test
    public void testAndCondition() {
        assertU(adoc("id", "1", "cond_expr", "(A AND B)"));
        assertU(adoc("id", "2", "cond_expr", "(A AND C)"));
        assertU(adoc("id", "3", "cond_expr", "(B AND C)"));
        assertU(adoc("id", "4", "cond_expr", "A"));
        assertU(adoc("id", "5", "cond_expr", "B"));
        assertU(commit());

        assertQ(req("q", "{!cond field=cond_expr true_ids=A,B}"),
                "//*[@numFound='3']",
                "//doc/str[@name='id'][.='1']",
                "//doc/str[@name='id'][.='4']",
                "//doc/str[@name='id'][.='5']");
    }

    @Test
    public void testOrCondition() {
        assertU(adoc("id", "1", "cond_expr", "(A OR B)"));
        assertU(adoc("id", "2", "cond_expr", "(C OR D)"));
        assertU(adoc("id", "3", "cond_expr", "(A OR D)"));
        assertU(adoc("id", "4", "cond_expr", "(B OR C)"));
        assertU(adoc("id", "5", "cond_expr", "E"));
        assertU(commit());

        assertQ(req("q", "{!cond field=cond_expr true_ids=B}"),
                "//*[@numFound='2']",
                "//doc/str[@name='id'][.='1']",
                "//doc/str[@name='id'][.='4']");
    }

    @Test
    public void testNotCondition() {
        assertU(adoc("id", "1", "cond_expr", "(NOT A)"));
        assertU(adoc("id", "2", "cond_expr", "(NOT B)"));
        assertU(adoc("id", "3", "cond_expr", "A"));
        assertU(adoc("id", "4", "cond_expr", "B"));
        assertU(adoc("id", "5", "cond_expr", "C"));
        assertU(commit());

        assertQ(req("q", "{!cond field=cond_expr true_ids=A}"),
                "//*[@numFound='2']",
                "//doc/str[@name='id'][.='2']",
                "//doc/str[@name='id'][.='3']");
    }

    @Test
    public void testComplexCondition() {
        assertU(adoc("id", "1", "cond_expr", "((A AND B) OR C)"));
        assertU(adoc("id", "2", "cond_expr", "(A AND (NOT B))"));
        assertU(adoc("id", "3", "cond_expr", "((A OR D) AND (NOT C))"));
        assertU(adoc("id", "4", "cond_expr", "(B OR C)"));
        assertU(adoc("id", "5", "cond_expr", "(A AND B AND C)"));
        assertU(commit());

        assertQ(req("q", "{!cond field=cond_expr true_ids=A,B}"),
                "//*[@numFound='3']",
                "//doc/str[@name='id'][.='1']",
                "//doc/str[@name='id'][.='3']",
                "//doc/str[@name='id'][.='4']");
    }
}
