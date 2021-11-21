package com.luna.sweeney.exception.constant;

import com.google.common.collect.ImmutableMap;

import java.util.Locale;
import java.util.Map;

/**
 * 业务ResultCode
 *
 * @author Tony
 */
public interface BizResultCode {
    /** 不支持（的操作系统、环境） */
    int                              NOT_SUPPORT     = 1301;
    String                           MSG_NOT_SUPPORT = "not support";

    Map<Locale, Map<String, String>> TRANSLATION_MAP =
        ImmutableMap.<Locale, Map<String, String>>builder()
            .put(Locale.SIMPLIFIED_CHINESE,
                ImmutableMap.<String, String>builder()
                    .put(MSG_NOT_SUPPORT, "不支持的操作")
                    .build())
            .build();
}
