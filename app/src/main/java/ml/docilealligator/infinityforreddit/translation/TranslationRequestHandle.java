package ml.docilealligator.infinityforreddit.translation;

import retrofit2.Call;

/**
 * 翻译请求的轻量句柄，持有 Call 引用并支持取消操作。
 */
public class TranslationRequestHandle {

    private volatile boolean cancelled = false;
    private volatile Call<?> activeCall;

    /**
     * 绑定 Retrofit Call。若句柄已被取消，则立即取消该 Call。
     */
    public void attachCall(Call<?> call) {
        this.activeCall = call;
        if (cancelled) {
            call.cancel();
        }
    }

    /**
     * 取消翻译请求。若已绑定 Call 则同时取消网络请求。
     */
    public void cancel() {
        cancelled = true;
        Call<?> call = activeCall;
        if (call != null) {
            call.cancel();
        }
    }

    /**
     * 返回是否已取消。
     */
    public boolean isCancelled() {
        return cancelled;
    }
}
