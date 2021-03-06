package ionix.Data.Oracle;

import ionix.Data.*;
import ionix.Utils.*;

import java.util.*;


public class EntitySqlQueryBuilderInsert extends ionix.Data.EntitySqlQueryBuilderInsert {

    private final DbAccess dataAccess;
    public EntitySqlQueryBuilderInsert(DbAccess dataAccess){
        this.dataAccess = dataAccess;
    }

    @Override
    public EntitySqlQueryBuilderResult createQuery(Object entity, EntityMetaData metaData) {
        if (null == entity)
            throw new IllegalArgumentException("entity is null");

        boolean insertFieldsEnabled = !Ext.isEmptyList(this.getInsertFields());

        SqlQuery query = new SqlQuery();
        StringBuilder text =  query.getText();
        text.append("INSERT INTO ")
                .append(metaData.getTableName())
                .append(" (");

        FieldMetaData sequenceIdentity = null;
        ArrayList<FieldMetaData> validFields = new ArrayList<>();
        for(FieldMetaData field : metaData.getFields()) {
            SchemaInfo schema = field.getSchema();

            switch (schema.getDatabaseGeneratedOption()) {
                case Computed:
                    break;
                case Identity:
                    throw new UnsupportedOperationException("Oracle InsertQueryBuilder does not support Identity");
                case AutoGenerateSequence:
                    if (null != sequenceIdentity)
                        throw new MultipleIdentityColumnFoundException(entity);
                    sequenceIdentity = field;
                    break;
                default:
                    if (insertFieldsEnabled && !this.getInsertFields().contains(schema.getColumnName()))
                        continue;
                    break;
            }

            text.append(schema.getColumnName())
                    .append(',');

            validFields.add(field);
        }

        text.delete(text.length() - 1, text.length());
        text.append(") VALUES (");

        boolean addSequenceIdentity = null != sequenceIdentity;
        for(FieldMetaData field : validFields){
            if (addSequenceIdentity && sequenceIdentity.equals(field)){
                text.append(SequenceManager.getSequenceName(this.dataAccess, metaData.getTableName(), sequenceIdentity.getSchema().getColumnName()));
                text.append(".NEXTVAL,");
                addSequenceIdentity = false;
            }
            else {
                SqlQueryHelper.setColumnValue(DbValueSetter.Instance, query, field, entity);
                text.append(',');
            }
        }
        text.delete(text.length() - 1, text.length());
        text.append(')');

        return new EntitySqlQueryBuilderResult(query, null, sequenceIdentity, query.getParameters().size());
    }
}
