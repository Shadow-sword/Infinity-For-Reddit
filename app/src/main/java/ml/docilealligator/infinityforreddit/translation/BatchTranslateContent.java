package ml.docilealligator.infinityforreddit.translation;

import android.os.Handler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import ml.docilealligator.infinityforreddit.apis.VolcanoEngineAPI;
import ml.docilealligator.infinityforreddit.comment.Comment;
import ml.docilealligator.infinityforreddit.post.Post;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * 批量翻译帖子和评论的核心逻辑
 */
public class BatchTranslateContent {

    // 字符数上限，超过此值则截断评论
    private static final int MAX_CHAR_LIMIT = 80000;

    private static final String BATCH_TRANSLATION_PROMPT =
        "你是一个翻译器。将以下内容翻译为简体中文。\n" +
        "规则：\n" +
        "1. 仅返回有效 JSON，不要输出其他文字\n" +
        "2. 保留专有名词（人名、地名）原文\n" +
        "3. 保留 markdown 格式\n" +
        "4. 翻译风格自然流畅\n\n" +
        "输入 JSON 包含帖子（title + body）和评论（id -> text）。\n" +
        "请按如下格式返回 JSON：\n" +
        "{\"post_title\":\"译文\",\"post_body\":\"译文\",\"comments\":{\"id\":\"译文\",...}}\n\n" +
        "以下是需要翻译的内容：\n";

    public interface BatchTranslateListener {
        void onTranslateSuccess(BatchTranslationResult result);
        void onTranslateFailed(String errorMessage);
    }

    /**
     * 带缓存的批量翻译
     */
    public static void translateBatchWithCache(TranslationCache cache,
                                                Executor executor, Handler handler, Retrofit retrofit,
                                                String apiKey, String model,
                                                Post post, ArrayList<Comment> comments,
                                                BatchTranslateListener listener) {
        String cacheKey = buildCacheKey(post, comments);
        String cached = cache.get(cacheKey);
        if (cached != null) {
            try {
                BatchTranslationResult result = parseTranslationJson(cached, post);
                handler.post(() -> listener.onTranslateSuccess(result));
                return;
            } catch (Exception e) {
                // 缓存数据解析失败，重新翻译
            }
        }

        translateBatch(executor, handler, retrofit, apiKey, model, post, comments, new BatchTranslateListener() {
            @Override
            public void onTranslateSuccess(BatchTranslationResult result) {
                // 构建缓存值
                try {
                    JSONObject cacheJson = new JSONObject();
                    cacheJson.put("post_title", result.getTranslatedTitle());
                    cacheJson.put("post_body", result.getTranslatedBody() != null ? result.getTranslatedBody() : "");
                    JSONObject commentsJson = new JSONObject();
                    for (Map.Entry<String, String> entry : result.getTranslatedComments().entrySet()) {
                        commentsJson.put(entry.getKey(), entry.getValue());
                    }
                    cacheJson.put("comments", commentsJson);
                    cache.put(cacheKey, cacheJson.toString());
                } catch (Exception ignored) {
                }
                listener.onTranslateSuccess(result);
            }

            @Override
            public void onTranslateFailed(String errorMessage) {
                listener.onTranslateFailed(errorMessage);
            }
        });
    }

    /**
     * 执行批量翻译
     */
    public static void translateBatch(Executor executor, Handler handler, Retrofit retrofit,
                                       String apiKey, String model,
                                       Post post, ArrayList<Comment> comments,
                                       BatchTranslateListener listener) {
        executor.execute(() -> {
            try {
                // 构建输入 JSON
                String inputJson = buildInputJson(post, comments);

                // 批量翻译内容较多，使用更长的超时时间（10 分钟）
                OkHttpClient longTimeoutClient = retrofit.callFactory() instanceof OkHttpClient
                        ? ((OkHttpClient) retrofit.callFactory()).newBuilder()
                            .connectTimeout(60, TimeUnit.SECONDS)
                            .readTimeout(10, TimeUnit.MINUTES)
                            .writeTimeout(60, TimeUnit.SECONDS)
                            .build()
                        : null;

                Retrofit batchRetrofit = longTimeoutClient != null
                        ? retrofit.newBuilder().client(longTimeoutClient).build()
                        : retrofit;

                VolcanoEngineAPI api = batchRetrofit.create(VolcanoEngineAPI.class);

                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Bearer " + apiKey);

                Map<String, Object> body = new HashMap<>();
                body.put("model", model);
                body.put("stream", false);

                List<Map<String, Object>> inputList = new ArrayList<>();
                Map<String, Object> userMessage = new HashMap<>();
                userMessage.put("role", "user");

                List<Map<String, Object>> contentList = new ArrayList<>();
                Map<String, Object> textContent = new HashMap<>();
                textContent.put("type", "input_text");
                textContent.put("text", BATCH_TRANSLATION_PROMPT + inputJson);
                contentList.add(textContent);

                userMessage.put("content", contentList);
                inputList.add(userMessage);
                body.put("input", inputList);

                Call<String> call = api.translate(headers, body);
                Response<String> response = call.execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body();
                    BatchTranslationResult result = parseResponse(responseBody, post);

                    if (result != null) {
                        handler.post(() -> listener.onTranslateSuccess(result));
                    } else {
                        handler.post(() -> listener.onTranslateFailed("无法解析翻译响应"));
                    }
                } else {
                    String errorMsg = "翻译失败: " + response.code();
                    if (response.errorBody() != null) {
                        errorMsg += " - " + response.errorBody().string();
                    }
                    final String finalErrorMsg = errorMsg;
                    handler.post(() -> listener.onTranslateFailed(finalErrorMsg));
                }
            } catch (Exception e) {
                handler.post(() -> listener.onTranslateFailed("错误: " + e.getMessage()));
            }
        });
    }

    /**
     * 构建输入 JSON
     */
    static String buildInputJson(Post post, ArrayList<Comment> comments) {
        try {
            JSONObject input = new JSONObject();
            input.put("post_title", post.getTitle() != null ? post.getTitle() : "");
            input.put("post_body", post.getSelfTextPlain() != null ? post.getSelfTextPlain() : "");

            JSONObject commentsJson = new JSONObject();
            int totalChars = (post.getTitle() != null ? post.getTitle().length() : 0)
                    + (post.getSelfTextPlain() != null ? post.getSelfTextPlain().length() : 0);

            for (Comment comment : comments) {
                if (comment.getPlaceholderType() != Comment.NOT_PLACEHOLDER) {
                    continue;
                }
                String text = comment.getCommentRawText();
                if (text == null || text.isEmpty()) {
                    continue;
                }
                totalChars += text.length();
                if (totalChars > MAX_CHAR_LIMIT) {
                    break;
                }
                commentsJson.put(comment.getId(), text);
            }

            input.put("comments", commentsJson);
            return input.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * 解析 API 响应
     */
    static BatchTranslationResult parseResponse(String responseBody, Post post) {
        try {
            JSONObject jsonResponse = new JSONObject(responseBody);

            String text = null;

            // ARK API 格式: output[0].content[0].text
            if (jsonResponse.has("output")) {
                JSONArray output = jsonResponse.getJSONArray("output");
                if (output.length() > 0) {
                    JSONObject firstOutput = output.getJSONObject(0);
                    if (firstOutput.has("content")) {
                        JSONArray content = firstOutput.getJSONArray("content");
                        if (content.length() > 0) {
                            JSONObject firstContent = content.getJSONObject(0);
                            if (firstContent.has("text")) {
                                text = firstContent.getString("text");
                            }
                        }
                    }
                }
            }

            // 备用格式: choices[0].message.content
            if (text == null && jsonResponse.has("choices")) {
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices.length() > 0) {
                    JSONObject firstChoice = choices.getJSONObject(0);
                    if (firstChoice.has("message")) {
                        JSONObject message = firstChoice.getJSONObject("message");
                        if (message.has("content")) {
                            text = message.getString("content");
                        }
                    }
                }
            }

            if (text == null) {
                return null;
            }

            // 从文本中提取 JSON（处理 LLM 可能添加的 markdown 代码块标记）
            String jsonStr = extractJsonFromText(text);
            return parseTranslationJson(jsonStr, post);

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 LLM 返回的文本中提取 JSON 字符串
     */
    private static String extractJsonFromText(String text) {
        if (text == null) return "{}";

        // 移除 markdown 代码块标记
        String trimmed = text.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        trimmed = trimmed.trim();

        // 查找第一个 { 到最后一个 }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    /**
     * 解析翻译 JSON 为 BatchTranslationResult
     */
    private static BatchTranslationResult parseTranslationJson(String jsonStr, Post post) {
        try {
            JSONObject json = new JSONObject(jsonStr);

            String translatedTitle = json.optString("post_title", "");
            String translatedBody = json.optString("post_body", "");

            HashMap<String, String> translatedComments = new HashMap<>();
            if (json.has("comments")) {
                JSONObject commentsObj = json.getJSONObject("comments");
                Iterator<String> keys = commentsObj.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    translatedComments.put(key, commentsObj.getString(key));
                }
            }

            return new BatchTranslationResult(
                    post.getTitle(),
                    translatedTitle,
                    post.getSelfTextPlain(),
                    translatedBody.isEmpty() ? null : translatedBody,
                    translatedComments
            );
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 构建缓存 key
     */
    private static String buildCacheKey(Post post, ArrayList<Comment> comments) {
        StringBuilder sb = new StringBuilder();
        sb.append(post.getId()).append("|");
        for (Comment comment : comments) {
            if (comment.getPlaceholderType() == Comment.NOT_PLACEHOLDER && comment.getId() != null) {
                sb.append(comment.getId()).append(",");
            }
        }
        return md5(sb.toString());
    }

    private static String md5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return "batch_" + sb.toString();
        } catch (Exception e) {
            return "batch_" + text.hashCode();
        }
    }

    /**
     * 返回被截断的评论数量
     */
    public static int getTruncatedCount(Post post, ArrayList<Comment> comments) {
        int totalChars = (post.getTitle() != null ? post.getTitle().length() : 0)
                + (post.getSelfTextPlain() != null ? post.getSelfTextPlain().length() : 0);
        int includedCount = 0;
        int totalValidCount = 0;

        for (Comment comment : comments) {
            if (comment.getPlaceholderType() != Comment.NOT_PLACEHOLDER) {
                continue;
            }
            String text = comment.getCommentRawText();
            if (text == null || text.isEmpty()) {
                continue;
            }
            totalValidCount++;
            totalChars += text.length();
            if (totalChars <= MAX_CHAR_LIMIT) {
                includedCount++;
            }
        }

        return totalValidCount > includedCount ? includedCount : -1;
    }
}
