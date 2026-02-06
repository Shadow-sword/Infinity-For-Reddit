package ml.docilealligator.infinityforreddit.translation;

import java.util.HashMap;

/**
 * 批量翻译结果数据类
 * 包含帖子标题、正文和所有评论的翻译结果
 */
public class BatchTranslationResult {
    private final String originalTitle;
    private final String translatedTitle;
    private final String originalBody;
    private final String translatedBody;
    private final HashMap<String, String> translatedComments;

    public BatchTranslationResult(String originalTitle, String translatedTitle,
                                   String originalBody, String translatedBody,
                                   HashMap<String, String> translatedComments) {
        this.originalTitle = originalTitle;
        this.translatedTitle = translatedTitle;
        this.originalBody = originalBody;
        this.translatedBody = translatedBody;
        this.translatedComments = translatedComments;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public String getTranslatedTitle() {
        return translatedTitle;
    }

    public String getOriginalBody() {
        return originalBody;
    }

    public String getTranslatedBody() {
        return translatedBody;
    }

    public HashMap<String, String> getTranslatedComments() {
        return translatedComments;
    }
}
