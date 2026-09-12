## oracle测试库的连接地址
"databaseType": "oracle",
  "jdbcDatabase": "XE",
  "jdbcIp": "192.168.3.78",
  "jdbcPassword": "test1",
  "jdbcUsername": "c##test1",
  "port": "1521"


## 106 oracle测试库新增
"databaseType": "oracle",
"jdbcDatabase": "XE",
"jdbcIp": "192.168.3.106",
"jdbcPassword": "123456",
"jdbcUsername": "CS",
"port": "49161"


## 模拟测试插入百万数据

declare--数据块头
v_cnt number :=0;--定义计数器
begin      --数据块执行部分
for i in 1..1000000 loop --for循环tou （for 条件 loop     end loop）
v_cnt := v_cnt +1;--循环一次计数器+1
insert into TEST values(
seq_log.nextval,--获取下一个序列
DBMS_RANDOM.STRING ('a', 5),--随机产生5个26字母的任意大小写
10,
1,
DBMS_RANDOM.STRING ('a', 10),
SYSDATE
); --随机产生10个26字母的任意大小写

    if v_cnt >= 10000 then --if条件判断（当数据插入到10000条时保存一次）
            commit;--保存
          v_cnt :=0;--清空计数器
    end if;--if结束
    end loop;--for循环结束
    commit; --不管最后数据是多少再保存一次，防止有零头没保存
end;--数据块结束


## 查询表空间的占用情况

SELECT UPPER(F.TABLESPACE_NAME) "TEST",     
D.TOT_GROOTTE_MB "表空间大小(M)",     
D.TOT_GROOTTE_MB - F.TOTAL_BYTES "已使用空间(M)",     
TO_CHAR(ROUND((D.TOT_GROOTTE_MB - F.TOTAL_BYTES) / D.TOT_GROOTTE_MB * 100,2),'990.99') "使用比",     
F.TOTAL_BYTES "空闲空间(M)",     
F.MAX_BYTES "最大块(M)"    
FROM (SELECT TABLESPACE_NAME,     
ROUND(SUM(BYTES) / (1024 * 1024), 2) TOTAL_BYTES,     
ROUND(MAX(BYTES) / (1024 * 1024), 2) MAX_BYTES     
FROM SYS.DBA_FREE_SPACE     
GROUP BY TABLESPACE_NAME) F,     
(SELECT DD.TABLESPACE_NAME,     
ROUND(SUM(DD.BYTES) / (1024 * 1024), 2) TOT_GROOTTE_MB     
FROM SYS.DBA_DATA_FILES DD     
GROUP BY DD.TABLESPACE_NAME) D     
WHERE D.TABLESPACE_NAME = F.TABLESPACE_NAME     
ORDER BY 4 DESC; 


alter table CSTEST move tablespace USERS   修改表空间名


