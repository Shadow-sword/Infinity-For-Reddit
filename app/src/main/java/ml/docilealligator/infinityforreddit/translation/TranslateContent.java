package ml.docilealligator.infinityforreddit.translation;

import android.os.Handler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ml.docilealligator.infinityforreddit.apis.VolcanoEngineAPI;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

public class TranslateContent {

    private static final String TRANSLATION_PROMPT =
        "You are a highly skilled translator tasked with translating various types of content from other languages into Chinese. Follow these instructions carefully to complete the translation task:\n\n" +
        "## Input\n\n" +
        "Depending on the input, follow these specific instructions:\n\n" +
        "1. proper nouns (such as names of people and places) do not need to be translated and should retain their original form.\n" +
        "2. proceed directly to the three-step translation process.\n\n" +
        "## Strategy\n\n" +
        "You will follow a three-step translation process:\n" +
        "1. Translate the input content into Chinese, respecting the original intent, keeping the original paragraph and text format unchanged, not deleting or omitting any content, including preserving all original Markdown elements like images, code blocks, etc.\n" +
        "2. Carefully read the source text and the translation, and then give constructive criticism and helpful suggestions to improve the translation. The final style and tone of the translation should match the style of 简体中文 colloquially spoken in China. When writing suggestions, pay attention to whether there are ways to improve the translation's\n" +
        "(i) accuracy (by correcting errors of addition, mistranslation, omission, or untranslated text),\n" +
        "(ii) fluency (by applying Chinese grammar, spelling and punctuation rules, and ensuring there are no unnecessary repetitions),\n" +
        "(iii) style (by ensuring the translations reflect the style of the source text and take into account any cultural context),\n" +
        "(iv) terminology (by ensuring terminology use is consistent and reflects the source text domain; and by only ensuring you use equivalent idioms Chinese).\n" +
        "3. Based on the results of steps 1 and 2, refine and polish the translation\n\n" +
        "## Output\n\n" +
        "For each step of the translation process, output your results within the appropriate XML tags:\n\n" +
        "<step1_initial_translation>\n" +
        "[Insert your initial translation here]\n" +
        "</step1_initial_translation>\n\n" +
        "<step2_reflection>\n" +
        "[Insert your reflection on the translation, write a list of specific, helpful and constructive suggestions for improving the translation. Each suggestion should address one specific part of the translation.]\n" +
        "</step2_reflection>\n\n" +
        "<step3_refined_translation>\n" +
        "[Insert your refined and polished translation here]\n" +
        "</step3_refined_translation>\n\n" +
        "Ensure that your final translation in step 3 accurately reflects the original meaning while sounding natural in Chinese.\n\n" +
        "Now, please translate the following text:\n\n";

    public interface TranslateListener {
        void onTranslateSuccess(String translatedText);
        void onTranslateFailed(String errorMessage);
    }

    /**
     * 带缓存的翻译方法
     * 先检查缓存，命中则直接返回，否则调用 API 并缓存结果
     */
    public static void translateWithCache(TranslationCache cache,
                                          Executor executor, Handler handler, Retrofit retrofit,
                                          String apiKey, String model, String text,
                                          TranslateListener listener) {
        // 检查缓存
        String cached = cache.get(text);
        if (cached != null) {
            handler.post(() -> listener.onTranslateSuccess(cached));
            return;
        }

        // 调用 API 翻译
        translate(executor, handler, retrofit, apiKey, model, text, new TranslateListener() {
            @Override
            public void onTranslateSuccess(String translatedText) {
                // 存入缓存
                cache.put(text, translatedText);
                listener.onTranslateSuccess(translatedText);
            }

            @Override
            public void onTranslateFailed(String errorMessage) {
                listener.onTranslateFailed(errorMessage);
            }
        });
    }

    public static void translate(Executor executor, Handler handler, Retrofit retrofit,
                                  String apiKey, String model, String text,
                                  TranslateListener listener) {
        executor.execute(() -> {
            try {
                VolcanoEngineAPI api = retrofit.create(VolcanoEngineAPI.class);

                // Prepare headers
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Bearer " + apiKey);

                // Prepare request body according to ARK API format
                Map<String, Object> body = new HashMap<>();
                body.put("model", model);
                body.put("stream", false);

                // Build input array
                List<Map<String, Object>> inputList = new ArrayList<>();
                Map<String, Object> userMessage = new HashMap<>();
                userMessage.put("role", "user");

                List<Map<String, Object>> contentList = new ArrayList<>();
                Map<String, Object> textContent = new HashMap<>();
                textContent.put("type", "input_text");
                textContent.put("text", TRANSLATION_PROMPT + text);
                contentList.add(textContent);

                userMessage.put("content", contentList);
                inputList.add(userMessage);
                body.put("input", inputList);

                Call<String> call = api.translate(headers, body);
                Response<String> response = call.execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body();

                    // Parse the response to extract the translated text
                    String translatedText = extractTranslation(responseBody);

                    if (translatedText != null && !translatedText.isEmpty()) {
                        handler.post(() -> listener.onTranslateSuccess(translatedText));
                    } else {
                        handler.post(() -> listener.onTranslateFailed("Could not extract translation from response"));
                    }
                } else {
                    String errorMsg = "Translation failed: " + response.code();
                    if (response.errorBody() != null) {
                        errorMsg += " - " + response.errorBody().string();
                    }
                    final String finalErrorMsg = errorMsg;
                    handler.post(() -> listener.onTranslateFailed(finalErrorMsg));
                }
            } catch (Exception e) {
                handler.post(() -> listener.onTranslateFailed("Error: " + e.getMessage()));
            }
        });
    }

    private static String extractTranslation(String responseBody) {
        try {
            // Parse JSON response
            JSONObject jsonResponse = new JSONObject(responseBody);

            // Try ARK API format: output[0].content[0].text
            if (jsonResponse.has("output")) {
                JSONArray output = jsonResponse.getJSONArray("output");
                if (output.length() > 0) {
                    JSONObject firstOutput = output.getJSONObject(0);
                    if (firstOutput.has("content")) {
                        JSONArray content = firstOutput.getJSONArray("content");
                        if (content.length() > 0) {
                            JSONObject firstContent = content.getJSONObject(0);
                            if (firstContent.has("text")) {
                                String text = firstContent.getString("text");

                                // Try to extract from step3_refined_translation XML tags
                                Pattern pattern = Pattern.compile("<step3_refined_translation>\\s*(.+?)\\s*</step3_refined_translation>",
                                                                Pattern.DOTALL);
                                Matcher matcher = pattern.matcher(text);
                                if (matcher.find()) {
                                    return matcher.group(1).trim();
                                }

                                // If no XML tags, return the full text
                                return text;
                            }
                        }
                    }
                }
            }

            // Try different possible response structures
            if (jsonResponse.has("choices")) {
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices.length() > 0) {
                    JSONObject firstChoice = choices.getJSONObject(0);
                    if (firstChoice.has("message")) {
                        JSONObject message = firstChoice.getJSONObject("message");
                        if (message.has("content")) {
                            String contentText = message.getString("content");
                            // Try to extract from XML tags in content
                            Pattern pattern = Pattern.compile("<step3_refined_translation>\\s*(.+?)\\s*</step3_refined_translation>",
                                                            Pattern.DOTALL);
                            Matcher matcher = pattern.matcher(contentText);
                            if (matcher.find()) {
                                return matcher.group(1).trim();
                            }
                            return contentText;
                        }
                    }
                }
            }

            // Fallback: return the whole response body
            return responseBody;
        } catch (Exception e) {
            return null;
        }
    }
}
