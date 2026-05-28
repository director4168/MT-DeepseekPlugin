/*
 * (C) 2026 director_Carter All Rights Reserved.
 * 开源地址：https://github.com/director4168/MT-DeepseekPlugin
 * 本版本为二次修改版本，相比较于原版添加更多功能
 * 开源地址中提供原始版本
 * 原插件已被下架，且原作者已未知，现在由『director_Carter』进行维护
 * 邮箱： 2705722903@qq.com
*/
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
    // API接口
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    // 余额接口
    private static final String BALANCE_API = "https://api.deepseek.com/user/balance";
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
        return Arrays.asList("ai", "analysis", "translate");
    }

    @Override
    public String getLanguageDisplayName(String language) {
        if (language.equals("ai")) return "智能AI";
        if (language.equals("analysis")) return "代码分析";
        if (language.equals("translate")) return "翻译内容";
        String displayName = MODEL_DISPLAY_NAMES.get(language);
        return displayName != null ? displayName : "未知";
    }

    @Override
    public String translate(String text, String sourceLanguage, String targetLanguage) {
        if (!targetLanguage.equals("ai") && !targetLanguage.equals("analysis") && !targetLanguage.equals("translate")) {
            return "不支持的目标语言";
        }
        try {
            SharedPreferences pref = getContext().getPreferences();
            String api_token = pref.getString("api_token", "");
            if (api_token.isEmpty()) {
                return "请先在「设置-基础设置-API Key」中输入您的deepseek API Key";
            }
            String model = sourceLanguage;
            String systemPrompt = getSystemPrompt(targetLanguage, pref);

            // 获取结构
            JSONObject fullResp = makeApiRequestWithUsage(api_token, systemPrompt, text, model);
            String result = fullResp.optString("content", "未获取到有效回答");

            // 显示token消耗量
            boolean showToken = pref.getBoolean("show_token_usage", false);
            if (showToken) {
                String tokenInfo = buildTokenInfo(fullResp.optJSONObject("usage"));
                if (tokenInfo != null && !tokenInfo.isEmpty()) {
                    result += "\n\n" + tokenInfo;
                }
            }

            // 显示余额
            boolean showBalance = pref.getBoolean("show_balance", false);
            if (showBalance) {
                String balanceInfo = getBalanceString(api_token);
                if (balanceInfo != null && !balanceInfo.isEmpty()) {
                    result += "\n\n" + balanceInfo;
                }
            }

            return result;
        } catch (Exception e) {
            return "处理失败：" + e.getMessage();
        }
    }

    private String getSystemPrompt(String targetLanguage, SharedPreferences pref) {
        switch (targetLanguage) {
            case "ai":
                return pref.getString("prompt_ai", "根据用户的要求回答，并请使用中文回答用户，除非用户指定使用其他语言。");
            case "analysis":
                return pref.getString("prompt_analysis", "详细解释这段代码的功能与逻辑。并请使用中文回答，除非用户指定使用其他语言。");
            case "translate":
                return pref.getString("prompt_translate", "下面的内容如若为非中文请翻译成中文，如若为中文请翻译成英文。");
            default:
                return "";
        }
    }

    // 返回的请求
    private JSONObject makeApiRequestWithUsage(String apiToken, String systemPrompt, String userContent, String model)
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
                    if (!response.isSuccessful() || response.body() == null) {
                        throw new IOException("API请求失败，错误码：" + response.code());
                    }
                    String json = response.body().string();
                    JSONObject resp = new JSONObject(json);

                    JSONObject result = new JSONObject();
                    result.put("content", parseResponse(resp));
                    result.put("usage", resp.optJSONObject("usage"));
                    return result;
                }
            } catch (IOException e) {
                retryCount++;
                if (retryCount >= maxRetries) throw e;
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException ignored) {
                }
            }
        }
        JSONObject err = new JSONObject();
        err.put("content", "请求失败");
        return err;
    }

    private JSONObject buildRequestBody(String systemPrompt, String userContent, String model)
            throws JSONException {
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

    private String parseResponse(JSONObject obj) throws JSONException {
        JSONArray choices = obj.optJSONArray("choices");
        if (choices == null || choices.length() == 0) return "未获取到有效回答";
        return choices.getJSONObject(0).getJSONObject("message").optString("content", "");
    }

    private String buildTokenInfo(JSONObject usage) {
        if (usage == null) return "获取token消耗量失败";

        int prompt = usage.optInt("prompt_tokens", 0);
        int out = usage.optInt("completion_tokens", 0);
        int total = usage.optInt("total_tokens", 0);
        int cached = 0;
        JSONObject details = usage.optJSONObject("prompt_tokens_details");
        if (details != null) {
            cached = details.optInt("cached_tokens", 0);
        }
        int noCache = prompt - cached;

        return "Token消耗量: 输入命中：" + cached + " | 输入未命中：" + noCache + " | 输出：" + out + " | 本次共消耗：" + total;
    }

    // 获取余额
    private String getBalanceString(String apiToken) {
        try {
            Request request = new Request.Builder()
                    .url(BALANCE_API)
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Accept", "application/json")
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return "余额：获取失败(" + response.code() + ")o_O";
                }
                return parseBalance(response.body().string());
            }
        } catch (Exception e) {
            return null;
        }
    }

    // 解析余额
    private String parseBalance(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray balanceInfos = obj.getJSONArray("balance_infos");
            if (balanceInfos.length() == 0) return "余额：没看到数据啊o_O";
            JSONObject info = balanceInfos.getJSONObject(0);
            String total = info.getString("total_balance");
            String currency = info.getString("currency");
            return "账户余额：" + total + " " + currency;
        } catch (JSONException e) {
            return "余额：获取成功但是解析失败o_O";
        }
    }
}