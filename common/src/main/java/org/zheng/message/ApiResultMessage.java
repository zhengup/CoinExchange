package org.zheng.message;


import org.zheng.ApiErrorResponse;
import org.zheng.enums.ApiError;
import org.zheng.model.trade.OrderEntity;

public class ApiResultMessage extends AbstractMessage {
    public ApiErrorResponse error;
    public Object result;
    private static ApiErrorResponse CREATE_ORDER_FAILED = new ApiErrorResponse(ApiError.NO_ENOUGH_ASSET,null,"no_enough_asset");
    private static ApiErrorResponse CANCEL_ORDER_FAILED = new ApiErrorResponse(ApiError.ORDER_NOT_FOUND, null, "Order not found..");

    public static ApiResultMessage createOrderFailed(String refId, long ts) {
        ApiResultMessage msg = new ApiResultMessage();
        msg.error = CREATE_ORDER_FAILED;
        msg.refId = refId;
        msg.createTime = ts;
        return msg;
    }
    public static ApiResultMessage cancelOrderFailed(String refId, long ts) {
        ApiResultMessage msg = new ApiResultMessage();
        msg.error = CANCEL_ORDER_FAILED;
        msg.refId = refId;
        msg.createTime = ts;
        return msg;
    }

    public static ApiResultMessage orderSuccess(String refId, OrderEntity order, long ts) {
        ApiResultMessage msg = new ApiResultMessage();
        msg.result = order;
        msg.refId = refId;
        msg.createTime = ts;
        return msg;
    }
}

