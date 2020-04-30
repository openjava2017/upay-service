package com.diligrp.xtrade.upay.trade.domain;

public class Application {
    // 应用ID
    private Long appId;
    // 商户ID
    private Long mchId;
    // 应用名称
    private String name;
    // 授权Token
    private String accessToken;
    // 安全密钥-数据签名验签的公钥
    private String secretKey;
    // 商户信息
    private Merchant merchant;

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    public Long getMchId() {
        return mchId;
    }

    public void setMchId(Long mchId) {
        this.mchId = mchId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }
}
