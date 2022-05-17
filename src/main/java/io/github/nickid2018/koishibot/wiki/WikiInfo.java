package io.github.nickid2018.koishibot.wiki;

import com.google.gson.*;
import io.github.nickid2018.koishibot.util.WebUtil;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class WikiInfo {

    public static final String WIKI_META = "action=query&format=json&meta=siteinfo&siprop=extensions%7Cgeneral%7Cinterwikimap";
    public static final String QUERY_PAGE = "action=query&format=json&inprop=url&iiprop=url&prop=info%7Cimageinfo%7Cextracts%7Cpageprops&" +
                                            "ppprop=description%7Cdisplaytitle%7Cdisambiguation%7Cinfoboxes&explaintext&" +
                                            "exsectionformat=plain&exchars=200&redirects";
    public static final String QUERY_PAGE_NOE = "action=query&format=json&inprop=url&iiprop=url&prop=info%7Cimageinfo&redirects";
    public static final String WIKI_SEARCH = "action=query&format=json&list=search&srwhat=text&srlimit=1&srenablerewrite";

    public static final Pattern EDIT_URI = Pattern.compile("<link\\w+rel=\"EditURI\"\\w+type=\"application/rsd\\+xml\"\\w+href=" +
            "\"https?://.*?/api\\.php\\?action=rsd\"\\w*/?>", Pattern.CASE_INSENSITIVE);

    public static final String EDIT_URI_STR = "<link rel=\"EditURI\" type=\"application/rsd+xml\" href=\"";

    private static final Map<String, WikiInfo> STORED_WIKI_INFO = new HashMap<>();

    private final Map<String, String> additionalHeaders;
    private String url;

    private boolean available;
    private boolean useTextExtracts;
    private String realURL;
    private String script;
    private final Map<String, String> interWikiMap = new HashMap<>();

    public WikiInfo(String url) {
        this.url = url;
        additionalHeaders = new HashMap<>();
        STORED_WIKI_INFO.put(url, this);
    }

    public WikiInfo(String url, Map<String, String> additionalHeaders) {
        this.url = url;
        this.additionalHeaders = additionalHeaders;
        STORED_WIKI_INFO.put(url, this);
    }

    public void checkAvailable() throws IOException {
        if (!available) {
            JsonObject object;
            String data = checkAndGet(url + WIKI_META);
            try {
                object = JsonParser.parseString(data).getAsJsonObject();
            } catch (JsonSyntaxException e) {
                // Not a valid API entrance, try to get the api.php
                int index = data.indexOf(EDIT_URI_STR);
                if (index < 0)
                    throw new IOException("无法获取信息，可能网站不是一个MediaWiki或被验证码阻止");
                String sub = data.substring(index + EDIT_URI_STR.length());
                url= sub.substring(0, sub.indexOf("?") + 1);
                STORED_WIKI_INFO.put(url, this);
                data = checkAndGet(url + WIKI_META);
                try {
                    object = JsonParser.parseString(data).getAsJsonObject();
                } catch (JsonSyntaxException ex) {
                    throw new IOException("无法获取信息，可能网站不是一个MediaWiki或被验证码阻止");
                }
            }

            JsonArray extensions = object.getAsJsonObject("query").getAsJsonArray("extensions");
            for (JsonElement element : extensions) {
                String name = element.getAsJsonObject().get("name").getAsString();
                if (name.equals("TextExtracts")) {
                    useTextExtracts = true;
                    break;
                }
            }
            String server = WebUtil.getDataInPathOrNull(object, "query.general.server");
            if (server != null && server.startsWith("/"))
                realURL = url.split("/")[0] + server;
            else
                realURL = server;
            script = realURL + WebUtil.getDataInPathOrNull(object, "query.general.script");

            JsonArray interwikiMap = object.getAsJsonObject("query").getAsJsonArray("interwikimap");
            for (JsonElement element : interwikiMap) {
                JsonObject obj = element.getAsJsonObject();
                String url = obj.get("url").getAsString();
                interWikiMap.put(obj.get("prefix").getAsString(), url);
                STORED_WIKI_INFO.put(url, new WikiInfo(url.contains("?") ?
                        url.substring(0, url.lastIndexOf('?') + 1) : url + "?"));
            }

            available = true;
        }
    }

    public PageInfo parsePageInfo(String title, int pageID, String prefix) throws IOException {
        checkAvailable();

        if (prefix != null && prefix.split(":").length > 5)
            throw new IOException("请求跳转wiki次数过多");

        if (title != null && title.contains(":")){
            String namespace = title.split(":")[0];
            if (interWikiMap.containsKey(namespace)) {
                WikiInfo skip = STORED_WIKI_INFO.get(interWikiMap.get(namespace));
                return skip.parsePageInfo(title.split(":", 2)[1], 0,
                        (prefix == null ? "" : prefix + ":") + namespace);
            }
        }

        JsonObject query;
        String queryFormat = useTextExtracts ? QUERY_PAGE : QUERY_PAGE_NOE;
        try {
            if (title == null)
                query = JsonParser.parseString(checkAndGet(url + queryFormat + "&pageid=" + pageID))
                        .getAsJsonObject().getAsJsonObject("query");
            else
                query = JsonParser.parseString(checkAndGet(url + queryFormat + "&titles=" + URLEncoder.encode(title, "UTF-8")))
                        .getAsJsonObject().getAsJsonObject("query");
        } catch (JsonSyntaxException e) {
            throw new IOException("返回了错误的数据，可能机器人被验证码阻止");
        }

        if (query == null)
            throw new IOException("无法获取数据");

        PageInfo pageInfo = new PageInfo();
        pageInfo.info = this;
        pageInfo.prefix = prefix;
        pageInfo.title = title;
        tryRedirect(query.get("redirects"), pageInfo);
        tryRedirect(query.get("normalized"), pageInfo);

        JsonObject pages = query.getAsJsonObject("pages");
        if (pages == null)
            throw new IOException("未查询到任何页面");

        for (Map.Entry<String, JsonElement> entry : pages.entrySet()) {
            String id = entry.getKey();
            JsonObject object = entry.getValue().getAsJsonObject();
            pageInfo.url = script + "?curid=" + id;
            if (object.has("missing")) {
                if (title != null)
                    return search(title);
                throw new IOException("无法找到页面，可能不存在或页面为文件");
            }
            if (useTextExtracts && object.has("extract")) {
                pageInfo.shortDescription = resolveText(object.get("extract").getAsString().trim());
            } else {
                // ...
            }
        }

        return pageInfo;
    }

    private PageInfo search(String key) throws IOException {
        JsonObject data = WebUtil.fetchDataInJson(getWithHeader(
                url + WIKI_SEARCH + "&srsearch=" + URLEncoder.encode(key, "UTF-8"))).getAsJsonObject();
        JsonArray search = data.getAsJsonObject("query").getAsJsonArray("search");
        PageInfo info = new PageInfo();
        info.isSearched = true;
        if (search.size() != 0)
            info.title = search.get(0).getAsJsonObject().get("title").getAsString();
        return info;
    }

    private void tryRedirect(JsonElement entry, PageInfo info) {
        if (entry instanceof JsonArray && info.title != null) {
            for (JsonElement element : (JsonArray) entry) {
                JsonObject object = element.getAsJsonObject();
                String from = object.get("from").getAsString();
                if (from.equalsIgnoreCase(info.title)) {
                    String to = element.getAsJsonObject().get("to").getAsString();
                    info.titlePast = info.title;
                    info.title = to;
                    info.redirected = true;
                    break;
                }
            }
        }
    }

    private HttpGet getWithHeader(String url) {
        HttpGet get = new HttpGet(url);
        for (Map.Entry<String, String> entry : additionalHeaders.entrySet())
            get.setHeader(entry.getKey(), entry.getValue());
        return get;
    }

    private String checkAndGet(String url) throws IOException {
        String data = WebUtil.fetchDataInPlain(getWithHeader(url), true);
        // Blocked by CloudFlare
        if (data.contains("Attention Required! | Cloudflare"))
            throw new IOException("机器人被CloudFlare拦截");
        return data;
    }

    private String resolveText(String source) {
        source = source.replaceAll("(\n){2,}+", "\n");
        int index = 0;
        int blankets = 0;
        boolean quote = false;
        boolean subQuote = false;
        INDEX_FIND: for (; index < source.length(); index++) {
            char now = source.charAt(index);
            switch (now) {
                case '《':
                case '「':
                case '<':
                case '[':
                case '{':
                case '(':
                case '（':
                    blankets++;
                    break;
                case '>':
                case ']':
                case '}':
                case '」':
                case '》':
                case ')':
                case '）':
                    blankets--;
                    break;
                case '"':
                    quote = !quote;
                    break;
                case '\'':
                    subQuote = !subQuote;
                    break;
                case '?':
                case '!':
                case '.':
                case '？':
                case '！':
                case '。':
                    if (blankets == 0 && !quote && !subQuote && index > 30)
                        break INDEX_FIND;
            }
        }
        return source.substring(0, Math.min(source.length(), index + 1));
    }
}
