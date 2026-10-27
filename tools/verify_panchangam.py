"""
Independent check of the app's panchangam against the Swiss Ephemeris (the library behind most
published panchangams). Usage (from the repo root):

    python3 -m venv /tmp/swe && /tmp/swe/bin/pip install pyswisseph
    ./gradlew :app:testDebugUnitTest --tests '*PanchangamExportTest' -DexportPath=/tmp/engine.csv
    /tmp/swe/bin/python tools/verify_panchangam.py /tmp/engine.csv

Location/zone below must match GeoLocation.NAPERVILLE.
"""
import csv, sys, datetime as dt, zoneinfo, statistics
import swisseph as swe
LAT, LON, TZ = 41.7508, -88.1535, zoneinfo.ZoneInfo("America/Chicago")
swe.set_sid_mode(swe.SIDM_LAHIRI)
F = swe.FLG_MOSEPH
def lon(jd, body, sid=False):
    return swe.calc_ut(jd, body, F | (swe.FLG_SIDEREAL if sid else 0))[0][0]
def elong(jd): return (lon(jd, swe.MOON) - lon(jd, swe.SUN)) % 360
def moonsid(jd): return lon(jd, swe.MOON, True)
def yoga(jd): return (lon(jd, swe.SUN, True) + lon(jd, swe.MOON, True)) % 360
def sunsid(jd): return lon(jd, swe.SUN, True)
def cross(f, jd0, target, maxd=2.5):
    g = lambda t: ((f(t) - target + 180) % 360) - 180
    t, g0 = jd0, g(jd0)
    while t < jd0 + maxd:
        t1 = t + 0.1; g1 = g(t1)
        if g0 < 0 <= g1 and g1 - g0 < 180:
            lo, hi = t, t1
            for _ in range(50):
                m = (lo + hi) / 2
                (lo, hi) = (m, hi) if g(m) < 0 else (lo, m)
            return (lo + hi) / 2
        t, g0 = t1, g1
    return None
def jd_of(d: dt.datetime): u = d.astimezone(dt.timezone.utc); return swe.julday(u.year, u.month, u.day, u.hour + u.minute/60 + u.second/3600)
def local(jd):
    y, m, d, h = swe.revjul(jd); return (dt.datetime(y, m, d, tzinfo=dt.timezone.utc) + dt.timedelta(hours=h)).astimezone(TZ)
def rise_set(date):
    jd0 = jd_of(dt.datetime(date.year, date.month, date.day, tzinfo=TZ))
    r = swe.rise_trans(jd0, swe.SUN, swe.CALC_RISE, (LON, LAT, 200), 0, 0, F)[1][0]
    s = swe.rise_trans(jd0, swe.SUN, swe.CALC_SET, (LON, LAT, 200), 0, 0, F)[1][0]
    return r, s
def parse_t(date, s): return dt.datetime.combine(date, dt.time.fromisoformat(s), TZ)
def parse_dt(s): return None if s == "null" else dt.datetime.fromisoformat(s).replace(tzinfo=TZ)

diffs = {"sunrise": [], "sunset": [], "tithiEnd": [], "nakEnd": [], "yogaEnd": []}
mism = {"tithi": [], "nak": [], "yoga": [], "tamil": []}
rows = list(csv.DictReader(open(sys.argv[1])))
for r in rows:
    date = dt.date.fromisoformat(r["date"])
    sr, ss = rise_set(date)
    diffs["sunrise"].append((parse_t(date, r["sunrise"]) - local(sr)).total_seconds() / 60)
    diffs["sunset"].append((parse_t(date, r["sunset"]) - local(ss)).total_seconds() / 60)
    for key, f, span, n, col, endcol in [("tithi", elong, 12.0, 30, "tithi", "tithiEnd"), ("nak", moonsid, 360/27, 27, "nak", "nakEnd"), ("yoga", yoga, 360/27, 27, "yoga", "yogaEnd")]:
        idx = int(f(sr) // span) % n
        if idx != int(r[col]): mism[key].append((r["date"], int(r[col]), idx))
        end = cross(f, sr, ((idx + 1) % n) * span)
        mine = parse_dt(r[endcol])
        if end and mine and idx == int(r[col]): diffs[endcol].append((mine - local(end)).total_seconds() / 60)
    # Tamil month: Sun's sidereal rasi at local sunset decides (sankranti before sunset rule).
    if int(sunsid(ss) // 30) != int(r["tmonth"]): mism["tamil"].append((r["date"], r["tmonth"], int(sunsid(ss) // 30)))

print(f"Days compared: {len(rows)}  (Naperville, {rows[0]['date']} .. {rows[-1]['date']})")
for k, v in diffs.items():
    a = [abs(x) for x in v]
    print(f"{k:9s} n={len(v):3d}  mean|Δ|={statistics.mean(a):5.2f} min  max|Δ|={max(a):5.2f} min  bias={statistics.mean(v):+5.2f} min")
for k, v in mism.items(): print(f"{k} mismatches: {len(v)} {v[:6]}")
