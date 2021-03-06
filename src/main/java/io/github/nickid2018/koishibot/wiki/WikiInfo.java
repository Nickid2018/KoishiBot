package io.github.nickid2018.koishibot.wiki;

import com.google.gson.*;
import io.github.nickid2018.koishibot.util.*;
import io.github.nickid2018.koishibot.util.value.MutableBoolean;
import org.apache.http.client.methods.HttpGet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
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
    public static final String QUERY_PAGE_TEXT = "action=parse&format=json&prop=text";
    public static final String WIKI_SEARCH = "action=query&format=json&list=search&srwhat=text";
    public static final String WIKI_RANDOM = "action=query&format=json&list=random";

    public static final String EDIT_URI_STR = "<link rel=\"EditURI\" type=\"application/rsd+xml\" href=\"";

    public static final Pattern USER_ANONYMOUS = Pattern.compile("User:\\d{1,3}(\\.\\d{1,3}){3}");

    public static final Set<String> SUPPORTED_IMAGE = WebUtil.SUPPORTED_IMAGE;
    public static final Set<String> NEED_TRANSFORM_IMAGE = new HashSet<>(
            Arrays.asList("webp", "ico")
    );
    public static final Set<String> NEED_TRANSFORM_AUDIO = new HashSet<>(
            Arrays.asList("oga", "ogg", "flac", "mp3", "wav")
    );
    public static final Map<String, WikiInfo> SUPPORT_WIKIS = new HashMap<>();

    private static final Map<String, WikiInfo> STORED_WIKI_INFO = new HashMap<>();
    private static final Map<WikiInfo, String> STORED_INTERWIKI_SOURCE_URL = new HashMap<>();
    public static String BASE_WIKI;

    private final Map<String, String> additionalHeaders;
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
        STORED_WIKI_INFO.put(url, this);
    }

    public WikiInfo(String url, Map<String, String> additionalHeaders) {
        this.url = url;
        this.additionalHeaders = additionalHeaders;
        STORED_WIKI_INFO.put(url, this);
    }

    @ReflectTarget
    public static void loadWiki(JsonObject settingsRoot) {
        JsonUtil.getData(settingsRoot, "wiki", JsonObject.class).ifPresent(wikiRoot -> {
            JsonObject wikisArray = wikiRoot.getAsJsonObject("wikis");
            for (Map.Entry<String, JsonElement> en : wikisArray.entrySet())
                if (!SUPPORT_WIKIS.containsKey(en.getKey())) {
                    if (en.getValue() instanceof JsonPrimitive)
                        SUPPORT_WIKIS.put(en.getKey(), new WikiInfo(en.getValue().getAsString() + "?"));
                    else {
                        JsonObject wikiData = en.getValue().getAsJsonObject();
                        JsonObject headers = wikiData.getAsJsonObject("headers");
                        Map<String, String> header = new HashMap<>();
                        for (Map.Entry<String, JsonElement> headerEntry : headers.entrySet())
                            header.put(headerEntry.getKey(), headerEntry.getValue().getAsString());
                        SUPPORT_WIKIS.put(en.getKey(), new WikiInfo(JsonUtil.getStringOrNull(wikiData, "url") + "?", header));
                    }
                }
            BASE_WIKI = wikiRoot.get("base").getAsString();
        });
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
                    throw new IOException("?????????????????????????????????????????????MediaWiki?????????????????????");
                String sub = data.substring(index + EDIT_URI_STR.length());
                url= sub.substring(0, sub.indexOf("?") + 1);
                STORED_WIKI_INFO.put(url, this);
                data = checkAndGet(url + WIKI_META);
                try {
                    object = JsonParser.parseString(data).getAsJsonObject();
                } catch (JsonSyntaxException ex) {
                    throw new IOException("?????????????????????????????????????????????MediaWiki?????????????????????");
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
            throw new IOException("?????????wiki??????");

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
                pageInfo.shortDescription = "????????????????????????MediaWiki?????????????????????????????????";
                return pageInfo;
            } else
                throw e;
        }

        if (prefix != null && prefix.split(":").length > 5)
            throw new IOException("????????????wiki????????????");

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
        if (title != null && title.toLowerCase(Locale.ROOT).startsWith("~search")) {
            String[] split = title.split(" ", 2);
            if (split.length == 2)
                return searches(split[1], prefix);
        }

        String section = null;
        if (title != null && title.contains("#")) {
            String[] titleSplit = title.split("#", 2);
            title = titleSplit[0];
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
            throw new IOException("????????????????????????????????????????????????????????????");
        }

        if (query == null)
            throw new IOException("??????????????????");

        PageInfo pageInfo = new PageInfo();
        pageInfo.info = this;
        pageInfo.prefix = prefix;
        pageInfo.title = title;
        tryRedirect(query.get("redirects"), pageInfo);
        tryRedirect(query.get("normalized"), pageInfo);

        JsonObject pages = query.getAsJsonObject("pages");
        if (pages == null)
            throw new IOException("????????????????????????");

        for (Map.Entry<String, JsonElement> entry : pages.entrySet()) {
            String id = entry.getKey();
            JsonObject object = entry.getValue().getAsJsonObject();
            if (object.has("special")) {
                pageInfo.url = articleURL.replace("$1", WebUtil.encode(title));
                pageInfo.shortDescription = "????????????";
                return pageInfo;
            }
            pageInfo.url = script + "?curid=" + id;
            if (object.has("missing")) {
                if (!object.has("known")) {
                    if (title != null)
                        return search(title, prefix);
                    throw new IOException("??????????????????");
                } else {
                    pageInfo.url = articleURL.replace("$1", WebUtil.encode(title));
                    pageInfo.shortDescription = "<???????????????>";
                    // Special:MyPage -> [MessageContext:IP]
                    if (RegexUtil.match(USER_ANONYMOUS, pageInfo.title))
                        pageInfo.title = "???????????????";
                }
            } else {
                pageInfo.title = title = object.get("title").getAsString();
                if (object.has("pageprops") && object.getAsJsonObject("pageprops").has("disambiguation"))
                    pageInfo.shortDescription = getDisambiguationText(title);
                else {
                    if (useTextExtracts && object.has("extract") && section == null)
                        pageInfo.shortDescription = resolveText(object.get("extract").getAsString().trim());
                    else
                        pageInfo.shortDescription = resolveText(getMarkdown(title, section, pageInfo));
                    pageInfo.infobox = InfoBoxShooter.getInfoBoxShot(pageInfo.url, baseURI);
                }
            }
            if (object.has("imageinfo")) {
                JsonArray array = object.getAsJsonArray("imageinfo");
                if (array.size() > 0) {
                    String type = JsonUtil.getStringOrNull(array.get(0).getAsJsonObject(), "descriptionurl");
                    String suffix = type.substring(Math.min(type.length() - 1, type.lastIndexOf('.') + 1))
                            .toLowerCase(Locale.ROOT);
                    URL link = new URL(JsonUtil.getStringOrNull(array.get(0).getAsJsonObject(), "url"));
                    if (SUPPORTED_IMAGE.contains(suffix))
                        pageInfo.imageStream = link.openStream();
                    else if (NEED_TRANSFORM_IMAGE.contains(suffix))
                        pageInfo.imageStream = FormatTransformer.transformImageToPNG(link.openStream(), suffix);
                    else if (NEED_TRANSFORM_AUDIO.contains(suffix)) {
                        if (allowAudio) {
                            pageInfo.shortDescription = "?????????????????????????????????";
                            pageInfo.audioFiles = AsyncUtil.submit(() -> FormatTransformer.transformWebAudioToSilks(
                                    suffix, link));
                        } else
                            pageInfo.shortDescription = "?????????????????????????????????????????????";
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

    private PageInfo search(String key, String prefix) throws IOException {
        JsonObject data = WebUtil.fetchDataInJson(getWithHeader(
                url + WIKI_SEARCH + "&srlimit=1&srsearch=" + WebUtil.encode(key))).getAsJsonObject();
        JsonArray search = data.getAsJsonObject("query").getAsJsonArray("search");
        PageInfo info = new PageInfo();
        info.prefix = prefix;
        info.info = this;
        info.isSearched = true;
        if (search.size() != 0)
            info.title = JsonUtil.getStringOrNull(search.get(0).getAsJsonObject(), "title");
        return info;
    }

    private PageInfo searches(String key, String prefix) throws IOException {
        JsonObject data = WebUtil.fetchDataInJson(getWithHeader(
                url + WIKI_SEARCH + "&srlimit=5&srsearch=" + WebUtil.encode(key))).getAsJsonObject();
        JsonArray search = data.getAsJsonObject("query").getAsJsonArray("search");
        PageInfo info = new PageInfo();
        info.prefix = prefix;
        info.info = this;
        info.searchTitles = new ArrayList<>();
        for (JsonElement element : search)
            info.searchTitles.add(JsonUtil.getStringOrNull(element.getAsJsonObject(), "title"));
        return info;
    }

    private PageInfo interwikiList() throws IOException {
        PageInfo info = new PageInfo();
        BufferedImage image = ImageRenderer.renderMap(interWikiMap, "???wiki??????", "??????",
                ImageRenderer.Alignment.RIGHT, ImageRenderer.Alignment.LEFT);
        ByteArrayOutputStream boas = new ByteArrayOutputStream();
        ImageIO.write(image, "png", boas);
        info.imageStream = new ByteArrayInputStream(boas.toByteArray());
        return info;
    }

    private void tryRedirect(JsonElement entry, PageInfo info) {
        if (entry instanceof JsonArray && info.title != null) {
            for (JsonElement element : (JsonArray) entry) {
                JsonObject object = element.getAsJsonObject();
                String from = JsonUtil.getStringOrNull(object, "from");
                if (from.equals(info.title)) {
                    String to = JsonUtil.getStringOrNull(object, "to");
                    if (info.titlePast == null)
                        info.titlePast = info.title;
                    info.title = to;
                    info.redirected = true;
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
            throw new IOException("????????????CloudFlare??????");
        // Blocked by Tencent
        if (data.contains("??????T-Sec Web???????????????(WAF)"))
            throw new IOException("????????????T-Sec Web???????????????");
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
                case '???':
                case '???':
                case '<':
                case '[':
                case '{':
                case '(':
                case '???':
                    blankets++;
                    break;
                case '>':
                case ']':
                case '}':
                case '???':
                case '???':
                case ')':
                case '???':
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
                case '???':
                case '???':
                case '???':
                    if (blankets == 0 && !quote && !subQuote && index > 15)
                        break INDEX_FIND;
                case '\n':
                    if (index > 20)
                        break INDEX_FIND;
            }
        }
        return source.substring(0, Math.min(source.length(), index + 1));
    }

    private String getMarkdown(String page, String section, PageInfo info) throws IOException {
        JsonObject data = WebUtil.fetchDataInJson(getWithHeader(url + QUERY_PAGE_TEXT + "&page="
                        + WebUtil.encode(page)))
                .getAsJsonObject();
        String html = JsonUtil.getStringInPathOrNull(data, "parse.text.*");
        if (html == null)
            throw new IOException("???????????????");
        String markdown = WebUtil.getAsMarkdownClean(html);
        if (section != null) {
            StringBuilder builder = new StringBuilder();
            MutableBoolean bool = new MutableBoolean(false);
            new BufferedReader(new StringReader(markdown)).lines().forEach(s -> {
                if (s.startsWith("##"))
                    bool.setValue(s.startsWith("## " + section));
                else if (bool.getValue())
                    builder.append(s).append("\n");
            });
            String sectionData = builder.toString().trim();
            if (!sectionData.isEmpty()) {
                info.url += "#" + WebUtil.encode(section);
                return sectionData;
            }
        }
        return markdown;
    }

    private String getDisambiguationText(String page) throws IOException {
        JsonObject data = WebUtil.fetchDataInJson(getWithHeader(url + QUERY_PAGE_TEXT + "&page="
                        + WebUtil.encode(page)))
                .getAsJsonObject();
        String html = JsonUtil.getStringInPathOrNull(data, "parse.text.*");
        if (html == null)
            throw new IOException("???????????????");

        List<String> items = new ArrayList<>();

        Document document = Jsoup.parse(html);
        Elements elements = document.getElementsByTag("a");

        for (Element element : elements) {
            String title = element.ownText();
            if (title.isEmpty())
                title = element.attr("title");
            items.add(title);
        }

        return "??????????????????" + page + "?????????:\n" + String.join(", ", items);
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
