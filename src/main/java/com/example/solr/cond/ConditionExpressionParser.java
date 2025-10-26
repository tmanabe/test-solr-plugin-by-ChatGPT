package com.example.solr.cond;

import java.io.*;
import java.util.*;

public class ConditionExpressionParser {

    private static class Tokenizer {
        private final String s;
        private int pos;

        Tokenizer(String s) {
            this.s = s;
        }

        String peek() {
            skipWs();
            if (pos >= s.length()) return null;
            int start = pos;
            char c = s.charAt(pos);
            if (Character.isLetterOrDigit(c)) {
                while (pos < s.length() && Character.isLetterOrDigit(s.charAt(pos))) pos++;
                String word = s.substring(start, pos);
                pos = start; // rewind for next()
                return word;
            }
            return String.valueOf(c);
        }

        String next() {
            skipWs();
            if (pos >= s.length()) return null;
            char c = s.charAt(pos);
            if (Character.isLetterOrDigit(c)) {
                int start = pos;
                while (pos < s.length() && Character.isLetterOrDigit(s.charAt(pos))) pos++;
                return s.substring(start, pos);
            }
            pos++;
            return String.valueOf(c);
        }

        void skipWs() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        }
    }

    public static abstract class Node {}
    public static class Leaf extends Node {
        final String id;
        Leaf(String id) { this.id = id; }
    }
    public static class And extends Node {
        final List<Node> children;
        And(List<Node> c) { this.children = c; }
    }
    public static class Or extends Node {
        final List<Node> children;
        Or(List<Node> c) { this.children = c; }
    }
    public static class Not extends Node {
        final Node child;
        Not(Node c) { this.child = c; }
    }

    public static Node parse(String expr) {
        Tokenizer t = new Tokenizer(expr);
        return parseOr(t);
    }

    private static Node parseOr(Tokenizer t) {
        List<Node> list = new ArrayList<>();
        list.add(parseAnd(t));
        while (true) {
            String tok = t.peek();
            if (tok != null && tok.equalsIgnoreCase("OR")) {
                t.next();
                list.add(parseAnd(t));
            } else break;
        }
        if (list.size() == 1) return list.get(0);
        return new Or(list);
    }

    private static Node parseAnd(Tokenizer t) {
        List<Node> list = new ArrayList<>();
        list.add(parseNot(t));
        while (true) {
            String tok = t.peek();
            if (tok != null && tok.equalsIgnoreCase("AND")) {
                t.next();
                list.add(parseNot(t));
            } else break;
        }
        if (list.size() == 1) return list.get(0);
        return new And(list);
    }

    private static Node parseNot(Tokenizer t) {
        String tok = t.peek();
        if (tok != null && tok.equalsIgnoreCase("NOT")) {
            t.next();
            return new Not(parsePrimary(t));
        }
        return parsePrimary(t);
    }

    private static Node parsePrimary(Tokenizer t) {
        String tok = t.next();
        if (tok == null) throw new IllegalArgumentException("unexpected end");
        if (tok.equals("(")) {
            Node n = parseOr(t);
            String c = t.next();
            if (!")".equals(c)) throw new IllegalArgumentException("expected )");
            return n;
        }
        if (tok.equalsIgnoreCase("AND") || tok.equalsIgnoreCase("OR") || tok.equalsIgnoreCase("NOT"))
            throw new IllegalArgumentException("unexpected operator: " + tok);
        return new Leaf(tok);
    }

    // ---- serialization ----

    public static byte[] parseToBytes(String expr) {
        Node root = parse(expr);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(out)) {
            writeNode(root, dos);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private static void writeNode(Node n, DataOutputStream out) throws IOException {
        if (n instanceof Leaf leaf) {
            out.writeByte(0);
            out.writeUTF(leaf.id);
        } else if (n instanceof And and) {
            out.writeByte(1);
            out.writeByte(and.children.size());
            for (Node c : and.children) writeNode(c, out);
        } else if (n instanceof Or or) {
            out.writeByte(2);
            out.writeByte(or.children.size());
            for (Node c : or.children) writeNode(c, out);
        } else if (n instanceof Not not) {
            out.writeByte(3);
            writeNode(not.child, out);
        } else throw new IOException("unknown node");
    }
}
