package com.sdu.paybackend.entity;

import lombok.Data;

@Data
public class AliPayDTO {

    // 订单编号
    private String traceNo;

    // 付款金额，必填
    private double totalAmount;

    // 订单名称，必填
    private String subject;

    // 商品描述，可空
    private String body;

    private String alipayTraceNo;
}


