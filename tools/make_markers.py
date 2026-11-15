#!/usr/bin/env python3
"""Make printable ArUco marker images for NOVA (IDs 0 to 10 by default).

Each PNG has the marker, a wide white border (so the marker's edge is clear after cutting
and sticking), and the ID printed below in plain text for the person who sticks it up.

    python3 tools/make_markers.py                   # writes tools/markers/marker_00.png ... marker_10.png
    python3 tools/make_markers.py --size-cm 12      # same, sized for printing at 12 cm
    python3 tools/make_markers.py --ids 0 1 2 --out /tmp/test

The dictionary must match `markers.json` ("DICT_4X4_50"). Print at 100 percent scale
("actual size", no "fit to page"), then measure the printed marker and write the size down:
the team needs it to choose marker size.

Needs: pip install opencv-python numpy   (version 4.7 or newer, for cv2.aruco.generateImageMarker)
"""
import argparse
import sys
from pathlib import Path

try:
    import cv2
    import numpy as np
except ImportError:
    sys.exit("This tool needs OpenCV for Python: pip install opencv-python numpy")

DICTIONARY = "DICT_4X4_50"  # keep in step with app/src/main/assets/markers.json
DPI = 300


def make_marker(marker_id: int, marker_px: int, border_px: int, label_px: int) -> np.ndarray:
    dictionary = cv2.aruco.getPredefinedDictionary(getattr(cv2.aruco, DICTIONARY))
    marker = cv2.aruco.generateImageMarker(dictionary, marker_id, marker_px)
    side = marker_px + 2 * border_px
    page = np.full((side + label_px, side), 255, dtype=np.uint8)  # white
    page[border_px:border_px + marker_px, border_px:border_px + marker_px] = marker
    # ID printed below the marker, centred.
    text = f"NOVA marker {marker_id}"
    scale = label_px / 90.0
    (w, h), _ = cv2.getTextSize(text, cv2.FONT_HERSHEY_SIMPLEX, scale, 2)
    cv2.putText(page, text, ((side - w) // 2, side + (label_px + h) // 2),
                cv2.FONT_HERSHEY_SIMPLEX, scale, 0, 2, cv2.LINE_AA)
    return page


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--ids", type=int, nargs="+", default=list(range(11)), help="marker IDs (default 0 to 10)")
    parser.add_argument("--size-cm", type=float, default=10.0, help="printed width of the black marker in cm (default 10)")
    parser.add_argument("--out", type=Path, default=Path(__file__).parent / "markers", help="output folder")
    args = parser.parse_args()

    marker_px = round(args.size_cm / 2.54 * DPI)
    border_px = marker_px // 4          # white border: a quarter of the marker on every side
    label_px = max(60, marker_px // 4)  # room for the printed text
    args.out.mkdir(parents=True, exist_ok=True)

    for marker_id in args.ids:
        if not 0 <= marker_id <= 49:
            sys.exit(f"ID {marker_id} is outside the dictionary (0 to 49).")
        path = args.out / f"marker_{marker_id:02d}.png"
        # The DPI is stored in the PNG so print dialogs can use the real size.
        cv2.imwrite(str(path), make_marker(marker_id, marker_px, border_px, label_px),
                    [cv2.IMWRITE_PNG_XDPI, DPI, cv2.IMWRITE_PNG_YDPI, DPI] if hasattr(cv2, "IMWRITE_PNG_XDPI") else [])
        print(f"wrote {path}")
    print(f"Marker width {args.size_cm} cm at {DPI} dpi. Print at 100 percent scale.")


if __name__ == "__main__":
    main()
