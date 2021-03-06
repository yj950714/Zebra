package com.dianping.zebra.administrator.service.impl;

import com.dianping.zebra.administrator.config.ZookeeperService;
import com.dianping.zebra.administrator.dao.ShardMapper;
import com.dianping.zebra.administrator.dto.shard.ShardConfigDto;
import com.dianping.zebra.administrator.dto.shard.TableShardConfigDto;
import com.dianping.zebra.administrator.entity.DimensionConfigs;
import com.dianping.zebra.administrator.entity.ShardConfig;
import com.dianping.zebra.administrator.entity.ShardEntity;
import com.dianping.zebra.administrator.entity.TableShardConfig;
import com.dianping.zebra.administrator.service.ShardService;
import com.dianping.zebra.administrator.service.ZookeeperConfigService;
import com.dianping.zebra.administrator.util.JaxbUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.dianping.zebra.administrator.GlobalConstants.SHARD_CONFIG_NAME_PATTERN;

/**
 * @author canhuang
 * @date 2018/3/25
 */
@Service
public class ShardServiceImpl extends BaseServiceImpl implements ShardService {

    @Autowired
    private ShardMapper shardDao;

    @Autowired
    private ZookeeperConfigService zkConfigDao;

    @Override
    @Transactional
    public void addRuleName(String ruleName, String zkAddr, String owner, String desc) {
        ruleName = ruleName.toLowerCase();

        ShardEntity shardEntity = new ShardEntity();
        shardEntity.setRuleName(ruleName);
        shardEntity.setEnv(zkAddr);
        shardEntity.setStatus(0);
        shardEntity.setOwner(owner);
        shardEntity.setDescription(desc);
        shardDao.createRule(shardEntity);
    }

    @Override
    public void saveRuleNameConfig(ShardConfigDto shardConfigDto) {
        ShardConfig shardConfig = new ShardConfig();
        List<TableShardConfig> tsConfigList = new ArrayList<>();
        for (TableShardConfigDto tsConfigDto : shardConfigDto.getTableShardConfigs()) {
            TableShardConfig tsConfig = new TableShardConfig();
            tsConfig.setTableName(tsConfigDto.getTableName());

            DimensionConfigs dimensionConfigs = new DimensionConfigs();
            dimensionConfigs.setDimensionConfig(tsConfigDto.getDimensionConfigs());
            tsConfig.setDimensionConfigs(dimensionConfigs);

            tsConfigList.add(tsConfig);
        }
        shardConfig.setTableShardConfigs(tsConfigList);
        byte[] shardData = JaxbUtils.jaxbWriteXml(ShardConfig.class, shardConfig);
        //shardconfig存到zk中
        String shardConfigKey = String.format(SHARD_CONFIG_NAME_PATTERN, shardConfigDto.getRuleName());

        String host = zkConfigDao.getZKHostByName(shardConfigDto.getEnv());
        ZookeeperService.createKey(host, shardConfigKey);

        ZookeeperService.setConfig(host, shardConfigKey, shardData);

    }

    @Override
    public void deleteRuleNameConfig(String ruleName, String env) {
        //shard表删除rulename
        shardDao.deleteRuleName(ruleName, env);

        //删除zk中配置
        String host = zkConfigDao.getZKHostByName(env);
        String shardConfigKey = String.format(SHARD_CONFIG_NAME_PATTERN, ruleName);
        ZookeeperService.deleteConfig(host,shardConfigKey);
    }
}
