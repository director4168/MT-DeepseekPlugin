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





        builder.addHeader("提示词设置");
        builder.addInput("翻译模式", "prompt_translate")
                .summary("翻译模式提示词")
                .defaultValue("下面的内容如若为非中文请翻译成中文，如若为中文请翻译成英文。")
                .valueAsSummary();

        builder.addInput("内容分析", "prompt_analysis")
                .summary("内容分析模式提示词")
                .defaultValue("详细解释这段代码的功能与逻辑，用中文回答。")
                .valueAsSummary();

        builder.addInput("智能AI", "prompt_ai")
                .summary("智能AI模式提示词")
                .defaultValue("请用中文回答用户。")
                .valueAsSummary();





        builder.addHeader("介绍");
        builder.addText("关于")
                .summary("插件SDK: V2\n基于原deepseek插件（已下架，且无法找到原作者）制作，现由director_Carter进行开发维护\n由于v2接口限制以及稳定性性考虑，目前仅有两个模型，且强制使用Deepseek API\n");

        builder.addText("API申请地址")
                .summary("DeepSeek API Key申请地址：https://platform.deepseek.com/api_keys，直接点击本列表可跳转")
                .url("https://platform.deepseek.com/api_keys");

        builder.addText("开发者")
                .summary("现开发者：director_Carter\n联系QQ: 2705722903\n注：原插件已下架，且原开发者已无法找到")
                .url("https://qm.qq.com/q/kfz1WgkdF0");





        builder.addHeader("其它");
        builder.addText("下载地址")
                .summary("本插件Github下载地址，并提供原包")
                .url("https://github.com/director4168/MT-DeepseekPlugin");
    }
}