package org.knime.orc.tableformat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.io.orc.CompressionKind;
import org.apache.hadoop.hive.ql.io.orc.OrcFile;
import org.apache.hadoop.hive.ql.io.orc.Writer;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.SettableStructObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.StructField;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

/** Taken from https://gist.github.com/omalley/ccabae7cccac28f64812
 * (by omally - main contributor of ORC)
 */
public class OrcWriter {

  private static Configuration conf = new Configuration();

  public static class OrcRow {
    public Object[] columns ;

    OrcRow (final int colCount) {
      columns = new Object[colCount] ;
    }

    void setFieldValue(final int FieldIndex,final Object value) {
      columns[FieldIndex] = value ;
    }

    void setNumFields(final int newSize) {
      if (newSize != columns.length) {
        Object[] oldColumns = columns;
        columns = new Object[newSize];
        System.arraycopy(oldColumns, 0, columns, 0, oldColumns.length);
      }
    }
  }
    static class OrcField implements StructField {
    private final String name;
    private final ObjectInspector inspector;
    private final int offset;

    OrcField(final String name, final ObjectInspector inspector, final int offset) {
      this.name = name;
      this.inspector = inspector;
      this.offset = offset;
    }

    @Override
    public String getFieldName() {
      return name;
    }

    @Override
    public ObjectInspector getFieldObjectInspector() {
      return inspector;
    }

    @Override
    public int getFieldID() {
      return offset;
    }

    @Override
    public String getFieldComment() {
      return null;
    }
  }
  static class OrcRowInspector extends SettableStructObjectInspector {
    private List<StructField> fields;

    public OrcRowInspector(final StructField... fields) {
      super();
      this.fields = Arrays.asList(fields);
    }

    @Override
    public List<StructField> getAllStructFieldRefs() {
      return fields;
    }

    @Override
    public StructField getStructFieldRef(final String s) {
      for(StructField field: fields) {
        if (field.getFieldName().equalsIgnoreCase(s)) {
          return field;
        }
      }
      return null;
    }

    @Override
    public Object getStructFieldData(final Object object, final StructField field) {
      if (object == null) {
        return null;
      }
      int offset = ((OrcField) field).offset;
      OrcRow struct = (OrcRow) object;
      if (offset >= struct.columns.length) {
        return null;
      }

      return struct.columns[offset];
    }

    @Override
    public List<Object> getStructFieldsDataAsList(final Object object) {
      if (object == null) {
        return null;
      }
      OrcRow struct = (OrcRow) object;
      List<Object> result = new ArrayList<Object>(struct.columns.length);
      for (Object child: struct.columns) {
        result.add(child);
      }
      return result;
    }

    @Override
    public String getTypeName() {
      StringBuilder buffer = new StringBuilder();
      buffer.append("struct<");
      for(int i=0; i < fields.size(); ++i) {
        StructField field = fields.get(i);
        if (i != 0) {
          buffer.append(",");
        }
        buffer.append(field.getFieldName());
        buffer.append(":");
        buffer.append(field.getFieldObjectInspector().getTypeName());
      }
      buffer.append(">");
      return buffer.toString();
    }

    @Override
    public Category getCategory() {
      return Category.STRUCT;
    }

    @Override
    public Object create() {
      return new OrcRow(0);
    }

    @Override
    public Object setStructFieldData(final Object struct, final StructField field,
                                     final Object fieldValue) {
      OrcRow orcStruct = (OrcRow) struct;
      int offset = ((OrcField) field).offset;
      // if the offset is bigger than our current number of fields, grow it
      if (orcStruct.columns.length <= offset) {
        orcStruct.setNumFields(offset+1);
      }
      orcStruct.setFieldValue(offset, fieldValue);
      return struct;
    }

    @Override
    public boolean equals(final Object o) {
      if (o == null || o.getClass() != getClass()) {
        return false;
      } else if (o == this) {
        return true;
      } else {
        List<StructField> other = ((OrcRowInspector) o).fields;
        if (other.size() != fields.size()) {
          return false;
        }
        for(int i = 0; i < fields.size(); ++i) {
          StructField left = other.get(i);
          StructField right = fields.get(i);
          if (!(left.getFieldName().equalsIgnoreCase(right.getFieldName()) &&
                left.getFieldObjectInspector()
                  .equals(right.getFieldObjectInspector()))) {
            return false;
          }
        }
        return true;
      }
    }
  }
    public static void main(final String[] args) throws IOException,
                                                InterruptedException,
                                                ClassNotFoundException {

    String path = "/tmp/file1.orc";

    try {

      conf = new Configuration();
      FileSystem fs = FileSystem.getLocal(conf);


      ObjectInspector ObjInspector =
        new OrcRowInspector(new OrcField("field1",
                                         PrimitiveObjectInspectorFactory.
                                           writableIntObjectInspector,
                                         0),
                            new OrcField("field2",
                                         PrimitiveObjectInspectorFactory.
                                           writableStringObjectInspector,
                                         1),
                            new OrcField("field3",
                                         PrimitiveObjectInspectorFactory.
                                           writableStringObjectInspector,
                                         2));
      Writer writer = OrcFile.createWriter(new Path(path),
                                    OrcFile.writerOptions(conf)
                                      .inspector(ObjInspector)
                                      .stripeSize(100000)
                                      .bufferSize(10000)
                                      .compress(CompressionKind.SNAPPY)
                                      .version(OrcFile.Version.V_0_12));

      OrcRow orcRecord = new OrcRow(3) ;
      orcRecord.setFieldValue(0,new IntWritable(1)) ;
      orcRecord.setFieldValue(1,new Text("hello")) ;
      orcRecord.setFieldValue(2,new Text("orcFile")) ;
      writer.addRow(orcRecord) ;

      writer.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

