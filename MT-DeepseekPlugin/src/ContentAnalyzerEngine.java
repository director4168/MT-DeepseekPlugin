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
    private static final String BALANCE_API = "https://api.deepseek.com/user/balance";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int TIMEOUT_SECONDS = 50;
    private static final int MAX_RETRIES = 2;
    private static final int RETRY_DELAY_MS = 1500;
    private static final Set<
            String> SUPPORTED_TARGETS = new HashSet<>(Arrays.asList("ai", "analysis", "translate"));
    private static final Set<
            String> SUPPORTED_MODELS = new HashSet<>(Arrays.asList("deepseek-v4-flash", "deepseek-v4-pro"));

    private static final String MEMORY_KEY = "memory_history";

    private OkHttpClient client;

    public ContentAnalyzerEngine() {
        super(new ConfigurationBuilder().setAcceptTranslated(true).build());
        client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String name() {
        return "Deepseek AI";
    }

    @Override
    public List<String> loadSourceLanguages() {
        return new ArrayList<>(SUPPORTED_MODELS);
    }

    @Override
    public List<String> loadTargetLanguages(String sourceLanguage) {
        return Arrays.asList("ai", "analysis", "translate");
    }

    @Override
    public String getLanguageDisplayName(String language) {
        switch (language) {
            case "ai":
                return "智能AI";
            case "analysis":
                return "代码分析";
            case "translate":
                return "翻译内容";
            default:
                return SUPPORTED_MODELS.contains(language) ? language : "未知";
        }
    }

    @Override
    public String translate(String text, String sourceLanguage, String targetLanguage) {
        SharedPreferences pref = null;
        try {
            pref = getContext().getPreferences();
        } catch (Exception e) {
            getContext().log("获取Preferences失败", e);
        }

        getContext().log("开始: 模式ID: " + targetLanguage + ", 长度: " + text.length());

        if (!SUPPORTED_TARGETS.contains(targetLanguage)) {
            return "不支持的目标语言";
        }
        try {
            if (pref == null) {
                pref = getContext().getPreferences();
            }
            String api_token = pref.getString("api_token", "");
            if (api_token.isEmpty()) {
                log(pref, "未配置API Key无法使用");
                return "请先在「设置-基础设置-API Key」中输入您的deepseek API Key";
            }
            String model = sourceLanguage;
            String systemPrompt = getSystemPrompt(targetLanguage, pref);

            boolean enableMemory = pref.getBoolean("enable_memory", true);
            if (enableMemory) {
                String memory = loadMemory(pref);
                if (!memory.isEmpty()) {
                    systemPrompt += "\n\n以下是之前的对话记录，请将这些对话记录识别为“记忆”，请参考：\n" + memory;
                    log(pref, "已加载记忆，共" + countMemoryRounds(pref) + "轮");
                }
            }

            String tempStr = pref.getString("temperature", "0.5");
            String topPStr = pref.getString("top_p", "0.8");
            double temperature, topP;
            try {
                temperature = Double.parseDouble(tempStr);
                if (temperature < 0 || temperature > 2) temperature = 0.5;
            } catch (NumberFormatException e) {
                temperature = 0.5;
            }
            try {
                topP = Double.parseDouble(topPStr);
                if (topP < 0.1 || topP > 1) topP = 0.8;
            } catch (NumberFormatException e) {
                topP = 0.8;
            }

            log(pref, "请求参数: 模型=" + model + ", temperature=" + temperature + ", top_p=" + topP);

            JSONObject fullResp = makeApiRequestWithUsage(pref, api_token, systemPrompt, text, model, temperature, topP);
            String result = fullResp.optString("content", "未获取到有效回答");

            log(pref, "API响应成功，长度=" + result.length());

            if (enableMemory) {
                saveMemory(pref, text, result);
                log(pref, "已保存本轮记忆");
            }

            boolean showToken = pref.getBoolean("show_token_usage", false);
            if (showToken) {
                String tokenInfo = buildTokenInfo(pref, fullResp.optJSONObject("usage"));
                if (tokenInfo != null && !tokenInfo.isEmpty()) {
                    result += "\n\n" + tokenInfo;
                }
            }
            boolean showBalance = pref.getBoolean("show_balance", false);
            if (showBalance) {
                String balanceInfo = getBalanceString(pref, api_token);
                if (balanceInfo != null && !balanceInfo.isEmpty()) {
                    result += "\n\n" + balanceInfo;
                }
            }
            return result;
        } catch (Exception e) {
            log(pref, "发生异常", e);
            return "处理失败：" + e.getMessage();
        }
    }

    private void log(SharedPreferences pref, String msg) {
        if (pref != null && pref.getBoolean("log_recording", false)) {
            getContext().log(msg);
        }
    }

    private void log(SharedPreferences pref, String msg, Throwable e) {
        if (pref != null && pref.getBoolean("log_recording", false)) {
            getContext().log(msg, e);
        }
    }

    private String loadMemory(SharedPreferences pref) {
        String history = pref.getString(MEMORY_KEY, "");
        if (history.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        try {
            JSONArray arr = new JSONArray(history);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject item = arr.getJSONObject(i);
                sb.append("第").append(i + 1).append("轮：");
                sb.append("用户提问：").append(item.optString("ask", "")).append("\n");
                sb.append("AI回答：").append(item.optString("answer", "")).append("\n\n");
            }
        } catch (JSONException e) {
            return "";
        }
        return sb.toString().trim();
    }

    private int countMemoryRounds(SharedPreferences pref) {
        try {
            String history = pref.getString(MEMORY_KEY, "[]");
            return new JSONArray(history).length();
        } catch (JSONException e) {
            return 0;
        }
    }

    private void saveMemory(SharedPreferences pref, String ask, String answer) {
        try {
            int maxRounds = getMemoryRounds(pref);
            String history = pref.getString(MEMORY_KEY, "[]");
            JSONArray arr = new JSONArray(history);

            while (arr.length() >= maxRounds) {
                JSONArray newArr = new JSONArray();
                for (int i = 1; i < arr.length(); i++) {
                    newArr.put(arr.get(i));
                }
                arr = newArr;
            }

            JSONObject item = new JSONObject();
            item.put("ask", truncateText(ask, 200));
            item.put("answer", truncateText(answer, 500));
            arr.put(item);

            pref.edit().putString(MEMORY_KEY, arr.toString()).apply();
        } catch (JSONException e) {
        }
    }

    private int getMemoryRounds(SharedPreferences pref) {
        try {
            String val = pref.getString("memory_rounds", "5");
            int rounds = Integer.parseInt(val);
            if (rounds < 1) return 5;
            if (rounds > 999) return 999;
            return rounds;
        } catch (NumberFormatException e) {
            return 5;
        }
    }

    private String truncateText(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...";
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

    private JSONObject makeApiRequestWithUsage(SharedPreferences pref, String apiToken, String systemPrompt, String userContent, String model, double temperature, double topP)
            throws JSONException, IOException {
        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                JSONObject requestBody = buildRequestBody(systemPrompt, userContent, model, temperature, topP);
                RequestBody body = RequestBody.create(JSON, requestBody.toString());
                Request request = new Request.Builder()
                        .url(API_URL)
                        .post(body)
                        .header("Authorization", "Bearer " + apiToken)
                        .build();

                log(pref, "请求API (第" + (retryCount + 1) + "次)");

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        String errMsg = "API请求失败，错误码：" + response.code();
                        log(pref, errMsg + "，响应：" + (response.body() != null ? response.body().string() : "无响应体"));
                        getContext().showToast(errMsg);
                        throw new IOException(errMsg);
                    }
                    String json = response.body().string();
                    JSONObject resp = new JSONObject(json);

                    if (resp.has("error")) {
                        JSONObject errorObj = resp.getJSONObject("error");
                        String errorMsg = errorObj.optString("message", "未知错误");
                        String errorType = errorObj.optString("type", "");
                        log(pref, "API返回错误: type=" + errorType + ", message=" + errorMsg);
                        throw new IOException("API错误: " + errorMsg);
                    }

                    JSONObject result = new JSONObject();
                    result.put("content", parseResponse(pref, resp));
                    result.put("usage", resp.optJSONObject("usage"));
                    log(pref, "API请求成功");
                    return result;
                }
            } catch (IOException e) {
                retryCount++;
                log(pref, "请求失败(第" + retryCount + "次): " + e.getMessage());
                if (retryCount >= MAX_RETRIES) {
                    log(pref, "已达最大重试次数o_O，放弃请求。director_Carter在此提醒，请检查API Key是否正确，账户余额是否充足等", e);
                    getContext().showToast("请求失败，错误：" + e.getMessage());
                    throw e;
                }
                log(pref, "等待" + RETRY_DELAY_MS + "ms后重试...");
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ignored) {
                }
            }
        }
        JSONObject err = new JSONObject();
        err.put("content", "请求失败");
        return err;
    }

    private JSONObject buildRequestBody(String systemPrompt, String userContent, String model, double temperature, double topP)
            throws JSONException {
        JSONObject requestBody = new JSONObject();
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
        messages.put(new JSONObject().put("role", "user").put("content", userContent));
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 16000);
        requestBody.put("temperature", temperature);
        requestBody.put("top_p", topP);
        return requestBody;
    }

    private String parseResponse(SharedPreferences pref, JSONObject obj) throws JSONException {
        JSONArray choices = obj.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            log(pref, "API响应中没有choices字段");
            return "未获取到有效回答";
        }
        String content = choices.getJSONObject(0).getJSONObject("message").optString("content", "");
        if (content.isEmpty()) {
            log(pref, "API返回的content为空");
        }
        return content;
    }

    private String buildTokenInfo(SharedPreferences pref, JSONObject usage) {
        if (usage == null) {
            log(pref, "usage为null，无法获取token消耗量");
            return "获取token消耗量失败";
        }
        int prompt = usage.optInt("prompt_tokens", 0);
        int out = usage.optInt("completion_tokens", 0);
        int total = usage.optInt("total_tokens", 0);
        int cached = 0;
        JSONObject details = usage.optJSONObject("prompt_tokens_details");
        if (details != null) {
            cached = details.optInt("cached_tokens", 0);
        }
        int noCache = prompt - cached;
        log(pref, "Token消耗量: 输入命中：" + cached + " | 输入未命中：" + noCache + " | 输出：" + out + " | 本次共消耗：" + total);
        return "Token消耗量: 输入命中：" + cached + " | 输入未命中：" + noCache + " | 输出：" + out + " | 本次共消耗：" + total;
    }

    private String getBalanceString(SharedPreferences pref, String apiToken) {
        try {
            Request request = new Request.Builder()
                    .url(BALANCE_API)
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Accept", "application/json")
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    String errMsg = "余额：获取失败(" + response.code() + ")o_O";
                    log(pref, "获取余额失败，错误码:" + response.code());
                    return errMsg;
                }
                return parseBalance(pref, response.body().string());
            }
        } catch (Exception e) {
            log(pref, "获取余额异常", e);
            return "余额：获取失败";
        }
    }

    private String parseBalance(SharedPreferences pref, String json) {
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray balanceInfos = obj.getJSONArray("balance_infos");
            if (balanceInfos.length() == 0) {
                log(pref, "余额接口返回的balance_infos为空");
                return "余额：没看到数据啊o_O";
            }
            JSONObject info = balanceInfos.getJSONObject(0);
            String total = info.getString("total_balance");
            String currency = info.getString("currency");
            log(pref, "当前余额: " + total + " " + currency);
            return "账户余额：" + total + " " + currency;
        } catch (JSONException e) {
            log(pref, "解析余额数据失败", e);
            return "余额：获取成功但是解析失败o_O";
        }
    }
}