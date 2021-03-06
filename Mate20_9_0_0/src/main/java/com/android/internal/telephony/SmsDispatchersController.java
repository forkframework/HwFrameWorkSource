package com.android.internal.telephony;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telephony.Rlog;
import android.telephony.SmsMessage;
import android.telephony.SmsMessage.MessageClass;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.SMSDispatcher.SmsTracker;
import com.android.internal.telephony.SmsMessageBase.SubmitPduBase;
import com.android.internal.telephony.cdma.CdmaInboundSmsHandler;
import com.android.internal.telephony.cdma.CdmaSMSDispatcher;
import com.android.internal.telephony.gsm.GsmInboundSmsHandler;
import java.util.ArrayList;
import java.util.HashMap;

public class SmsDispatchersController extends Handler {
    private static final int EVENT_IMS_STATE_CHANGED = 12;
    private static final int EVENT_IMS_STATE_DONE = 13;
    private static final int EVENT_RADIO_ON = 11;
    private static final String TAG = "SmsDispatchersController";
    private SMSDispatcher mCdmaDispatcher;
    private CdmaInboundSmsHandler mCdmaInboundSmsHandler;
    private final CommandsInterface mCi;
    private final Context mContext;
    private SMSDispatcher mGsmDispatcher;
    private GsmInboundSmsHandler mGsmInboundSmsHandler;
    private boolean mIms = false;
    private ImsSmsDispatcher mImsSmsDispatcher;
    private String mImsSmsFormat = "unknown";
    private Phone mPhone;
    private final SmsUsageMonitor mUsageMonitor;

    public interface SmsInjectionCallback {
        void onSmsInjectedResult(int i);
    }

    public SmsDispatchersController(Phone phone, SmsStorageMonitor storageMonitor, SmsUsageMonitor usageMonitor) {
        Rlog.d(TAG, "SmsDispatchersController created");
        this.mContext = phone.getContext();
        this.mUsageMonitor = usageMonitor;
        this.mCi = phone.mCi;
        this.mPhone = phone;
        this.mImsSmsDispatcher = new ImsSmsDispatcher(phone, this);
        this.mCdmaDispatcher = HwTelephonyFactory.getHwInnerSmsManager().createHwCdmaSMSDispatcher(phone, this);
        this.mGsmInboundSmsHandler = GsmInboundSmsHandler.makeInboundSmsHandler(phone.getContext(), storageMonitor, phone);
        this.mCdmaInboundSmsHandler = CdmaInboundSmsHandler.makeInboundSmsHandler(phone.getContext(), storageMonitor, phone, (CdmaSMSDispatcher) this.mCdmaDispatcher);
        this.mGsmDispatcher = HwTelephonyFactory.getHwInnerSmsManager().createHwGsmSMSDispatcher(phone, this, this.mGsmInboundSmsHandler);
        SmsBroadcastUndelivered.initialize(phone.getContext(), this.mGsmInboundSmsHandler, this.mCdmaInboundSmsHandler, phone.getSubId());
        InboundSmsHandler.registerNewMessageNotificationActionHandler(phone.getContext());
        this.mCi.registerForOn(this, 11, null);
        this.mCi.registerForImsNetworkStateChanged(this, 12, null);
    }

    protected void updatePhoneObject(Phone phone) {
        Rlog.d(TAG, "In IMS updatePhoneObject ");
        this.mCdmaDispatcher.updatePhoneObject(phone);
        this.mGsmDispatcher.updatePhoneObject(phone);
        this.mGsmInboundSmsHandler.updatePhoneObject(phone);
        this.mCdmaInboundSmsHandler.updatePhoneObject(phone);
    }

    public void dispose() {
        this.mCi.unregisterForOn(this);
        this.mCi.unregisterForImsNetworkStateChanged(this);
        this.mGsmDispatcher.dispose();
        this.mCdmaDispatcher.dispose();
        this.mGsmInboundSmsHandler.dispose();
        this.mCdmaInboundSmsHandler.dispose();
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 11:
            case 12:
                this.mCi.getImsRegistrationState(obtainMessage(13));
                return;
            case 13:
                AsyncResult ar = msg.obj;
                if (ar.exception == null) {
                    updateImsInfo(ar);
                    return;
                }
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("IMS State query failed with exp ");
                stringBuilder.append(ar.exception);
                Rlog.e(str, stringBuilder.toString());
                return;
            default:
                if (isCdmaMo()) {
                    this.mCdmaDispatcher.handleMessage(msg);
                    return;
                } else {
                    this.mGsmDispatcher.handleMessage(msg);
                    return;
                }
        }
    }

    private void setImsSmsFormat(int format) {
        switch (format) {
            case 1:
                this.mImsSmsFormat = "3gpp";
                return;
            case 2:
                this.mImsSmsFormat = "3gpp2";
                return;
            default:
                this.mImsSmsFormat = "unknown";
                return;
        }
    }

    private void updateImsInfo(AsyncResult ar) {
        int[] responseArray = ar.result;
        boolean z = true;
        setImsSmsFormat(responseArray[1]);
        if (responseArray[0] != 1 || "unknown".equals(this.mImsSmsFormat)) {
            z = false;
        }
        this.mIms = z;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("IMS registration state: ");
        stringBuilder.append(this.mIms);
        stringBuilder.append(" format: ");
        stringBuilder.append(this.mImsSmsFormat);
        Rlog.d(str, stringBuilder.toString());
    }

    @VisibleForTesting
    public void injectSmsPdu(byte[] pdu, String format, SmsInjectionCallback callback) {
        injectSmsPdu(SmsMessage.createFromPdu(pdu, format), format, callback, false);
    }

    @VisibleForTesting
    public void injectSmsPdu(SmsMessage msg, String format, SmsInjectionCallback callback, boolean ignoreClass) {
        Exception e;
        Rlog.d(TAG, "SmsDispatchersController:injectSmsPdu");
        if (msg == null) {
            try {
                Rlog.e(TAG, "injectSmsPdu: createFromPdu returned null");
                callback.onSmsInjectedResult(2);
            } catch (Exception e2) {
                Rlog.e(TAG, "injectSmsPdu failed: ", e2);
                callback.onSmsInjectedResult(2);
            }
        } else if (ignoreClass || msg.getMessageClass() == MessageClass.CLASS_1) {
            e2 = new AsyncResult(callback, msg, null);
            String str;
            StringBuilder stringBuilder;
            if (format.equals("3gpp")) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("SmsDispatchersController:injectSmsText Sending msg=");
                stringBuilder.append(msg);
                stringBuilder.append(", format=");
                stringBuilder.append(format);
                stringBuilder.append("to mGsmInboundSmsHandler");
                Rlog.i(str, stringBuilder.toString());
                this.mGsmInboundSmsHandler.sendMessage(8, e2);
            } else if (format.equals("3gpp2")) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("SmsDispatchersController:injectSmsText Sending msg=");
                stringBuilder.append(msg);
                stringBuilder.append(", format=");
                stringBuilder.append(format);
                stringBuilder.append("to mCdmaInboundSmsHandler");
                Rlog.i(str, stringBuilder.toString());
                this.mCdmaInboundSmsHandler.sendMessage(8, e2);
            } else {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Invalid pdu format: ");
                stringBuilder2.append(format);
                Rlog.e(str, stringBuilder2.toString());
                callback.onSmsInjectedResult(2);
            }
        } else {
            Rlog.e(TAG, "injectSmsPdu: not class 1");
            callback.onSmsInjectedResult(2);
        }
    }

    public void sendRetrySms(SmsTracker tracker) {
        String oldFormat = tracker.mFormat;
        String newFormat = (2 == this.mPhone.getPhoneType() ? this.mCdmaDispatcher : this.mGsmDispatcher).getFormat();
        if (!oldFormat.equals(newFormat)) {
            HashMap map = tracker.getData();
            boolean pdu = false;
            if (map.containsKey("scAddr") && map.containsKey("destAddr") && (map.containsKey("text") || (map.containsKey("data") && map.containsKey("destPort")))) {
                String scAddr = (String) map.get("scAddr");
                String destAddr = (String) map.get("destAddr");
                SubmitPduBase pdu2 = null;
                if (map.containsKey("text")) {
                    SubmitPduBase pdu3;
                    Rlog.d(TAG, "sms failed was text");
                    String text = (String) map.get("text");
                    if (isCdmaFormat(newFormat)) {
                        Rlog.d(TAG, "old format (gsm) ==> new format (cdma)");
                        if (tracker.mDeliveryIntent != null) {
                            pdu = true;
                        }
                        pdu3 = com.android.internal.telephony.cdma.SmsMessage.getSubmitPdu(scAddr, destAddr, text, pdu, null);
                    } else {
                        Rlog.d(TAG, "old format (cdma) ==> new format (gsm)");
                        if (tracker.mDeliveryIntent != null) {
                            pdu3 = 1;
                        }
                        pdu3 = com.android.internal.telephony.gsm.SmsMessage.getSubmitPdu(scAddr, destAddr, text, pdu3, null);
                    }
                    pdu2 = pdu3;
                } else if (map.containsKey("data")) {
                    Rlog.d(TAG, "sms failed was data");
                    byte[] data = (byte[]) map.get("data");
                    Integer destPort = (Integer) map.get("destPort");
                    int intValue;
                    if (isCdmaFormat(newFormat)) {
                        Rlog.d(TAG, "old format (gsm) ==> new format (cdma)");
                        intValue = destPort.intValue();
                        if (tracker.mDeliveryIntent != null) {
                            pdu = true;
                        }
                        pdu2 = com.android.internal.telephony.cdma.SmsMessage.getSubmitPdu(scAddr, destAddr, intValue, data, pdu);
                    } else {
                        Rlog.d(TAG, "old format (cdma) ==> new format (gsm)");
                        intValue = destPort.intValue();
                        if (tracker.mDeliveryIntent != null) {
                            pdu = true;
                        }
                        pdu2 = com.android.internal.telephony.gsm.SmsMessage.getSubmitPdu(scAddr, destAddr, intValue, data, pdu);
                    }
                }
                map.put("smsc", pdu2.encodedScAddress);
                map.put("pdu", pdu2.encodedMessage);
                SMSDispatcher dispatcher = isCdmaFormat(newFormat) ? this.mCdmaDispatcher : this.mGsmDispatcher;
                tracker.mFormat = dispatcher.getFormat();
                dispatcher.sendSms(tracker);
                return;
            }
            Rlog.e(TAG, "sendRetrySms failed to re-encode per missing fields!");
            tracker.onFailed(this.mContext, 1, 0);
        } else if (isCdmaFormat(newFormat)) {
            Rlog.d(TAG, "old format matched new format (cdma)");
            this.mCdmaDispatcher.sendSms(tracker);
        } else {
            Rlog.d(TAG, "old format matched new format (gsm)");
            this.mGsmDispatcher.sendSms(tracker);
        }
    }

    public boolean isIms() {
        return this.mIms;
    }

    public String getImsSmsFormat() {
        return this.mImsSmsFormat;
    }

    protected boolean isCdmaMo() {
        if (isIms()) {
            return isCdmaFormat(this.mImsSmsFormat);
        }
        return 2 == this.mPhone.getPhoneType();
    }

    public boolean isCdmaFormat(String format) {
        return this.mCdmaDispatcher.getFormat().equals(format);
    }

    protected void sendData(String destAddr, String scAddr, int destPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        if (this.mImsSmsDispatcher.isAvailable()) {
            this.mImsSmsDispatcher.sendData(destAddr, scAddr, destPort, data, sentIntent, deliveryIntent);
        } else if (isCdmaMo()) {
            this.mCdmaDispatcher.sendData(destAddr, scAddr, destPort, data, sentIntent, deliveryIntent);
        } else {
            this.mGsmDispatcher.sendData(destAddr, scAddr, destPort, data, sentIntent, deliveryIntent);
        }
    }

    public void sendText(String destAddr, String scAddr, String text, PendingIntent sentIntent, PendingIntent deliveryIntent, Uri messageUri, String callingPkg, boolean persistMessage, int priority, boolean expectMore, int validityPeriod) {
        if (this.mImsSmsDispatcher.isAvailable()) {
            this.mImsSmsDispatcher.sendText(destAddr, scAddr, text, sentIntent, deliveryIntent, messageUri, callingPkg, persistMessage, -1, false, -1);
        } else if (isCdmaMo()) {
            this.mCdmaDispatcher.sendText(destAddr, scAddr, text, sentIntent, deliveryIntent, messageUri, callingPkg, persistMessage, priority, expectMore, validityPeriod);
        } else {
            this.mGsmDispatcher.sendText(destAddr, scAddr, text, sentIntent, deliveryIntent, messageUri, callingPkg, persistMessage, priority, expectMore, validityPeriod);
        }
    }

    protected void sendMultipartText(String destAddr, String scAddr, ArrayList<String> parts, ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents, Uri messageUri, String callingPkg, boolean persistMessage, int priority, boolean expectMore, int validityPeriod) {
        if (this.mImsSmsDispatcher.isAvailable()) {
            this.mImsSmsDispatcher.sendMultipartText(destAddr, scAddr, parts, sentIntents, deliveryIntents, messageUri, callingPkg, persistMessage, -1, false, -1);
        } else if (isCdmaMo()) {
            this.mCdmaDispatcher.sendMultipartText(destAddr, scAddr, parts, sentIntents, deliveryIntents, messageUri, callingPkg, persistMessage, priority, expectMore, validityPeriod);
        } else {
            this.mGsmDispatcher.sendMultipartText(destAddr, scAddr, parts, sentIntents, deliveryIntents, messageUri, callingPkg, persistMessage, priority, expectMore, validityPeriod);
        }
    }

    public int getPremiumSmsPermission(String packageName) {
        return this.mUsageMonitor.getPremiumSmsPermission(packageName);
    }

    public void setPremiumSmsPermission(String packageName, int permission) {
        this.mUsageMonitor.setPremiumSmsPermission(packageName, permission);
    }

    public SmsUsageMonitor getUsageMonitor() {
        return this.mUsageMonitor;
    }

    public Pair<Boolean, Boolean> handleSmsStatusReport(SmsTracker tracker, String format, byte[] pdu) {
        if (isCdmaFormat(format)) {
            return handleCdmaStatusReport(tracker, format, pdu);
        }
        return handleGsmStatusReport(tracker, format, pdu);
    }

    private Pair<Boolean, Boolean> handleCdmaStatusReport(SmsTracker tracker, String format, byte[] pdu) {
        tracker.updateSentMessageStatus(this.mContext, 0);
        return new Pair(Boolean.valueOf(triggerDeliveryIntent(tracker, format, pdu)), Boolean.valueOf(true));
    }

    private Pair<Boolean, Boolean> handleGsmStatusReport(SmsTracker tracker, String format, byte[] pdu) {
        com.android.internal.telephony.gsm.SmsMessage sms = com.android.internal.telephony.gsm.SmsMessage.newFromCDS(pdu);
        boolean complete = false;
        boolean success = false;
        if (sms != null) {
            int tpStatus = sms.getStatus();
            if (tpStatus >= 64 || tpStatus < 32) {
                tracker.updateSentMessageStatus(this.mContext, tpStatus);
                complete = true;
            }
            success = triggerDeliveryIntent(tracker, format, pdu);
        }
        return new Pair(Boolean.valueOf(success), Boolean.valueOf(complete));
    }

    private boolean triggerDeliveryIntent(SmsTracker tracker, String format, byte[] pdu) {
        PendingIntent intent = tracker.mDeliveryIntent;
        Intent fillIn = new Intent();
        fillIn.putExtra("pdu", pdu);
        fillIn.putExtra("format", format);
        try {
            intent.send(this.mContext, -1, fillIn);
            return true;
        } catch (CanceledException e) {
            return false;
        }
    }
}
