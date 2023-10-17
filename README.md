
## 说明

由前端在接口中定义查询参数及返回结果, 服务端无需生成任何 model dao service 等, 自动处理数据查询并组装数据, 可以极大的简化后端代码量

## 使用

添加引用
```xml
<dependency>
    <groupId>com.github.liuanxin</groupId>
    <artifactId>table-column</artifactId>
    <version>???</version>
</dependency>
```

添加配置
```java
/** 表关联关系及别名都没有, 可以无需此文件, 查询时就只能基于单表 */
@Configuration
public class TableColumnConfig {

    /** 设置表关联关系 */
    @Bean
    public List<TableColumnRelation> tableRelationList() {
        return List.of(
                // 主表表名, 主表字段, 跟子表的关系(一对一, 一对多, 多对多), 子表表名, 子表字段
                new TableColumnRelation("t_order", "order_no", TableRelationType.ONE_TO_ONE, "t_order_address", "order_no"),
                new TableColumnRelation("t_order", "order_no", TableRelationType.ONE_TO_ONE, "t_order_item", "order_no"),
                new TableColumnRelation("t_order", "order_no", TableRelationType.ONE_TO_MANY, "t_order_log", "order_no")
        );
    }

    /** 设置查询别名 */
    @Bean
    public Map<String, ReqAliasTemplate> queryAliasMap() {
        return Map.of(
                "all-order", new ReqAliasTemplate("Order"),
                "order-address-item-log", orderAddressItemLogAlias()
        );
    }

    private ReqAliasTemplate orderAddressItemLogAlias() {
        /* 指定查询条件及其表达式 */
        ReqAliasTemplateQuery query = new ReqAliasTemplateQuery(OperateType.AND, List.of(
                Map.of("id", ConditionType.$GT), // "id": "$GT"  ->  id >
                Map.of("orderNo", ConditionType.$NN), // orderNo is not null
                Map.of(QueryConst.TEMPLATE_META_NAME, "startTime", "createTime", ConditionType.$GE), // "startTime": "createTime:$ge"  ->  createTime >
                Map.of(QueryConst.TEMPLATE_META_NAME, "endTime", "createTime", ConditionType.$LE), // "endTime": "createTime:$le"  ->  createTime <
                /* 这里的 xxx 表示分组, 参数的时候如果有分组会用到 */
                new ReqAliasTemplateQuery(OperateType.OR, "xxx", List.of(
                        Map.of("orderStatus", ConditionType.$EQ),
                        Map.of("amount", ConditionType.$BET)
                )), // "xxx": { "type": "OR", "cons": { "orderStatus": "$EQ", "amount": "$BET" } } ->  orderStatus =  or  amount between
                Map.of("orderStatus", ConditionType.$IN),
                Map.of("OrderItem.productName", ConditionType.$NE),
                Map.of("OrderAddress.contact", ConditionType.$NE)
        ));
        // 指定默认的排序
        Map<String, String> sort = Map.of("createTime", "desc");
        // 指定默认的分页
        List<String> page = List.of("1");
        // 指定查询时多张表之间的关联方式
        List<List<String>> relationList = List.of(List.of("Order", "inner", "OrderAddress"), List.of("Order", "left", "OrderItem"));
        // 指定返回字段: 订单表 + 订单地址表 订单项表 订单日志表, 查询 Order 时 distinct
        ReqResult result = new ReqResult(List.of(
                "orderNo", "orderStatus", "amount", "desc", "createTime",
                Map.of("address", Map.of("table", "OrderAddress", "columns", List.of("contact", "phone", "address"))),
                Map.of("items", Map.of("table", "OrderItem", "columns", List.of("productName", "price", "number"))),
                Map.of("logs", Map.of("table", "OrderLog", "columns", List.of("operator", "message", "time")))
        ), true);
        // return new ReqAliasTemplate("Order", query, relationList, result);
        return new ReqAliasTemplate("Order", query, sort, page, relationList, result);
    }
}
```

添加以下 mapping
```java
@RestController
@RequiredArgsConstructor
public class TableColumnController {

    private final TableColumnTemplate tableColumnTemplate;

    @GetMapping("/table-column")
    public List<QueryInfo> info(String tables) {
        return tableColumnTemplate.info(tables);
    }

    @PostMapping("/table-column")
    public Object query(@RequestBody ReqInfo req) {
        return tableColumnTemplate.dynamicQuery(req);
    }

    @PostMapping("/query-{alias}")
    public Object query(@PathVariable("alias") String alias, @RequestBody ReqInfo req) {
        return tableColumnTemplate.dynamicQuery(alias, req);
    }
}
```

相关的配置中下
```yaml
query:
  # 存放关联表的类的包地址, 多个用英文逗号隔开.
  scan-packages:
  # 当关联表的类上没有标注解, 将类名转换成表名时, 表的前缀(比如 t_ 开头).
  table-prefix:
  # 设置为 true 时查询表及字段信息将返回空, 比如只在生产环境设置成 true, 默认是 false.
  has-not-return-info:
  # 设置为 true 时, 查询只能基于别名, 默认是 false.
  required-alias:
  # 设置为 true 时, 查询可以不需要有条件或分页, 默认是 false.
  not-required-condition-or-page:
  # 表名 字段名 生成别名的规则(比如表名: t_user_info, 字段: user_name):
  #   Standard(表: UserInfo,  字段: userName, 不设置则默认是此值)
  #   Horizontal(表: User-Info,  字段: user-name)
  #   Under(表: User_Info,  字段: user_name)
  #   Letter(表: A-B...Z-AA...ZZ,  字段: a-b...z-aa...zz)
  #   Number(表: 100001-100002...,  字段: 1-2...)
  #   Same(一致)
  #   Lower(表: user_info,  字段: user_name)
  #   Upper(表: USER_INFO,  字段: USER_NAME)
  alias-generate-rule:
  # 当要请求的分页数据(page * limit)大于当前值时, sql 查询将会拆分成 2 条, 先只查 id 再用 id 查具体的数据, 默认是 10000.
  deep-max-page-size:
  # 查询条件是集合时的最大个数, 默认是 1000.
  max-list-count:
  # 当表与表之间是一对一关联, 但查询时数据却出现了多条时的处理: Exception(抛出异常, 此为默认), First(以前面的为准), Cover(后面覆盖前面).
  one-to-one-has-many-rule:
  # 用来做逻辑删除的字段名, 标了 @LogicInfo 注解则以注解所在字段为主.
  logic-delete-column:
  # 用来做逻辑删除的默认值, 比如设置成 0, 实体上的字段标了 @LogicDelete 则以注解为主.
  logic-value:
  # 用来做逻辑删除的字段类型是 tinyint(1) 时的删除值, 比如设置成 1, 实体上的字段标了 @LogicDelete 则以注解为主.
  logic-delete-boolean-value:
  # 用来做逻辑删除的字段类型是 int 时的删除值, 比如设置成 UNIX_TIMESTAMP(), 实体上的字段标了 @LogicDelete 则以注解为主.
  logic-delete-int-value:
  # 用来做逻辑删除的字段类型是 bigint 时的值, 比如设置成 id, 实体上的字段标了 @LogicDelete 则以注解为主.
  logic-delete-long-value:
```

比如有如下表
```sql
create table `t_order` (
  `id` bigint unsigned not null auto_increment,
  `order_no` varchar(32) not null comment '订单号',
  `order_status` int unsigned not null default '0' comment '订单状态(0.用户已创建待支付, 1.用户已支付待商户发货, 2.商户已发货待用户签收, 3.用户已签收待确认完结, 4.已完结)',
  `amount` decimal(20,2) unsigned not null default '0.00' comment '订单金额',
  `desc` varchar(32) not null default '' comment '备注',
  `create_time` datetime(6) not null default current_timestamp(6) comment '创建时间',
  `update_time` datetime(6) not null default current_timestamp(6) on update current_timestamp(6) comment '更新时间',
  `deleted` int unsigned not null default '0' comment '0.未删除, 非 0.已删除',
  primary key (`id`),
  unique key `uk_order_no` (`order_no`,`deleted`)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='订单';

create table `t_order_address` (
  `id` bigint unsigned not null auto_increment,
  `order_no` varchar(32) not null default '' comment '订单号',
  `contact` varchar(16) not null default '' comment '联系人',
  `phone` varchar(16) not null default '' comment '联系电话',
  `address` varchar(128) not null default '' comment '联系人地址',
  `create_time` datetime(6) not null default current_timestamp(6) comment '创建时间',
  `update_time` datetime(6) not null default current_timestamp(6) on update current_timestamp(6) comment '更新时间',
  `deleted` bigint unsigned not null default '0' comment '0.未删除, 非 0.已删除',
  primary key (`id`),
  unique key `uk_order_no` (`order_no`,`deleted`)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='订单地址';

create table `t_order_item` (
  `id` bigint unsigned not null auto_increment,
  `order_no` varchar(32) not null default '' comment '订单号',
  `product_name` varchar(32) not null default '' comment '商品名',
  `price` decimal(20,2) unsigned not null default '0.00' comment '商品价格',
  `number` int unsigned not null default '0' comment '商品数量',
  primary key (`id`),
  key `idx_order_no` (`order_no`)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='订单项(商品)';

create table `t_order_log` (
  `id` bigint unsigned not null auto_increment,
  `order_no` varchar(32) not null default '' comment '订单号',
  `operator` varchar(32) not null default '' comment '操作人',
  `message` text comment '操作内容',
  `time` datetime(6) not null default current_timestamp(6) comment '创建时间',
  `deleted` tinyint(1) not null default '0' comment '0.未删除, 1.已删除',
  primary key (`id`),
  key `idx_order_no` (`order_no`)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='订单日志';
```
请求 `GET /table-column` 将会返回表及字段的结构数据(如果想要此接口不返回数据, 配置 `query.has-not-return-info = true` 即可), 
```json5
[
  {
    "name": "Order",
    "desc": "订单",
    "columnList": [
      {
        "name": "id",
        "type": "int"
      },
      {
        "name": "orderNo",
        "desc": "订单号",
        "type": "string",
        "writeRequired": true,
        "maxLength": 32
      },
      {
        "name": "orderStatus",
        "desc": "订单状态(0.用户已创建待支付, 1.用户已支付待商户发货, 2.商户已发货待用户签收, 3.用户已签收待确认完结, 4.已完结)",
        "type": "int"
      },
      {
        "name": "amount",
        "desc": "订单金额",
        "type": "number"
      },
      {
        "name": "desc",
        "desc": "备注",
        "type": "string",
        "maxLength": 32
      },
      {
        "name": "createTime",
        "desc": "创建时间",
        "type": "date-time"
      },
      {
        "name": "updateTime",
        "desc": "更新时间",
        "type": "date-time"
      }
    ]
  },
  {
    "name": "OrderAddress",
    "desc": "订单地址",
    "columnList": [
      {
        "name": "id",
        "type": "int"
      },
      {
        "name": "orderNo",
        "desc": "订单号",
        "type": "string",
        "maxLength": 32,
        "relationTable": "Order",
        "relationColumn": "orderNo"
      },
      {
        "name": "contact",
        "desc": "联系人",
        "type": "string",
        "maxLength": 16
      },
      {
        "name": "phone",
        "desc": "联系电话",
        "type": "string",
        "maxLength": 16
      },
      {
        "name": "address",
        "desc": "联系人地址",
        "type": "string",
        "maxLength": 128
      },
      {
        "name": "createTime",
        "desc": "创建时间",
        "type": "date-time"
      },
      {
        "name": "updateTime",
        "desc": "更新时间",
        "type": "date-time"
      }
    ]
  },
  {
    "name": "OrderItem",
    "desc": "订单项(商品)",
    "columnList": [
      {
        "name": "id",
        "type": "int"
      },
      {
        "name": "orderNo",
        "desc": "订单号",
        "type": "string",
        "maxLength": 32,
        "relationTable": "Order",
        "relationColumn": "orderNo"
      },
      {
        "name": "productName",
        "desc": "商品名",
        "type": "string",
        "maxLength": 32
      },
      {
        "name": "price",
        "desc": "商品价格",
        "type": "number"
      },
      {
        "name": "number",
        "desc": "商品数量",
        "type": "int"
      }
    ]
  },
  {
    "name": "OrderLog",
    "desc": "订单日志",
    "columnList": [
      {
        "name": "id",
        "type": "int"
      },
      {
        "name": "orderNo",
        "desc": "订单号",
        "type": "string",
        "maxLength": 32,
        "relationTable": "Order",
        "relationColumn": "orderNo"
      },
      {
        "name": "operator",
        "desc": "操作人",
        "type": "string",
        "maxLength": 32
      },
      {
        "name": "message",
        "desc": "操作内容",
        "type": "string",
        "maxLength": 65535
      },
      {
        "name": "time",
        "desc": "创建时间",
        "type": "date-time"
      }
    ]
  }
]
```

请求 `POST /table-column` 时, 将会自动处理数据查询并组装数据, 其入参示例如下
```json5
{
  "table": "Order", /* 表名 */
  "param": {
    "query": {      /* 查询条件 */
      /* "operate": "下面的条件拼接时的表达式, 并且(and) 和 或者(or) 两种, 不设置则默认是 and.", */
      "conditions": [
        [ "id", "$nn(表达式, 见下面的说明)" ],           /* 无值 */
        [ "orderNo", "$eq", "x" ],                    /* 单值(长度不能超过上面的 maxLength 值) */
        [ "orderStatus", "$in", [ "0", "1", "2" ] ],  /* 多值(长度不能超过 query.max-list-count 设置的值) */
        [ "amount", "$bet", [ "10", "1000.5" ] ],
        [ "OrderItem.productName", "$fuzzy", "xx" ],  /* 子表 */
        [ "OrderLog.operator", "$start", "xxx" ],     /* 子表 */
        {
          "operate": "or", /* 嵌套条件( orderStatus = 3 OR OrderLog.time >= "2020-01-01" ) */
          "conditions": [
            [ "orderStatus", "$eq", "3" ],
            [ "OrderLog.time", "$bet", [ "2020-01-01" ] ]
          ]
        },
      ]
    },
    "sort": { "createTime": "desc",  "OrderLog.operator": "asc" },
    "page": [ 2, 20 ],  /* 分页查询, 如果省略第 2 个参数如 [ 2 ] 则等同于 [ 2, 10 ] */
    "relation": [ [ "Order(主表)", "inner(连接类型, 有 left inner right 三种)", "OrderItem(子表)" ],  [ "Order", "inner", "OrderLog" ] ] # 当上面的 conditions 有多个表时需要
  },
  "result": {
    "columns": [
      "id", "orderNo", "orderStatus", "amount", "desc",
      { "createTime" : [ "yyyy-MM-dd", "GMT+8" ] },  /* 格式化: [ "pattern", "timeZone" ], 默认是 yyyy-MM-dd HH:mm:ss */
      {
        "address(子表数据返回时的自定义属性名)": {
          "table": "OrderAddress",
          "columns": [ "contact", "address" ]
        }
      },
      {
        "items": {
          "table": "OrderItem",
          "columns": [ "productName", "price", "number" ]
        }
      },
      {
        "logs": {
          "table": "OrderLog",
          "columns": [ "operator", "message", "time" ]
        }
      }
    ],
    "distinct": true /* true 表示将查询数据去重, 不设置则默认是 false */
  }
}
```

请求 `POST /query-order-address-item-log` 将使用别名中配置的规则, 接口只关注参数即可
```json5
{
  "req" : {
    "query" : {
      "id" : 1,
      "orderNo": null, /* 任意值(null "" 1 0 均可), 只要有这个项就行. 对应上面的 $nn 条件 */
      "createTime": [ "2020-01-01", "2020-02-01" ],
      "xxx": { "orderStatus": "3", "amount": [ "100.50", "200" ] } /* xxx 跟别名模板中的分组名对应 */
      "orderStatus": [ 1, 2, 3 ],
      "OrderItem.productName": "xx",
      "OrderAddress.contact": "xxx"
    },
    "sort": { "id": "desc" }, /* 排序, 忽略则使用别名中设置的值 */
    "page": [ 2, 10 ] /* 分页, 忽略则使用别名中设置的值 */
  }
}
```

最终会生成如下 `sql`
```sql
/* 如果没有分页查询入参(page)则不会生成此 sql */
SELECT COUNT(DISTINCT `Order`.id)
FROM t_order `Order` INNER JOIN t_order_item OrderItem ON ... INNER JOIN t_order_log OrderLog ON ...
WHERE `Order`.orderNo IS NOT NULL AND `Order`.orderNo = 'x'
AND `Order`.order_status IN ( 0, 1, 2 ) AND `Order`.amount BETWEEN 10 AND 1000.5
AND OrderItem.product_name LIKE '%xx%' AND OrderLog.operator LIKE 'xx%'
AND ( `Order`.order_status = 3 OR OrderLog.time >= '2020-01-01' )


/* 如果没有分页查询入参(page)则不会有 LIMIT */
SELECT DISTINCT `Order`.id, `Order`.order_no, `Order`.order_status, `Order`.amount, `Order`.`desc`
FROM t_order `Order` INNER JOIN t_order_item OrderItem ON ... INNER JOIN t_order_log OrderLog ON ...
WHERE `Order`.orderNo IS NOT NULL AND `Order`.orderNo = 'x'
AND `Order`.order_status IN ( 0, 1, 2 ) AND `Order`.amount BETWEEN 10 AND 1000.5
AND OrderItem.product_name LIKE '%xx%' AND OrderLog.operator LIKE 'xx%'
AND ( `Order`.order_status = 3 OR OrderLog.time >= '2020-01-01' )
ORDER BY `Order`.create_time DESC, OrderLog.operator
LIMIT 10, 20


/* 下面的 xxx 和 yyy 由上面的查询而来, 如果量很大会分批查询, 单次查询的个数由 query.max-list-count 控制 */
SELECT order_no, contact, address
FROM t_order_address
WHERE order_no IN ( 'xxx', 'yyy' )


SELECT order_no, productName, price, number
FROM t_order_item
WHERE order_no IN ( 'xxx', 'yyy' )


SELECT order_no, operator, message, time
FROM t_order_log
WHERE order_no IN ( 'xxx', 'yyy' )
```

返回数据如下
```json5
{
  "count": 123,
  "list": [ /* 如果没有分页查询入参(page)则返回的是此数组 */
    {
      "id": 1234,
      "orderNo": "xx",
      "amount": "xxx",
      "desc": "xxxxxx",
      "createTime": "yyyy-MM-dd",
      "address": {
        "contact": "y",
        "address": "yy"
      },
      "items": [
        {
          "productName": "z",
          "price": "10.5",
          "number": 2
        }
      ],
      "logs": [
        {
          "operator": "z",
          "message": "zz",
          "time": "yyyy-MM-dd HH:mm:ss"
        },
        {
          "operator": "zzz",
          "message": "zzzz",
          "time": "yyyy-MM-dd HH:mm:ss"
        }
      ]
    },
    { ... }
  ]
}
```


## 表达式说明

| 表达式(忽略大小写) | 说明      | 对应 sql       |
| :------------------ | :--------:| -------------: |
| $nu                 | 为空      | IS NULL        |
| $nn                 | 不为空    | IS NOT NULL    |
| $eq                 | 等于      | =              |
| $ne                 | 不等于    | <>             |
| $in                 | 包含      | IN             |
| $ni                 | 不包含    | NOT IN         |
| $bet                | 区间      | BETWEEN        |
| $nbe                | 不在区间  | NOT BETWEEN    |
| $gt                 | 大于      | >              |
| $ge                 | 大于等于  | >=             |
| $lt                 | 小于      | <              |
| $le                 | 小于等于  | <=             |
| $fuzzy              | 模糊      | LIKE '%x%'     |
| $nfuzzy             | 不模糊    | NOT LIKE '%x%' |
| $start              | 开头      | LIKE 'x%'      |
| $nstart             | 不开头    | NOT LIKE 'x%'  |
| $end                | 结尾      | LIKE '%x'      |
| $nend               | 不结尾    | NOT LIKE '%x'  |

-----
