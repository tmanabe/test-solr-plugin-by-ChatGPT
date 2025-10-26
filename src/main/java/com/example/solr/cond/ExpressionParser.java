package com.example.solr.cond;

import java.util.*;

abstract class ExpressionNode {
    abstract boolean evaluate(Set<String> trueIds);
}

class LeafNode extends ExpressionNode {
    final String id;
    LeafNode(String id) { this.id = id; }
    @Override
    boolean evaluate(Set<String> trueIds) { return trueIds.contains(id); }
}

class AndNode extends ExpressionNode {
    final List<ExpressionNode> children;
    AndNode(List<ExpressionNode> c) { children = c; }
    @Override
    boolean evaluate(Set<String> trueIds) {
        for (ExpressionNode n : children) if (!n.evaluate(trueIds)) return false;
        return true;
    }
}

class OrNode extends ExpressionNode {
    final List<ExpressionNode> children;
    OrNode(List<ExpressionNode> c) { children = c; }
    @Override
    boolean evaluate(Set<String> trueIds) {
        for (ExpressionNode n : children) if (n.evaluate(trueIds)) return true;
        return false;
    }
}

class NotNode extends ExpressionNode {
    final ExpressionNode child;
    NotNode(ExpressionNode c) { child = c; }
    @Override
    boolean evaluate(Set<String> trueIds) { return !child.evaluate(trueIds); }
}

class ExpressionParser {
    // parse S-expression string like: (AND 1 (OR 2 (NOT 3)))
    static ExpressionNode parse(String s) {
        Tokenizer t = new Tokenizer(s);
        ExpressionNode n = parseExpr(t);
        if (t.hasNext()) throw new IllegalArgumentException("trailing tokens");
        return n;
    }

    private static ExpressionNode parseExpr(Tokenizer t) {
        if (!t.hasNext()) throw new IllegalArgumentException("empty expression");
        String tok = t.peek();
        if ("(".equals(tok)) {
            t.next(); // consume '('
            String op = t.next();
            if (op == null) throw new IllegalArgumentException("expected operator");
            if ("AND".equalsIgnoreCase(op)) {
                List<ExpressionNode> ch = new ArrayList<>();
                while (!")".equals(t.peek())) ch.add(parseExpr(t));
                t.next(); // consume ')'
                if (ch.isEmpty()) throw new IllegalArgumentException("AND requires children");
                return new AndNode(ch);
            } else if ("OR".equalsIgnoreCase(op)) {
                List<ExpressionNode> ch = new ArrayList<>();
                while (!")".equals(t.peek())) ch.add(parseExpr(t));
                t.next();
                if (ch.isEmpty()) throw new IllegalArgumentException("OR requires children");
                return new OrNode(ch);
            } else if ("NOT".equalsIgnoreCase(op)) {
                ExpressionNode c = parseExpr(t);
                if (!")".equals(t.next())) throw new IllegalArgumentException("expected ')'");
                return new NotNode(c);
            } else {
                throw new IllegalArgumentException("unknown operator: " + op);
            }
        } else {
            // leaf id token
            String id = t.next();
            if (id == null || id.isEmpty() || ")".equals(id)) throw new IllegalArgumentException("expected id");
            return new LeafNode(id);
        }
    }

    // simple tokenizer for parentheses and tokens separated by whitespace
    private static class Tokenizer {
        private final List<String> tokens = new ArrayList<>();
        private int pos = 0;
        Tokenizer(String s) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); ++i) {
                char c = s.charAt(i);
                if (c == '(' || c == ')') {
                    if (sb.length() > 0) { tokens.add(sb.toString()); sb.setLength(0); }
                    tokens.add(String.valueOf(c));
                } else if (Character.isWhitespace(c)) {
                    if (sb.length() > 0) { tokens.add(sb.toString()); sb.setLength(0); }
                } else {
                    sb.append(c);
                }
            }
            if (sb.length() > 0) tokens.add(sb.toString());
        }
        boolean hasNext() { return pos < tokens.size(); }
        String peek() { return hasNext() ? tokens.get(pos) : null; }
        String next() { return hasNext() ? tokens.get(pos++) : null; }
    }
}
