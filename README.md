推送服务以延时任务为基础，提供单条推送和批量推送，无论是哪种方式，都是具有失败重试机制。所有推送数据会先入库。建议使用批量推送。
其中

单条推送：每个推送消息都是一个延时任务，提供失败重试，次数默认为5，不能更改
批量推送：相同AppId和PushType的数据公用一个延时任务，提供满足条数推送和失败重试，详见下表。
其中enabled、capacity、next_task_interval、url可动态配置，在下一次定时扫描数据库配置后，第一次有推送任务时生效

Db配置：52库-db_sleep_push
表 tb_push_config
字段名称				字段含义
push_config_id	
app_id					应用标识
next_task_interval		下次任务的时间间隔，ms，批量推送生效
least_retry_times		至少重试次数，批量推送生效
url						推送地址
capacity				批量推送满足条件，批量推送生效
push_type				推送类型，类型相同的使用同一个任务，批量推送生效
first_delayed_ms		数据库扫描，延时任务创建后，第一次延迟ms后执行，批量推送生效
delayed_cycle_ms		数据库扫描，周期ms执行，批量推送生效
enabled					0：启用，1：禁用
此表是配置推送的一些基本信息，其中app_id和push_type可以组合使用，用来区分相同应用不同类型的任务，例如：睡眠报告和睡眠实时数据，它们的推送频率、满足推送数量可能不同。

推送成功或失败的消息可以在tb_push_record表中查看，未配置tb_push_config的推送数据存储在tb_push_record_no_config中。详见表结构。

高频率案例 
INSERT INTO `db_sleep_push`.`tb_push_config`(`push_config_id`, `app_id`, `next_task_interval`, `least_retry_times`, `url`, `capacity`, `push_type`, `first_delayed_ms`, `delayed_cycle_ms`, `enabled`) VALUES (1, 1, 60000, 3, 'xxx', 100, 0, 10000, 20000, 0);  
低频率案例 INSERT INTO `db_sleep_push`.`tb_push_config`(`push_config_id`, `app_id`, `next_task_interval`, `least_retry_times`, `url`, `capacity`, `push_type`, `first_delayed_ms`, `delayed_cycle_ms`, `enabled`) VALUES (2, 2, 60000, 3, 'xxx', 10, 1, 10000, 45000, 0);

注意：
1.其中缓存tb_push_config使用的是ConcurrentHashMap，你可以换成guava，让缓存拥有失效时间
2.失败重试后仍然失败的数据，没有做任何处理，你可以写个定时任务，定时删除这些任务
3.推送成功的数据会立即删除，因为数据多时，查询数据库的效率会很慢
4.批量推送使用了synchronized，但是在扫描数据库数据和添加数据时，我已做了优化，当然你可以换成lock
5.注意tb_push_config配置任务时间间隔的合理性
6.服务调用和监听mq消息两种方式处理推送数据，建议使用mq，需要自己实现

MQ监听：
	客户端：
		Map<String, Object> map = new HashMap<>();
		map.put("appId", 12301);
		map.put("data", "JSON数据");
		map.put("pushType", 0);
		map.put("flag", true);
		// 默认为批量推送
		rocketMqServiceProducer.send("SLEEP_BUSINESS_PUSH_TOPIC", "hotel_push", "123", JSON.toJSONString(map));<br/>
	服务端：
		Map<String, Object> dataMap = JSON.parseObject(msgContent, Map.class);
		Object appId = dataMap.get("appId");
		Object data = dataMap.get("data");
		Object batchPush = dataMap.get("flag");
		Object pushType = dataMap.get("pushType");
		if (appId == null || data == null || pushType == null) {
			logger.error("param error , appId is {} , data is {} , pushType is {}", appId, data, pushType);
			continue;
		}
		if (batchPush == null) {
			batchPush = true;
		}
		String appIdStr = String.valueOf(appId);
		String dataStr = String.valueOf(data);
		String pushTypeStr = String.valueOf(pushType);
		boolean batchPushFlag = Boolean.valueOf(batchPush.toString());
		if (!StringUtils.isNumeric(appIdStr) || !StringUtils.isNumeric(pushTypeStr)) {
			logger.error("param type error , appIdStr is {} , pushTypeStr is {}", appIdStr, pushTypeStr);
			continue;
		}
		if (batchPushFlag) {
			httpPushService.delayBatchPushJson(dataStr, Integer.valueOf(appIdStr), Integer.valueOf(pushTypeStr));
		} else {
			httpPushService.singlePushJson(dataStr, Integer.valueOf(appIdStr), Integer.valueOf(pushTypeStr));
		}
最终推送的格式为：
{\"msgId\":\"uuid\",\"list\":[{Your Json Data},{Your Json Data},...]}



