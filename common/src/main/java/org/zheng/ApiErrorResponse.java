package org.zheng;

import org.zheng.enums.ApiError;

public record ApiErrorResponse (ApiError error, String data, String message)  {
}
