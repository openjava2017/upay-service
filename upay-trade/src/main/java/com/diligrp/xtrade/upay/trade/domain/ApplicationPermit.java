package com.diligrp.xtrade.upay.trade.domain;

public class ApplicationPermit {
    // 应用ID
    private Long appId;
    // 授权Token
    private String accessToken;
    // 安全密钥-数据签名验签的公钥
    private String secretKey;
    // 商户信息
    private MerchantPermit merchant;

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
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

    public MerchantPermit getMerchant() {
        return merchant;
    }

    public void setMerchant(MerchantPermit merchant) {
        this.merchant = merchant;
    }

    public static ApplicationPermit of(Long appId, String accessToken, String secretKey, MerchantPermit merchant) {
        ApplicationPermit permit = new ApplicationPermit();
        permit.setAppId(appId);
        permit.setAccessToken(accessToken);
        permit.setSecretKey(secretKey);
        permit.setMerchant(merchant);

        return permit;
    }
}
