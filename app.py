import os
import re
import json
import math
import shutil
import uuid
import time
import subprocess
from pathlib import Path
from typing import Optional, List, Dict

from flask import Flask, request, jsonify, render_template, send_from_directory

# =========================
# CONFIG
# =========================
BASE_DIR = Path(__file__).resolve().parent
UPLOAD_DIR = BASE_DIR / "uploads"
OUTPUT_DIR = BASE_DIR / "outputs"
TMP_DIR = BASE_DIR / "tmp"

for d in [UPLOAD_DIR, OUTPUT_DIR, TMP_DIR]:
    d.mkdir(parents=True, exist_ok=True)

MAX_UPLOAD_MB = 2048  # 2GB
ALLOWED_EXT = {".mp4", ".mov", ".mkv", ".webm", ".m4v", ".avi"}

app = Flask(__name__, template_folder="templates", static_folder="static")
app.config["MAX_CONTENT_LENGTH"] = MAX_UPLOAD_MB * 1024 * 1024


# =========================
# HELPERS
# =========================
def safe_name(name: str) -> str:
    name = re.sub(r"[^a-zA-Z0-9._ -]", "", name).strip()
    return name[:120] if name else f"video_{int(time.time())}.mp4"


def run_cmd(cmd: List[str], timeout: Optional[int] = None) -> Dict:
    try:
        p = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=timeout,
        )
        return {
            "ok": p.returncode == 0,
            "code": p.returncode,
            "stdout": p.stdout,
            "stderr": p.stderr,
        }
    except subprocess.TimeoutExpired as e:
        return {
            "ok": False,
            "code": -1,
            "stdout": e.stdout or "",
            "stderr": f"Timeout: {e}",
        }
    except Exception as e:
        return {
            "ok": False,
            "code": -2,
            "stdout": "",
            "stderr": str(e),
        }


def ffprobe_info(video_path: Path) -> Dict:
    cmd = [
        "ffprobe", "-v", "error",
        "-print_format", "json",
        "-show_format",
        "-show_streams",
        str(video_path)
    ]
    res = run_cmd(cmd, timeout=30)
    if not res["ok"]:
        return {"ok": False, "error": res["stderr"]}
    try:
        data = json.loads(res["stdout"])
        return {"ok": True, "data": data}
    except Exception as e:
        return {"ok": False, "error": f"ffprobe parse error: {e}"}


def get_video_meta(video_path: Path) -> Dict:
    info = ffprobe_info(video_path)
    if not info["ok"]:
        return {"ok": False, "error": info["error"]}

    data = info["data"]
    streams = data.get("streams", [])
    format_data = data.get("format", {})

    vstream = None
    for s in streams:
        if s.get("codec_type") == "video":
            vstream = s
            break

    if not vstream:
        return {"ok": False, "error": "No video stream found"}

    width = int(vstream.get("width", 0) or 0)
    height = int(vstream.get("height", 0) or 0)
    duration = float(format_data.get("duration", 0) or 0)
    fps_raw = vstream.get("r_frame_rate", "0/1")
    try:
        n, d = fps_raw.split("/")
        fps = float(n) / float(d) if float(d) != 0 else 0
    except Exception:
        fps = 0

    return {
        "ok": True,
        "meta": {
            "width": width,
            "height": height,
            "duration_sec": duration,
            "fps": round(fps, 2),
            "filename": video_path.name,
            "path": str(video_path),
        }
    }


def seconds_to_hms(sec: float) -> str:
    sec = max(0, int(sec))
    h = sec // 3600
    m = (sec % 3600) // 60
    s = sec % 60
    if h > 0:
        return f"{h:02d}:{m:02d}:{s:02d}"
    return f"{m:02d}:{s:02d}"


def auto_segments(duration_sec: float, clip_len: int = 75, max_clips: int = 6,
                  intro_skip: int = 10, outro_skip: int = 10) -> List[Dict]:
    """
    Heuristic ringan:
    - Lewati intro/outro sedikit
    - Sebar segmen merata
    - Pastikan tidak melebihi durasi
    """
    duration_sec = float(duration_sec)
    if duration_sec <= 0:
        return []

    usable_start = max(0, intro_skip)
    usable_end = max(usable_start, duration_sec - max(0, outro_skip))
    usable = max(0, usable_end - usable_start)

    if usable <= 5:
        return []

    clip_len = max(15, int(clip_len))
    max_possible = max(1, int(usable // clip_len))
    n = min(max(1, int(max_clips)), max(1, max_possible))

    # Kalau video pendek, buat 1 segmen saja
    if usable <= clip_len + 5:
        start = usable_start
        end = min(usable_end, start + clip_len)
        return [{
            "index": 1,
            "start_sec": round(start, 2),
            "end_sec": round(end, 2),
            "duration_sec": round(end - start, 2),
            "start_label": seconds_to_hms(start),
            "end_label": seconds_to_hms(end),
            "score": 70
        }]

    # Sebar segmen merata di area usable
    gaps = n + 1
    step = usable / gaps
    segments = []
    for i in range(n):
        center = usable_start + step * (i + 1)
        start = max(usable_start, center - clip_len / 2)
        end = start + clip_len
        if end > usable_end:
            end = usable_end
            start = max(usable_start, end - clip_len)

        # score dummy (bisa di-upgrade nanti)
        score = int(65 + (i % 3) * 8)  # 65,73,81...
        segments.append({
            "index": i + 1,
            "start_sec": round(start, 2),
            "end_sec": round(end, 2),
            "duration_sec": round(end - start, 2),
            "start_label": seconds_to_hms(start),
            "end_label": seconds_to_hms(end),
            "score": score
        })

    # hilangkan overlap besar
    cleaned = []
    last_end = -1
    for seg in segments:
        if seg["start_sec"] >= last_end - 5:
            cleaned.append(seg)
            last_end = seg["end_sec"]

    return cleaned[:max_clips]


def build_center_crop_filter(target_w=720, target_h=1280) -> str:
    """
    Center crop ke rasio 9:16 lalu scale ke target.
    """
    # Jika video lebih lebar dari rasio target -> crop width
    # Jika video lebih tinggi -> crop height
    # Menggunakan expression ffmpeg
    target_ratio = target_w / target_h  # 0.5625
    # crop_w / crop_h = target_ratio
    # pakai if(gt(iw/ih, target_ratio), ih*target_ratio, iw)
    filter_str = (
        f"crop='if(gt(iw/ih,{target_ratio}),ih*{target_ratio},iw)':'"
        f"if(gt(iw/ih,{target_ratio}),ih,iw/{target_ratio})':"
        f"(iw-ow)/2:(ih-oh)/2,"
        f"scale={target_w}:{target_h}"
    )
    return filter_str


def export_clip_9x16(input_path: Path, output_path: Path, start_sec: float, duration_sec: float,
                     target_w=720, target_h=1280) -> Dict:
    vf = build_center_crop_filter(target_w=target_w, target_h=target_h)

    # -ss sebelum -i lebih cepat, tapi kurang akurat; cukup untuk MVP
    cmd = [
        "ffmpeg",
        "-y",
        "-ss", str(start_sec),
        "-i", str(input_path),
        "-t", str(duration_sec),
        "-vf", vf,
        "-r", "30",
        "-c:v", "libx264",
        "-preset", "veryfast",
        "-crf", "23",
        "-c:a", "aac",
        "-b:a", "128k",
        "-movflags", "+faststart",
        str(output_path)
    ]
    return run_cmd(cmd, timeout=1800)


def list_media_files(folder: Path) -> List[Dict]:
    items = []
    for p in sorted(folder.glob("*")):
        if p.is_file():
            items.append({
                "name": p.name,
                "size_mb": round(p.stat().st_size / (1024 * 1024), 2),
                "modified_ts": int(p.stat().st_mtime),
            })
    return items


def generate_metadata_pack(topic: str = "Viral Clip") -> Dict:
    topic_clean = (topic or "Viral Clip").strip()
    hooks = [
        f"Bagian paling penting dari {topic_clean} 😳",
        f"Jangan skip! Ini inti {topic_clean} 🔥",
        f"Momen paling bikin penasaran dari {topic_clean}",
        f"{topic_clean} versi singkat yang wajib kamu tonton",
        f"Ini alasan kenapa {topic_clean} ramai dibahas"
    ]
    hashtags = [
        "#shorts", "#youtubeshorts", "#viral", "#fyp",
        "#clip", "#trending", "#kontenviral", "#shortvideo"
    ]
    descriptions = [
        f"Klip singkat {topic_clean}. Potongan terbaik untuk kamu tonton cepat.",
        f"Highlight {topic_clean} dalam format Shorts. Simpan dan share kalau bermanfaat.",
        f"Potongan momen menarik dari {topic_clean}. Tonton sampai habis!"
    ]
    return {
        "titles": hooks,
        "hashtags": " ".join(hashtags),
        "descriptions": descriptions,
    }


def save_metadata_txt(output_folder: Path, video_name: str, segments: List[Dict], metadata: Dict):
    text_lines = []
    text_lines.append(f"Source: {video_name}")
    text_lines.append("")
    text_lines.append("=== Suggested Titles ===")
    for i, t in enumerate(metadata.get("titles", []), start=1):
        text_lines.append(f"{i}. {t}")
    text_lines.append("")
    text_lines.append("=== Suggested Descriptions ===")
    for i, d in enumerate(metadata.get("descriptions", []), start=1):
        text_lines.append(f"{i}. {d}")
    text_lines.append("")
    text_lines.append("=== Hashtags ===")
    text_lines.append(metadata.get("hashtags", ""))
    text_lines.append("")
    text_lines.append("=== Segments ===")
    for seg in segments:
        text_lines.append(
            f"Clip {seg['index']}: {seg['start_label']} - {seg['end_label']} "
            f"({seg['duration_sec']}s), score={seg.get('score', 0)}"
        )

    out_file = output_folder / "meta.txt"
    out_file.write_text("\n".join(text_lines), encoding="utf-8")
    return out_file


# =========================
# ROUTES
# =========================
@app.route("/")
def index():
    uploads = list_media_files(UPLOAD_DIR)
    outputs = list_media_files(OUTPUT_DIR)
    return render_template("index.html", uploads=uploads, outputs=outputs)


@app.route("/media/uploads/<path:filename>")
def media_uploads(filename):
    return send_from_directory(UPLOAD_DIR, filename, as_attachment=False)


@app.route("/media/outputs/<path:filename>")
def media_outputs(filename):
    return send_from_directory(OUTPUT_DIR, filename, as_attachment=False)


@app.route("/download/output/<path:filename>")
def download_output(filename):
    return send_from_directory(OUTPUT_DIR, filename, as_attachment=True)


@app.route("/api/upload", methods=["POST"])
def api_upload():
    if "video" not in request.files:
        return jsonify({"ok": False, "error": "Field 'video' tidak ditemukan"}), 400

    file = request.files["video"]
    if not file or not file.filename:
        return jsonify({"ok": False, "error": "Tidak ada file dipilih"}), 400

    filename = safe_name(file.filename)
    ext = Path(filename).suffix.lower()
    if ext not in ALLOWED_EXT:
        return jsonify({"ok": False, "error": f"Format tidak didukung: {ext}"}), 400

    save_path = UPLOAD_DIR / filename
    # hindari overwrite
    if save_path.exists():
        stem = save_path.stem
        suffix = save_path.suffix
        save_path = UPLOAD_DIR / f"{stem}_{int(time.time())}{suffix}"

    file.save(str(save_path))
    meta = get_video_meta(save_path)
    if not meta["ok"]:
        return jsonify({"ok": False, "error": meta["error"]}), 500

    return jsonify({"ok": True, "file": save_path.name, "meta": meta["meta"]})


@app.route("/api/download_youtube", methods=["POST"])
def api_download_youtube():
    data = request.get_json(force=True, silent=True) or {}
    url = (data.get("url") or "").strip()
    if not url:
        return jsonify({"ok": False, "error": "URL YouTube kosong"}), 400

    job_id = str(uuid.uuid4())[:8]
    out_tpl = str(UPLOAD_DIR / f"yt_{job_id}.%(ext)s")

    cmd = [
        "yt-dlp",
        "-f", "mp4/best",
        "--merge-output-format", "mp4",
        "-o", out_tpl,
        url
    ]
    res = run_cmd(cmd, timeout=3600)
    if not res["ok"]:
        return jsonify({
            "ok": False,
            "error": "Gagal download YouTube",
            "stderr": res["stderr"][-2000:]
        }), 500

    # cari file terbaru prefix yt_jobid
    matches = sorted(UPLOAD_DIR.glob(f"yt_{job_id}*"), key=lambda p: p.stat().st_mtime, reverse=True)
    if not matches:
        return jsonify({"ok": False, "error": "Download selesai tapi file tidak ditemukan"}), 500

    video_path = matches[0]
    meta = get_video_meta(video_path)
    if not meta["ok"]:
        return jsonify({"ok": False, "error": meta["error"]}), 500

    return jsonify({
        "ok": True,
        "file": video_path.name,
        "meta": meta["meta"],
        "log_tail": res["stdout"][-1500:] if res["stdout"] else ""
    })


@app.route("/api/meta", methods=["POST"])
def api_meta():
    data = request.get_json(force=True, silent=True) or {}
    filename = data.get("file", "")
    if not filename:
        return jsonify({"ok": False, "error": "Nama file kosong"}), 400

    video_path = UPLOAD_DIR / filename
    if not video_path.exists():
        return jsonify({"ok": False, "error": "File tidak ditemukan"}), 404

    meta = get_video_meta(video_path)
    if not meta["ok"]:
        return jsonify({"ok": False, "error": meta["error"]}), 500

    return jsonify({"ok": True, "meta": meta["meta"]})


@app.route("/api/analyze", methods=["POST"])
def api_analyze():
    data = request.get_json(force=True, silent=True) or {}
    filename = data.get("file", "")
    clip_len = int(data.get("clip_len", 75) or 75)
    max_clips = int(data.get("max_clips", 6) or 6)
    intro_skip = int(data.get("intro_skip", 10) or 10)
    outro_skip = int(data.get("outro_skip", 10) or 10)

    video_path = UPLOAD_DIR / filename
    if not video_path.exists():
        return jsonify({"ok": False, "error": "File tidak ditemukan"}), 404

    meta = get_video_meta(video_path)
    if not meta["ok"]:
        return jsonify({"ok": False, "error": meta["error"]}), 500

    duration = meta["meta"]["duration_sec"]
    segments = auto_segments(
        duration_sec=duration,
        clip_len=clip_len,
        max_clips=max_clips,
        intro_skip=intro_skip,
        outro_skip=outro_skip
    )

    metadata_pack = generate_metadata_pack(topic=Path(filename).stem)

    return jsonify({
        "ok": True,
        "video_meta": meta["meta"],
        "segments": segments,
        "metadata_pack": metadata_pack
    })


@app.route("/api/export", methods=["POST"])
def api_export():
    data = request.get_json(force=True, silent=True) or {}
    filename = data.get("file", "")
    segments = data.get("segments", [])
    target_w = int(data.get("target_w", 720) or 720)
    target_h = int(data.get("target_h", 1280) or 1280)
    save_meta = bool(data.get("save_meta", True))
    topic = data.get("topic", "")

    video_path = UPLOAD_DIR / filename
    if not video_path.exists():
        return jsonify({"ok": False, "error": "File tidak ditemukan"}), 404

    if not isinstance(segments, list) or not segments:
        return jsonify({"ok": False, "error": "Segments kosong"}), 400

    ts = time.strftime("%Y%m%d_%H%M%S")
    base_name = Path(filename).stem
    export_folder = OUTPUT_DIR / f"{base_name}_{ts}"
    export_folder.mkdir(parents=True, exist_ok=True)

    logs = []
    outputs = []

    for i, seg in enumerate(segments, start=1):
        try:
            start_sec = float(seg.get("start_sec", 0))
            end_sec = float(seg.get("end_sec", 0))
            dur = max(1.0, end_sec - start_sec)
        except Exception:
            logs.append(f"[Clip {i}] Segment invalid: {seg}")
            continue

        out_file = export_folder / f"clip_{i:02d}_{int(start_sec)}s_to_{int(end_sec)}s.mp4"
        logs.append(
            f"[Clip {i}] Export {seconds_to_hms(start_sec)} - {seconds_to_hms(end_sec)} "
            f"({round(dur,2)}s) -> {out_file.name}"
        )
        res = export_clip_9x16(
            input_path=video_path,
            output_path=out_file,
            start_sec=start_sec,
            duration_sec=dur,
            target_w=target_w,
            target_h=target_h
        )
        if res["ok"]:
            outputs.append({
                "name": f"{export_folder.name}/{out_file.name}",
                "relative_folder": export_folder.name,
                "filename": out_file.name
            })
            logs.append(f"[Clip {i}] OK")
        else:
            logs.append(f"[Clip {i}] FAIL")
            logs.append(res["stderr"][-2000:])

    # simpan metadata
    if save_meta:
        metadata_pack = generate_metadata_pack(topic=topic or base_name)
        save_metadata_txt(export_folder, filename, segments, metadata_pack)
        outputs.append({
            "name": f"{export_folder.name}/meta.txt",
            "relative_folder": export_folder.name,
            "filename": "meta.txt"
        })

    return jsonify({
        "ok": True,
        "export_folder": export_folder.name,
        "logs": logs,
        "outputs": outputs
    })


@app.route("/download/output-folder/<path:folder>/<path:filename>")
def download_output_from_folder(folder, filename):
    folder_path = OUTPUT_DIR / folder
    if not folder_path.exists():
        return jsonify({"ok": False, "error": "Folder output tidak ditemukan"}), 404
    return send_from_directory(folder_path, filename, as_attachment=True)


@app.route("/api/list")
def api_list():
    uploads = list_media_files(UPLOAD_DIR)
    outputs = []
    for folder in sorted(OUTPUT_DIR.glob("*")):
        if folder.is_dir():
            files = []
            for f in sorted(folder.glob("*")):
                if f.is_file():
                    files.append({
                        "name": f.name,
                        "size_mb": round(f.stat().st_size / (1024 * 1024), 2)
                    })
            outputs.append({"folder": folder.name, "files": files})
    return jsonify({"ok": True, "uploads": uploads, "outputs": outputs})


@app.route("/api/delete_upload", methods=["POST"])
def api_delete_upload():
    data = request.get_json(force=True, silent=True) or {}
    filename = data.get("file", "")
    p = UPLOAD_DIR / filename
    if not p.exists():
        return jsonify({"ok": False, "error": "File upload tidak ditemukan"}), 404
    p.unlink(missing_ok=True)
    return jsonify({"ok": True})


@app.route("/api/delete_output_folder", methods=["POST"])
def api_delete_output_folder():
    data = request.get_json(force=True, silent=True) or {}
    folder = data.get("folder", "")
    p = OUTPUT_DIR / folder
    if not p.exists() or not p.is_dir():
        return jsonify({"ok": False, "error": "Folder output tidak ditemukan"}), 404
    shutil.rmtree(p, ignore_errors=True)
    return jsonify({"ok": True})


if __name__ == "__main__":
    # host 0.0.0.0 agar bisa dibuka dari browser HP
    app.run(host="0.0.0.0", port=5000, debug=True)
