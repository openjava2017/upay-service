package com.diligrp.xtrade.upay.boss.controller;

import com.diligrp.xtrade.shared.domain.Message;
import com.diligrp.xtrade.shared.domain.MessageEnvelop;
import com.diligrp.xtrade.shared.domain.RequestContext;
import com.diligrp.xtrade.shared.exception.MessageEnvelopException;
import com.diligrp.xtrade.shared.exception.ServiceAccessException;
import com.diligrp.xtrade.shared.sapi.ICallableServiceManager;
import com.diligrp.xtrade.shared.util.AssertUtils;
import com.diligrp.xtrade.shared.util.ObjectUtils;
import com.diligrp.xtrade.upay.boss.util.Constants;
import com.diligrp.xtrade.upay.core.exception.PaymentServiceException;
import com.diligrp.xtrade.upay.core.ErrorCode;
import com.diligrp.xtrade.upay.trade.domain.ApplicationPermit;
import com.diligrp.xtrade.upay.trade.service.IAccessPermitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;

@RestController
@RequestMapping("/payment/spi")
public class PaymentPlatformController {

    private Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Resource
    private ICallableServiceManager callableServiceManager;

    @Resource
    private IAccessPermitService accessPermitService;

    @RequestMapping(value = "/gateway.do")
    public Message<?> gateway(HttpServletRequest request) {
        try {
            String payload = httpBody(request);
            LOG.debug("payment request received, http body: {}", payload);
            AssertUtils.notEmpty(payload, "payment request payload missed");

            RequestContext context = requestContext(request);
            String service = context.getString(Constants.PARAM_SERVICE);
            Long appId = context.getLong(Constants.PARAM_APPID);
            String accessToken = context.getString(Constants.PARAM_ACCESS_TOKEN);
            // 数据签名验签参数 - 默认utf8编码
            String signature = context.getString(Constants.PARAM_SIGNATURE);
            String charset = context.getString(Constants.PARAM_CHARSET);
            MessageEnvelop envelop = MessageEnvelop.of(appId, service, accessToken, payload, signature, charset);

            ApplicationPermit application = checkAccessPermission(context, envelop);
            // 如果应用配置了调用方的公钥信息，则进行数据验签
            if (ObjectUtils.isNotEmpty(application.getSecretKey())) {
                envelop.unpackEnvelop(application.getSecretKey());
            }
            // TODO:如果设置了商户的私钥，则还需对数据进行签名
            return callableServiceManager.callService(context, envelop);
        } catch (IllegalArgumentException iex) {
            LOG.error(iex.getMessage());
            return Message.failure(ErrorCode.ILLEGAL_ARGUMENT_ERROR, iex.getMessage());
        } catch (ServiceAccessException sex) {
            LOG.error("Payment service not available exception", sex);
            return Message.failure(ErrorCode.SERVICE_NOT_AVAILABLE, sex.getMessage());
        } catch (MessageEnvelopException mex) {
            LOG.error("Payment service data exception", mex);
            return Message.failure(ErrorCode.UNAUTHORIZED_ACCESS_ERROR, mex.getMessage());
        } catch (PaymentServiceException pex) {
            LOG.error("Payment service process exception", pex);
            return Message.failure(pex.getCode(), pex.getMessage());
        } catch (Throwable ex) {
            LOG.error("Payment service unknown exception", ex);
            return Message.failure(ErrorCode.SYSTEM_UNKNOWN_ERROR, ex.getMessage());
        }
    }

    private ApplicationPermit checkAccessPermission(RequestContext context, MessageEnvelop envelop) {
        ApplicationPermit application = accessPermitService.loadApplicationPermit(envelop.getAppId());

        // 校验应用访问权限, 暂时不校验商户状态
        if (!ObjectUtils.equals(envelop.getAccessToken(), application.getAccessToken())) {
            throw new ServiceAccessException(ErrorCode.UNAUTHORIZED_ACCESS_ERROR, "未授权的服务访问");
        }
        context.put(ApplicationPermit.class.getName(), application);
        return application;
    }

    private String httpBody(HttpServletRequest request) {
        StringBuilder payload = new StringBuilder();
        try {
            String line;
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                payload.append(line);
            }
        } catch (IOException iex) {
            LOG.error("Failed to extract http body", iex);
        }

        return payload.toString();
    }

    private RequestContext requestContext(HttpServletRequest request) {
        RequestContext context = new RequestContext();
        Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()) {
            String name = headers.nextElement();
            String value = request.getHeader(name);
            context.put(name, value);
        }

        Enumeration<String> params = request.getParameterNames();
        while (params.hasMoreElements()) {
            String name = params.nextElement();
            String value = request.getParameter(name);
            context.put(name, value);
        }
        return context;
    }
}
