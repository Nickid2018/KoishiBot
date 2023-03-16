package io.github.nickid2018.koishibot.resolver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.message.DelegateEnvironment;
import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.ChainMessage;
import io.github.nickid2018.koishibot.message.api.ImageMessage;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.RegexUtil;
import io.github.nickid2018.koishibot.util.web.WebUtil;
import org.apache.hc.client5.http.classic.methods.HttpGet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.regex.Pattern;

@ResolverName("mojira-bug")
@Syntax(syntax = "<bug:[编号]>", help = "获取Mojira上的漏洞信息")
@Syntax(syntax = "<bug:search:[JQL查询指令]>", help = "查询漏洞信息")
@Syntax(syntax = "<bug:search:[页数],(候选数):[JQL查询指令]>", help = "指定页数和候选数查询漏洞信息")
public class BugTrackerResolver extends MessageResolver {

    public static final Pattern BUG_TRACKER_PATTERN = Pattern.compile("<bug:.+?>");
    public static final Pattern BUG_NAME_PATTERN = Pattern.compile("[A-Z]+-\\d+");
    public static final Pattern BUG_SEARCH_PATTERN = Pattern.compile("search:.+");
    public static final Pattern BUG_SEARCH_PAGE_PATTERN = Pattern.compile("\\d*(,\\d+)?:.+");

    public static final String MOJIRA_API_URL = "https://bugs.mojang.com/rest/api/2/issue/";
    public static final String MOJIRA_SEARCH_API_URL = "https://bugs.mojang.com/rest/api/2/search?jql=";

    public BugTrackerResolver() {
        super(BUG_TRACKER_PATTERN);
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, DelegateEnvironment environment) {
        String resolve = key.substring(5, key.length() - 1);
        boolean isDisplay = RegexUtil.match(BUG_NAME_PATTERN, resolve);
        boolean isSearch = RegexUtil.match(BUG_SEARCH_PATTERN, resolve);
        if (!isDisplay && !isSearch)
            return false;
        AsyncUtil.execute(() -> {
            try {
                if (isDisplay)
                    doBugDisplay(resolve, context, environment);
                if (isSearch)
                    doBugSearch(resolve.substring(7), context, environment);
            } catch (Exception e) {
                environment.getMessageSender().onError(e, "bugtracker", context, false);
            }
        });
        return true;
    }

    private static void doBugSearch(String searchKey, MessageContext context, DelegateEnvironment environment) throws IOException {
        int bufferSize = 10;
        int page = 0;
        if (RegexUtil.match(BUG_SEARCH_PAGE_PATTERN, searchKey)) {
            String[] split = searchKey.split(":", 2);
            searchKey = split[1];
            String[] regions = split[0].split(",");
            if (!regions[0].isEmpty())
                page = Integer.parseInt(regions[0]);
            if (regions.length > 1 && !regions[1].isEmpty())
                bufferSize = Math.min(30, Integer.parseInt(regions[1]));
        }
        searchKey = WebUtil.encode(searchKey);

        HttpGet get = new HttpGet(MOJIRA_SEARCH_API_URL + searchKey
                + "&fields=key,summary&maxResults=10&startAt=" + page * bufferSize);
        JsonObject data = WebUtil.fetchDataInJson(get).getAsJsonObject();
        if (data.has("errorMessages"))
            // Error while getting
            throw new IOException(data.get("errorMessages").getAsString());

        int total = JsonUtil.getIntOrZero(data, "total");
        JsonArray issues = data.getAsJsonArray("issues");
        StringBuilder builder = new StringBuilder("查找结果\n");
        if (total != 0) {
            builder.append("总结果数量").append(total);
            if (total > bufferSize)
                builder.append("[仅显示").append(bufferSize).append("个候选项]");
            if (page > 0)
                builder.append("(第").append(page).append("页，第").append(page * bufferSize).append("项结果开始)");
            builder.append("\n");
            for (JsonElement element : issues) {
                JsonObject issue = element.getAsJsonObject();
                String key = JsonUtil.getStringOrNull(issue, "key");
                String summary = JsonUtil.getStringInPathOrNull(issue, "fields.summary");
                builder.append("[").append(key).append("]").append(summary).append("\n");
            }
        } else
            builder.append("未搜索到任何结果");
        environment.getMessageSender().sendMessageRecallable(context, environment.newChain(
                environment.newQuote(context.message()),
                environment.newText(builder.toString().trim())
        ));
    }

    private static void doBugDisplay(String id, MessageContext context, DelegateEnvironment environment) throws IOException {
        HttpGet get = new HttpGet(MOJIRA_API_URL + id);
        JsonObject data = WebUtil.fetchDataInJson(get).getAsJsonObject();
        if (data.has("errorMessage"))
            // Error while getting
            throw new IOException(data.get("errorMessage").getAsString());

        JsonObject fields = data.getAsJsonObject("fields");
        if (fields == null)
            throw new IOException("无法获取JSON文本，可能是该漏洞报告被删除或无权访问");
        String title = JsonUtil.getStringOrNull(data, "key") + ": " + JsonUtil.getStringOrNull(fields, "summary");
        String project = JsonUtil.getStringInPathOrNull(fields, "project.name");
        String created = JsonUtil.getStringOrNull(fields, "created");
        String status = JsonUtil.getStringInPathOrNull(fields, "status.name");
        String subStatus = JsonUtil.getStringInPathOrNull(fields, "customfield_10500.value");
        String resolution = JsonUtil.getStringInPathOrNull(fields, "resolution.name");
        String mojangPriority = JsonUtil.getStringInPathOrNull(fields, "customfield_12200.value");

        if (resolution == null)
            resolution = "Unresolved";
        if (resolution.equals("Duplicate")) {
            JsonArray issueLinks = fields.getAsJsonArray("issuelinks");
            for (JsonElement element : issueLinks) {
                JsonObject issue = element.getAsJsonObject();
                String type = JsonUtil.getStringInPathOrNull(issue, "type.name");
                String outwardIssue = JsonUtil.getStringInPathOrNull(issue, "outwardIssue.key");
                if (type != null && outwardIssue != null && type.equals("Duplicate")) {
                    resolution += "(与" + outwardIssue + "重复)";
                    break;
                }
            }
        }

        String versions = JsonUtil.getData(fields, "versions", JsonArray.class).map(versionsArray -> {
            if (versionsArray.size() == 1) {
                JsonObject versionRoot = versionsArray.get(0).getAsJsonObject();
                return JsonUtil.getStringOrNull(versionRoot, "name") + "("
                        + JsonUtil.getStringOrNull(versionRoot, "releaseDate") + ")";
            } else {
                JsonObject versionRoot1 = versionsArray.get(0).getAsJsonObject();
                JsonObject versionRoot2 = versionsArray.get(versionsArray.size() - 1).getAsJsonObject();
                return JsonUtil.getStringOrNull(versionRoot1, "name") + "("
                        + JsonUtil.getStringOrNull(versionRoot1, "releaseDate") + ") ~ " +
                        JsonUtil.getStringOrNull(versionRoot2, "name") + "("
                        + JsonUtil.getStringOrNull(versionRoot2, "releaseDate") + ")";
            }
        }).orElse(null);

        String finalResolution = resolution;
        String fixVersion = JsonUtil.getData(fields, "fixVersions", JsonArray.class).map(fixArray -> {
            if (fixArray.size() != 0) {
                JsonObject lastFix = fixArray.get(fixArray.size() - 1).getAsJsonObject();
                String ret = JsonUtil.getStringOrNull(lastFix, "name") + "("
                        + JsonUtil.getStringOrNull(lastFix, "releaseDate") + ")";
                if (!finalResolution.equals("Resolved") && !finalResolution.equals("Fixed"))
                    ret += "(尝试修复)";
                if (fixArray.size() > 1)
                    ret += "(仅显示最后一次修复)";
                return ret;
            } else return null;
        }).orElse(null);

        StringBuilder builder = new StringBuilder(title).append("\n");
        if (project != null)
            builder.append("项目: ").append(project).append("\n");
        if (created != null)
            builder.append("创建时间: ").append(created).append("\n");
        if (versions != null)
            builder.append("影响版本: ").append(versions).append("\n");
        builder.append("目前状态: ");
        if (subStatus != null)
            builder.append(subStatus);
        if (status != null)
            builder.append("[").append(status).append("]");
        builder.append("\n");
        builder.append("目前解决状态: ").append(resolution).append("\n");
        if (fixVersion != null)
            builder.append("修复版本: ").append(fixVersion).append("\n");
        if (mojangPriority != null)
            builder.append("Mojang优先级: ").append(mojangPriority).append("\n");
        builder.append("主条目URL: https://bugs.mojang.com/browse/").append(id).append("\n");

        BufferedReader reader = new BufferedReader(new StringReader(JsonUtil.getStringOrNull(fields, "description")));
        String line;
        while ((line = reader.readLine()) != null && builder.length() <= 600) {
            line = line.trim();
            if (!line.isEmpty())
                builder.append(line).append("\n");
        }
        if (line != null)
            builder.append("(原文过长截断，完整信息请访问主条目URL)");

        String image = null;
        JsonArray array = fields.getAsJsonArray("attachment");
        for (JsonElement element : array) {
            JsonObject object = element.getAsJsonObject();
            String[] fileNameSplit = JsonUtil.getStringOrNull(object, "filename").split("\\.");
            if (WebUtil.SUPPORTED_IMAGE.contains(fileNameSplit[fileNameSplit.length - 1])) {
                image = JsonUtil.getStringOrNull(object, "content");
                break;
            }
        }

        ChainMessage chain;
        if (image != null) {
            ImageMessage imageSend = environment.newImage(new URL(image));
            chain = environment.newChain(
                    environment.newQuote(context.message()),
                    environment.newText(builder.toString()),
                    imageSend
            );
        } else
            chain = environment.newChain(
                    environment.newQuote(context.message()),
                    environment.newText(builder.toString())
            );
        environment.getMessageSender().sendMessage(context, chain);
    }
}
