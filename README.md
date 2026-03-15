# AntiOpAbuse ⛔

yo server admins are probably abusing yk so this plugin gives you trust

AntiOpAbuse watches the server console 24/7 and rats out everything to a Discord webhook in real time. *Every* command, ANY item taken from creative inventory. but with safeguards; no join IPs shown, no auth messages shown, no /msg shown

No alerts are give to OP, nor can they disable it.

---

## What it does

- 📡 **snitchmaxxer** — every console line gets forwarded to Discord in real time
- 🎨 **Catches creative inventory abuse** — logs every item a creative mode player takes, because "I was just looking" is not an excuse
- 🤐 **Keeps actual secrets secret** — filters out IPs, DMs, and passwords so no leaking sensitive stuff
- ⚡ **Won't kill your server** — runs completely async, the main thread has no idea this is even happening
- 🛡️ **Completely abuse-proof** — there is no command to turn it off. No permission node. No secret backdoor. The only way to stop it is to physically remove the jar, which, good luck explaining that one
- 🔁 **discord rate limits? haha no** — it waits, it retries, it doesn't give up
- 🔧 **Three config options** — ye js two. its minimal af. (or i was js not able to find more config options lol)
- 🧹 **Commands ONLY option in config** — tired of reading your whole chat and only wanna see the abuse?, this one fixes that (it does not affect creative menu logging)
---

## Works on

| Server | Supported |
|--------|-----------|
| PaperMC | ✅ |
| Spigot | ✅ |
| Bukkit | ✅ |
| Purpur | ✅ |

- **Minecraft:** 1.18.x – 1.21.x (guaranteed), prob works on 1.15+ too but js to be safe
- **Java:** 17+ (if you're still on Java 8 please close this tab)

---

## Setup

1. grab the jar from download option
2. chunk it in `plugins/`
3. start the server, itll generate the config
4. open `plugins/AntiOpAbuse/config.yml` and paste your webhook URL
5. Run `/antiopabuse reload`
6. now try to abuse haha

---

## Config

```yaml
# AntiOpAbuse Configuration
# ─────────────────────────────────────────────────────────────────
# webhook-url          : Your Discord webhook URL.
#                        Create one under Server Settings → Integrations → Webhooks.
# send-as-codeblock    : Wrap each log line in a Discord code block for
#                        monospace / easy reading. Set false for plain text.
# commands-only        : When true, only forwards lines where a player or the
#                        console issued a command. All other log output (join/leave,
#                        plugin info, server warnings etc.) is ignored.
#                        /login and /register are always excluded regardless.
#                         (does not affect creative menu interactions)
# ─────────────────────────────────────────────────────────────────

webhook-url: "DISCORD_WEBHOOK_HERE"
send-as-codeblock: true
commands-only: false

```

yep no backdoors or disable options

---

## What it looks like in Discord

**Console relay:**
```
[INFO]: adminabuser issued server command: /op xX_GrieferKing_Xx
[INFO]: scaryop issued server command: /give @a diamond 64
[INFO]: katR issued server command: /ban Steve
[INFO]: katR is the goat fr
```

**Creative inventory logging:**
```
[CREATIVE] ezznub took 64x golden_apples
[CREATIVE] abuser1 took 1x bedrock
[CREATIVE] fullnethin1day took 64x netherite_ingot
```
> caught instantly nubz

---

## Commands

| Command | What it does |
|---------|-------------|
| `/antiopabuse webhook` | Pings Discord and tells you if it's working |
| `/antiopabuse reload` | Reloads the config so you don't have to restart |

**Alias:** `/aoa` for when your js lazy

Only OPs can run these. And yea, running them gets logged too. 

---

## What it won't snitch on

we trynna help players not expose them

- **IP addresses** — nobody needs those in Discord
- **`/msg`, `/tell`, `/w`, `/whisper`** — private messages stay private
- **Auth plugin stuff** — passwords, login attempts, all filtered out

---

## Building it yourself

```bash
git clone https://github.com/oiupoyt/AntiOpAbuse.git
cd AntiOpAbuse
mvn clean package
# jar is at target/AntiOpAbuse-1.X.X.jar
```

Requires Java 17+ and Maven 3.8+.

---
