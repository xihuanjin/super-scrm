package com.scrm.server.wx.cp.service.impl;

import com.alibaba.fastjson.JSON;
import com.scrm.common.config.ScrmConfig;
import com.scrm.server.wx.cp.config.WxCpConfiguration;
import com.scrm.server.wx.cp.service.IWxPortalService;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.message.WxCpXmlMessage;
import me.chanjar.weixin.cp.bean.message.WxCpXmlOutMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 企业微信回调门户服务 —— 多租户改造：按 corpId 动态路由
 *
 * @author xuxh
 * @date 2022/2/15 9:26
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class WxPortalServiceImpl implements IWxPortalService {

    @Autowired
    private WxCpConfiguration wxCpConfiguration;

    @Override
    public String handler(Integer agentId, String requestBody, String signature, String timestamp, String nonce) {
        log.info("\n接收微信请求：[signature=[{}], timestamp=[{}], nonce=[{}], requestBody=[\n{}\n] ",
                signature, timestamp, nonce, requestBody);

        // 先用默认WxCpService解密消息（解密不依赖corpId，使用服务商统一密钥即可）
        final WxCpService wxCpService;
        if (ScrmConfig.isSaasMode()) {
            // SaaS模式：使用服务商配置解密，corpId在消息体中
            wxCpService = WxCpConfiguration.getCustomerSecretWxCpService();
        } else {
            wxCpService = WxCpConfiguration.getCustomerSecretWxCpService();
        }

        WxCpXmlMessage inMessage = WxCpXmlMessage.fromEncryptedXml(requestBody, wxCpService.getWxCpConfigStorage(),
                timestamp, nonce, signature);
        log.debug("\n消息解密后内容为：\n{} ", JSON.toJSONString(inMessage));

        // 从解密后的消息中提取 corpId（ToUserName = 企业微信 corpId）
        String corpId = inMessage.getToUserName();
        if (StringUtils.isBlank(corpId)) {
            log.warn("回调消息中ToUserName(corpId)为空，使用默认企业");
        }

        // 返回
        WxCpXmlOutMessage outMessage = route(agentId, inMessage, corpId);
        if (outMessage == null) {
            return "";
        }

        // 使用对应企业的WxCpService加密回复
        WxCpService replyService;
        if (ScrmConfig.isSaasMode() && StringUtils.isNotBlank(corpId)) {
            replyService = wxCpConfiguration.getWxCpServiceByCorpId(corpId);
        } else {
            replyService = wxCpService;
        }

        String out = outMessage.toEncryptedXml(replyService.getWxCpConfigStorage());
        log.debug("\n组装回复信息：{}", out);
        return out;
    }

    public WxCpXmlOutMessage route(Integer agentId, WxCpXmlMessage message, String corpId) {
        try {
            if (ScrmConfig.isSaasMode() && StringUtils.isNotBlank(corpId)) {
                // SaaS模式：按corpId动态路由
                return wxCpConfiguration.getRouterByCorpId(corpId, agentId).route(message);
            } else {
                // Private模式：使用默认路由
                return WxCpConfiguration.getRouters()
                        .getOrDefault(ScrmConfig.getExtCorpID() + ":" + ScrmConfig.getMainAgentID(),
                                WxCpConfiguration.getRouters().get(ScrmConfig.getMainAgentID().toString()))
                        .route(message);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}
