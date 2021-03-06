package com.android.server.power;

import android.os.Bundle;
import android.os.HwBrightnessProcessor;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import com.android.server.display.AutomaticBrightnessController;
import com.android.server.display.CryogenicPowerProcessor;
import com.android.server.display.ManualBrightnessController;
import huawei.android.aod.HwAodManager;
import java.util.ArrayList;

public class HwDisplayPowerController {
    private static final boolean DEBUG;
    private static final int MAX_RETRY_COUNT = 4;
    private static final int MIN_LUX_VALUE = 3;
    private static final String TAG = "HwDisplayPowerController";
    private static boolean mCoverClose = false;
    private static final ArrayMap<String, HwBrightnessProcessor> mHwBrightnessProcessors = new ArrayMap();
    private static int mSensorCount = 4;

    private static final class PersonalizedBrightnessCurveLevelProcessor extends HwBrightnessProcessor {
        private AutomaticBrightnessController mAutomaticBrightnessController;

        public PersonalizedBrightnessCurveLevelProcessor(AutomaticBrightnessController controller) {
            this.mAutomaticBrightnessController = controller;
        }

        public boolean setData(Bundle data, int[] retValue) {
            if (this.mAutomaticBrightnessController == null) {
                retValue[0] = -1;
                return true;
            }
            this.mAutomaticBrightnessController.setPersonalizedBrightnessCurveLevel(data.getInt("TopApkLevel", 0));
            retValue[0] = 0;
            return true;
        }
    }

    private static final class PersonalizedBrightnessProcessor extends HwBrightnessProcessor {
        private final boolean DEBUG;
        private String TAG = "PersonalizedBrightnessProcessor";
        private AutomaticBrightnessController mAutomaticBrightnessController;

        public PersonalizedBrightnessProcessor(AutomaticBrightnessController controller) {
            boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(this.TAG, 4));
            this.DEBUG = z;
            this.mAutomaticBrightnessController = controller;
        }

        public boolean setData(Bundle data, int[] retValue) {
            if (this.mAutomaticBrightnessController == null) {
                return false;
            }
            int isCurveUpdate = data.getInt("CurveUpdateFlag", 0);
            if (this.DEBUG) {
                String str = this.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("isCurveUpdate = ");
                stringBuilder.append(isCurveUpdate);
                Slog.i(str, stringBuilder.toString());
            }
            if (isCurveUpdate == 1) {
                this.mAutomaticBrightnessController.updateNewBrightnessCurveTmp();
            }
            retValue[0] = 0;
            return true;
        }

        public boolean getData(Bundle data, int[] retValue) {
            if (this.mAutomaticBrightnessController == null) {
                return false;
            }
            data.putParcelableArrayList("DefaultCurve", (ArrayList) this.mAutomaticBrightnessController.getCurrentDefaultNewCurveLine());
            if (this.DEBUG) {
                Slog.i(this.TAG, "defaultCurve ");
            }
            retValue[0] = 0;
            return true;
        }
    }

    private static final class SceneRecognitionProcessor extends HwBrightnessProcessor {
        private AutomaticBrightnessController mAutomaticBrightnessController;

        public SceneRecognitionProcessor(AutomaticBrightnessController controller) {
            this.mAutomaticBrightnessController = controller;
        }

        public boolean getData(Bundle data, int[] retValue) {
            if (this.mAutomaticBrightnessController == null) {
                retValue[0] = -1;
                return true;
            }
            this.mAutomaticBrightnessController.getUserDragInfo(data);
            retValue[0] = 0;
            return true;
        }
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        DEBUG = z;
    }

    public static void setIfCoverClosed(boolean isClosed) {
        mCoverClose = isClosed;
        mSensorCount = 0;
    }

    public static boolean isCoverClosed() {
        return mCoverClose;
    }

    public static boolean shouldFilteInvalidSensorVal(float lux) {
        if (mCoverClose) {
            return true;
        }
        if (4 <= mSensorCount || lux >= 3.0f) {
            mSensorCount = 4;
            return false;
        }
        mSensorCount++;
        return true;
    }

    public static void setPowerState(int powerState) {
        HwAodManager.getInstance().setPowerState(powerState);
    }

    public static boolean hwBrightnessSetData(String name, Bundle data, int[] result) {
        HwBrightnessProcessor processor = (HwBrightnessProcessor) mHwBrightnessProcessors.get(name);
        if (processor != null) {
            return processor.setData(data, result);
        }
        return false;
    }

    public static boolean hwBrightnessGetData(String name, Bundle data, int[] result) {
        HwBrightnessProcessor processor = (HwBrightnessProcessor) mHwBrightnessProcessors.get(name);
        if (processor != null) {
            return processor.getData(data, result);
        }
        return false;
    }

    public static void loadHwBrightnessProcessors(AutomaticBrightnessController autoController, ManualBrightnessController manualController) {
        mHwBrightnessProcessors.put("Cryogenic", new CryogenicPowerProcessor(autoController));
        mHwBrightnessProcessors.put("SceneRecognition", new SceneRecognitionProcessor(autoController));
        mHwBrightnessProcessors.put("PersonalizedBrightnessCurveLevel", new PersonalizedBrightnessCurveLevelProcessor(autoController));
        mHwBrightnessProcessors.put("PersonalizedBrightness", new PersonalizedBrightnessProcessor(autoController));
    }
}
