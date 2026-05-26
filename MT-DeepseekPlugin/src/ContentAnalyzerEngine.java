import android.content.SharedPreferences;
import okhttp3.*;
import bin.mt.plugin.api.translation.BaseTranslationEngine;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ContentAnalyzerEngine extends BaseTranslationEngine {
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient client;

    private static final Map<String, String> MODEL_DISPLAY_NAMES = new HashMap<>();
    static {
        MODEL_DISPLAY_NAMES.put("deepseek-v4-flash", "deepseek-v4-flash");
        MODEL_DISPLAY_NAMES.put("deepseek-v4-pro", "deepseek-v4-pro");
    }

    public ContentAnalyzerEngine() {
        super(new ConfigurationBuilder()
                .setAcceptTranslated(true)
                .build());
        client = new OkHttpClient.Builder()
                .connectTimeout(50, TimeUnit.SECONDS)
                .readTimeout(50, TimeUnit.SECONDS)
                .writeTimeout(50, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String name() {
        return "AI代码分析";
    }

    @Override
    public List<String> loadSourceLanguages() {
        return new ArrayList<>(MODEL_DISPLAY_NAMES.keySet());
    }

    @Override
    public List<String> loadTargetLanguages(String sourceLanguage) {
        return Arrays.asList("analysis", "translate", "ai");
    }

    @Override
    public String getLanguageDisplayName(String language) {
        if (language.equals("analysis")) return "内容分析";
        if (language.equals("translate")) return "翻译内容";
        if (language.equals("ai")) return "智能AI";
        String displayName = MODEL_DISPLAY_NAMES.get(language);
        return displayName != null ? displayName : "未知";
    }

    @Override
    public String translate(String text, String sourceLanguage, String targetLanguage) {
        if (!targetLanguage.equals("analysis") && !targetLanguage.equals("translate") && !targetLanguage.equals("ai")) {
            return "不支持的目标语言";
        }
        try {
            SharedPreferences pref = getContext().getPreferences();
            String api_token = pref.getString("api_token", "");
            if (api_token.isEmpty()) {
                return "请先在设置中填写 API Keys";
            }
            String model = sourceLanguage;
            String systemPrompt = getSystemPrompt(targetLanguage, pref);
            return makeApiRequest(api_token, systemPrompt, text, model);
        } catch (Exception e) {
            return "处理失败：" + e.getMessage();
        }
    }

    // 提示词
    private String getSystemPrompt(String targetLanguage, SharedPreferences pref) {
        switch (targetLanguage) {
            case "translate":
                return pref.getString("prompt_translate", "下面的内容如若为非中文请翻译成中文，如若为中文请翻译成英文。");
            case "analysis":
                return pref.getString("prompt_analysis", "详细解释这段代码的功能与逻辑，用中文回答。");
            case "ai":
                return pref.getString("prompt_ai", "请用中文回答用户。");
            default:
                return "";
        }
    }

    private String makeApiRequest(String apiToken, String systemPrompt, String userContent, String model)
            throws JSONException, IOException {
        int maxRetries = 2;
        int retryCount = 0;
        while (retryCount < maxRetries) {
            try {
                JSONObject requestBody = buildRequestBody(systemPrompt, userContent, model);
                RequestBody body = RequestBody.create(JSON, requestBody.toString());
                Request request = new Request.Builder()
                        .url(API_URL)
                        .post(body)
                        .header("Authorization", "Bearer " + apiToken)
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("API请求失败，状态码：" + response.code());
                    }
                    ResponseBody body1 = response.body();
                    if (body1 == null) return "无响应";
                    String content = body1.string();
                    return parseResponse(content);
                }
            } catch (IOException e) {
                retryCount++;
                if (retryCount >= maxRetries) throw e;
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
            }
        }
        return "请求失败";
    }

    private JSONObject buildRequestBody(String systemPrompt, String userContent, String model) throws JSONException {
        JSONObject requestBody = new JSONObject();
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
        messages.put(new JSONObject().put("role", "user").put("content", userContent));
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 3900);
        requestBody.put("temperature", 0.9);
        requestBody.put("top_p", 0.9);
        return requestBody;
    }

    private String parseResponse(String jsonResponse) throws JSONException {
        JSONObject obj = new JSONObject(jsonResponse);
        JSONArray choices = obj.getJSONArray("choices");
        if (choices.length() > 0) {
            return choices.getJSONObject(0).getJSONObject("message").getString("content");
        }
        return "未获取到有效回答";
    }
}