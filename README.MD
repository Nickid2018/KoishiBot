# 多平台机器人 KoishiBot
![GitHub](https://img.shields.io/github/license/Nickid2018/KoishiBot)
![Workflow](https://img.shields.io/github/actions/workflow/status/Nickid2018/KoishiBot/build.yml)

![GitHub top language](https://img.shields.io/github/languages/top/Nickid2018/KoishiBot)
![GitHub file lines](https://img.shields.io/tokei/lines/github/Nickid2018/KoishiBot)
![GitHub file size](https://img.shields.io/github/languages/code-size/Nickid2018/KoishiBot)

![GitHub Repo stars](https://img.shields.io/github/stars/Nickid2018/KoishiBot?style=social)
![GitHub forks](https://img.shields.io/github/forks/Nickid2018/KoishiBot?style=social)

## 支持平台
* QQ - [Mirai](https://github.com/mamoe/mirai)
* KOOK - [KookyBot](https://github.com/KookyBot/KookyBot)
* Telegram - [TelegramBots](https://github.com/rubenlagus/TelegramBots)

## 快速开始
使用此 bot 请先下载最新 Action 内的 monitor 工件，并在同级目录配置`monitor-settings.json`。下列为一个简单的配置：

```json
{
  "enable_backends": "qq-backend,kook-backend,telegram-backend",
  "github_token": "替换为 GitHub Token"
}
```

* **enable_backends**：必需项，要启用的后端。monitor 会自行下载最新 Action 中的对应文件，在之后的启动更新或手动更新中也会读取这个配置。
* **github_token**：必需项，要求为一个 [GitHub Token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token)。读取 Action 时要求使用 Token，所以此项必填。
* **action_repo**：monitor 进行更新检查等操作时调用的 Action 位置，默认为`Nickid2018/KoishiBot;build.yml`。以分号分割，前面一项是 GitHub 仓库名称，后一项是 Workflow 文件。
* **core_port**：用于与机器人内核程序进行通信的端口，默认为`23333`。
* **proxy**：代理设置，可不存在。
  - **type**：代理类型，可以为`http`和`socks`，默认`http`。
  - **host**：代理主机，默认为`127.0.0.1`。
  - **port**：代理端口，对于`http`默认为`7890`，`socks`默认为`1080`。
  - **user**：代理使用的用户名，默认不需要配置。
  - **password**：代理使用的密码，默认不需要配置。

在配置写完后，可以直接执行 monitor，它会在本目录创建 bot 的运行环境，包括核心和后端，并将这些文件的校验和存入`monitor-data.json`。创建环境完成后会自动退出。

接下来需要配置核心，名称为`botKoishi.json`。基础配置如下：

* **core_port**：必需项，内核程序与 monitor 进行通信的端口，默认为`23333`。必须与 monitor 中的一致。
* **delegate_port**：必需项，用于连接各个后端进行通信的端口，默认为`52514`。
* **proxy**：与上文相同。
* **local_ip**：非必须，本机的 IP 地址。用于 OAuth 回调等。
* **port**：非必须，开放端口，作用与 IP 地址相同。
* **owner**：列表，设置机器人的主人。被指定的用户具有`OWNER`执行权限。

一些模块通用的配置如下：

* **audio**：转换音频的设置。
  - **ffmpeg**：指定 [FFmpeg](https://ffmpeg.org/) 的位置。
  - **encoder**：指定 [Silk Encoder](https://github.com/kn007/silk-v3-decoder) 的位置。
* **image**：生成图片设置。（不包括网页渲染图片）
  - **font**：图片内使用的字体。
  - **size**：字体大小。
* **webdriver**：指定 [Firefox Driver](https://github.com/mozilla/geckodriver/releases) 的位置。用于网页渲染。
* **sensitives**：敏感词过滤设置。
  - **type**：敏感词过滤器的类型，可以为`baidu`，不写则使用单词过滤。
  - **file**：单词过滤的文件，敏感词需要以`\n`分割。
  - **appId**：使用百度过滤的 App ID。
  - **appKey**：使用百度过滤的 App Key。

下列是对应模块的对应设置：

* **github** 模块：
  - **github_token**：一个 GitHub Token。用于读取 GitHub 上的信息。
  - **github_oauth2**：[GitHub OAuth](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/creating-an-oauth-app) 设置。
    * **client_id**：Client ID。
    * **client_secret**：Client Secret。
* **wakatime** 模块：
  - **wakatime**：[Wakatime OAuth](https://wakatime.com/developers#authentication) 设置。与前文 GitHub OAuth 设置方法相同。
* **translate** 模块：
  - **youdao**：有道翻译后端。
    * **app_key**：App Key。
    * **app_secret**：App Secret。
* **wiki** 模块：
  - **wiki**：wiki 模块的设置。
    * **wikis**：所有可以直接访问的 wiki。
      - *wiki 前缀*：一个 wiki 的前缀，用于标识访问。
        * **url**：wiki 的 [api.php](https://www.mediawiki.org/wiki/API:Main_page) 位置。
        * **render**：网页渲染设置。
          - **width**：网页渲染宽度。
          - **height**：网页渲染高度。
          - **enable**：是否进行渲染。
    * **base**：基础 wiki，指定的 wiki 不需要打前缀就能访问。

配置核心后，还需要配置各个后端。后端的配置在各个子项目的说明文件中。

配置完成后，再次运行 monitor 就可以启动。

## monitor 命令
### `enter 子进程`
monitor 会将核心和后端进行执行，但不是在本进程内进行操作，而是开新的进程，并通过网络进行连接。这样可以让机器人可以自动更新和阻止意外掉线等问题。

进入子进程的控制台需要使用此命令，此时输入流会被重定向到子进程的输入流，子进程的输出流会重定向到控制台。如果子进程结束，monitor 会自动断开连接。

monitor 在启动时自动进入核心`core`进程。

### `quit`
退出子进程，但不关闭子进程。必须在进入后输入此命令。

### `exit`
关闭 monitor，同时关闭核心和所有后端。