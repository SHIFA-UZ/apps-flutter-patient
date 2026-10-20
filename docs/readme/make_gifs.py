# Builds the README flow GIFs from the Roborazzi screenshots: python3 docs/readme/make_gifs.py composeApp/screenshots docs/readme
import sys
from PIL import Image
src, out = sys.argv[1], sys.argv[2]
W, H = 786, 1704  # w393dp-h852dp at xhdpi
flows = {
    "booking-payment": ["Home_loaded", "DoctorProfile_about", "CreateBooking_payNow", "CreateBooking_confirmPayment",
                        "AppointmentDetails_paid"],  # Payment is a blank webview in tests
    "video-visit": ["Home_loaded", "LiveAppointment_video", "VideoCall_join", "VideoCall_waiting",
                    "VideoCall_minimized", "LiveAppointment_sign"],
}
for name, shots in flows.items():
    frames = []
    for s in shots:
        im = Image.open(f"{src}/{s}.png").convert("RGB").crop((0, 0, W, H)).resize((360, 780), Image.LANCZOS)
        frames.append(im.quantize(colors=128, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE))
    frames[0].save(f"{out}/{name}.gif", save_all=True, append_images=frames[1:], duration=1600, loop=0, optimize=True)
    print(name, len(frames))
