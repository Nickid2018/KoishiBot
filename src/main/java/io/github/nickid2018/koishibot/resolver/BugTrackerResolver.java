package io.github.nickid2018.koishibot.resolver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.KoishiBotMain;
import io.github.nickid2018.koishibot.message.api.ChainMessage;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.ImageMessage;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.RegexUtil;
import io.github.nickid2018.koishibot.util.WebUtil;
import org.apache.http.client.methods.HttpGet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.regex.Pattern;

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
    public boolean resolveInternal(String key, MessageContext context, Pattern pattern, Environment environment) {
        String resolve = key.substring(5, key.length() - 1);
        boolean isDisplay = RegexUtil.match(BUG_NAME_PATTERN, resolve);
        boolean isSearch = RegexUtil.match(BUG_SEARCH_PATTERN, resolve);
        if (!isDisplay && !isSearch)
            return false;
        KoishiBotMain.INSTANCE.executor.execute(() -> {
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

    private static void doBugSearch(String searchKey, MessageContext context, Environment environment) throws IOException {
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
        HttpGet get = new HttpGet(MOJIRA_SEARCH_API_URL + searchKey + "&fields=key,summary&maxResults=10&startAt=" + page * bufferSize);
        JsonObject data = WebUtil.fetchDataInJson(get).getAsJsonObject();
        if (data.has("errorMessages"))
            // Error while getting
            throw new IOException(data.get("errorMessages").getAsString());
        int total = data.get("total").getAsInt();
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
                String key = issue.get("key").getAsString();
                String summary = WebUtil.getDataInPathOrNull(issue, "fields.summary");
                builder.append("[").append(key).append("]").append(summary).append("\n");
            }
        } else
            builder.append("未搜索到任何结果");
        environment.getMessageSender().sendMessageRecallable(context, environment.newChain(
                environment.newQuote(context.getMessage()),
                environment.newText(builder.toString().trim())
        ));
    }

    private static void doBugDisplay(String id, MessageContext context, Environment environment) throws IOException {
        HttpGet get = new HttpGet(MOJIRA_API_URL + id);
        JsonObject data = WebUtil.fetchDataInJson(get).getAsJsonObject();
        if (data.has("errorMessage"))
            // Error while getting
            throw new IOException(data.get("errorMessage").getAsString());
        JsonObject fields = data.getAsJsonObject("fields");
        if (fields == null)
            throw new IOException("无法获取JSON文本，可能是该漏洞报告被删除或无权访问");
        String title = data.get("key").getAsString() + ": " + fields.get("summary").getAsString();
        String project = WebUtil.getDataInPathOrNull(fields, "project.name");
        String created = WebUtil.getDataInPathOrNull(fields, "created");
        String status = WebUtil.getDataInPathOrNull(fields, "status.name");
        String subStatus = WebUtil.getDataInPathOrNull(fields, "customfield_10500.value");
        String resolution = WebUtil.getDataInPathOrNull(fields, "resolution.name");
        String versions = null;
        String fixVersion = null;
        String mojangPriority = WebUtil.getDataInPathOrNull(fields, "customfield_12200.value");

        if (resolution == null)
            resolution = "Unresolved";
        if (resolution.equals("Duplicate")) {
            JsonArray issueLinks = fields.getAsJsonArray("issuelinks");
            for (JsonElement element : issueLinks) {
                JsonObject issue = element.getAsJsonObject();
                String type = WebUtil.getDataInPathOrNull(issue, "type.name");
                String outwardIssue = WebUtil.getDataInPathOrNull(issue, "outwardIssue.key");
                if (type != null && outwardIssue != null && type.equals("Duplicate")) {
                    resolution += "(与" + outwardIssue + "重复)";
                    break;
                }
            }
        }
        if (fields.has("versions")) {
            JsonArray versionsArray = fields.getAsJsonArray("versions");
            if (versionsArray.size() == 1) {
                JsonObject versionRoot = versionsArray.get(0).getAsJsonObject();
                versions = versionRoot.get("name").getAsString() + "(" + versionRoot.get("releaseDate").getAsString() + ")";
            } else {
                JsonObject versionRoot1 = versionsArray.get(0).getAsJsonObject();
                JsonObject versionRoot2 = versionsArray.get(versionsArray.size() - 1).getAsJsonObject();
                versions = versionRoot1.get("name").getAsString() + "(" + versionRoot1.get("releaseDate").getAsString() + ") ~ " +
                        versionRoot2.get("name").getAsString() + "(" + versionRoot2.get("releaseDate").getAsString() + ")";
            }
        }
        if (fields.has("fixVersions")) {
            JsonArray fixArray = fields.getAsJsonArray("fixVersions");
            if (fixArray.size() != 0) {
                JsonObject lastFix = fixArray.get(fixArray.size() - 1).getAsJsonObject();
                fixVersion = lastFix.get("name").getAsString() + "(" + lastFix.get("releaseDate").getAsString() + ")";
                if (!resolution.equals("Resolved") && !resolution.equals("Fixed"))
                    fixVersion += "(尝试修复)";
                if (fixArray.size() > 1)
                    fixVersion += "(仅显示最后一次修复)";
            }
        }

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
        BufferedReader reader = new BufferedReader(new StringReader(fields.get("description").getAsString()));
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
            String[] fileNameSplit = object.get("filename").getAsString().split("\\.");
            if (WebUtil.SUPPORTED_IMAGE.contains(fileNameSplit[fileNameSplit.length - 1])) {
                image = object.get("content").getAsString();
                break;
            }
        }

        ChainMessage chain;
        if (image != null) {
            ImageMessage imageSend = environment.newImage();
            try (InputStream stream = new URL(image).openStream()) {
                imageSend.fillImage(stream);
            }
            chain = environment.newChain(
                    environment.newQuote(context.getMessage()),
                    environment.newText(builder.toString()),
                    imageSend
            );
        } else
            chain = environment.newChain(
                    environment.newQuote(context.getMessage()),
                    environment.newText(builder.toString())
            );
        environment.getMessageSender().sendMessage(context, chain);
    }
}
