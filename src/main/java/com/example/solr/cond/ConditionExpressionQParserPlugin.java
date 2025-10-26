package com.example.solr.cond;

import org.apache.lucene.search.Query;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;

public class ConditionExpressionQParserPlugin extends QParserPlugin {
    @Override
    public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
        return new QParser(qstr, localParams, params, req) {
            @Override
            public Query parse() {
                String field = localParams.get("field");
                String trueIdsCsv = localParams.get("true_ids", "");
                if (field == null) throw new IllegalArgumentException("missing local param: field");
                // parse true ids into set of strings
                java.util.Set<String> trueIds = new java.util.HashSet<>();
                for (String s : trueIdsCsv.split(",")) {
                    s = s.trim();
                    if (!s.isEmpty()) trueIds.add(s);
                }
                return new ConditionExpressionQuery(field, trueIds);
            }
        };
    }
}
