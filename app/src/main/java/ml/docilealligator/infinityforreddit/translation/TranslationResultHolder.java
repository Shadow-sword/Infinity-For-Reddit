package ml.docilealligator.infinityforreddit.translation;

import java.util.ArrayList;

import ml.docilealligator.infinityforreddit.comment.Comment;

/**
 * 静态持有者，用于在 Activity 之间传递大量翻译数据
 * 避免 Intent 的 1MB TransactionTooLargeException 限制
 */
public class TranslationResultHolder {
    private static BatchTranslationResult sResult;
    private static ArrayList<Comment> sComments;

    public static void set(BatchTranslationResult result, ArrayList<Comment> comments) {
        sResult = result;
        sComments = comments;
    }

    public static BatchTranslationResult getResult() {
        return sResult;
    }

    public static ArrayList<Comment> getComments() {
        return sComments;
    }

    public static void clear() {
        sResult = null;
        sComments = null;
    }
}
