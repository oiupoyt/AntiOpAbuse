# AntiOpAbuse 🔍

yo server admins are probably abusing yk so this plugin gives you trust

AntiOpAbuse watches the server console 24/7 and rats out everything to a Discord webhook in real time. *Every* command, ANY item taken from creative inventory.

No alerts are give to OP, nor can they disable it.

---

## What it does

- 📡 **Snitches on everything** — every console line gets forwarded to Discord in real time
- 🎨 **Catches creative inventory abuse** — logs every item a creative mode player takes, because "I was just looking" is not an excuse
- 🤐 **Keeps actual secrets secret** — filters out IPs, DMs, and passwords so you're not leaking sensitive stuff while spying on everyone else
- ⚡ **Won't kill your server** — runs completely async, the main thread has no idea this is even happening
- 🛡️ **Completely abuse-proof** — there is no command to turn it off. No permission node. No secret backdoor. The only way to stop it is to physically remove the jar, which, good luck explaining that one
- 🔁 **Discord rate-limits? Handled** — it waits, it retries, it doesn't give up
- 🔧 **Two config options** — yes, two. its minimal af.

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

1. Grab the jar from [Releases](../../releases)
2. Throw it in `plugins/`
3. Start the server, let it generate the config
4. Open `plugins/AntiOpAbuse/config.yml` and paste your webhook URL
5. Run `/antiopabuse reload`
6. Go do something suspicious and watch it appear in Discord instantly

---

## Config

```yaml
# plugins/AntiOpAbuse/config.yml

webhook-url: "https://discord.com/api/webhooks/YOUR_ID/YOUR_TOKEN"
send-as-codeblock: true
```

That's it. That's the whole config. You're welcome.

---

## What it looks like in Discord

**Console relay:**
```
[INFO]: adminabuser issued server command: /op xX_GrieferKing_Xx
[INFO]: scaryop issued server command: /give @a diamond 64
[INFO]: katR issued server command: /ban Steve
[WARN]: Can't keep up! Is the server overloaded?
```

**Creative inventory logging:**
```
[CREATIVE] ezznub took 64x Diamond Sword
[CREATIVE] abuser1 took 1x Bedrock
[CREATIVE] fullnethin1day took 64x Tnt
```
> caught instantly nubz

---

## Commands

| Command | What it does |
|---------|-------------|
| `/antiopabuse webhook` | Pings Discord and tells you if it's working |
| `/antiopabuse reload` | Reloads the config so you don't have to restart |

**Alias:** `/aoa` for when you're in a hurry to catch someone

Only OPs can run these. And yes, running them gets logged too. Hi.

---

## What it won't snitch on

Look we're spies, not monsters.

- **IP addresses** — nobody needs those in Discord
- **`/msg`, `/tell`, `/w`, `/whisper`** — private messages stay private
- **Auth plugin stuff** — passwords, login attempts, all filtered out

---

## Building it yourself

```bash
git clone https://github.com/oiupoyt/AntiOpAbuse.git
cd AntiOpAbuse
mvn clean package
# jar is at target/AntiOpAbuse-1.1.0.jar
```

Requires Java 17+ and Maven 3.8+.

---

## License

MIT. Go wild. Just don't use it for evil.

*(Using it to catch people doing evil is fine and in fact the whole point.)*
