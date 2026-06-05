package bin.mt.plugin;

import bin.mt.plugin.api.MTPluginContext;
import bin.mt.plugin.api.preference.PluginPreference;
import android.content.SharedPreferences;

public class Setting implements PluginPreference {
    @Override
    public void onBuild(MTPluginContext context, Builder builder) {
        builder.addHeader("基础设置");
        builder.addInput("API Key", "api_token")
                .summary("输入您的API Key，若没有请前往https://platform.deepseek.com/api_keys创建")
                .defaultValue("")
                .validator(value -> {
                    if (value == null || value.trim().isEmpty()) {
                        return "不可为空";
                    }
                    return null;
                });

        builder.addText("获取API Key")
                .summary("没有DeepSeek API Key？点我直达API Key创建&管理地址！")
                .url("https://platform.deepseek.com/api_keys");


        builder.addSwitch("显示账户余额", "show_balance")
                .summary("开启后，将尝试获取账户剩余余额，并添加到回复内容的末尾\n注：开启后会增加少量耗时和流量消耗！默认关闭")
                .defaultValue(false)
                .summaryOn("已开启账户余额显示，账户剩余余额将添加到回复内容末尾\n如若不需要该功能请关闭\n警告：不保证完全准确！");

        builder.addSwitch("显示Token消耗量", "show_token_usage")
                .summary("开启后，将尝试获取本次token消耗量，消耗量将添加到回复内容末尾\n注：开启后会增加少量耗时和流量消耗！默认关闭")
                .defaultValue(false)
                .summaryOn("已开启Token消耗量显示，消耗量将添加到回复内容末尾\n如若不需要该功能请关闭\n警告：不保证完全准确！");


        String TEXT_MEMORY = "⭕️提示: Deepseek API本身不提供记忆功能，本插件的记忆功能原理是将之前保存在本地的对话，连同用户新提问一起发送给AI (笑哭)";
        builder.addSwitch("记忆", "enable_memory")
                .defaultValue(true)
                .summaryOn("已开启记忆功能，AI可以记住最近多轮的对话了\n记忆数据位于MT插件数据目录下的shared_prefs中\n" + TEXT_MEMORY)
                .summaryOff("开启记忆功能后，AI可以记住最近多轮的对话内容，但会在一定程度上消耗更多token\n" + TEXT_MEMORY + "\n默认开启");

        builder.addInput("记忆量", "memory_rounds")
                .summary("设置AI记忆对话轮数，默认5轮，需要开启上面的记忆功能\n日常使用不宜过大，过大可能造成AI回复所用时间变久，以及增加token消耗，但需要较长的上下文记录，如代码分析与编写，适当多的记忆对话轮数更好")
                .defaultValue("5")
                .inputType(android.text.InputType.TYPE_CLASS_NUMBER)
                .validator(value -> {
                    if (value == null || value.isEmpty()) return "不可为空！";
                    try {
                        int rounds = Integer.parseInt(value);
                        if (rounds < 1 || rounds > 999) return "请输入1-999之间的整数";
                        return null;
                    } catch (NumberFormatException e) {
                        return "请输入有效的正整数";
                    }
                });





        builder.addHeader("提示词设置");
        builder.addInput("智能AI", "prompt_ai")
                .summary("智能AI模式提示词")
                .defaultValue("根据用户的要求回答，并请使用中文回答用户，除非用户指定使用其他语言。")
                .valueAsSummary();

        builder.addInput("代码分析", "prompt_analysis")
                .summary("内容分析模式提示词")
                .defaultValue("详细解释这段代码的功能与逻辑。并请使用中文回答，除非用户指定使用其他语言。")
                .valueAsSummary();

        builder.addInput("翻译模式", "prompt_translate")
                .summary("翻译模式提示词")
                .defaultValue("下面的内容如若为非中文请翻译成中文，如若为中文请翻译成英文。")
                .valueAsSummary();





        builder.addHeader("高级设置");
        builder.addInput("Temperature", "temperature")
                .summary("温度，控制输出随机性，值越低越保守反之亦然，默认0.5")
                .defaultValue("0.5")
                .inputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL)
                .validator(value -> {
                    if (value == null || value.isEmpty()) return "不可为空！";
                    try {
                        double d = Double.parseDouble(value);
                        if (d < 0 || d > 2) return "请输入0-2之间的数值";
                        int dotIdx = value.indexOf('.');
                        if (dotIdx >= 0 && value.substring(dotIdx + 1).length() > 1)
                            return "最多1位小数";
                        return null;
                    } catch (NumberFormatException e) {
                        return "请输入有效的数字";
                    }
                });


        builder.addInput("Top P", "top_p")
                .summary("核采样，控制候选词范围，默认0.8")
                .defaultValue("0.8")
                .inputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL)
                .validator(value -> {
                    if (value == null || value.isEmpty()) return "不可为空！";
                    try {
                        double d = Double.parseDouble(value);
                        if (d < 0.1 || d > 1) return "请输入0.1-1之间的数值";
                        int dotIdx = value.indexOf('.');
                        if (dotIdx >= 0 && value.substring(dotIdx + 1).length() > 1)
                            return "最多1位小数";
                        return null;
                    } catch (NumberFormatException e) {
                        return "请输入有效的数字";
                    }
                });


        builder.addSwitch("日志", "log_recording")
                .defaultValue(false)
                .summaryOn("已启用记录日志，如果不需要请关闭")
                .summaryOff("未启用记录日志，开启后将会记录日志\n默认关闭");





        builder.addHeader("介绍");
        builder.addText("关于")
                .summary("插件SDK: 2\n基于「deepseek」插件制作（原插件已下架，且无法找到原作者），现由『director_Carter』进行维护\n默认有两个模型（「deepseek-v4-flash」和「deepseek-v4-Pro」）\n\n插件使用的大模型均由「deepseek」提供，回答内容并不绝对准确！\n如若出现请求失败，并跟随错误码，请查看deepseek官方文档中错误码文档: https://api-docs.deepseek.com/zh-cn/quick_start/error_codes");


        builder.addText("开发者")
                .summary("现开发者：director_Carter\n联系QQ: 2705722903\n注：原插件已下架，且原开发者已无法找到")
                .url("https://director4168.github.io/ContactInformation.html");





        builder.addHeader("其它");
        builder.addText("开源地址")
                .summary("本插件开源地址（MPL-2.0），并提供原始插件")
                .url("https://github.com/director4168/MT-DeepseekPlugin");
    }
}