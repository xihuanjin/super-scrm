package com.scrm.server.wx.cp.feign.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 使用授权码获取授权信息 返回的 authorization_info
 * @author qiuzl
 * @date Created in 2022/5/1 15:07
 */
@Data
@Accessors(chain = true)
public class QueryAuthDetailRes {

    private String authorizer_appid;

    private String authorizer_access_token;

    private String authorizer_refresh_token;

    private Integer expires_in;

    private Object func_info;

    @SerializedName("permanent_code")
    private String permanent_code;

    @SerializedName("auth_corp_info")
    private AuthCorpInfo auth_corp_info;

    @SerializedName("auth_info")
    private AuthInfo auth_info;
}
