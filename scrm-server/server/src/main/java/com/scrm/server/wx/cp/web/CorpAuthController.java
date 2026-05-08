package com.scrm.server.wx.cp.web;

import com.scrm.common.config.ScrmConfig;
import com.scrm.common.constant.R;
import com.scrm.common.entity.BrCorpAccredit;
import com.scrm.common.exception.BaseException;
import com.scrm.common.log.annotation.Log;
import com.scrm.common.util.UUID;
import com.scrm.server.wx.cp.config.CorpConfigContext;
import com.scrm.server.wx.cp.config.WxCpTpConfiguration;
import com.scrm.server.wx.cp.feign.MpAuthFeign;
import com.scrm.server.wx.cp.feign.dto.*;
import com.scrm.server.wx.cp.service.IBrCorpAccreditService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.tp.service.WxCpTpService;
import me.chanjar.weixin.cp.tp.service.impl.WxCpTpServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;

/**
 * 企业微信授权控制器 —— 多租户核心入口。
 *
 * 实现企业微信管理员扫码授权流程：
 * 1. GET /corp-auth/installUrl → 获取授权安装链接（生成二维码）
 * 2. GET /corp-auth/callback → 授权回调（企业微信→系统）
 * 3. GET /corp-auth/list → 已授权企业列表
 *
 * @author generated
 * @since 2026-05-07
 */
@Slf4j
@RestController
@RequestMapping("/corp-auth")
@Api(tags = {"企业微信授权（多租户）"})
public class CorpAuthController {

    @Autowired
    private WxCpTpConfiguration wxCpTpConfiguration;

    @Autowired
    private MpAuthFeign mpAuthFeign;

    @Autowired
    private IBrCorpAccreditService brCorpAccreditService;

    @Autowired
    private CorpConfigContext corpConfigContext;

    /**
     * 获取企业微信授权安装链接。
     * 企业管理员扫描生成的二维码后，进入企业微信授权页面。
     */
    @GetMapping("/installUrl")
    @ApiOperation("获取企业微信授权安装链接")
    @Log(modelName = "企业微信授权", operatorType = "获取安装链接")
    public R<String> getInstallUrl() {
        if (ScrmConfig.isPrivateMode()) {
            throw new BaseException("私有化部署模式不支持此接口，请使用自建应用方式登录");
        }

        try {
            WxCpTpService tpService = new WxCpTpServiceImpl();
            tpService.setWxCpTpConfigStorage(wxCpTpConfiguration.getBaseConfig());

            // 获取预授权码
            PreAuthCodeParams params = new PreAuthCodeParams();
            params.setComponent_appid(ScrmConfig.getMainSuiteID());
            PreAuthCodeRes preAuthCode = mpAuthFeign.apiCreatePreAuthCode(
                    params, tpService.getSuiteAccessToken());

            // 构造授权链接
            String redirectUri = ScrmConfig.getDomainName() + "/api/corp-auth/callback";
            String installUrl = String.format(
                    "https://open.work.weixin.qq.com/3rdapp/install?suite_id=%s&pre_auth_code=%s&redirect_uri=%s&state=%s",
                    ScrmConfig.getMainSuiteID(),
                    preAuthCode.getPre_auth_code(),
                    redirectUri,
                    UUID.get16UUID()
            );

            log.info("生成授权安装链接: {}", installUrl);
            return R.data(installUrl);
        } catch (Exception e) {
            log.error("生成授权安装链接失败", e);
            throw new BaseException("生成授权安装链接失败: " + e.getMessage());
        }
    }

    /**
     * 授权回调 —— 企业管理员扫码确认后，企业微信回调此接口。
     *
     * @param authCode 授权码（企业微信带回的临时凭证）
     * @param expiresIn 过期时间
     * @param state 状态值
     */
    @GetMapping("/callback")
    @ApiOperation("授权回调")
    @Log(modelName = "企业微信授权", operatorType = "授权回调")
    public void authCallback(
            @RequestParam("auth_code") String authCode,
            @RequestParam(value = "expires_in", required = false) Integer expiresIn,
            @RequestParam(value = "state", required = false) String state,
            HttpServletResponse response) {

        log.info("收到授权回调: authCode={}, expiresIn={}, state={}", authCode, expiresIn, state);

        if (ScrmConfig.isPrivateMode()) {
            throw new BaseException("私有化部署模式不支持此接口");
        }

        try {
            WxCpTpService tpService = new WxCpTpServiceImpl();
            tpService.setWxCpTpConfigStorage(wxCpTpConfiguration.getBaseConfig());

            // 用 authCode 换取 permanentCode + 企业信息
            QueryAuthParams queryParams = new QueryAuthParams();
            queryParams.setAuthorization_code(authCode);
            QueryAuthRes authRes = mpAuthFeign.apiQueryAuth(queryParams,
                    tpService.getSuiteAccessToken());

            String corpId = authRes.getAuthorization_info().getAuth_corp_info().getCorpid();
            log.info("授权企业: corpId={}, corpName={}",
                    corpId, authRes.getAuthorization_info().getAuth_corp_info().getCorp_name());

            // 检查是否已存在（更新授权）
            brCorpAccreditService.deleteByCorpId(corpId);

            // 保存授权信息
            BrCorpAccredit accredit = new BrCorpAccredit();
            accredit.setId(UUID.get32UUID());
            accredit.setCorpId(corpId);
            accredit.setPermanentCode(authRes.getAuthorization_info().getPermanent_code());
            accredit.setCreatedAt(new Date());
            accredit.setUpdatedAt(new Date());

            // 转换并存储企业信息
            accredit.setAuthCorpInfo(convertAuthCorpInfo(authRes.getAuthorization_info().getAuth_corp_info()));
            accredit.setAuthInfo(convertAuthInfo(authRes.getAuthorization_info().getAuth_info()));

            brCorpAccreditService.save(accredit);
            log.info("企业授权信息已保存: corpId={}", corpId);

            // 重定向到管理后台登录页
            String loginUrl = ScrmConfig.getDomainName() + "/app/login?corpId=" + corpId;
            response.sendRedirect(loginUrl);

        } catch (WxErrorException e) {
            log.error("授权回调处理失败（企微API错误）", e);
            throw BaseException.buildBaseException(e.getError(), "授权失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("授权回调处理失败", e);
            throw new BaseException("授权失败: " + e.getMessage());
        }
    }

    /**
     * 获取已授权企业列表（管理后台使用）
     */
    @GetMapping("/list")
    @ApiOperation("获取已授权企业列表")
    @Log(modelName = "企业微信授权", operatorType = "查询授权列表")
    public R<List<BrCorpAccredit>> listAuthorizedCorps() {
        if (ScrmConfig.isPrivateMode()) {
            throw new BaseException("私有化部署模式不支持此接口");
        }
        List<BrCorpAccredit> list = brCorpAccreditService.list();
        // 脱敏处理：不返回 permanentCode 等敏感信息
        list.forEach(accredit -> {
            accredit.setPermanentCode(null);
        });
        return R.data(list);
    }

    // ==================== DTO 转换方法 ====================

    private me.chanjar.weixin.cp.bean.WxCpTpPermanentCodeInfo.AuthCorpInfo convertAuthCorpInfo(
            AuthCorpInfo params) {
        me.chanjar.weixin.cp.bean.WxCpTpPermanentCodeInfo.AuthCorpInfo result =
                new me.chanjar.weixin.cp.bean.WxCpTpPermanentCodeInfo.AuthCorpInfo();
        result.setCorpId(params.getCorpid());
        result.setCorpName(params.getCorp_name());
        result.setCorpType(params.getCorp_type());
        result.setCorpSquareLogoUrl(params.getCorp_square_logo_url());
        result.setCorpRoundLogoUrl(params.getCorp_round_logo_url());
        result.setCorpUserMax(params.getCorp_user_max());
        result.setCorpAgentMax(params.getCorp_agent_max());
        result.setCorpFullName(params.getCorp_full_name());
        result.setVerifiedEndTime(params.getVerified_end_time());
        result.setSubjectType(params.getSubject_type());
        result.setCorpWxQrcode(params.getCorp_wxqrcode());
        result.setCorpScale(params.getCorp_scale());
        result.setCorpIndustry(params.getCorp_industry());
        result.setCorpSubIndustry(params.getCorp_sub_industry());
        result.setLocation(params.getLocation());
        return result;
    }

    private me.chanjar.weixin.cp.bean.WxCpTpPermanentCodeInfo.AuthInfo convertAuthInfo(
            com.scrm.server.wx.cp.feign.dto.AuthInfo params) {
        me.chanjar.weixin.cp.bean.WxCpTpPermanentCodeInfo.AuthInfo result =
                new me.chanjar.weixin.cp.bean.WxCpTpPermanentCodeInfo.AuthInfo();
        if (params.getAgent() != null) {
            java.util.List<me.chanjar.weixin.cp.bean.WxCpTpPermanentCodeInfo.Agent> agents =
                    params.getAgent().stream().map(e -> {
                        me.chanjar.weixin.cp.bean.WxCpTpPermanentCodeInfo.Agent agent =
                                new me.chanjar.weixin.cp.bean.WxCpTpPermanentCodeInfo.Agent();
                        agent.setAgentId(e.getAgentid());
                        agent.setName(e.getName());
                        agent.setRoundLogoUrl(e.getRound_logo_url());
                        agent.setSquareLogoUrl(e.getSquare_logo_url());
                        agent.setAppid(e.getEdition_id());
                        agent.setAuthMode(e.getAuth_mode());
                        agent.setIsCustomizedApp(e.getIs_customized_app());
                        if (e.getPrivilege() != null) {
                            agent.setPrivilege(convertPrivilege(e.getPrivilege()));
                        }
                        return agent;
                    }).collect(java.util.stream.Collectors.toList());
            result.setAgents(agents);
        }
        return result;
    }

    private me.chanjar.weixin.cp.bean.WxCpTpPermanentCodeInfo.Privilege convertPrivilege(
            Privilege params) {
        me.chanjar.weixin.cp.bean.WxCpTpPermanentCodeInfo.Privilege result =
                new me.chanjar.weixin.cp.bean.WxCpTpPermanentCodeInfo.Privilege();
        result.setLevel(params.getLevel());
        result.setAllowParties(params.getAllow_party());
        result.setAllowUsers(params.getAllow_user());
        result.setAllowTags(params.getAllow_tag());
        result.setExtraParties(params.getExtra_party());
        result.setExtraUsers(params.getExtra_user());
        result.setExtraTags(params.getExtra_tag());
        return result;
    }
}
