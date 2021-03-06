package huawei.android.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import huawei.android.widget.Attributes.Mode;
import huawei.android.widget.SwipeLayout.OnLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class SwipeItemMangerImpl implements SwipeItemMangerInterface {
    public static final int INVALID_POSITION = -1;
    private List<View> mAnimatedViews = new LinkedList();
    private final Object[] mAnimationLock = new Object[0];
    private int mDismissAnimationRefCount;
    private SwipeDismissCallback mDismissCallback;
    protected int mOpenPosition = -1;
    protected Set<Integer> mOpenPositions = new HashSet();
    private SortedSet<PendingDismissData> mPendingDismisses = new TreeSet();
    protected Set<SwipeLayout> mShownLayouts = new HashSet();
    private Mode mode = Mode.Single;
    protected SwipeAdapterInterface swipeAdapterInterface;

    private static class PendingDismissData implements Comparable<PendingDismissData> {
        public View childView;
        public int position;
        public View view;

        PendingDismissData(int position, View view, View childView) {
            this.position = position;
            this.view = view;
            this.childView = childView;
        }

        public int hashCode() {
            return super.hashCode();
        }

        public boolean equals(Object other) {
            boolean z = false;
            if (!(other instanceof PendingDismissData)) {
                return false;
            }
            if (compareTo((PendingDismissData) other) == 0) {
                z = true;
            }
            return z;
        }

        public int compareTo(PendingDismissData other) {
            return other.position - this.position;
        }
    }

    private static class UpdateListener implements AnimatorUpdateListener {
        private View listItemView;
        private LayoutParams lp;

        /* synthetic */ UpdateListener(LayoutParams x0, View x1, AnonymousClass1 x2) {
            this(x0, x1);
        }

        private UpdateListener(LayoutParams lp, View listItemView) {
            this.lp = lp;
            this.listItemView = listItemView;
        }

        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            if (valueAnimator.getAnimatedValue() instanceof Integer) {
                this.lp.height = ((Integer) valueAnimator.getAnimatedValue()).intValue();
                this.listItemView.setLayoutParams(this.lp);
            }
        }
    }

    public static class ValueBox {
        OnLayoutListener onLayoutListener;
        int position;
        SwipeMemory swipeMemory;

        ValueBox(int position, SwipeMemory swipeMemory, OnLayoutListener onLayoutListener) {
            this.swipeMemory = swipeMemory;
            this.onLayoutListener = onLayoutListener;
            this.position = position;
        }

        public int getPosition() {
            return this.position;
        }
    }

    class OnLayoutListener implements OnLayout {
        private int position;

        OnLayoutListener(int position) {
            this.position = position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public void onLayout(SwipeLayout v) {
            if (SwipeItemMangerImpl.this.isOpen(this.position)) {
                v.open(false, false);
            } else {
                v.close(false, false);
            }
        }
    }

    public class SwipeMemory extends SimpleSwipeListener {
        private int position;

        SwipeMemory(int position) {
            this.position = position;
        }

        public void onClose(SwipeLayout layout) {
            if (SwipeItemMangerImpl.this.mode == Mode.Multiple) {
                SwipeItemMangerImpl.this.mOpenPositions.remove(Integer.valueOf(this.position));
            } else if (this.position == SwipeItemMangerImpl.this.mOpenPosition) {
                SwipeItemMangerImpl.this.mOpenPosition = -1;
            }
        }

        public void onStartOpen(SwipeLayout layout) {
            if (SwipeItemMangerImpl.this.mode == Mode.Single) {
                SwipeItemMangerImpl.this.closeAllExcept(layout);
            }
        }

        public void onOpen(SwipeLayout layout) {
            if (SwipeItemMangerImpl.this.mode == Mode.Multiple) {
                SwipeItemMangerImpl.this.mOpenPositions.add(Integer.valueOf(this.position));
                return;
            }
            SwipeItemMangerImpl.this.closeAllExcept(layout);
            SwipeItemMangerImpl.this.mOpenPosition = this.position;
        }

        public void setPosition(int position) {
            this.position = position;
        }
    }

    static /* synthetic */ int access$306(SwipeItemMangerImpl x0) {
        int i = x0.mDismissAnimationRefCount - 1;
        x0.mDismissAnimationRefCount = i;
        return i;
    }

    public SwipeItemMangerImpl(SwipeAdapterInterface swipeAdapterInterface) {
        if (swipeAdapterInterface != null) {
            this.swipeAdapterInterface = swipeAdapterInterface;
            return;
        }
        throw new IllegalArgumentException("SwipeAdapterInterface can not be null");
    }

    public Mode getMode() {
        return this.mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        this.mOpenPositions.clear();
        this.mShownLayouts.clear();
        this.mOpenPosition = -1;
    }

    public void bind(View view, int position) {
        int resId = this.swipeAdapterInterface.getSwipeLayoutResourceId(position);
        SwipeLayout swipeLayout = (SwipeLayout) view.findViewById(resId);
        if (swipeLayout == null) {
            throw new IllegalStateException("can not find SwipeLayout in target view");
        } else if (swipeLayout.getTag(resId) == null) {
            OnLayoutListener onLayoutListener = new OnLayoutListener(position);
            SwipeMemory swipeMemory = new SwipeMemory(position);
            swipeLayout.addSwipeListener(swipeMemory);
            swipeLayout.addOnLayoutListener(onLayoutListener);
            swipeLayout.setTag(resId, new ValueBox(position, swipeMemory, onLayoutListener));
            this.mShownLayouts.add(swipeLayout);
        } else {
            ValueBox valueBox = (ValueBox) swipeLayout.getTag(resId);
            valueBox.swipeMemory.setPosition(position);
            valueBox.onLayoutListener.setPosition(position);
            valueBox.position = position;
        }
    }

    public void openItem(int position) {
        if (this.mode != Mode.Multiple) {
            this.mOpenPosition = position;
        } else if (!this.mOpenPositions.contains(Integer.valueOf(position))) {
            this.mOpenPositions.add(Integer.valueOf(position));
        }
        this.swipeAdapterInterface.notifyDatasetChanged();
    }

    public void closeItem(int position) {
        if (this.mode == Mode.Multiple) {
            this.mOpenPositions.remove(Integer.valueOf(position));
        } else if (this.mOpenPosition == position) {
            this.mOpenPosition = -1;
        }
        this.swipeAdapterInterface.notifyDatasetChanged();
    }

    public void closeAllExcept(SwipeLayout layout) {
        for (SwipeLayout s : this.mShownLayouts) {
            if (s != layout) {
                s.close();
            }
        }
    }

    public void closeAllItems() {
        if (this.mode == Mode.Multiple) {
            this.mOpenPositions.clear();
        } else {
            this.mOpenPosition = -1;
        }
        for (SwipeLayout s : this.mShownLayouts) {
            s.close();
        }
    }

    public void removeShownLayouts(SwipeLayout layout) {
        this.mShownLayouts.remove(layout);
    }

    public List<Integer> getOpenItems() {
        if (this.mode == Mode.Multiple) {
            return new ArrayList(this.mOpenPositions);
        }
        return Collections.singletonList(Integer.valueOf(this.mOpenPosition));
    }

    public List<SwipeLayout> getOpenLayouts() {
        return new ArrayList(this.mShownLayouts);
    }

    public boolean isOpen(int position) {
        if (this.mode == Mode.Multiple) {
            return this.mOpenPositions.contains(Integer.valueOf(position));
        }
        return this.mOpenPosition == position;
    }

    public void setDismissCallback(SwipeDismissCallback dismissCallback) {
        this.mDismissCallback = dismissCallback;
    }

    public void deleteItem(View childView, int position) {
        if (this.mDismissCallback != null) {
            View surfaceView = null;
            if (childView instanceof SwipeLayout) {
                ((SwipeLayout) childView).close();
                surfaceView = ((SwipeLayout) childView).getSurfaceView();
            }
            if (surfaceView == null) {
                surfaceView = childView;
            }
            slideOutView(surfaceView, childView, position, true);
            return;
        }
        throw new IllegalStateException("SwipeDismissCallback has to be set before deleting items.");
    }

    /* JADX WARNING: Missing block: B:9:0x0019, code skipped:
            r0 = r5.animate();
     */
    /* JADX WARNING: Missing block: B:10:0x001d, code skipped:
            if (r8 == false) goto L_0x0025;
     */
    /* JADX WARNING: Missing block: B:11:0x001f, code skipped:
            r1 = r5.getWidth();
     */
    /* JADX WARNING: Missing block: B:13:0x0025, code skipped:
            r1 = -r5.getWidth();
     */
    /* JADX WARNING: Missing block: B:14:0x002b, code skipped:
            r0.translationX((float) r1).alpha(0.0f).setDuration(200).setListener(new huawei.android.widget.SwipeItemMangerImpl.AnonymousClass1(r4)).start();
     */
    /* JADX WARNING: Missing block: B:15:0x0046, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void slideOutView(final View view, final View childView, final int position, boolean toRightSide) {
        synchronized (this.mAnimationLock) {
            if (this.mAnimatedViews.contains(view)) {
            } else {
                this.mDismissAnimationRefCount++;
                this.mAnimatedViews.add(view);
            }
        }
    }

    private void performDismiss(final View dismissView, View listItemView, int dismissPosition) {
        LayoutParams lp = listItemView.getLayoutParams();
        final int originalLayoutHeight = lp.height;
        ValueAnimator animator = ValueAnimator.ofInt(new int[]{listItemView.getHeight(), 1}).setDuration(200);
        animator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                boolean noAnimationLeft;
                synchronized (SwipeItemMangerImpl.this.mAnimationLock) {
                    SwipeItemMangerImpl.access$306(SwipeItemMangerImpl.this);
                    SwipeItemMangerImpl.this.mAnimatedViews.remove(dismissView);
                    noAnimationLeft = SwipeItemMangerImpl.this.mDismissAnimationRefCount == 0;
                }
                if (noAnimationLeft) {
                    for (PendingDismissData dismiss : SwipeItemMangerImpl.this.mPendingDismisses) {
                        SwipeItemMangerImpl.this.mDismissCallback.onDismiss(dismiss.position);
                    }
                    for (PendingDismissData dismiss2 : SwipeItemMangerImpl.this.mPendingDismisses) {
                        dismiss2.view.setAlpha(1.0f);
                        dismiss2.view.setTranslationX(0.0f);
                        LayoutParams lp = dismiss2.childView.getLayoutParams();
                        lp.height = originalLayoutHeight;
                        dismiss2.childView.setLayoutParams(lp);
                    }
                    SwipeItemMangerImpl.this.mPendingDismisses.clear();
                }
            }
        });
        animator.addUpdateListener(new UpdateListener(lp, listItemView, null));
        this.mPendingDismisses.add(new PendingDismissData(dismissPosition, dismissView, listItemView));
        animator.start();
    }
}
