package com.diligrp.xtrade.upay.channel.exception;

import com.diligrp.xtrade.upay.core.exception.PaymentServiceException;

public class PaymentChannelException extends PaymentServiceException {
    public PaymentChannelException(String message) {
        super(message);
    }

    public PaymentChannelException(int code, String message) {
        super(code, message);
    }

    public PaymentChannelException(String message, Throwable ex) {
        super(message, ex);
    }
}
