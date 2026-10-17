# Génère frontend/public/manuel-hooppicks.pdf à partir de manuel.html (Chrome sans interface).
# Deux passes : la première sert à repérer la page de chaque chapitre (pdftotext),
# la seconde écrit ces numéros dans le sommaire.
# Usage : python docs/manuel/build.py
import os, re, shutil, subprocess, sys, tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))
SRC = os.path.join(HERE, "manuel.html")
OUT = os.path.join(REPO, "frontend", "public", "manuel-hooppicks.pdf")
CHROME = os.environ.get("CHROME", r"C:\Program Files\Google\Chrome\Application\chrome.exe")
CHAPTERS = ["Premiers pas", "La semaine de jeu", "Parier", "La page d'un match",
            "La compétition", "Ton profil", "L'actu NBA", "Ton compte"]


def render(html_path, pdf_path):
    profile = tempfile.mkdtemp()
    try:
        subprocess.run([CHROME, "--headless=new", "--disable-gpu", f"--user-data-dir={profile}",
                        "--no-pdf-header-footer", "--allow-file-access-from-files",
                        f"--print-to-pdf={pdf_path}", "file:///" + html_path.replace("\\", "/")],
                       check=True, capture_output=True, timeout=120)
    finally:
        shutil.rmtree(profile, ignore_errors=True)


def chapter_pages(pdf_path):
    text = subprocess.run(["pdftotext", "-enc", "UTF-8", "-layout", pdf_path, "-"], capture_output=True, check=True).stdout.decode("utf8")
    pages = text.split("\f")
    found = {}
    for i, page in enumerate(pages, start=1):
        head = page.strip().splitlines()[:6]
        for n, title in enumerate(CHAPTERS, start=1):
            if f"c{n}" in found:
                continue
            if any(l.strip() == f"{n:02d}" for l in head) and title.upper() in page.upper():
                found[f"c{n}"] = i
    return found


html = open(SRC, encoding="utf8").read()
tmp_pdf = os.path.join(tempfile.gettempdir(), "manuel-pass1.pdf")
render(SRC, tmp_pdf)
pages = chapter_pages(tmp_pdf)
missing = [c for c in (f"c{n}" for n in range(1, 9)) if c not in pages]
if missing:
    print("chapitres introuvables :", missing, file=sys.stderr)

numbered = re.sub(r'(<span class="pg" data-toc="(c\d)">)(</span>)',
                  lambda m: m.group(1) + str(pages.get(m.group(2), "")) + m.group(3), html)
tmp_html = os.path.join(HERE, "_manuel-numerote.html")
open(tmp_html, "w", encoding="utf8").write(numbered)
try:
    render(tmp_html, OUT)
finally:
    os.remove(tmp_html)
print("PDF :", OUT, pages)
