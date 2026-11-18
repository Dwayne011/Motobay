"""OSGB36 British National Grid easting/northing to WGS84 lat/lon (Ordnance Survey's
transverse Mercator inverse plus a 7-parameter Helmert shift; about 5 m accuracy)."""
import math

def bng_to_wgs84(E, N):
    a, b = 6377563.396, 6356256.909          # Airy 1830
    F0 = 0.9996012717
    lat0, lon0 = math.radians(49), math.radians(-2)
    N0, E0 = -100000, 400000
    e2 = 1 - (b * b) / (a * a)
    n = (a - b) / (a + b)
    lat, M = lat0, 0
    while True:
        lat = (N - N0 - M) / (a * F0) + lat
        Ma = (1 + n + 5/4*n**2 + 5/4*n**3) * (lat - lat0)
        Mb = (3*n + 3*n**2 + 21/8*n**3) * math.sin(lat - lat0) * math.cos(lat + lat0)
        Mc = (15/8*n**2 + 15/8*n**3) * math.sin(2*(lat - lat0)) * math.cos(2*(lat + lat0))
        Md = 35/24*n**3 * math.sin(3*(lat - lat0)) * math.cos(3*(lat + lat0))
        M = b * F0 * (Ma - Mb + Mc - Md)
        if abs(N - N0 - M) < 1e-5:
            break
    s = math.sin(lat); c = math.cos(lat); t = math.tan(lat)
    nu = a * F0 / math.sqrt(1 - e2 * s * s)
    rho = a * F0 * (1 - e2) / (1 - e2 * s * s) ** 1.5
    eta2 = nu / rho - 1
    VII = t / (2 * rho * nu)
    VIII = t / (24 * rho * nu**3) * (5 + 3*t*t + eta2 - 9*t*t*eta2)
    IX = t / (720 * rho * nu**5) * (61 + 90*t*t + 45*t**4)
    X = 1 / (c * nu)
    XI = 1 / (c * 6 * nu**3) * (nu / rho + 2*t*t)
    XII = 1 / (c * 120 * nu**5) * (5 + 28*t*t + 24*t**4)
    XIIA = 1 / (c * 5040 * nu**7) * (61 + 662*t*t + 1320*t**4 + 720*t**6)
    dE = E - E0
    lat1 = lat - VII*dE**2 + VIII*dE**4 - IX*dE**6
    lon1 = lon0 + X*dE - XI*dE**3 + XII*dE**5 - XIIA*dE**7
    # OSGB36 geodetic -> cartesian
    s, c = math.sin(lat1), math.cos(lat1)
    nu = a / math.sqrt(1 - e2 * s * s)
    x = nu * c * math.cos(lon1); y = nu * c * math.sin(lon1); z = (1 - e2) * nu * s
    # Helmert OSGB36 -> WGS84
    tx, ty, tz = 446.448, -125.157, 542.060
    sc = 20.4894e-6
    rx, ry, rz = [math.radians(v / 3600) for v in (0.1502, 0.2470, 0.8421)]
    x2 = tx + (1 + sc) * x - rz * y + ry * z
    y2 = ty + rz * x + (1 + sc) * y - rx * z
    z2 = tz - ry * x + rx * y + (1 + sc) * z
    # cartesian -> WGS84 geodetic
    a2, b2 = 6378137.0, 6356752.3142
    e22 = 1 - b2 * b2 / (a2 * a2)
    p = math.hypot(x2, y2)
    lat = math.atan2(z2, p * (1 - e22))
    for _ in range(10):
        nu = a2 / math.sqrt(1 - e22 * math.sin(lat) ** 2)
        lat = math.atan2(z2 + e22 * nu * math.sin(lat), p)
    return math.degrees(lat), math.degrees(math.atan2(y2, x2))

if __name__ == "__main__":
    # Ordnance Survey's worked example: Caister water tower TG 51409 13177 -> 52.65757 N, 1.71792 E (OSGB36).
    print(bng_to_wgs84(651409.903, 313177.270))
