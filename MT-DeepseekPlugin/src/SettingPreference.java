package bin.mt.plugin;

import bin.mt.plugin.api.MTPluginContext;
import bin.mt.plugin.api.preference.PluginPreference;

public class SettingPreference implements PluginPreference {

    @Override
    public void onBuild(MTPluginContext context, Builder builder) {
        builder.addHeader("基础设置");

        // 输入API Keys
        builder.addInput("API Keys", "api_token")
                .summary("输入您的API Keys，若没有请前往https://platform.deepseek.com/usage申请")
                .defaultValue("")
                .valueAsSummary();

        builder.addHeader("介绍");
        // 介绍
        builder.addText("关于")
                .summary("本插件为MT管理器V2插件，基于原deepseek插件（已下架，且无法找到原作者）制作，现由director_Carter进行开发维护\n由于限制以及模型兼容性考虑，目前仅有两个模型，且强制使用Deepseek API\n");

        builder.addText("API申请地址")
                .summary("DeepSeek API申请地址：https://platform.deepseek.com/，也可以直接点击本列表跳转")
                .url("https://platform.deepseek.com/");

        // 我的联系方式
        builder.addText("开发者")
                .summary("现开发者： director_Carter\n联系QQ: 2705722903\n注：原插件已下架，且原开发者已无法找到")
                .url("https://qm.qq.com/q/kfz1WgkdF0");

        builder.addHeader("其它");
        // 其他
        builder.addText("下载地址")
                .summary("本插件Github下载地址，并提供原包")
                .url("https://github.com/director4168/MT-DeepseekPlugin");
    }
}