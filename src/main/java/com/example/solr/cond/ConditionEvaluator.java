package com.example.solr.cond;

import org.apache.lucene.util.BytesRef;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Set;

public class ConditionEvaluator {

    public static boolean evaluate(BytesRef ref, Set<String> active) {
        try (var in = new DataInputStream(
                new ByteArrayInputStream(ref.bytes, ref.offset, ref.length))) {
            return evalNode(in, active);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean evalNode(DataInputStream in, Set<String> active) throws IOException {
        byte type = in.readByte();
        switch (type) {
            case 0: // LEAF
                String cond = in.readUTF();
                return active.contains(cond);
            case 1: { // AND
                int n1 = in.readByte();
                boolean result = true;
                for (int i = 0; i < n1; i++) {
                    boolean child = evalNode(in, active);
                    if (!child) result = false; // すぐ return せず最後まで読む
                }
                return result;
            }
            case 2: { // OR
                int n2 = in.readByte();
                boolean result = false;
                for (int i = 0; i < n2; i++) {
                    boolean child = evalNode(in, active);
                    if (child) result = true; // 読み切るまで進む
                }
                return result;
            }
            case 3: // NOT
                return !evalNode(in, active);
            default:
                throw new IOException("unknown node type: " + type);
        }
    }
}
