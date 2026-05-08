package com.scrm.server.wx.cp.config;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.scrm.common.config.ScrmConfig;
import com.scrm.common.constant.Constants;
import com.scrm.server.wx.cp.handler.AbstractHandler;
import com.scrm.server.wx.cp.handler.LogHandler;
import com.scrm.server.wx.cp.handler.MsgHandler;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.api.impl.WxCpServiceImpl;
import me.chanjar.weixin.cp.config.impl.WxCpDefaultConfigImpl;
import me.chanjar.weixin.cp.message.WxCpMessageRouter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 企业微信配置 —— 管理 WxCpService 和 WxCpMessageRouter 实例。
 *
 * 多租户改造：支持按 corpId 创建和缓存独立的 WxCpService 实例。
 * Private 模式保持向下兼容，SaaS 模式动态创建。
 *
 * @author qiuzl
 * @date Created in 2021/12/12 20:57
 */
@Slf4j
@Component
public class WxCpConfiguration extends ScrmConfig {

    @Autowired
    private LogHandler logHandler;

    @Autowired
    private MsgHandler msgHandler;

    @Autowired
    Map<String, AbstractHandler> eventHandlerMap;

    @Autowired
    private CorpConfigContext corpConfigContext;

    @Value("${baseApiUrl}")
    private volatile static String baseApiUrl;

    /** 每个企业的 WxCpService 缓存 (key=corpId) */
    private static final Map<String, WxCpService> cpServiceMap = new ConcurrentHashMap<>();

    /** 消息路由器缓存 (key=corpId:agentId) */
    private static final Map<String, WxCpMessageRouter> routers = new ConcurrentHashMap<>();

    public static Map<String, WxCpMessageRouter> getRouters() {
        return routers;
    }

    // ==================== 多租户 API（推荐使用） ====================

    /**
     * 根据 corpId 获取 WxCpService。SaaS 模式按企业创建，Private 模式使用默认配置。
     */
    public WxCpService getWxCpServiceByCorpId(String corpId) {
        if (cpServiceMap.containsKey(corpId)) {
            return cpServiceMap.get(corpId);
        }
        synchronized (cpServiceMap) {
            if (cpServiceMap.containsKey(corpId)) {
                return cpServiceMap.get(corpId);
            }
            WxCpDefaultConfigImpl configStorage = new WxCpDefaultConfigImpl();
            configStorage.setCorpId(corpId);
            configStorage.setAgentId(corpConfigContext.getAgentId(corpId));
            configStorage.setBaseApiUrl(ScrmConfig.getBaseApiUrl());

            if (ScrmConfig.isSaasMode()) {
                // SaaS 模式：使用默认回调密钥（由服务商统一管理）
                configStorage.setToken(ScrmConfig.getCallbackToken());
                configStorage.setAesKey(ScrmConfig.getCallbackAesKey());
            } else {
                // Private 模式：使用全局配置
                configStorage.setCorpSecret(ScrmConfig.getCustomerSecret());
                configStorage.setToken(ScrmConfig.getShortCallbackToken());
                configStorage.setAesKey(ScrmConfig.getCustomerAesKey());
            }

            WxCpService service = new WxCpServiceImpl();
            service.setWxCpConfigStorage(configStorage);
            cpServiceMap.put(corpId, service);
            log.info("创建WxCpService: corpId={}", corpId);
            return service;
        }
    }

    /**
     * 根据 corpId 获取消息路由器，不存在则自动创建。
     */
    public WxCpMessageRouter getRouterByCorpId(String corpId, Integer agentId) {
        String key = corpId + ":" + agentId;
        return routers.computeIfAbsent(key, k -> {
            WxCpService service = getWxCpServiceByCorpId(corpId);
            WxCpMessageRouter router = buildRouter(service);
            log.info("创建WxCpMessageRouter: corpId={}, agentId={}", corpId, agentId);
            return router;
        });
    }

    // ==================== 向下兼容 API（Private 模式） ====================

    /**
     * 获取默认企业的 WxCpService（Private 模式）。
     * @deprecated SaaS 模式下请使用 getWxCpServiceByCorpId(corpId)
     */
    @Deprecated
    public static WxCpService getWxCpService() {
        return getBySecretInternal(ScrmConfig.getMainAgentID(),
                ScrmConfig.getMainAgentSecret(), null, ScrmConfig.getCallbackAesKey());
    }

    /**
     * 获取通讯录同步用的 WxCpService。
     * @deprecated SaaS 模式下请使用 getWxCpServiceByCorpId(corpId)
     */
    @Deprecated
    public static WxCpService getAddressBookWxCpService() {
        return getBySecretInternal(ScrmConfig.getMainAgentID(),
                ScrmConfig.getContactSecret(), ScrmConfig.getCallbackToken(),
                ScrmConfig.getCallbackAesKey());
    }

    /**
     * 获取客户联系用的 WxCpService。
     * @deprecated SaaS 模式下请使用 getWxCpServiceByCorpId(corpId)
     */
    @Deprecated
    public static WxCpService getCustomerSecretWxCpService() {
        return getBySecretInternal(ScrmConfig.getMainAgentID(),
                ScrmConfig.getCustomerSecret(), ScrmConfig.getShortCallbackToken(),
                ScrmConfig.getCustomerAesKey());
    }

    public static WxCpService getWxCpService(Integer agentId, String secret) {
        return getBySecretInternal(agentId, secret, null, ScrmConfig.getCallbackAesKey());
    }

    private static WxCpService getBySecretInternal(Integer agentId, String secret,
                                                    String token, String aesKey) {
        String mapKey = secret + ":" + agentId;
        if (cpServiceMap.containsKey(mapKey)) {
            return cpServiceMap.get(mapKey);
        }
        WxCpDefaultConfigImpl configStorage = new WxCpDefaultConfigImpl();
        configStorage.setCorpId(ScrmConfig.getExtCorpID());
        configStorage.setAgentId(agentId);
        configStorage.setCorpSecret(secret);
        configStorage.setToken(token);
        configStorage.setAesKey(aesKey);
        configStorage.setBaseApiUrl(ScrmConfig.getBaseApiUrl());
        WxCpService wxCpService = new WxCpServiceImpl();
        wxCpService.setWxCpConfigStorage(configStorage);
        cpServiceMap.put(mapKey, wxCpService);
        return wxCpService;
    }

    // ==================== 初始化 ====================

    @PostConstruct
    public void initServices() {
        if (ScrmConfig.isSaasMode()) {
            log.info("SaaS模式：WxCpService 按需创建，不预初始化");
            return;
        }

        // Private 模式：预初始化默认企业的路由
        val configStorage = new WxCpDefaultConfigImpl();
        configStorage.setCorpId(ScrmConfig.getExtCorpID());
        configStorage.setAgentId(ScrmConfig.getMainAgentID());
        configStorage.setCorpSecret(ScrmConfig.getCustomerSecret());
        configStorage.setToken(ScrmConfig.getShortCallbackToken());
        configStorage.setAesKey(ScrmConfig.getCustomerAesKey());
        configStorage.setBaseApiUrl(baseApiUrl);
        val service = new WxCpServiceImpl();
        service.setWxCpConfigStorage(configStorage);

        String key = ScrmConfig.getExtCorpID() + ":" + ScrmConfig.getMainAgentID();
        routers.put(key, buildRouter(service));
        log.info("Private模式：预初始化完成, corpId={}", ScrmConfig.getExtCorpID());
    }

    private WxCpMessageRouter buildRouter(WxCpService wxCpService) {
        final val newRouter = new WxCpMessageRouter(wxCpService);

        // 事件业务处理
        Constants.WX_ALL_EVENT_TYPES.forEach(eventType -> {
            AbstractHandler abstractHandler = eventHandlerMap.get(eventType);
            if (abstractHandler != null) {
                newRouter.rule().async(false).msgType(WxConsts.XmlMsgType.EVENT)
                        .event(eventType).handler(this.logHandler, abstractHandler).end();
            }
        });

        // 记录所有事件的日志（异步执行）
        newRouter.rule().handler(this.logHandler).next();

        // 默认
        newRouter.rule().async(false).handler(this.logHandler).end();

        return newRouter;
    }
}
