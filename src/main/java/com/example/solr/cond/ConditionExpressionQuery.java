package com.example.solr.cond;

import java.io.IOException;
import java.util.*;

import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.search.*;

public class ConditionExpressionQuery extends Query {
    private final String dvField;
    private final Set<String> trueIds;

    public ConditionExpressionQuery(String dvField, Set<String> trueIds) {
        this.dvField = dvField;
        this.trueIds = Collections.unmodifiableSet(new HashSet<>(trueIds));
    }

    @Override
    public String toString(String field) {
        return "ConditionExpressionQuery(field=" + dvField + ", trueIds=" + trueIds + ")";
    }

    @Override
    public void visit(QueryVisitor visitor) {
        visitor.visitLeaf(this);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ConditionExpressionQuery)) return false;
        ConditionExpressionQuery o = (ConditionExpressionQuery) other;
        return dvField.equals(o.dvField) && trueIds.equals(o.trueIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dvField, trueIds);
    }

    @Override
    public Weight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) throws IOException {
        return new ConditionWeight(this);
    }

    private class ConditionWeight extends Weight {
        protected ConditionWeight(Query query) { super(query); }

        @Override
        public Explanation explain(LeafReaderContext context, int doc) throws IOException {
            // return simple explanation
            BinaryDocValues dv = getBinaryDocValuesOrNull(context, dvField);
            boolean match = false;
            if (dv != null) {
                BytesRef ref = getBytesRefForDoc(dv, doc);
                if (ref != null) {
                    ExpressionNode node = ExpressionParser.parse(ref.utf8ToString());
                    match = node.evaluate(trueIds);
                }
            }
            return Explanation.match(match ? 1f : 0f, "ConditionExpressionQuery match=" + match);
        }

        @Override
        public Scorer scorer(LeafReaderContext context) throws IOException {
            BinaryDocValues dv = getBinaryDocValuesOrNull(context, dvField);
            if (dv == null) return null;
            final int maxDoc = context.reader().maxDoc();
            return new Scorer(this) {
                private int docID = -1;

                @Override
                public DocIdSetIterator iterator() {
                    return new DocIdSetIterator() {
                        private final BytesRef scratch = new BytesRef();

                        @Override
                        public int docID() { return docID; }

                        @Override
                        public int nextDoc() throws IOException {
                            return advance(docID + 1);
                        }

                        @Override
                        public int advance(int target) throws IOException {
                            for (int d = Math.max(target, 0); d < maxDoc; ++d) {
                                BytesRef ref = getBytesRefForDoc(dv, d);
                                if (ref == null) continue;
                                ExpressionNode node;
                                try {
                                    node = ExpressionParser.parse(ref.utf8ToString());
                                } catch (RuntimeException e) {
                                    // parse error -> treat as non-match
                                    continue;
                                }
                                if (node.evaluate(trueIds)) {
                                    docID = d;
                                    return docID;
                                }
                            }
                            docID = NO_MORE_DOCS;
                            return NO_MORE_DOCS;
                        }

                        @Override
                        public long cost() { return maxDoc; }
                    };
                }

                @Override
                public int docID() { return docID; }

                @Override
                public float score() { return 1.0f; }

                @Override
                public float getMaxScore(int upTo) {
                    return 1.0f;
                }
            };
        }

        @Override
        public boolean isCacheable(LeafReaderContext ctx) { return false; } // conservative

        private BinaryDocValues getBinaryDocValuesOrNull(LeafReaderContext ctx, String field) {
            try {
                return DocValues.getBinary(ctx.reader(), field);
            } catch (IOException e) {
                return null;
            } catch (IllegalArgumentException e) {
                // field not found
                return null;
            }
        }
    }

    // Solrのバージョンが違っても使えるような不要に柔軟なコードを書こうとしていた
    // （リフレクションによる）ので、人間が手で書き直した
    private static BytesRef getBytesRefForDoc(BinaryDocValues dv, int docId) throws IOException {
        if (dv == null) return null;

        dv.advance(docId);
        if (dv.docID() == docId) {
            return dv.binaryValue();
        } else {
            return null;
        }
    }
}
