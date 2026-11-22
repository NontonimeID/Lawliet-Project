package com.lawliet.project

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.*

/** Mesin 3D software kecil (tanpa library): kamera orbit, perspektif, kotak berbayang, karakter blok ala Roblox. */
class V3(@JvmField val x: Float, @JvmField val y: Float, @JvmField val z: Float) {
    operator fun plus(o: V3) = V3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: V3) = V3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = V3(x * s, y * s, z * s)
    fun dot(o: V3) = x * o.x + y * o.y + z * o.z
    fun cross(o: V3) = V3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    fun len() = sqrt(x * x + y * y + z * z)
    fun norm(): V3 { val l = len(); return if (l < 1e-6f) this else V3(x / l, y / l, z / l) }
}

class Cam {
    var tx = 0f; var ty = 0f; var tz = 0f
    var yaw = 0f; var pitch = 0.35f; var dist = 14f
    var cx = 0f; var cy = 0f; var focal = 1f
    var pos = V3(0f, 0f, 0f); var fwd = V3(0f, 0f, 1f); var right = V3(1f, 0f, 0f); var up = V3(0f, 1f, 0f)

    fun update(w: Float, h: Float) {
        cx = w / 2f; cy = h / 2f; focal = h * 0.85f
        val sy = sin(yaw); val cyw = cos(yaw); val sp = sin(pitch); val cp = cos(pitch)
        fwd = V3(sy * cp, -sp, cyw * cp)
        right = V3(cyw, 0f, -sy)
        up = V3(sy * sp, cp, cyw * sp)
        pos = V3(tx, ty, tz) - fwd * dist
    }

    fun toCam(p: V3): V3 { val d = p - pos; return V3(d.dot(right), d.dot(up), d.dot(fwd)) }

    /** Proyeksi titik dunia ke layar; null jika di belakang kamera. */
    fun project(p: V3): Offset? {
        val c = toCam(p)
        if (c.z < 0.3f) return null
        return Offset(cx + focal * c.x / c.z, cy - focal * c.y / c.z)
    }
}

class Item(@JvmField val key: Float, @JvmField val draw: DrawScope.() -> Unit)

private val LIGHT = V3(0.45f, 0.8f, 0.35f).norm()
private val FACES = arrayOf(
    intArrayOf(0, 2, 6, 4), intArrayOf(1, 3, 7, 5), intArrayOf(0, 1, 5, 4),
    intArrayOf(2, 3, 7, 6), intArrayOf(0, 1, 3, 2), intArrayOf(4, 5, 7, 6), // index 5 = sisi depan (+Z lokal)
)

fun shade(c: Color, k: Float) =
    Color((c.red * k).coerceIn(0f, 1f), (c.green * k).coerceIn(0f, 1f), (c.blue * k).coerceIn(0f, 1f), 1f)

/** Gambar poligon 3D dengan clipping near-plane. */
fun DrawScope.poly(cam: Cam, pts: Array<V3>, color: Color, seam: Boolean = true) {
    val near = 0.25f
    val cs = Array(pts.size) { cam.toCam(pts[it]) }
    val out = ArrayList<V3>(pts.size + 2)
    for (i in cs.indices) {
        val a = cs[i]; val b = cs[(i + 1) % cs.size]
        val ain = a.z >= near; val bin = b.z >= near
        if (ain) out.add(a)
        if (ain != bin) {
            val t = (near - a.z) / (b.z - a.z)
            out.add(V3(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, near))
        }
    }
    if (out.size < 3) return
    val path = Path()
    for (i in out.indices) {
        val p = out[i]
        val sx = cam.cx + cam.focal * p.x / p.z; val sy = cam.cy - cam.focal * p.y / p.z
        if (i == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
    }
    path.close()
    drawPath(path, color)
    if (seam) drawPath(path, color, style = Stroke(1.2f)) // tutup celah antar sisi
}

/** Kotak dari 8 titik dunia (bit0=x, bit1=y, bit2=z). decals digambar hanya jika sisi depan terlihat. */
fun DrawScope.drawBoxWorld(cam: Cam, v: Array<V3>, color: Color, decals: List<Pair<Array<V3>, Color>>? = null) {
    var sx = 0f; var sy = 0f; var sz = 0f
    for (p in v) { sx += p.x; sy += p.y; sz += p.z }
    val center = V3(sx / 8f, sy / 8f, sz / 8f)
    for (fi in FACES.indices) {
        val f = FACES[fi]
        val a = v[f[0]]; val b = v[f[1]]; val c = v[f[2]]
        var n = (b - a).cross(c - b).norm()
        val fc = (v[f[0]] + v[f[2]]) * 0.5f
        if (n.dot(fc - center) < 0f) n = n * -1f
        if (n.dot(cam.pos - fc) <= 0f) continue
        val k = 0.55f + 0.5f * max(0f, n.dot(LIGHT))
        poly(cam, arrayOf(v[f[0]], v[f[1]], v[f[2]], v[f[3]]), shade(color, k))
        if (fi == 5 && decals != null) for ((d, col) in decals) poly(cam, d, col, seam = false)
    }
}

/** Stud: oval kecil bergaya Roblox (sisi atas terang + bayangan bawah). */
fun DrawScope.drawStud(cam: Cam, p: V3, r: Float, color: Color) {
    val c = cam.toCam(p)
    if (c.z < 0.8f) return
    val sx = cam.cx + cam.focal * c.x / c.z; val sy = cam.cy - cam.focal * c.y / c.z
    val rx = cam.focal * r / c.z
    if (sx < -rx || sx > size.width + rx || sy < -rx || sy > size.height + rx) return
    val ry = rx * max(0.18f, abs(cam.fwd.y))
    drawOval(shade(color, 0.6f), Offset(sx - rx, sy - ry + ry * 0.45f), Size(rx * 2, ry * 2))
    drawOval(shade(color, 1.18f), Offset(sx - rx, sy - ry), Size(rx * 2, ry * 2))
}

// ---------------- Karakter blok ala Roblox ----------------
class Look(val shirt: Color, val pants: Color, val skin: Color = Color(0xFFF5CD30))

fun lookFor(name: String): Look {
    val h = abs(name.hashCode())
    val shirts = listOf(0xFF0D69AC, 0xFFC4281C, 0xFF4B974B, 0xFF6D3EA8, 0xFFDA8541, 0xFF2B2B2B, 0xFF0097A7, 0xFFD64A8B)
    val pants = listOf(0xFF4B974B, 0xFF1B2A34, 0xFF6B5A3C, 0xFF0D69AC, 0xFF3A3A3A)
    return Look(Color(shirts[h % shirts.size]), Color(pants[(h / 7) % pants.size]))
}

private val NAME_COLORS = listOf(
    0xFFFF4D4D, 0xFF4DA6FF, 0xFF5BD75B, 0xFFC77DFF, 0xFFFFA94D, 0xFFFFE14D, 0xFFFF7EB6, 0xFF4DE0D4,
)
/** Warna nama pemain (berwarna-warni seperti chat Roblox). */
fun nameColor(name: String) = Color(NAME_COLORS[abs(name.hashCode()) % NAME_COLORS.size])

/**
 * Tambah karakter ke daftar gambar. Titik kaki di (x, y, z), menghadap heading (radian).
 * swing = ayunan kaki/tangan, air = pose lompat (tangan terangkat).
 */
fun addCharacter(
    items: MutableList<Item>, cam: Cam, x: Float, y: Float, z: Float, heading: Float,
    swing: Float, air: Boolean, look: Look, groundY: Float = 0f,
) {
    val ch = cos(heading); val sh = sin(heading)
    fun world(lx: Float, ly: Float, lz: Float) = V3(x + lx * ch + lz * sh, y + ly, z - lx * sh + lz * ch)

    fun part(
        cx: Float, cy: Float, cz: Float, hx: Float, hy: Float, hz: Float,
        px: Float, py: Float, pz: Float, ang: Float, color: Color,
        decals: List<Triple<FloatArray, Float, Color>>? = null,
    ) {
        val ca = cos(ang); val sa = sin(ang)
        fun xf(lx: Float, ly: Float, lz: Float): V3 {
            val dy = ly - py; val dz = lz - pz
            return world(lx, py + dy * ca - dz * sa, pz + dy * sa + dz * ca)
        }
        val verts = Array(8) {
            xf(cx + if (it and 1 != 0) hx else -hx, cy + if (it and 2 != 0) hy else -hy, cz + if (it and 4 != 0) hz else -hz)
        }
        val dec = decals?.map { (r, zOff, col) ->
            // r = [x0, y0, x1, y1] pada bidang depan
            arrayOf(xf(r[0], r[1], cz + hz + zOff), xf(r[2], r[1], cz + hz + zOff), xf(r[2], r[3], cz + hz + zOff), xf(r[0], r[3], cz + hz + zOff)) to col
        }
        val center = xf(cx, cy, cz)
        val key = (center - cam.pos).len()
        items.add(Item(key) { drawBoxWorld(cam, verts, color, dec) })
    }

    // bayangan bulat di tanah
    val sp = V3(x, groundY + 0.03f, z)
    items.add(Item((sp - cam.pos).len() + 0.6f) {
        val c = cam.toCam(sp)
        if (c.z > 0.8f) {
            val sx = cam.cx + cam.focal * c.x / c.z; val sy = cam.cy - cam.focal * c.y / c.z
            val rx = cam.focal * 1.3f / c.z; val ry = rx * max(0.15f, abs(cam.fwd.y))
            val fade = (1f - ((y - groundY) / 14f)).coerceIn(0.2f, 1f)
            drawOval(Color(0x55000000).copy(alpha = 0.33f * fade), Offset(sx - rx, sy - ry), Size(rx * 2, ry * 2))
        }
    })

    val legA = if (air) 0.25f else swing
    val armA = if (air) -2.8f else swing
    part(-0.5f, 1f, 0f, 0.5f, 1f, 0.5f, -0.5f, 2f, 0f, legA, look.pants)
    part(0.5f, 1f, 0f, 0.5f, 1f, 0.5f, 0.5f, 2f, 0f, if (air) -0.25f else -swing, look.pants)
    part(0f, 3f, 0f, 1f, 1f, 0.5f, 0f, 3f, 0f, 0f, look.shirt)
    part(-1.5f, 3f, 0f, 0.5f, 1f, 0.5f, -1.5f, 4f, 0f, if (air) armA else -swing, look.skin)
    part(1.5f, 3f, 0f, 0.5f, 1f, 0.5f, 1.5f, 4f, 0f, if (air) armA else swing, look.skin)
    val ink = Color(0xFF202020)
    val face = listOf(
        Triple(floatArrayOf(-0.38f, 4.72f, -0.20f, 4.98f), 0.02f, ink), // mata kiri
        Triple(floatArrayOf(0.20f, 4.72f, 0.38f, 4.98f), 0.02f, ink),   // mata kanan
        Triple(floatArrayOf(-0.42f, 4.34f, -0.30f, 4.46f), 0.02f, ink), // ujung senyum
        Triple(floatArrayOf(0.30f, 4.34f, 0.42f, 4.46f), 0.02f, ink),
        Triple(floatArrayOf(-0.30f, 4.22f, 0.30f, 4.34f), 0.02f, ink),  // dasar senyum
    )
    part(0f, 4.6f, 0f, 0.6f, 0.6f, 0.6f, 0f, 4.6f, 0f, 0f, look.skin, face)
}

fun lerpAngle(a: Float, b: Float, t: Float): Float {
    var d = (b - a) % (2f * PI.toFloat())
    if (d > PI) d -= 2f * PI.toFloat()
    if (d < -PI) d += 2f * PI.toFloat()
    return a + d * t.coerceIn(0f, 1f)
}
