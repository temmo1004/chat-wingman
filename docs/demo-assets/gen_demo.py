"""生成 IG DM 風格的假聊天截圖 HTML（demo 用）。三個『快被句點』情境。"""
import os, html

OUT = os.path.dirname(os.path.abspath(__file__))

# 每個情境：標題名、線上狀態、訊息列表 (sender, text)；sender: 'them' 左 / 'me' 右
SCENES = [
    ("demo_chat1", "Yuki", "剛剛在線上", [
        ("me", "嗨嗨～看你也喜歡爬山！上週去了哪座呀？"),
        ("them", "還好"),
        ("me", "哈哈 那你平常放假都做什麼？"),
        ("them", "沒幹嘛"),
        ("me", "喔喔…"),
    ]),
    ("demo_chat2", "子涵", "3 分鐘前在線上", [
        ("me", "所以你也覺得那部電影結局超雷XD"),
        ("them", "對啊"),
        ("me", "你有看過導演其他作品嗎？我覺得都滿有風格的"),
        ("me", "？"),
    ]),
    ("demo_chat3", "Ray", "上午 11:02 在線上", [
        ("them", "今天有點想找人聊天"),
        ("me", "那我很榮幸被選中😌"),
        ("them", "誰說是選你了 哈哈"),
        ("me", "……"),
    ]),
]

CSS = """
* { margin:0; padding:0; box-sizing:border-box; font-family:'PingFang TC','Noto Sans TC',sans-serif; -webkit-font-smoothing:antialiased; }
body { width:1080px; height:1920px; background:#fff; display:flex; flex-direction:column; }
.status { height:56px; display:flex; align-items:center; justify-content:space-between; padding:0 36px; font-size:30px; font-weight:600; color:#111; }
.status .right { letter-spacing:6px; }
.header { display:flex; align-items:center; gap:20px; padding:18px 28px; border-bottom:1px solid #eee; }
.back { font-size:44px; color:#111; }
.avatar { width:76px; height:76px; border-radius:50%; background:linear-gradient(135deg,#f58529,#dd2a7b,#8134af); }
.hname { font-size:38px; font-weight:700; color:#111; }
.hstatus { font-size:26px; color:#8e8e8e; margin-top:2px; }
.icons { margin-left:auto; display:flex; gap:28px; font-size:40px; color:#111; }
.chat { flex:1; padding:32px 28px; display:flex; flex-direction:column; gap:20px; overflow:hidden; background:#fff; }
.row { display:flex; align-items:flex-end; gap:14px; }
.row.me { justify-content:flex-end; }
.bub { max-width:640px; padding:22px 30px; font-size:34px; line-height:1.35; border-radius:36px; }
.them .bub { background:#efefef; color:#000; border-bottom-left-radius:10px; }
.me .bub { background:#3797f0; color:#fff; border-bottom-right-radius:10px; }
.sav { width:64px; height:64px; border-radius:50%; background:linear-gradient(135deg,#f58529,#dd2a7b,#8134af); flex:none; }
.seen { text-align:right; font-size:24px; color:#8e8e8e; padding-right:8px; }
.inbar { display:flex; align-items:center; gap:20px; padding:24px 28px; border-top:1px solid #eee; }
.inbox { flex:1; height:88px; border:1px solid #ddd; border-radius:44px; display:flex; align-items:center; padding:0 34px; font-size:32px; color:#9a9a9a; }
.camera { width:70px; height:70px; border-radius:50%; background:#3797f0; flex:none; }
.mic { font-size:40px; color:#111; }
"""

def build(name, hname, hstatus, msgs):
    rows = []
    for i, (who, text) in enumerate(msgs):
        av = '<div class="sav"></div>' if who == "them" else ""
        rows.append(
            f'<div class="row {who}">{av}<div class="bub">{html.escape(text)}</div></div>'
        )
    # 最後一則若是 me，加「已讀」暗示句點
    seen = '<div class="seen">已讀</div>' if msgs[-1][0] == "me" else ""
    return f"""<!doctype html><html><head><meta charset="utf-8"><style>{CSS}</style></head>
<body>
<div class="status"><span>11:02</span><span class="right">▶ ▂▄▆ 100%</span></div>
<div class="header"><span class="back">‹</span><div class="avatar"></div>
<div><div class="hname">{html.escape(hname)}</div><div class="hstatus">{html.escape(hstatus)}</div></div>
<div class="icons"><span>📞</span><span>🎥</span></div></div>
<div class="chat">{''.join(rows)}{seen}</div>
<div class="inbar"><div class="camera"></div><div class="inbox">訊息…</div><span class="mic">🎤</span></div>
</body></html>"""

for name, hname, hstatus, msgs in SCENES:
    p = os.path.join(OUT, name + ".html")
    with open(p, "w", encoding="utf-8") as f:
        f.write(build(name, hname, hstatus, msgs))
    print(p)
