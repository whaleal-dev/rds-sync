package com.whaleal.photon.source.oracle;

import common.photonV.entity.Datasource;
import datasource.DBUtil;
import dbconnection.oracle.OracleConnection;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;

public class ConnectionTest {

    public static void main(String[] args) {

        Datasource proc4 = DBUtil.getSourceByProcName("proc4");
        Connection conn = OracleConnection.createConnection(proc4);

        JdbcTemplate jdbcTemplate = OracleConnection.getJdbcTemplateBySource(proc4);
        //List<Map<String, Object>> dbTableList = jdbcTemplate.queryForList("select min(id) from STUDENT");
//        Map<String, Object> dbTableList = jdbcTemplate.queryForMap("select a.constraint_name,  a.column_name \n" +
//                "from user_cons_columns a, user_constraints b \n" +
//                "where a.constraint_name = b.constraint_name \n" +
//                "  and b.constraint_type = 'P' and a.table_name = 'STUDENT'");
        Integer max = jdbcTemplate.queryForObject("select max(ID) from STUDENT", Integer.class);

        System.out.println("xxxx" + max);
        //System.out.println(dbTableList);
//        for (Map dbTableMap : dbTableList) {
//            System.out.println(dbTableMap);
//        }

//        {TABLE_NAME=test, TABLESPACE_NAME=USERS, CLUSTER_NAME=null, IOT_NAME=null, STATUS=VALID, PCT_FREE=10, PCT_USED=null, INI_TRANS=1, MAX_TRANS=255, INITIAL_EXTENT=65536, NEXT_EXTENT=1048576, MIN_EXTENTS=1, MAX_EXTENTS=2147483645, PCT_INCREASE=null, FREELISTS=null, FREELIST_GROUPS=null, LOGGING=YES, BACKED_UP=N, NUM_ROWS=2, BLOCKS=5, EMPTY_BLOCKS=0, AVG_SPACE=0, CHAIN_CNT=0, AVG_ROW_LEN=13, AVG_SPACE_FREELIST_BLOCKS=0, NUM_FREELIST_BLOCKS=0, DEGREE=         1, INSTANCES=         1, CACHE=    N, TABLE_LOCK=ENABLED, SAMPLE_SIZE=2, LAST_ANALYZED=2021-08-31 10:00:09.0, PARTITIONED=NO, IOT_TYPE=null, TEMPORARY=N, SECONDARY=N, NESTED=NO, BUFFER_POOL=DEFAULT, FLASH_CACHE=DEFAULT, CELL_FLASH_CACHE=DEFAULT, ROW_MOVEMENT=DISABLED, GLOBAL_STATS=YES, USER_STATS=NO, DURATION=null, SKIP_CORRUPT=DISABLED, MONITORING=YES, CLUSTER_OWNER=null, DEPENDENCIES=DISABLED, COMPRESSION=DISABLED, COMPRESS_FOR=null, DROPPED=NO, READ_ONLY=NO, SEGMENT_CREATED=YES, RESULT_CACHE=DEFAULT}
//        {TABLE_NAME=STUDENT, TABLESPACE_NAME=USERS, CLUSTER_NAME=null, IOT_NAME=null, STATUS=VALID, PCT_FREE=10, PCT_USED=null, INI_TRANS=1, MAX_TRANS=255, INITIAL_EXTENT=65536, NEXT_EXTENT=1048576, MIN_EXTENTS=1, MAX_EXTENTS=2147483645, PCT_INCREASE=null, FREELISTS=null, FREELIST_GROUPS=null, LOGGING=YES, BACKED_UP=N, NUM_ROWS=1, BLOCKS=5, EMPTY_BLOCKS=0, AVG_SPACE=0, CHAIN_CNT=0, AVG_ROW_LEN=12, AVG_SPACE_FREELIST_BLOCKS=0, NUM_FREELIST_BLOCKS=0, DEGREE=         1, INSTANCES=         1, CACHE=    N, TABLE_LOCK=ENABLED, SAMPLE_SIZE=1, LAST_ANALYZED=2021-09-01 10:00:09.0, PARTITIONED=NO, IOT_TYPE=null, TEMPORARY=N, SECONDARY=N, NESTED=NO, BUFFER_POOL=DEFAULT, FLASH_CACHE=DEFAULT, CELL_FLASH_CACHE=DEFAULT, ROW_MOVEMENT=DISABLED, GLOBAL_STATS=YES, USER_STATS=NO, DURATION=null, SKIP_CORRUPT=DISABLED, MONITORING=YES, CLUSTER_OWNER=null, DEPENDENCIES=DISABLED, COMPRESSION=DISABLED, COMPRESS_FOR=null, DROPPED=NO, READ_ONLY=NO, SEGMENT_CREATED=YES, RESULT_CACHE=DEFAULT}
//        {TABLE_NAME=STU, TABLESPACE_NAME=USERS, CLUSTER_NAME=null, IOT_NAME=null, STATUS=VALID, PCT_FREE=10, PCT_USED=null, INI_TRANS=1, MAX_TRANS=255, INITIAL_EXTENT=65536, NEXT_EXTENT=1048576, MIN_EXTENTS=1, MAX_EXTENTS=2147483645, PCT_INCREASE=null, FREELISTS=null, FREELIST_GROUPS=null, LOGGING=YES, BACKED_UP=N, NUM_ROWS=1, BLOCKS=5, EMPTY_BLOCKS=0, AVG_SPACE=0, CHAIN_CNT=0, AVG_ROW_LEN=9, AVG_SPACE_FREELIST_BLOCKS=0, NUM_FREELIST_BLOCKS=0, DEGREE=         1, INSTANCES=         1, CACHE=    N, TABLE_LOCK=ENABLED, SAMPLE_SIZE=1, LAST_ANALYZED=2021-09-01 10:00:09.0, PARTITIONED=NO, IOT_TYPE=null, TEMPORARY=N, SECONDARY=N, NESTED=NO, BUFFER_POOL=DEFAULT, FLASH_CACHE=DEFAULT, CELL_FLASH_CACHE=DEFAULT, ROW_MOVEMENT=DISABLED, GLOBAL_STATS=YES, USER_STATS=NO, DURATION=null, SKIP_CORRUPT=DISABLED, MONITORING=YES, CLUSTER_OWNER=null, DEPENDENCIES=DISABLED, COMPRESSION=DISABLED, COMPRESS_FOR=null, DROPPED=NO, READ_ONLY=NO, SEGMENT_CREATED=YES, RESULT_CACHE=DEFAULT}

//        List<Map<String, Object>> dbTableList = jdbcTemplate.queryForList("SELECT * FROM USER_TABLES");
//        for (Map dbTableMap : dbTableList) {
//            System.out.println(dbTableMap);
//        }
//        Configuration configuration = ConfigurationUtil.getConfiguration("proc4");
//        OracleSource oracleSource = new OracleSource(configuration, new MemoryCache());
//        oracleSource.getAllDbCollections("123");
    }
}
