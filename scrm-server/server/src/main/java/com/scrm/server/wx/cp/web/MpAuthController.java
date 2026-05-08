package com.scrm.server.wx.cp.web;

import com.scrm.api.wx.cp.entity.WxCustomer;
import com.scrm.common.config.ScrmConfig;
import com.scrm.common.constant.R;
import com.scrm.common.util.JwtUtil;
import com.scrm.server.wx.cp.service.IMpAuthService;
import com.scrm.server.wx.cp.service.IWxCustomerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 微信公众号授权接口（多租户改造：corpId 从 JWT Token 动态获取）
 *
 * @author qiuzl
 * @date Created in 2022/3/17 14:32
 */
@RestController
@RequestMapping("/mp/auth")
@Api(tags = {"微信公众号授权接口"})
@Valid
public class MpAuthController {

    @Autowired
    private IMpAuthService mpAuthService;

    @Autowired
    private IWxCustomerService customerService;

    /**
     * 获取当前请求的 corpId。SaaS 模式从 JWT 获取，Private 模式使用全局配置。
     */
    private String resolveCorpId() {
        try {
            String corpId = JwtUtil.getExtCorpId();
            if (StringUtils.isNotBlank(corpId)) {
                return corpId;
            }
        } catch (Exception e) {
            // JWT 解析失败，回退到全局配置
        }
        return ScrmConfig.getExtCorpID();
    }

    @GetMapping("/getUnionIdByCode")
    @ApiOperation("根据授权code查询unionId")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "授权code", name = "code", required = true)
    })
    public R<String> getUnionIdByCode(@RequestParam String code) {
        return R.data(mpAuthService.getUnionIdByCode(code, resolveCorpId()));
    }


    @GetMapping("/getAppIdIdByCorpId")
    @ApiOperation("根据授权CorpId查询AppIdId")
    public R<String> getAppIdIdByCorpId() {
        return R.data(mpAuthService.getAppIdIdByCorpId(resolveCorpId()));
    }

    @GetMapping("/getCustomerByCode")
    @ApiOperation("根据授权code查询客户信息")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "授权code", name = "code", required = true)
    })
    public R<WxCustomer> getCustomerByCode(@RequestParam String code) {
        return R.data(mpAuthService.getCustomerByCode(code, resolveCorpId()));
    }
}
