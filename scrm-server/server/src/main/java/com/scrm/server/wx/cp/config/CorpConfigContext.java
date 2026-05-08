package com.scrm.server.wx.cp.config;

import com.scrm.common.config.ScrmConfig;
import com.scrm.common.entity.BrCorpAccredit;
import com.scrm.common.util.JwtUtil;
import com.scrm.common.util.RequestUtils;
import com.scrm.server.wx.cp.service.IBrCorpAccreditService;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.api.impl.WxCpServiceImpl;
import me.chanjar.weixin.cp.config.impl.WxCpDefaultConfigImpl;
import me.chanjar.weixin.cp.message.WxCpMessageRouter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 租户配置上下文 —— 多租户架构的核心组件。
 *
 * 统一管理企业级配置的获取，按 deployMode 分流：
 * - private 模式：从 ScrmConfig 的全局配置读取（向后兼容）
 * - saas 模式：从 BrCorpAccredit 授权表按 corpId 动态加载
 *
 * @author generated
 * @since 2026-05-07
 */
@Slf4j
@Component
public class CorpConfigContext {

    @Autowired
    private IBrCorpAccreditService brCorpAccreditService;

    /** 每个企业的 WxCpService 实例缓存 */
    private final Map<String, WxCpService> cpServiceMap = new ConcurrentHashMap<>();

    /** 每个企业+Agent的消息路由器缓存 (key = corpId:agentId) */
    private final Map<String, WxCpMessageRouter> routerMap = new ConcurrentHashMap<>();

    /**
     * 判断当前是否为 SaaS 多租户模式
     */
    public boolean isSaasMode() {
        return ScrmConfig.isSaasMode();
    }

    /**
     * 判断当前是否为私有化单企业模式
     */
    public boolean isPrivateMode() {
        return ScrmConfig.isPrivateMode();
    }

    // ==================== 当前请求的租户标识 ====================

    /**
     * 从 JWT Token 中获取当前请求的 corpId。
     * SaaS 模式下返回该用户所属企业；Private 模式下返回默认企业ID。
     */
    public String getCurrentCorpId() {
        if (isSaasMode()) {
            try {
                String corpId = JwtUtil.getExtCorpId();
                if (StringUtils.isNotBlank(corpId)) {
                    return corpId;
                }
            } catch (Exception e) {
                log.debug("无法从JWT获取corpId，回退到默认企业: {}", e.getMessage());
            }
        }
        return ScrmConfig.getExtCorpID();
    }

    /**
     * 从请求头 Token 中获取当前请求的 corpId（不抛异常版本）。
     * 如果无法获取则返回 null，调用方自行处理。
     */
    public String tryGetCurrentCorpId() {
        try {
            if (isSaasMode()) {
                String token = RequestUtils.getToken();
                if (StringUtils.isNotBlank(token)) {
                    String corpId = JwtUtil.getExtCorpId();
                    if (StringUtils.isNotBlank(corpId)) {
                        return corpId;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("tryGetCurrentCorpId失败: {}", e.getMessage());
        }
        return null;
    }

    // ==================== 企业级配置获取 ====================

    /**
     * 获取指定企业的授权信息。
     * Private 模式返回模拟对象；SaaS 模式从数据库查询。
     */
    public BrCorpAccredit getCorpAccredit(String corpId) {
        if (isSaasMode() && StringUtils.isNotBlank(corpId)) {
            BrCorpAccredit accredit = brCorpAccreditService.getByCorpId(corpId);
            if (accredit != null && StringUtils.isNotBlank(accredit.getCorpId())) {
                return accredit;
            }
        }
        // Private 模式：构造一个模拟对象，使用 ScrmConfig 中的值
        return buildDefaultAccredit();
    }

    /**
     * 获取指定企业的 agentId
     */
    public Integer getAgentId(String corpId) {
        if (isSaasMode() && StringUtils.isNotBlank(corpId)) {
            BrCorpAccredit accredit = getCorpAccredit(corpId);
            if (accredit.getAuthInfo() != null
                    && accredit.getAuthInfo().getAgents() != null
                    && !accredit.getAuthInfo().getAgents().isEmpty()) {
                return accredit.getAuthInfo().getAgents().get(0).getAgentId();
            }
        }
        return ScrmConfig.getMainAgentID();
    }

    /**
     * 获取指定企业的名称
     */
    public String getCorpName(String corpId) {
        if (isSaasMode() && StringUtils.isNotBlank(corpId)) {
            BrCorpAccredit accredit = getCorpAccredit(corpId);
            if (accredit.getAuthCorpInfo() != null
                    && StringUtils.isNotBlank(accredit.getAuthCorpInfo().getCorpName())) {
                return accredit.getAuthCorpInfo().getCorpName();
            }
        }
        return ScrmConfig.getCorpName();
    }

    // ==================== URL 构建 ====================

    /**
     * 构建企业级 URL（domainName + 相对路径）。
     * 当前 domainName 为全局配置，所有企业共用。
     */
    public String buildUrl(String corpId, String relativePath) {
        return ScrmConfig.getDomainName() + relativePath;
    }

    /**
     * 获取客户详情URL
     */
    public String getCustomerDetailUrl(String corpId) {
        return ScrmConfig.getDomainName() + ScrmConfig.getCustomerDetailRelativePath();
    }

    /**
     * 获取跟进详情URL
     */
    public String getFollowDetailUrl(String corpId) {
        return ScrmConfig.getDomainName() + ScrmConfig.getFollowDetailRelativePath();
    }

    /**
     * 获取群欢迎语URL
     */
    public String getGroupChatWelcomeUrl(String corpId) {
        return ScrmConfig.getDomainName() + ScrmConfig.getGroupChatWelcomeRelativePath();
    }

    /**
     * 获取轨迹素材URL
     */
    public String getMediaInfoUrl(String corpId) {
        return ScrmConfig.getDomainName() + ScrmConfig.getMediaInfoRelativePath();
    }

    /**
     * 获取销售日报URL
     */
    public String getSaleReportUrl(String corpId) {
        return ScrmConfig.getDomainName() + ScrmConfig.getSaleReportRelativePath();
    }

    // ==================== WxCpService 管理（多企业） ====================

    /**
     * 获取或创建指定企业的 WxCpService 实例。
     * 按 corpId 缓存，避免重复创建。
     */
    public WxCpService getOrCreateWxCpService(String corpId) {
        if (StringUtils.isBlank(corpId)) {
            corpId = getCurrentCorpId();
        }
        final String key = corpId;
        return cpServiceMap.computeIfAbsent(key, k -> createWxCpService(k));
    }

    /**
     * 获取默认的 WxCpService（Private 模式或向下兼容）
     */
    public WxCpService getDefaultWxCpService() {
        return getOrCreateWxCpService(getCurrentCorpId());
    }

    private WxCpService createWxCpService(String corpId) {
        log.info("创建WxCpService实例, corpId={}", corpId);
        WxCpDefaultConfigImpl configStorage = new WxCpDefaultConfigImpl();
        configStorage.setCorpId(corpId);
        configStorage.setAgentId(getAgentId(corpId));
        configStorage.setBaseApiUrl(ScrmConfig.getBaseApiUrl());

        if (isSaasMode()) {
            // SaaS 模式：从授权表获取密钥
            BrCorpAccredit accredit = getCorpAccredit(corpId);
            // 服务商模式使用 permanentCode 代调用，不需要 corpSecret
            // 但自建应用模式仍需配置密钥
        } else {
            // Private 模式：使用全局配置
            configStorage.setCorpSecret(ScrmConfig.getCustomerSecret());
            configStorage.setToken(ScrmConfig.getShortCallbackToken());
            configStorage.setAesKey(ScrmConfig.getCustomerAesKey());
        }

        WxCpService service = new WxCpServiceImpl();
        service.setWxCpConfigStorage(configStorage);
        return service;
    }

    /**
     * 获取指定企业的消息路由器（用于回调事件分发）
     */
    public WxCpMessageRouter getRouter(String corpId, Integer agentId) {
        String key = corpId + ":" + agentId;
        return routerMap.get(key);
    }

    /**
     * 注册路由器
     */
    public void registerRouter(String corpId, Integer agentId, WxCpMessageRouter router) {
        String key = corpId + ":" + agentId;
        routerMap.put(key, router);
    }

    /**
     * 获取默认路由器（Private 模式向下兼容）
     */
    public WxCpMessageRouter getDefaultRouter() {
        String corpId = getCurrentCorpId();
        Integer agentId = ScrmConfig.getMainAgentID();
        return getRouter(corpId, agentId);
    }

    // ==================== 初始化 ====================

    @PostConstruct
    public void init() {
        log.info("CorpConfigContext 初始化, deployMode={}, isSaas={}",
                ScrmConfig.getDeployMode(), isSaasMode());
        if (isPrivateMode()) {
            // Private 模式：预初始化默认企业
            String defaultCorpId = ScrmConfig.getExtCorpID();
            log.info("Private模式，默认企业: corpId={}, corpName={}",
                    defaultCorpId, ScrmConfig.getCorpName());
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 为 Private 模式构建默认的企业授权模拟对象
     */
    private BrCorpAccredit buildDefaultAccredit() {
        BrCorpAccredit accredit = new BrCorpAccredit();
        accredit.setCorpId(ScrmConfig.getExtCorpID());
        return accredit;
    }
}
