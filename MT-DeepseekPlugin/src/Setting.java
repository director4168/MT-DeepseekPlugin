/*
 * (C) 2026 director_Carter All Rights Reserved.
 * 开源地址：https://github.com/director4168/MT-DeepseekPlugin
 * 邮箱： 2705722903@qq.com
*/
package bin.mt.plugin;

import bin.mt.plugin.api.MTPluginContext;
import bin.mt.plugin.api.preference.PluginPreference;
import android.content.SharedPreferences;

public class Setting implements PluginPreference {
    @Override
    public void onBuild(MTPluginContext context, Builder builder) {
        builder.addHeader("基础设置");
        builder.addInput("API Key", "api_token")
                .summary("输入您的API Key，若没有请前往https://platform.deepseek.com/api_keys申请")
                .defaultValue("")
                .valueAsSummary();

        builder.addSwitch("显示账户余额", "show_balance")
                .summary("开启后，将尝试获取账户剩余余额，并添加到回复内容的末尾\n注：开启后会增加少量耗时和流量消耗！默认关闭")
                .defaultValue(false)
                .summaryOn("已开启账户余额显示，账户剩余余额将添加到回复内容末尾\n如若不需要该功能请关闭\n警告：不保证完全准确！");

        builder.addSwitch("显示Token消耗量", "show_token_usage")
                .summary("开洗后，将尝试获取本次token消耗量，消耗量将添加到回复内容末尾\n注：开启后会增加少量耗时和流量消耗！默认关闭")
                .defaultValue(false)
                .summaryOn("已开启Token消耗量显示，消耗量将添加到回复内容末尾\n如若不需要该功能请关闭\n警告：不保证完全准确！");

        builder.addText("获取API Key")
                .summary("没有DeepSeek API Key？点我直达API Key创建地址！\n注：您需年满14周岁")
                .url("https://platform.deepseek.com/api_keys");





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