package org.auctionfx.demodatabase.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;

// https://currencylayer.com/documentation
// Api trả về User sẽ loại bỏ kết quả là Null
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    // Cần một code để User có thể biết code để lookup xem code lỗi đó như nào
    private int code = 1000;
    // Thông báo api trả về cho User
    private String message;
    // Kết quả trả về
    private T result;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }
}
