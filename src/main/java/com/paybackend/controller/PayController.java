package com.paybackend.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.paybackend.entity.AliPay;
import com.paybackend.entity.AlipayProperties;
import com.paybackend.entity.RefundVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/alipay")
public class PayController {

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private AlipayProperties alipayProperties;

    @Value("${alipay.notifyUrl}")
    private String notifyUrl;

    @Value("${alipay.returnUrl}")
    private String returnUrl;

    @RequestMapping(value = "/pay", method = RequestMethod.GET)
    // http://localhost:9090/alipay/pay?subject=支付&totalAmount=1000
    public String pay(AliPay aliPay) throws AlipayApiException {
        // 随机一个订单id
        Random random = new Random();
        int tradeNo = random.nextInt(1000000);
        // 设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        // 同步回调地址
        alipayRequest.setReturnUrl(returnUrl);
        // 异步回调地址
        alipayRequest.setNotifyUrl(notifyUrl);
        // 订单过期时间
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime plusOneMinute = now.plusMinutes(1L);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String expireTime = plusOneMinute.format(formatter);
        alipayRequest.setBizContent("{\"out_trade_no\":\"" + tradeNo + "\","
                + "\"total_amount\":\"" + aliPay.getTotalAmount() + "\","
                + "\"subject\":\"" + aliPay.getSubject() + "\","
                + "\"body\":\"" + aliPay.getBody() + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\","
                + "\"timeout_express\":\"1m\","                 // 设置订单过期时间为1分钟
                + "\"time_expire\":\"" + expireTime + "\"}");   // 设置订单过期时间
        return alipayClient.pageExecute(alipayRequest).getBody();
    }

    @PostMapping("/notifyUrl")  // 注意这里必须是POST接口
    public String payNotify(HttpServletRequest request) {
        if (request.getParameter("trade_status").equals("TRADE_SUCCESS")) {
            System.out.println("=========支付宝异步回调========");

            Map<String, String> params = new HashMap<>();
            Map<String, String[]> requestParams = request.getParameterMap();
            for (String name : requestParams.keySet()) {
                params.put(name, request.getParameter(name));
            }
            System.out.println("交易名称: " + params.get("subject"));
            System.out.println("交易状态: " + params.get("trade_status"));
            System.out.println("支付宝交易凭证号: " + params.get("trade_no"));
            System.out.println("商户订单号: " + params.get("out_trade_no"));
            System.out.println("交易金额: " + params.get("total_amount"));
            System.out.println("买家在支付宝唯一id: " + params.get("buyer_id"));
            System.out.println("买家付款时间: " + params.get("gmt_payment"));
            System.out.println("买家付款金额: " + params.get("buyer_pay_amount"));
        }
        return "success";
    }

    @RequestMapping("/returnUrl")
    public String returnUrl(@RequestParam Map<String, String> params) {
        System.out.println("同步回调......");

        // TODO:跳转到商户页面
        // 返回订单信息
        return "Return OK";
    }

    @RequestMapping("/refund")
    public String refund(@RequestBody RefundVo refundVo) throws AlipayApiException {
        AlipayTradeRefundRequest alipayRequest = new AlipayTradeRefundRequest();
        alipayRequest.setBizContent("{\"out_trade_no\":\"" + refundVo.getOutTradeNo() + "\","
                + "\"trade_no\":\"\","
                + "\"refund_amount\":\"" + refundVo.getRefundAmount() + "\","
                + "\"refund_reason\":\"" + refundVo.getRefundReason() + "\","
                + "\"out_request_no\":\"\"}");
        AlipayTradeRefundResponse res = alipayClient.execute(alipayRequest);
        return res.isSuccess() ? "OK" : "FAIL";
    }

    /**
     * rsa签名验证
     * @param params 加密参数
     * @return 成功/失败
     * @throws AlipayApiException AlipayApiException
     */
    private Boolean rsaCheckV1(Map<String, String> params) throws AlipayApiException {
        return AlipaySignature.rsaCheckV1(params,
                alipayProperties.getAlipayPublicKey(),
                alipayProperties.getCharset(),
                alipayProperties.getSignType());
    }

}
