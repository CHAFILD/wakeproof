"""Synthesizes the default alarm ringtone for WakeProof (spec stage 2).

Generates res/raw/alarm_ring.wav: a classic digital-alarm beep pattern
(groups of short, slightly rising beeps with pauses), 16-bit PCM mono,
44.1 kHz, ~2.4 s, loops seamlessly in MediaPlayer. Fully synthesized,
no third-party audio, so no licensing questions.
"""

import os
import wave

import numpy as np

SAMPLE_RATE = 44_100
OUT_PATH = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "app", "src", "main", "res", "raw", "alarm_ring.wav",
)

# Pattern: three beep groups; each group has 4 beeps with a rising pitch,
# groups are separated by a longer pause. Timings chosen so the total
# length is an integer number of samples for a clean loop.
BEEP_LEN = 0.11      # s, single beep
BEEP_GAP = 0.075     # s, silence between beeps inside a group
GROUP_GAP = 0.33     # s, silence between groups
N_GROUPS = 3
N_BEEPS = 4
TAIL = 0.05          # s, trailing silence before the loop restarts

# Base frequency of the first beep in the first group; each next beep and
# each next group rises slightly, giving the familiar "urgent" alarm feel.
BASE_FREQ = 1318.0   # E6
PITCH_STEP = 1.06    # semitone-ish step between consecutive beeps

AMP = 0.82           # peak amplitude, leaves headroom before clipping
HARMONICS = ((1.0, 1.0), (2.0, 0.45), (3.0, 0.18))
ENV_MS = 6.0         # raised-cosine attack/release to avoid clicks


def beep(freq: float, length: float) -> np.ndarray:
    n = int(round(length * SAMPLE_RATE))
    t = np.arange(n) / SAMPLE_RATE
    tone = np.zeros(n, dtype=np.float64)
    for mult, gain in HARMONICS:
        tone += gain * np.sin(2.0 * np.pi * freq * mult * t)
    # Raised-cosine envelope on both ends.
    env_n = int(ENV_MS / 1000.0 * SAMPLE_RATE)
    env = np.ones(n)
    ramp = 0.5 * (1.0 - np.cos(np.pi * np.arange(env_n) / env_n))
    env[:env_n] = ramp
    env[-env_n:] = ramp[::-1]
    return tone * env


def silence(length: float) -> np.ndarray:
    return np.zeros(int(round(length * SAMPLE_RATE)), dtype=np.float64)


def main() -> None:
    parts = []
    freq = BASE_FREQ
    for group in range(N_GROUPS):
        if group > 0:
            parts.append(silence(GROUP_GAP))
        for i in range(N_BEEPS):
            if i > 0:
                parts.append(silence(BEEP_GAP))
            parts.append(beep(freq, BEEP_LEN))
            freq *= PITCH_STEP
    parts.append(silence(TAIL))

    signal = np.concatenate(parts)
    peak = np.max(np.abs(signal))
    signal = signal / peak * AMP
    pcm = (signal * 32767.0).astype(np.int16)

    os.makedirs(os.path.dirname(OUT_PATH), exist_ok=True)
    with wave.open(OUT_PATH, "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(SAMPLE_RATE)
        wav.writeframes(pcm.tobytes())

    duration = len(pcm) / SAMPLE_RATE
    size = os.path.getsize(OUT_PATH)
    print(f"wrote {OUT_PATH}: {duration:.2f}s, {size} bytes")


if __name__ == "__main__":
    main()
