package at.jku.dke.task_app.trigger.services;

import at.jku.dke.task_app.trigger.dto.SchemaInfoDto;
import at.jku.dke.task_app.trigger.dto.TableDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class TriggerSchemaService {
    private static final Logger LOG = LoggerFactory.getLogger(TriggerSchemaService.class);

    private final TriggerDataSourceService dataSource;

    public TriggerSchemaService(TriggerDataSourceService dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Gets information about the tables.
     *
     * @param conn The database connection.
     * @return The schema information.
     * @throws SQLException If the information could not be retrieved.
     */
    public SchemaInfoDto getSchemaInfoDto(Connection conn) throws SQLException {
        List<TableDto> tables = new ArrayList<>();
        ResultSet rsTables = conn.getMetaData().getTables(null, null, null, new String[]{"TABLE"});
        while (rsTables.next()) {
            String tableName = rsTables.getString("TABLE_NAME");

            // load columns
            List<TableDto.ColumnDto> columns = new ArrayList<>();
            ResultSet rsColumns = conn.getMetaData().getColumns(null, null, tableName, null);
            while (rsColumns.next()) {
                String columnName = rsColumns.getString("COLUMN_NAME");
                String columnType = rsColumns.getString("TYPE_NAME");
                boolean nullable = rsColumns.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls;
                boolean pk = false;

                // load primary keys
                ResultSet rsPk = conn.getMetaData().getPrimaryKeys(null, null, tableName);
                while (rsPk.next()) {
                    if (rsPk.getString("COLUMN_NAME").equals(rsColumns.getString("COLUMN_NAME"))) {
                        pk = true;
                        break;
                    }
                }

                // add column to list
                columns.add(new TableDto.ColumnDto(columnName, columnType, nullable, pk));
            }

            // load foreign keys
            List<TableDto.ForeignKeyDto> foreignKeys = new ArrayList<>();
            ResultSet rsFk = conn.getMetaData().getImportedKeys(null, null, tableName);
            while (rsFk.next()) {
                String fkName = rsFk.getString("FK_NAME");
                String fkTableName = rsFk.getString("FKTABLE_NAME");
                String fkColumnName = rsFk.getString("FKCOLUMN_NAME");
                String pkTableName = rsFk.getString("PKTABLE_NAME");
                String pkColumnName = rsFk.getString("PKCOLUMN_NAME");

                var existing = foreignKeys.stream().filter(x -> x.name().equals(fkName)).findFirst();
                if (existing.isEmpty())
                    foreignKeys.add(new TableDto.ForeignKeyDto(fkName, fkTableName, new ArrayList<>() {{
                        add(fkColumnName);
                    }}, pkTableName, new ArrayList<>() {{
                        add(pkColumnName);
                    }}));
                else {
                    existing.get().columns().add(fkColumnName);
                    existing.get().referencedColumns().add(pkColumnName);
                }
            }

            tables.add(new TableDto(tableName, columns, foreignKeys, null));
        }
        return new SchemaInfoDto(tables);
    }
}
