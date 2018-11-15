package com.huawei.nb.coordinator.helper.verify;

import android.content.Context;
import com.huawei.nb.coordinator.helper.http.HttpRequest.Builder;

public class VerifyNone implements IVerify {
    public boolean generateAuthorization(Context context, Builder builder, String appID) {
        return true;
    }

    public String verifyTokenHeader() {
        return null;
    }
}
