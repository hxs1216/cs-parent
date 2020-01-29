package com.rebn.common.errors;

/**
 * Title: 错误信息
 * Description: 错误信息
 * Create Time: 2020/1/27
 *
 * @author hxs
 * Update Time:
 * Updater:
 * Update Comments:
 */
public enum ErrorCode {
    error(-1,""),
    connect_failed(1,"connect DB failed"),
    cannot_get_data_source(2,"无法获取数据源"),
    data_source_has_init(3,"数据源已经初始化"),
    current_tenant_code_not_found(4,"当前租户信息不存在"),
    tenant_code_is_different(5,"租户编号不一致"),
    process_failed(6, "处理失败"),
    return_type_error(7, "租户返回值类型有误"),
    invalid_params(7, "非法参数"),
    ;

    private ErrorCode(int code, String message) {
        this.code = code + offset;
        this.message = message;
    }

    private int code;
    private String message;
    private static final int offset = 50000;

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
