package ml.docilealligator.infinityforreddit.translation;

import android.util.LruCache;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * 翻译结果内存缓存
 * 使用 LruCache 存储翻译结果，避免重复翻译同一内容浪费 token
 */
@Singleton
public class TranslationCache {

    // 最大缓存条目数（约 1000-2000 KB 内存）
    private static final int MAX_ENTRIES = 500;

    private final LruCache<String, String> cache;

    @Inject
    public TranslationCache() {
        this.cache = new LruCache<>(MAX_ENTRIES);
    }

    /**
     * 获取缓存的翻译结果
     * @param originalText 原文
     * @return 翻译结果，未命中返回 null
     */
    public String get(String originalText) {
        if (originalText == null || originalText.isEmpty()) {
            return null;
        }
        String key = generateCacheKey(originalText);
        return cache.get(key);
    }

    /**
     * 存入翻译结果
     * @param originalText 原文
     * @param translatedText 翻译结果
     */
    public void put(String originalText, String translatedText) {
        if (originalText == null || originalText.isEmpty() ||
            translatedText == null || translatedText.isEmpty()) {
            return;
        }
        String key = generateCacheKey(originalText);
        cache.put(key, translatedText);
    }

    /**
     * 生成缓存键（使用 MD5 哈希）
     * @param text 原文
     * @return MD5 哈希值作为缓存键
     */
    private String generateCacheKey(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // MD5 应该总是可用的，回退到 hashCode
            return String.valueOf(text.hashCode());
        }
    }

    /**
     * 清空缓存
     */
    public void clear() {
        cache.evictAll();
    }

    /**
     * 获取当前缓存大小
     * @return 缓存条目数
     */
    public int size() {
        return cache.size();
    }
}
