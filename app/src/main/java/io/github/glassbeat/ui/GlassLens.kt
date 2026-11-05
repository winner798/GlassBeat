package io.github.glassbeat.ui

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.min

/** Distort the sampled backdrop only; foreground labels and touch targets remain sharp. */
@Composable
internal fun Modifier.glassLens(enabled: Boolean): Modifier =
    if (enabled && Build.VERSION.SDK_INT >= 33) then(api33Lens()) else this

@RequiresApi(33)
@Composable
private fun api33Lens(): Modifier {
    val shader = remember { RuntimeShader(GLASS_LENS) }
    return Modifier.graphicsLayer {
        if (size.width > 0 && size.height > 0) {
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("radius", min(28.dp.toPx(), min(size.width, size.height) * .5f))
            shader.setFloatUniform("bevel", min(14.dp.toPx(), min(size.width, size.height) * .22f))
            shader.setFloatUniform("bend", 10.dp.toPx())
            renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "backdrop").asComposeRenderEffect()
        }
    }
}

// Rounded-box distance field; independently implemented for FavSoda. See docs/GLASS.md.
private const val GLASS_LENS = """
uniform shader backdrop;
uniform float2 resolution;
uniform float radius;
uniform float bevel;
uniform float bend;
float distanceToEdge(float2 p) {
    float2 q = abs(p - resolution * 0.5) - (resolution * 0.5 - radius);
    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - radius;
}
half4 main(float2 p) {
    float d = distanceToEdge(p);
    float2 gradient = float2(distanceToEdge(p + float2(0.75, 0.0)) - distanceToEdge(p - float2(0.75, 0.0)),
                             distanceToEdge(p + float2(0.0, 0.75)) - distanceToEdge(p - float2(0.0, 0.75)));
    float2 normal = gradient / max(length(gradient), 0.001);
    float rim = 1.0 - smoothstep(0.0, bevel, max(-d, 0.0));
    float2 samplePosition = clamp(p - normal * bend * rim * rim, float2(0.5), resolution - 0.5);
    half4 glass = backdrop.eval(samplePosition);
    float light = dot(normal, normalize(float2(-0.55, -0.85)));
    float shine = max(light, 0.0) * 0.18 * rim;
    float shade = max(-light, 0.0) * 0.14 * rim;
    glass.rgb = clamp(glass.rgb * (1.0 - shade) + half3(shine) * glass.a, half3(0.0), half3(glass.a));
    return glass;
}
"""
