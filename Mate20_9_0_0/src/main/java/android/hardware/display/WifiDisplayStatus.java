package android.hardware.display;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.Arrays;

public final class WifiDisplayStatus implements Parcelable {
    public static final Creator<WifiDisplayStatus> CREATOR = new Creator<WifiDisplayStatus>() {
        public WifiDisplayStatus createFromParcel(Parcel in) {
            int featureState = in.readInt();
            int scanState = in.readInt();
            int activeDisplayState = in.readInt();
            WifiDisplay activeDisplay = null;
            if (in.readInt() != 0) {
                activeDisplay = (WifiDisplay) WifiDisplay.CREATOR.createFromParcel(in);
            }
            WifiDisplay activeDisplay2 = activeDisplay;
            WifiDisplay[] displays = (WifiDisplay[]) WifiDisplay.CREATOR.newArray(in.readInt());
            for (int i = 0; i < displays.length; i++) {
                displays[i] = (WifiDisplay) WifiDisplay.CREATOR.createFromParcel(in);
            }
            return new WifiDisplayStatus(featureState, scanState, activeDisplayState, activeDisplay2, displays, (WifiDisplaySessionInfo) WifiDisplaySessionInfo.CREATOR.createFromParcel(in));
        }

        public WifiDisplayStatus[] newArray(int size) {
            return new WifiDisplayStatus[size];
        }
    };
    public static final int DISPLAY_CONNECTION_STATUS_BUSY = 2;
    public static final int DISPLAY_CONNECTION_STATUS_DEVICE_NOT_FOUND = 5;
    public static final int DISPLAY_CONNECTION_STATUS_GET_ADDRESS_FAILED = 3;
    public static final int DISPLAY_CONNECTION_STATUS_INIT = -1;
    public static final int DISPLAY_CONNECTION_STATUS_INTERNAL_ERROR = 0;
    public static final int DISPLAY_CONNECTION_STATUS_LOST_RTSP_CONNECTION = 4;
    public static final int DISPLAY_CONNECTION_STATUS_P2P_UNSUPPORTED = 1;
    public static final int DISPLAY_CONNECTION_STATUS_RTSP_TIMEOUT = 7;
    public static final int DISPLAY_CONNECTION_STATUS_TIMEOUT = 6;
    public static final int DISPLAY_STATE_CONNECTED = 2;
    public static final int DISPLAY_STATE_CONNECTING = 1;
    public static final int DISPLAY_STATE_NOT_CONNECTED = 0;
    public static final int FEATURE_STATE_DISABLED = 1;
    public static final int FEATURE_STATE_OFF = 2;
    public static final int FEATURE_STATE_ON = 3;
    public static final int FEATURE_STATE_UNAVAILABLE = 0;
    public static final int SCAN_STATE_NOT_SCANNING = 0;
    public static final int SCAN_STATE_SCANNING = 1;
    private final WifiDisplay mActiveDisplay;
    private final int mActiveDisplayState;
    private final WifiDisplay[] mDisplays;
    private final int mFeatureState;
    private final int mScanState;
    private final WifiDisplaySessionInfo mSessionInfo;

    public WifiDisplayStatus() {
        this(0, 0, 0, null, WifiDisplay.EMPTY_ARRAY, null);
    }

    public WifiDisplayStatus(int featureState, int scanState, int activeDisplayState, WifiDisplay activeDisplay, WifiDisplay[] displays, WifiDisplaySessionInfo sessionInfo) {
        if (displays != null) {
            this.mFeatureState = featureState;
            this.mScanState = scanState;
            this.mActiveDisplayState = activeDisplayState;
            this.mActiveDisplay = activeDisplay;
            this.mDisplays = displays;
            this.mSessionInfo = sessionInfo != null ? sessionInfo : new WifiDisplaySessionInfo();
            return;
        }
        throw new IllegalArgumentException("displays must not be null");
    }

    public int getFeatureState() {
        return this.mFeatureState;
    }

    public int getScanState() {
        return this.mScanState;
    }

    public int getActiveDisplayState() {
        return this.mActiveDisplayState;
    }

    public WifiDisplay getActiveDisplay() {
        return this.mActiveDisplay;
    }

    public WifiDisplay[] getDisplays() {
        return this.mDisplays;
    }

    public WifiDisplaySessionInfo getSessionInfo() {
        return this.mSessionInfo;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mFeatureState);
        dest.writeInt(this.mScanState);
        dest.writeInt(this.mActiveDisplayState);
        int i = 0;
        if (this.mActiveDisplay != null) {
            dest.writeInt(1);
            this.mActiveDisplay.writeToParcel(dest, flags);
        } else {
            dest.writeInt(0);
        }
        dest.writeInt(this.mDisplays.length);
        WifiDisplay[] wifiDisplayArr = this.mDisplays;
        int length = wifiDisplayArr.length;
        while (i < length) {
            wifiDisplayArr[i].writeToParcel(dest, flags);
            i++;
        }
        this.mSessionInfo.writeToParcel(dest, flags);
    }

    public int describeContents() {
        return 0;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WifiDisplayStatus{featureState=");
        stringBuilder.append(this.mFeatureState);
        stringBuilder.append(", scanState=");
        stringBuilder.append(this.mScanState);
        stringBuilder.append(", activeDisplayState=");
        stringBuilder.append(this.mActiveDisplayState);
        stringBuilder.append(", activeDisplay=");
        stringBuilder.append(this.mActiveDisplay);
        stringBuilder.append(", displays=");
        stringBuilder.append(Arrays.toString(this.mDisplays));
        stringBuilder.append(", sessionInfo=");
        stringBuilder.append(this.mSessionInfo);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
