package io.github.nickid2018.koishibot.module.wiki;

import com.google.gson.*;
import io.github.nickid2018.koishibot.util.*;
import io.github.nickid2018.koishibot.util.web.WebUtil;
import org.apache.http.client.methods.HttpGet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

public class WikiInfo {

    public static final String WIKI_META = "action=query&format=json&meta=siteinfo&siprop=extensions%7Cgeneral%7Cinterwikimap";
    public static final String QUERY_PAGE = "action=query&format=json&prop=info%7Cimageinfo%7Cextracts%7Cpageprops&inprop=url&iiprop=url&" +
                                            "ppprop=description%7Cdisplaytitle%7Cdisambiguation%7Cinfoboxes&explaintext&" +
                                            "exsectionformat=plain&exchars=200&redirects";
    public static final String QUERY_PAGE_NOE = "action=query&format=json&inprop=url&iiprop=url&prop=info%7Cimageinfo&redirects";
    public static final String WIKI_SEARCH = "action=query&format=json&list=search&srwhat=text";
    public static final String WIKI_RANDOM = "action=query&format=json&list=random";

    public static final String EDIT_URI_STR = "<link rel=\"EditURI\" type=\"application/rsd+xml\" href=\"";

    public static final Pattern USER_ANONYMOUS = Pattern.compile("User:\\d{1,3}(\\.\\d{1,3}){3}");

    public static final Set<String> SUPPORTED_IMAGE = WebUtil.SUPPORTED_IMAGE;
    public static final Set<String> NEED_TRANSFORM_IMAGE = Set.of(
            "webp", "ico"
    );
    public static final Set<String> NEED_TRANSFORM_AUDIO = Set.of(
            "oga", "ogg", "flac", "mp3", "wav"
    );
    public static final Map<String, WikiInfo> SUPPORT_WIKIS = new HashMap<>();

    private static final Map<String, WikiInfo> STORED_WIKI_INFO = new HashMap<>();
    private static final Map<WikiInfo, String> STORED_INTERWIKI_SOURCE_URL = new HashMap<>();
    public static String BASE_WIKI;

    private final Map<String, String> additionalHeaders;
    private final WikiRenderSettings renderSettings;
    private String url;

    private boolean available;
    private boolean useTextExtracts;
    private String baseURI;
    private String articleURL;
    private String script;
    private final Map<String, String> interWikiMap = new HashMap<>();

    public WikiInfo(String url) {
        this.url = url;
        additionalHeaders = new HashMap<>();
        renderSettings = new WikiRenderSettings(0, 0, true);
        STORED_WIKI_INFO.put(url, this);
    }

    public WikiInfo(String url, Map<String, String> additionalHeaders, WikiRenderSettings renderSettings) {
        this.url = WebUtil.mirror(url);
        this.additionalHeaders = additionalHeaders;
        this.renderSettings = renderSettings;
        STORED_WIKI_INFO.put(url, this);
    }

    public static void loadWiki(JsonObject settingsRoot) {
        SUPPORT_WIKIS.clear();
        STORED_WIKI_INFO.clear();
        JsonUtil.getData(settingsRoot, "wiki", JsonObject.class).ifPresent(wikiRoot -> {
            JsonObject wikisArray = wikiRoot.getAsJsonObject("wikis");
            for (Map.Entry<String, JsonElement> en : wikisArray.entrySet())
                if (!SUPPORT_WIKIS.containsKey(en.getKey())) {
                    if (en.getValue() instanceof JsonPrimitive)
                        SUPPORT_WIKIS.put(en.getKey(), new WikiInfo(en.getValue().getAsString() + "?"));
                    else {
                        JsonObject wikiData = en.getValue().getAsJsonObject();
                        Map<String, String> header = new HashMap<>();
                        JsonUtil.getData(wikiData, "headers", JsonObject.class).ifPresent(headers -> {
                                    for (Map.Entry<String, JsonElement> headerEntry : headers.entrySet())
                                        header.put(headerEntry.getKey(), headerEntry.getValue().getAsString());
                                });
                        WikiRenderSettings renderSettings = JsonUtil.getData(wikiData, "render", JsonObject.class)
                                        .map(object -> {
                                            int width = JsonUtil.getIntOrZero(object, "width");
                                            int height = JsonUtil.getIntOrZero(object, "height");
                                            boolean render = JsonUtil.getData(object, "enable", JsonPrimitive.class)
                                                    .filter(JsonPrimitive::isBoolean)
                                                    .map(JsonPrimitive::getAsBoolean)
                                                    .orElse(true);
                                            return new WikiRenderSettings(width, height, render);
                                        }).orElse(new WikiRenderSettings(0, 0, true));
                        SUPPORT_WIKIS.put(en.getKey(), new WikiInfo(
                                JsonUtil.getStringOrNull(wikiData, "url") + "?", header, renderSettings));
                    }
                }
            BASE_WIKI = wikiRoot.get("base").getAsString();
        });
    }

    public WikiRenderSettings getRenderSettings() {
        return renderSettings;
    }

    public Map<String, String> getAdditionalHeaders() {
        return additionalHeaders;
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

            JsonUtil.getDataInPath(object, "query.extensions", JsonArray.class).ifPresent(extensions -> {
                for (JsonElement element : extensions) {
                    String name = element.getAsJsonObject().get("name").getAsString();
                    if (name.equals("TextExtracts")) {
                        useTextExtracts = true;
                        break;
                    }
                }
            });

            String server = JsonUtil.getStringInPathOrNull(object, "query.general.server");
            String realURL;
            if (server != null && server.startsWith("/"))
                realURL = url.split("/")[0] + server;
            else
                realURL = server;
            articleURL = realURL + JsonUtil.getStringInPathOrNull(object, "query.general.articlepath");
            script = realURL + JsonUtil.getStringInPathOrNull(object, "query.general.script");
            baseURI = "https://" + new URL(articleURL).getHost();

            if (!getInterWikiDataFromPage()) {
                JsonUtil.getDataInPath(object, "query.interwikiMap", JsonArray.class).ifPresent(interwikiMap -> {
                    for (JsonElement element : interwikiMap) {
                        JsonObject obj = element.getAsJsonObject();
                        String url = JsonUtil.getStringOrNull(obj, "url");
                        String prefix = JsonUtil.getStringOrNull(obj, "prefix");
                        interWikiMap.put(prefix, url);
                        WikiInfo info = new WikiInfo(url.contains("?") ?
                                url.substring(0, url.lastIndexOf('?') + 1) : url + "?");
                        STORED_WIKI_INFO.put(url, info);
                        STORED_INTERWIKI_SOURCE_URL.put(info, url);
                    }
                });
            }

            available = true;
        }
    }

    public PageInfo parsePageInfo(String title, int pageID, String prefix, boolean allowAudio) throws Exception {
        if (title != null && title.isEmpty())
            throw new IOException("无效的wiki查询");

        try {
            checkAvailable();
        } catch (IOException e) {
            if (STORED_INTERWIKI_SOURCE_URL.containsKey(this)) {
                PageInfo pageInfo = new PageInfo();
                pageInfo.info = this;
                pageInfo.prefix = prefix;
                pageInfo.title = title;
                pageInfo.url = STORED_INTERWIKI_SOURCE_URL.get(this)
                        .replace("$1", URLEncoder.encode(title, StandardCharsets.UTF_8).replace("%2F", "/"));
                pageInfo.shortDescription = "目标可能不是一个MediaWiki，已自动转换为网址链接";
                return pageInfo;
            } else
                throw e;
        }

        if (prefix != null && prefix.split(":").length > 5)
            throw new IOException("请求跳转wiki次数过多");

        boolean forceNoInterwiki = title != null && title.startsWith("[") && title.endsWith("]");
        if (forceNoInterwiki)
            title = title.substring(1, title.length() - 1);

        if (title != null && !forceNoInterwiki && title.contains(":")){
            String namespace = title.split(":")[0];
            if (interWikiMap.containsKey(namespace)) {
                WikiInfo skip = STORED_WIKI_INFO.get(interWikiMap.get(namespace));
                return skip.parsePageInfo(title.split(":", 2)[1], 0,
                        (prefix == null ? "" : prefix + ":") + namespace, allowAudio);
            }
        }

        if (title != null && title.equalsIgnoreCase("~rd"))
            return random(prefix, allowAudio);
        if (title != null && title.equalsIgnoreCase("~iw"))
            return interwikiList();
        if (title != null && title.toLowerCase(Locale.ROOT).startsWith("~page"))
            return parsePageInfo(null, Integer.parseInt(title.split(" ", 2)[1]), prefix, allowAudio);
        if (title != null && title.toLowerCase(Locale.ROOT).startsWith("~search")) {
            String[] split = title.split(" ", 2);
            if (split.length == 2)
                return searches(split[1], prefix);
        }

        String section = null;
        boolean takeFullPage = false;
        if (title != null && title.contains("#")) {
            String[] titleSplit = title.split("#", 2);
            title = titleSplit[0];
            if (titleSplit.length == 1 || titleSplit[1].isEmpty())
                takeFullPage = true;
            else
                section = titleSplit[1];
        }

        JsonObject query;
        String queryFormat = useTextExtracts ? QUERY_PAGE : QUERY_PAGE_NOE;
        try {
            if (title == null)
                query = JsonParser.parseString(checkAndGet(url + queryFormat + "&pageids=" + pageID))
                        .getAsJsonObject().getAsJsonObject("query");
            else
                query = JsonParser.parseString(checkAndGet(url + queryFormat + "&titles=" + WebUtil.encode(title)))
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
        tryRedirect(query.get("redirects"), pageInfo, takeFullPage, section, PageInfo.RedirectType.REDIRECT);
        tryRedirect(query.get("normalized"), pageInfo, takeFullPage, section, PageInfo.RedirectType.NORMALIZED);

        JsonObject pages = query.getAsJsonObject("pages");
        if (pages == null)
            throw new IOException("未查询到任何页面");

        for (Map.Entry<String, JsonElement> entry : pages.entrySet()) {
            String id = entry.getKey();
            JsonObject object = entry.getValue().getAsJsonObject();
            if (object.has("special")) {
                pageInfo.url = articleURL.replace("$1", WebUtil.encode(title));
                pageInfo.shortDescription = "特殊页面";
                pageInfo.infobox = WikiPageShooter.getFullPageShot(pageInfo.url, baseURI, this);
                return pageInfo;
            }
            pageInfo.url = script + "?curid=" + id;
            if (object.has("missing")) {
                if (!object.has("known")) {
                    if (title != null)
                        return search(title, prefix, takeFullPage, section);
                    throw new IOException("无法找到页面");
                } else {
                    pageInfo.url = articleURL.replace("$1", WebUtil.encode(title));
                    pageInfo.shortDescription = "<页面无内容>";
                    // Special:MyPage -> [MessageContext:IP]
                    if (RegexUtil.match(USER_ANONYMOUS, pageInfo.title))
                        pageInfo.title = "匿名用户页";
                }
            } else {
                pageInfo.title = title = object.get("title").getAsString();
                if (object.has("pageprops") && object.getAsJsonObject("pageprops").has("disambiguation")) {
                    pageInfo.shortDescription = "消歧义页面";
                    pageInfo.infobox = WikiPageShooter.getFullPageShot(pageInfo.url, baseURI, this);
                } else if (section != null)
                    makeSection(section, pageInfo);
                else if (useTextExtracts && object.has("extract")) {
                    pageInfo.shortDescription = resolveText(object.get("extract").getAsString().trim());
                    pageInfo.infobox = takeFullPage ?
                            WikiPageShooter.getFullPageShot(pageInfo.url, baseURI, this) :
                            WikiPageShooter.getInfoBoxShot(pageInfo.url, baseURI, this);
                } else
                    makeFullPageAndInfobox(pageInfo, takeFullPage);
            }
            if (object.has("imageinfo")) {
                JsonArray array = object.getAsJsonArray("imageinfo");
                if (array.size() > 0) {
                    String type = JsonUtil.getStringOrNull(array.get(0).getAsJsonObject(), "descriptionurl");
                    String suffix = type.substring(Math.min(type.length() - 1, type.lastIndexOf('.') + 1))
                            .toLowerCase(Locale.ROOT);
                    URL link = new URL(JsonUtil.getStringOrNull(array.get(0).getAsJsonObject(), "url"));
                    pageInfo.shortDescription = "文件页面，此页面含有" + array.size() + "个文件\n其中第一个文件地址为:\n" + link;
                    if (SUPPORTED_IMAGE.contains(suffix))
                        pageInfo.imageStream = link.openStream();
                    else if (NEED_TRANSFORM_IMAGE.contains(suffix))
                        pageInfo.imageStream = FormatTransformer.transformImageToPNG(link.openStream(), suffix);
                    else if (NEED_TRANSFORM_AUDIO.contains(suffix)) {
                        if (allowAudio) {
                            pageInfo.shortDescription = "音频信息，将分割后发送";
                            pageInfo.audioFiles = AsyncUtil.submit(() -> FormatTransformer.transformWebAudioToSilks(
                                    suffix, link));
                        } else
                            pageInfo.shortDescription = "音频信息，但平台不支持音频发送";
                    }
                }
            }
        }

        if (pageInfo.url.startsWith("//"))
            pageInfo.url = "https:" + pageInfo.url;

        return pageInfo;
    }

    private PageInfo random(String prefix, boolean allowAudio) throws Exception {
        JsonObject data = WebUtil.fetchDataInJson(getWithHeader(url + WIKI_RANDOM)).getAsJsonObject();
        PageInfo info =  parsePageInfo(Objects.requireNonNull(
                JsonUtil.getStringInPathOrNull(data, "query.random.0.title")), 0, prefix, allowAudio);
        info.isRandom = true;
        return info;
    }

    private PageInfo search(String key, String prefix, boolean full, String section) throws IOException {
        JsonObject data = WebUtil.fetchDataInJson(getWithHeader(
                url + WIKI_SEARCH + "&srlimit=1&srsearch=" + WebUtil.encode(key))).getAsJsonObject();
        JsonObject queryObject = data.getAsJsonObject("query");
        JsonArray search = queryObject == null ? null : queryObject.getAsJsonArray("search");
        PageInfo info = new PageInfo();
        info.prefix = prefix;
        info.info = this;
        info.isSearched = true;
        if (search != null && search.size() != 0) {
            info.title = JsonUtil.getStringOrNull(search.get(0).getAsJsonObject(), "title");
            if (full)
                info.title += "#";
            if (section != null)
                info.title += "#" + section;
        }
        return info;
    }

    private PageInfo searches(String key, String prefix) throws IOException {
        JsonObject data = WebUtil.fetchDataInJson(getWithHeader(
                url + WIKI_SEARCH + "&srlimit=5&srsearch=" + WebUtil.encode(key))).getAsJsonObject();
        JsonObject queryObject = data.getAsJsonObject("query");
        JsonArray search = queryObject == null ? null : queryObject.getAsJsonArray("search");
        PageInfo info = new PageInfo();
        info.prefix = prefix;
        info.info = this;
        info.searchTitles = new ArrayList<>();
        if (search != null)
            for (JsonElement element : search)
                info.searchTitles.add(JsonUtil.getStringOrNull(element.getAsJsonObject(), "title"));
        return info;
    }

    private PageInfo interwikiList() throws IOException {
        PageInfo info = new PageInfo();
        BufferedImage image = ImageRenderer.renderMap(interWikiMap, "跨wiki前缀", "目标",
                ImageRenderer.Alignment.RIGHT, ImageRenderer.Alignment.LEFT);
        ByteArrayOutputStream boas = new ByteArrayOutputStream();
        ImageIO.write(image, "png", boas);
        info.imageStream = new ByteArrayInputStream(boas.toByteArray());
        return info;
    }

    private void tryRedirect(JsonElement entry, PageInfo info, boolean full, String section, PageInfo.RedirectType redirectType) {
        if (entry instanceof JsonArray && info.title != null) {
            for (JsonElement element : (JsonArray) entry) {
                JsonObject object = element.getAsJsonObject();
                String from = JsonUtil.getStringOrNull(object, "from");
                if (from.equals(info.title)) {
                    String to = JsonUtil.getStringOrNull(object, "to");
                    if (info.titlePast == null)
                        info.titlePast = info.title;
                    info.title = to;
                    info.redirected = redirectType;
                    if (full)
                        info.title += "#";
                    if (section != null)
                        info.title += "#" + section;
                    break;
                }
            }
        }
    }

    private HttpGet getWithHeader(String url) {
        HttpGet get = new HttpGet(WebUtil.mirror(url));
        get.setHeader("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        for (Map.Entry<String, String> entry : additionalHeaders.entrySet())
            get.setHeader(entry.getKey(), entry.getValue());
        return get;
    }

    private String checkAndGet(String url) throws IOException {
        String data = WebUtil.fetchDataInText(getWithHeader(url), true);
        // Blocked by CloudFlare
        if (data.contains("Attention Required! | Cloudflare"))
            throw new IOException("机器人被CloudFlare拦截");
        // Blocked by Tencent
        if (data.contains("腾讯T-Sec Web应用防火墙(WAF)"))
            throw new IOException("机器人被T-Sec Web防火墙拦截");
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
                    if (blankets == 0 && !quote && !subQuote && index > 15)
                        break INDEX_FIND;
                case '\n':
                    if (index > 20)
                        break INDEX_FIND;
            }
        }
        return source.substring(0, Math.min(source.length(), index + 1));
    }

    private void makeSection(String section, PageInfo info) throws IOException {
        Document document = WikiPageShooter.fetchWikiPage(info.url, additionalHeaders);
        Elements elements = document.getElementsByClass("mw-headline");
        Element found = null;
        for (Element element : elements) {
            if (element.text().equalsIgnoreCase(section)) {
                found = element.parent();
                break;
            }
        }
        if (found == null)
            throw new IOException("未找到此章节");
        Element sibling = found.nextElementSibling();
        info.infobox = WikiPageShooter.getSectionShot(info.url, document, baseURI, section, this);
        info.shortDescription = resolveText(sibling == null ? "章节无内容" : sibling.text());
        info.url += "#" + WebUtil.encode(section);
    }

    private void makeFullPageAndInfobox(PageInfo info, boolean takeFullPage) throws IOException {
        Document document = WikiPageShooter.fetchWikiPage(info.url, additionalHeaders);
        Elements elements = document.getElementsByClass("mw-parser-output");
        if (elements.size() != 1)
            throw new IOException("非正常页面");
        Element found = elements.get(0);
        info.shortDescription = resolveText(found.text());
        info.infobox = takeFullPage ?
                WikiPageShooter.getFullPageShot(info.url, baseURI, document, this) :
                WikiPageShooter.getInfoBoxShot(info.url, baseURI, document, this);
    }

    private boolean getInterWikiDataFromPage(){
        try {
            String data = WebUtil.fetchDataInText(
                    getWithHeader(articleURL.replace("$1", "Special:Interwiki")));
            Document page = Jsoup.parse(data);
            Elements interWikiSection = page.getElementsByClass("mw-interwikitable-row");
            for (Element entry : interWikiSection) {
                Element prefixEntry = entry.getElementsByClass("mw-interwikitable-prefix").get(0);
                Element urlEntry = entry.getElementsByClass("mw-interwikitable-url").get(0);
                String prefix = prefixEntry.ownText();
                String url = urlEntry.ownText();
                interWikiMap.put(prefix, url);
                WikiInfo info = new WikiInfo(url.contains("?") ?
                        url.substring(0, url.lastIndexOf('?') + 1) : url + "?");
                STORED_WIKI_INFO.put(url, info);
                STORED_INTERWIKI_SOURCE_URL.put(info, url);
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
