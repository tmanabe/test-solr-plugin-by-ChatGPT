package com.example.solr.cond;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.lucene.document.BinaryDocValuesField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.response.TextResponseWriter;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.uninverting.UninvertingReader;

/**
 * 条件式専用 FieldType。
 * DocValues にバイナリとして格納し、インデックスは行わない。
 */
public class ConditionExpressionField extends FieldType {
    @Override
    public boolean isTokenized() {
        return false;
    }

    @Override
    public UninvertingReader.Type getUninversionType(SchemaField sf) {
        return UninvertingReader.Type.SORTED;
    }

    @Override
    public void write(TextResponseWriter writer, String name, IndexableField f)
            throws IOException {
        BytesRef val = f.binaryValue();
        if (val != null) {
            writer.writeStr(name, val.utf8ToString(), true);
        } else {
            writer.writeNull(name);
        }
    }

    @Override
    public IndexableField createField(SchemaField field, Object value) {
        if (value == null) return null;
        byte[] bytes = ConditionExpressionParser.parseToBytes(value.toString());
        return new BinaryDocValuesField(field.getName(), new BytesRef(bytes));
    }

    @Override
    public SortField getSortField(SchemaField field, boolean top) {
        // top=true は昇順か降順かの指定（Solr 側では reverse として扱われる）
        // field.sortMissingLast() / field.sortMissingFirst() は必要に応じて設定可
        return new SortField(field.getName(), SortField.Type.STRING, top);
    }

    @Override
    protected void checkSupportsDocValues() {}  // Indicates docValues support by not throwing an Exception
}
